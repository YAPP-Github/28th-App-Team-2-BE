# FCM Sending Reference

Detailed reference for the `fcm` skill — port shapes, `Message` building, single/batch/multicast/topic sends, error-code mapping and token cleanup. The classification *policy* is in the skill body; this file is the mechanics.

Working implementation: `module-notification/notification-adapter-out/.../adapter/fcm/FcmPushNotificationAdapter.kt`.

---

## Port and value types (domain, no Firebase import)

```kotlin
// {domain}-domain
data class PushNotification(
    val token: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)

data class PushResult(
    val token: String,
    val success: Boolean,
    val tokenExpired: Boolean = false,   // UNREGISTERED only → the application layer deletes the token
    val errorCode: String? = null,       // diagnosis only (logs/metrics); never persisted
)

interface PushNotificationPort {
    fun send(notification: PushNotification): PushResult

    fun sendAll(notifications: List<PushNotification>): List<PushResult>
}
```

`errorCode` exists so that a batch failure is still diagnosable after all retries are exhausted. Without it, a `success = false` row carries no cause and the incident becomes unreconstructable.

## Building the `Message`

```kotlin
// inside the adapter only
@Suppress("DEPRECATION")
private fun PushNotification.toMessage(): Message =
    Message
        .builder()
        .setToken(token)
        .setNotification(FcmNotification.builder().setTitle(title).setBody(body).build())
        .putAllData(data)
        .build()
```

- Import `com.google.firebase.messaging.Notification as FcmNotification` — the unaliased name collides with the domain's own `Notification` entity.
- **`setToken()` is deprecated but still correct here.** FCM is migrating to Firebase Installation IDs (`setFid()`), but an FID is a *different identifier* than a registration token, so it can't be swapped in until the client issues and sends FIDs. Both paths are supported during the transition, so the deprecation is suppressed with that reasoning recorded at the call site; migrating is a separate client-coordinated task.

## Single send

```kotlin
override fun send(notification: PushNotification): PushResult =
    try {
        firebaseMessaging.send(notification.toMessage())
        PushResult(token = notification.token, success = true)
    } catch (e: FirebaseMessagingException) {
        if (e.isTokenExpired()) {
            PushResult(token = notification.token, success = false, tokenExpired = true)
        } else {
            throw PushSendFailedException(e)
        }
    }

private fun FirebaseMessagingException.isTokenExpired(): Boolean =
    messagingErrorCode == MessagingErrorCode.UNREGISTERED
```

`PushSendFailedException` is an `AppException` subclass in `{domain}-domain/.../exception/`. Never throw a bare `RuntimeException` (`error-handling` skill).

## Batch send — and why it does not throw

```kotlin
override fun sendAll(notifications: List<PushNotification>): List<PushResult> {
    if (notifications.isEmpty()) return emptyList()
    val batch = firebaseMessaging.sendEach(notifications.map { it.toMessage() })
    return notifications.zip(batch.responses, ::toPushResult).also(::logFailures)
}

private fun toPushResult(notification: PushNotification, response: SendResponse): PushResult =
    when {
        response.isSuccessful -> PushResult(token = notification.token, success = true)
        response.exception?.isTokenExpired() == true ->
            PushResult(token = notification.token, success = false, tokenExpired = true)
        else ->
            PushResult(
                token = notification.token,
                success = false,
                // messagingErrorCode is only populated for structured FCM errors. A 5xx/timeout is a plain
                // HTTP-level error and leaves it null, so fall back to errorCode or the cause vanishes from logs.
                errorCode = response.exception?.messagingErrorCode?.name ?: response.exception?.errorCode?.name,
            )
    }
```

**The asymmetry with `send` is deliberate.** A batch covers many members; promoting one recipient's failure to an exception would roll back the follow-up work (delivery history, token cleanup) for every member who *did* receive it. So a batch reports per-recipient outcomes and additionally `warn`-logs the non-expiry failures.

Use `sendEach(List<Message>)` for per-recipient bodies. Never loop `send()` per token — that is one HTTP round trip each.

## Multicast and topics

| Case | API |
|------|-----|
| Same body, many tokens | `MulticastMessage` + `firebaseMessaging.sendEachForMulticast(...)`, then iterate `BatchResponse.responses` for a per-token `PushResult` |
| Broadcast to subscribers | `Message.builder().setTopic("notice")`; manage membership with `subscribeToTopic(tokens, topic)` / `unsubscribeFromTopic(...)` |

Topic sends have **no per-token result**, so they cannot drive token cleanup — don't use a topic where you need expiry detection.

## Platform differences

`setApnsConfig(...)` (iOS: badge, sound, `content-available`) and `setAndroidConfig(...)` (Android: priority, notification channel, collapse key) both stay **entirely inside the adapter**. The domain's `PushNotification` must not grow platform fields; if one platform needs a distinct payload, branch in the adapter on data you already have.

## Token cleanup flow

Cleanup is an **application-layer** policy, driven by the facts the adapter reports:

1. The use case calls `sendAll(...)`.
2. For each result with `tokenExpired = true`, it deletes that token through the token-storage port.
3. For `success = false` without expiry, it records a delivery failure for the retry path.

The token repository offers two deletes with **different scopes, for different callers** — don't collapse them:

| Method | Caller | Why the scope |
|--------|--------|---------------|
| `deleteByToken(token)` | Expiry cleanup after a send | A registration token is globally unique, so the token alone identifies the row |
| `deleteByMemberIdAndToken(memberId, token)` | User action (logout / unregister) | Scoped to the owner so a caller cannot delete someone else's token |

## Retry touchpoint

Transiently-failed tokens are persisted as delivery failures and retried against the **intersection with the member's current tokens**, so logged-out tokens drop out and already-delivered ones are never resent. Attempts and backoff live in the retry policy in `{domain}-domain/.../policy/`.

**Known gap**: retry eligibility is not yet filtered by permanence, so a permanent non-`UNREGISTERED` error (e.g. `INVALID_ARGUMENT` from a malformed payload) still enters the retry queue. Modeling expiry and retry-worthiness as separate axes is tracked in **issue #109** — read it before changing the classification.
