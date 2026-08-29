---
name: fcm
description: Load when integrating push notifications via FCM (Firebase Cloud Messaging). Hexagonal placement (*PushNotificationPort/adapter), Firebase Admin SDK init (ADC), device-token storage & cleanup, single/multicast/topic send, config & env vars, testing, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# FCM (Push Notification) Rules

Use **Firebase Admin SDK** to send push notifications through **FCM (Firebase Cloud Messaging)** to AOS/iOS clients. FCM is an **external system**, so it is isolated as an **outbound adapter** — exactly like the GCS / Spring AI integrations.

| Item | Decision |
|------|----------|
| SDK | Firebase Admin SDK (`com.google.firebase:firebase-admin`, version-managed in `libs.versions.toml`) |
| Auth | **ADC** (Application Default Credentials) — same as GCS. VM auto-authenticates via the instance service account; locally use `gcloud auth application-default login`. No key file / secret needed. |
| Firebase project | The Firebase project **is** the GCP project → reuse `GCP_PROJECT_ID` |
| Enable/disable | `fcm.enabled` flag. When `false` the Firebase beans are not created (no-op), so local runs need no Firebase setup (same spirit as Sentry DSN-empty → no-op). |
| Client SDK token | The client (AOS/iOS) obtains the FCM registration token and registers it with the server; the server stores it and targets it when sending. |

---

## 1. Hexagonal Placement (most important)

FCM is an external system, so treat it as an **outbound adapter**. The domain / use cases **never import** Firebase types (`FirebaseMessaging`, `Message`, `FirebaseApp`, etc.).

```
{domain}-domain        →  PushNotificationPort interface + PushNotification/PushResult domain types (pure Kotlin)
{domain}-application    →  use case calls PushNotificationPort (and DeviceTokenPort for token lookup/cleanup)
{domain}-adapter-out    →  Firebase adapter (implements the port, uses FirebaseMessaging)
```

| Type | Naming | Location (package) |
|------|--------|--------------------|
| Outbound port (send) | `PushNotificationPort` | `com.yapp.todakun.{domain}.port` (domain) |
| Outbound port (token store) | `DeviceTokenPort` | `com.yapp.todakun.{domain}.port` (domain) |
| Send domain types | `PushNotification`, `PushResult` | `com.yapp.todakun.{domain}` (domain) |
| Firebase adapter | `FcmPushNotificationAdapter` | `com.yapp.todakun.{domain}.adapter.fcm` |
| Token JPA adapter | `DeviceTokenAdapter` (+ `*JpaEntity` in Java) | `com.yapp.todakun.{domain}.adapter.persistence` |

- Use a technology package `adapter.fcm` (per the `architecture` skill's `.adapter.{tech}` rule).
- The port deals only in domain types. `Message` building, `FirebaseMessaging.send(...)`, and error-code inspection all finish **inside** the adapter.
- Which domain owns this? Push sending + device tokens usually belong to a `notification` (or `user`) bounded context. This doc is domain-agnostic — substitute `{domain}` for the owning module. To scaffold a new one, use `/new-domain`.

```kotlin
// {domain}-domain : pure port + types (no Firebase imports)
data class PushNotification(
    val token: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)

data class PushResult(
    val token: String,
    val success: Boolean,
    val tokenExpired: Boolean = false,   // UNREGISTERED/INVALID → application layer deletes the token
)

interface PushNotificationPort {
    fun send(notification: PushNotification): PushResult

    fun sendAll(notifications: List<PushNotification>): List<PushResult>
}
```

---

## 2. Firebase Init (ADC)

Initialize `FirebaseApp` once and expose `FirebaseMessaging` as a bean, in the **adapter-out** module. Gate it on `fcm.enabled` so it is a no-op when disabled.

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

- Injecting `FirebaseMessaging` where `fcm.enabled=false` fails (no bean). If a use case must run with FCM off, gate the calling adapter on the same property or make the port injection optional — decide per domain.

---

## 3. Device Token Storage & Cleanup

The FCM registration token is client-issued and **expires / rotates**. Store it (JPA), and **delete stale tokens** when a send reports the token is gone.

- Storage is an ordinary JPA outbound adapter: `*JpaEntity` in **Java**, domain entity in **Kotlin**, PK via `Uuid.generateV7().toJavaUuid()` (never `randomUUID`). See the `architecture` skill.
- On send, if `PushResult.tokenExpired == true`, the **application** layer removes the token via `DeviceTokenPort` (keep this policy out of the Firebase adapter — the adapter only reports the fact).

```kotlin
// {domain}-domain : token store port
interface DeviceTokenPort {
    fun findTokens(userId: UserId): List<String>

    fun save(userId: UserId, token: String)

    fun delete(token: String)
}
```

---

## 4. Sending & Error Handling

Build the `Message` and call `FirebaseMessaging` **inside the adapter**. Map Firebase errors to `PushResult` (for stale tokens) or to an `AppException` subclass (for real failures). **Never throw `RuntimeException` directly** (`error-handling` skill).

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
                // 등록 해제/무효 토큰 → 성공 실패가 아니라 "정리 대상"으로 보고 (application이 삭제)
                MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT ->
                    PushResult(token = notification.token, success = false, tokenExpired = true)
                // 그 외(쿼터/서버 오류 등)는 실패로 승격
                else -> throw NotificationException(NotificationErrorCode.PUSH_SEND_FAILED, e)
            }
        }

    // 대량 발송: 토큰 수만큼 send()를 직렬 호출하지 않고 단일 배치 호출(sendEach)로 처리한다.
    // (본문이 서로 다른 이기종 알림 → sendEach(List<Message>). 동일 본문·다수 토큰이면 sendEachForMulticast.)
    override fun sendAll(notifications: List<PushNotification>): List<PushResult> {
        if (notifications.isEmpty()) return emptyList()
        val batch = firebaseMessaging.sendEach(notifications.map { it.toMessage() })
        return notifications.mapIndexed { i, notification ->
            val response = batch.responses[i]
            when {
                response.isSuccessful -> PushResult(token = notification.token, success = true)
                // 등록 해제/무효 토큰 → 실패가 아니라 "정리 대상"으로 보고 (application이 삭제)
                response.exception?.messagingErrorCode in
                    setOf(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT) ->
                    PushResult(token = notification.token, success = false, tokenExpired = true)
                // 그 외(쿼터/서버 오류 등)는 실패로 승격
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

- Define error codes in the **domain** (`{domain}-domain`), e.g. `NotificationErrorCode` implementing `ResponseCode`, and a `NotificationException : AppException` (`error-handling` skill).
- **Multicast**: for many tokens use `MulticastMessage` + `firebaseMessaging.sendEachForMulticast(...)`, then walk `BatchResponse.responses` to collect per-token `PushResult` (mark `UNREGISTERED`/`INVALID_ARGUMENT` as `tokenExpired`).
- **Topics**: `Message.builder().setTopic("notice")` for broadcast; subscribe/unsubscribe via `firebaseMessaging.subscribeToTopic(tokens, topic)`.
- **iOS vs AOS**: for platform-specific behavior use `setApnsConfig(...)` (badge/sound) and `setAndroidConfig(...)` (priority/channel) on the `Message`. This stays entirely inside the adapter.

---

## 5. Config & Environment Variables

Reuse `GCP_PROJECT_ID` (Firebase project = GCP project). Add one flag. Never commit secrets; ADC needs none (`code-style` skill).

```yaml
# application-{profile}.yaml
fcm:
  enabled: ${FCM_ENABLED:false}     # 비활성 시 Firebase 빈 미생성(no-op)
  project-id: ${GCP_PROJECT_ID}     # Firebase 프로젝트 = GCP 프로젝트
```

| Environment variable | Purpose |
|----------------------|---------|
| `FCM_ENABLED` | `true`로 켤 때만 Firebase 초기화. 로컬은 `false` 권장(푸시 불필요 시). |
| `GCP_PROJECT_ID` | GCP/Firebase 프로젝트 ID (기존 GCS 설정과 공유) |

- Auth uses **ADC** — no `GOOGLE_APPLICATION_CREDENTIALS` needed on the VM (instance service account). Locally, run `gcloud auth application-default login`. The service account needs the **Firebase Cloud Messaging API** enabled and a role that grants `cloudmessaging.messages.create` (e.g. `roles/firebase.admin` or a custom role).
- Gradle: add the dependency to `{domain}-adapter-out`'s `build.gradle.kts` only (`implementation(libs.firebase.admin)`). Never add Firebase to domain/application modules.

---

## 6. Testing

| Layer | Target | Method |
|-------|--------|--------|
| `*-application` | Whether the use case sends via `PushNotificationPort` and deletes expired tokens via `DeviceTokenPort` | Mock both with `mockk<...>()` (no real FCM) |
| `*-adapter-out` (fcm) | `Message` construction + error→`PushResult` mapping | Stub `FirebaseMessaging` with MockK; simulate `FirebaseMessagingException` w/ `UNREGISTERED` |
| `*-adapter-out` (persistence) | Device-token CRUD | TestContainer (shared `pgvector/pgvector:pg17`) (`testing` skill) |

- **Never send a real push in tests** (network/cost/non-determinism). Stub `FirebaseMessaging.send(...)`.
- All Specs are `DescribeSpec`, mocking is MockK, assertions are Kotest matchers (`testing` skill).

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

Compatible with the `konsist` skill rules. Additionally guarantee:

- `com.google.firebase..` imports are allowed **only** in the `.adapter` package (forbidden in domain/application).
- `PushNotificationPort` / `DeviceTokenPort` interfaces live only in the `*-domain` package (`..{domain}.port`).
- `Fcm*Adapter` lives only in the `.adapter.fcm` package.

Add new rules to `architecture-test/ArchitectureTest.kt` as `@Test` (see the `konsist` skill for how to add rules).
