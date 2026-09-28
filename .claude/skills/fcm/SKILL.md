---
name: fcm
description: Load when integrating push notifications via FCM (Firebase Cloud Messaging). Hexagonal placement (*PushNotificationPort/adapter), Firebase Admin SDK init (ADC), device-token storage & cleanup, single/multicast/topic send, config & env vars, testing, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# FCM (Push Notification) Rules

Send push notifications to AOS/iOS clients via **FCM (Firebase Cloud Messaging)** using the **Firebase Admin SDK**. FCM is an **external system**, so it's isolated as an outbound adapter, exactly like the GCS / Spring AI integrations.

| Item | Decision |
|------|----------|
| SDK | Firebase Admin SDK (`com.google.firebase:firebase-admin`, version managed in `libs.versions.toml`) |
| Auth | **ADC** (Application Default Credentials) — same as GCS. The server authenticates automatically via ADC (GCP SA key / Workload Identity); locally use `gcloud auth application-default login`. |
| Firebase project | Firebase project = GCP project → reuses `GCP_PROJECT_ID` |
| Enable/disable | `fcm.enabled` flag. When `false`, the Firebase bean isn't created (no-op), so a local run needs no Firebase setup (same pattern as the Sentry DSN no-op when empty). |
| Client-side token | The client (AOS/iOS) obtains an FCM registration token and registers it with the server; the server stores it and uses it as the send target. |

---

## 1. Hexagonal Placement (most important)

FCM is an external system, so it's treated as an **outbound adapter**. The domain/use-case layer **never imports Firebase types** (`FirebaseMessaging`, `Message`, `FirebaseApp`, etc.).

```
{domain}-domain        →  PushNotificationPort interface + PushNotification/PushResult domain types (pure Kotlin)
{domain}-application    →  use case calls PushNotificationPort (token lookup/cleanup via DeviceTokenPort)
{domain}-adapter-out    →  Firebase adapter (implements the port, uses FirebaseMessaging)
```

| Type | Naming | Location (package) |
|------|--------|--------------------|
| Outbound port (send) | `PushNotificationPort` | `com.yapp.todakun.{domain}.port.outbound` (domain) |
| Outbound port (token storage) | `DeviceTokenPort` | `com.yapp.todakun.{domain}.port.outbound` (domain) |
| Send domain types | `PushNotification`, `PushResult` | `com.yapp.todakun.{domain}` (domain) |
| Firebase adapter | `FcmPushNotificationAdapter` | `com.yapp.todakun.{domain}.adapter.fcm` |
| Token JPA adapter | `DeviceTokenAdapter` (+ Java `*JpaEntity`) | `com.yapp.todakun.{domain}.adapter.persistence` |

- Use the tech package `adapter.fcm` (per the `architecture` skill's `.adapter.{tech}` rule).
- The port only handles domain types. Building the `Message`, calling `FirebaseMessaging.send(...)`, and checking error codes all end **inside** the adapter.
- Which domain owns this? Push sending + device tokens usually belong to a `notification` (or `user`) bounded context. This doc is domain-agnostic, so substitute `{domain}` with the owning module. Use `/new-domain` to scaffold a new one.

```kotlin
// {domain}-domain : pure port + types (no Firebase import)
data class PushNotification(
    val token: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)

data class PushResult(
    val token: String,
    val success: Boolean,
    val tokenExpired: Boolean = false,   // UNREGISTERED/INVALID → the application layer deletes the token
)

interface PushNotificationPort {
    fun send(notification: PushNotification): PushResult

    fun sendAll(notifications: List<PushNotification>): List<PushResult>
}
```

---

## 2. Firebase Initialization (ADC)

Initialize `FirebaseApp` once and expose `FirebaseMessaging` as a bean from the **adapter-out** module. Gate it with `fcm.enabled` so it's a no-op when disabled.

```kotlin
// {domain}-adapter-out : .adapter.fcm.config
@Configuration
@EnableConfigurationProperties(FcmProperties::class)
@ConditionalOnProperty(prefix = "fcm", name = ["enabled"], havingValue = "true")
class FcmConfig(
    private val fcmProperties: FcmProperties,
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }   // idempotent (avoids re-init on refresh)
        val options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())   // ADC — no key file
                .setProjectId(fcmProperties.projectId)
                .build()
        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)
}
```

```kotlin
// {domain}-adapter-out : .adapter.fcm.properties
@ConfigurationProperties(prefix = "fcm")
data class FcmProperties(
    val enabled: Boolean = false,
    val projectId: String,
)
```

- Injecting `FirebaseMessaging` where `fcm.enabled=false` fails (no bean). If a use case must work with FCM off, gate the calling adapter with the same property, or make the port injection optional — decide per domain.

---

## 3. Device Token Storage & Cleanup

FCM registration tokens are issued by the client and **expire/rotate**. Store them (JPA), and **delete expired tokens** when a send result reports the token is gone.

- Storage is an ordinary JPA outbound adapter: `*JpaEntity` in **Java**, domain entity in **Kotlin**, PK via `Uuid.generateV7().toJavaUuid()` (never `randomUUID`). See the `architecture` skill.
- When `PushResult.tokenExpired == true` on send, the **application** layer removes the token via `DeviceTokenPort` (this policy doesn't live in the Firebase adapter — the adapter only reports facts).

```kotlin
// {domain}-domain : token storage port
interface DeviceTokenPort {
    fun findTokens(userId: UserId): List<String>

    fun save(userId: UserId, token: String)

    fun delete(token: String)
}
```

---

## 4. Sending & Error Handling

Building the `Message` and calling `FirebaseMessaging` happen **inside the adapter**. Firebase errors map either to a `PushResult` (expired token) or to an `AppException` subclass (a real failure). **Never throw `RuntimeException` directly** (`error-handling` skill).

```kotlin
// {domain}-adapter-out : .adapter.fcm
@Component
class FcmPushNotificationAdapter(
    private val firebaseMessaging: FirebaseMessaging,
) : PushNotificationPort {
    override fun send(notification: PushNotification): PushResult =
        try {
            firebaseMessaging.send(notification.toMessage())
            PushResult(token = notification.token, success = true)
        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode) {
                // unregistered/invalid token → not a failure, report as "needs cleanup" (application deletes it)
                MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT ->
                    PushResult(token = notification.token, success = false, tokenExpired = true)
                // everything else (quota/server errors, etc.) is promoted to a failure
                else -> throw NotificationException(NotificationErrorCode.PUSH_SEND_FAILED, e)
            }
        }

    // Bulk send: instead of calling send() once per token serially, use a single batch call (sendEach).
    // (Different bodies per recipient → sendEach(List<Message>). Same body, many tokens → sendEachForMulticast.)
    override fun sendAll(notifications: List<PushNotification>): List<PushResult> {
        if (notifications.isEmpty()) return emptyList()
        val batch = firebaseMessaging.sendEach(notifications.map { it.toMessage() })
        return notifications.mapIndexed { i, notification ->
            val response = batch.responses[i]
            when {
                response.isSuccessful -> PushResult(token = notification.token, success = true)
                // unregistered/invalid token → not a failure, report as "needs cleanup" (application deletes it)
                response.exception?.messagingErrorCode in
                    setOf(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT) ->
                    PushResult(token = notification.token, success = false, tokenExpired = true)
                // everything else (quota/server errors, etc.) is promoted to a failure
                else -> throw NotificationException(NotificationErrorCode.PUSH_SEND_FAILED, response.exception)
            }
        }
    }

    private fun PushNotification.toMessage(): Message =
        Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build()
}
```

- Define error codes in the **domain** (`{domain}-domain`). E.g. `NotificationErrorCode` implementing `ResponseCode`, and `NotificationException : AppException` (`error-handling` skill).
- **Multicast**: when there are many tokens, use `MulticastMessage` + `firebaseMessaging.sendEachForMulticast(...)`, and iterate `BatchResponse.responses` to collect a per-token `PushResult` (mark `UNREGISTERED`/`INVALID_ARGUMENT` as `tokenExpired`).
- **Topics**: for broadcasts, use `Message.builder().setTopic("notice")`, and subscribe/unsubscribe with `firebaseMessaging.subscribeToTopic(tokens, topic)`.
- **iOS vs AOS**: handle platform-specific behavior via `Message`'s `setApnsConfig(...)` (badge/sound) and `setAndroidConfig(...)` (priority/channel). This stays entirely inside the adapter.

---

## 5. Configuration & Environment Variables

Reuse `GCP_PROJECT_ID` (Firebase project = GCP project). Add only one new flag. Never commit secrets; ADC needs none (`code-style` skill).

```yaml
# application-{profile}.yaml
fcm:
  enabled: ${FCM_ENABLED:false}     # no Firebase bean when disabled (no-op)
  project-id: ${GCP_PROJECT_ID}     # Firebase project = GCP project
```

| Env var | Purpose |
|----------------------|---------|
| `FCM_ENABLED` | Only initializes Firebase when `true`. Recommend `false` locally if push isn't needed. |
| `GCP_PROJECT_ID` | GCP/Firebase project ID (shared with the existing GCS config) |

- Auth uses **ADC** — server environments authenticate automatically via ADC (GCP SA key / Workload Identity). Locally, run `gcloud auth application-default login`. The service account needs the **Firebase Cloud Messaging API** enabled and a custom role containing only `cloudmessaging.messages.create`. Use `roles/firebase.admin` only when broader Firebase permissions are required.
- Gradle: add the dependency only to `{domain}-adapter-out`'s `build.gradle.kts` (`implementation(libs.firebase.admin)`). Never add Firebase to the domain/application modules.

---

## 6. Testing

| Layer | Target | Approach |
|-------|--------|------|
| `*-application` | The use case sends via `PushNotificationPort` and deletes expired tokens via `DeviceTokenPort` | Mock both with `mockk<...>()` (no real FCM) |
| `*-adapter-out` (fcm) | `Message` construction + error→`PushResult` mapping | Stub `FirebaseMessaging` with MockK; simulate a `FirebaseMessagingException` with `UNREGISTERED` |
| `*-adapter-out` (persistence) | Device token CRUD | TestContainer (shared `pgvector/pgvector:pg17`) (`testing` skill) |

- **Never send a real push in a test** (network/cost/non-determinism). Stub `FirebaseMessaging.send(...)`.
- Every spec uses `DescribeSpec`, mocking uses MockK, assertions use Kotest matchers (`testing` skill).

```kotlin
class FcmPushNotificationAdapterTest : DescribeSpec({
    val firebaseMessaging = mockk<FirebaseMessaging>()
    val adapter = FcmPushNotificationAdapter(firebaseMessaging)

    afterTest { clearMocks(firebaseMessaging) }

    describe("send") {
        context("when the token is unregistered") {
            it("reports the token as expired instead of failing") {
                val ex = mockk<FirebaseMessagingException>()
                every { ex.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
                every { firebaseMessaging.send(any()) } throws ex

                val result = adapter.send(PushNotification("stale", "t", "b"))

                result.tokenExpired shouldBe true
                result.success shouldBe false
            }
        }
    }
})
```

---

## 7. Architecture Verification (Konsist)

Compatible with the `konsist` skill's rules. Additionally guarantees:

- `com.google.firebase..` imports are only allowed in `.adapter` packages (forbidden in domain/application).
- `PushNotificationPort` / `DeviceTokenPort` interfaces only exist in the `*-domain` package (`..{domain}.port.outbound`).
- `Fcm*Adapter` only exists in the `.adapter.fcm` package.

Add new rules as a `@Test` in `architecture-test/ArchitectureTest.kt` (see the `konsist` skill for how to add a rule).
