---
name: fcm
description: Load when integrating or changing FCM push notifications (Firebase Cloud Messaging). Hexagonal placement of the send port vs the Firebase adapter, failure/token-expiry classification, and where the real implementation lives. Setup, sending and testing details live in references/.
paths:
  - module-notification/**
  - "**/adapter/fcm/**"
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# FCM (Push Notification) Rules

Push to AOS/iOS clients via **FCM** using the **Firebase Admin SDK**. FCM is an external system, so it is isolated as an outbound adapter — exactly like the GCS / Spring AI integrations.

| Item | Decision |
|------|----------|
| SDK | Firebase Admin SDK (`libs.firebase.admin`, version in `libs.versions.toml`) |
| Auth | **ADC** — no key file. Server: GCP SA key / Workload Identity. Local: `gcloud auth application-default login` |
| Firebase project | Firebase project = GCP project → reuses `GCP_PROJECT_ID` |
| Enable/disable | `fcm.enabled`. When false/absent, `NoOpPushNotificationAdapter` takes over, so the app boots with no Firebase credentials |
| Client-side token | The client obtains the FCM registration token and registers it; the server stores it and sends to it |

**`notification` owns this.** It is the reference implementation — read it before writing a new one (see the pointers below). This skill states the rules; it does not restate the code.

---

## 1. Hexagonal Placement (most important)

The domain and application layers **never import Firebase types** (`FirebaseMessaging`, `Message`, `FirebaseApp`, …). Building the `Message`, calling `FirebaseMessaging`, and interpreting error codes all end **inside the adapter**.

```
{domain}-domain       →  PushNotificationPort + PushNotification/PushResult (pure Kotlin)
{domain}-application  →  use case calls the port; deletes expired tokens via the token repository
{domain}-adapter-out  →  Firebase adapter (+ a no-op twin), .adapter.fcm
```

| Type | Naming | Location (package) |
|------|--------|--------------------|
| Outbound port (send) | `PushNotificationPort` | `..{domain}.port.outbound` (domain) |
| Outbound port (token storage) | `*Repository` — in `notification` it is `DeviceTokenRepository` | `..{domain}.port.outbound` (domain) |
| Send value types | `PushNotification`, `PushResult` | `..{domain}` (domain) |
| Firebase adapter | `FcmPushNotificationAdapter` | `..{domain}.adapter.fcm` |
| No-op adapter | `NoOpPushNotificationAdapter` | `..{domain}.adapter.fcm` |
| Token JPA adapter | `*RepositoryAdapter` (+ Java `*JpaEntity`) | `..{domain}.adapter.persistence` |

- Use the tech package `adapter.fcm` (the `architecture` skill's `.adapter.{tech}` rule).
- **Exactly one `PushNotificationPort` bean must always exist.** The real adapter is `@ConditionalOnProperty(havingValue = "true")`, the no-op one `havingValue = "false", matchIfMissing = true`. Injecting the port must never fail just because FCM is off.
- The token-storage port is named `*Repository`, not `*Port` — it is ordinary persistence. Konsist enforces `*Port`/`*Repository` placement either way.

**Real implementation** (`module-notification`):
- ports — `notification-domain/.../port/outbound/{PushNotificationPort,DeviceTokenRepository}.kt`
- adapters — `notification-adapter-out/.../adapter/fcm/{FcmPushNotificationAdapter,NoOpPushNotificationAdapter}.kt`
- value types — `notification-domain/.../{PushNotification,PushResult}.kt`

## 2. Failure Classification (the judgment this skill exists for)

Two axes that are easy to conflate: **is the token dead** vs **is the send worth retrying**.

- **Only `UNREGISTERED` means the token is dead** (`tokenExpired = true`) → the **application** layer deletes it. The adapter reports facts; it never applies the cleanup policy.
- **`INVALID_ARGUMENT` must NOT be treated as expiry.** It also fires on payload errors, so trusting it deletes healthy tokens. This is a deliberate decision — see the KDoc on `FcmPushNotificationAdapter`.
- **Single vs batch differ on purpose.** `send` promotes any other error to `PushSendFailedException`. `sendAll` returns `success = false` with `errorCode` instead of throwing, so one bad recipient cannot roll back the successful rows' follow-up work (history, token cleanup).
- Retry eligibility is a **separate** axis from expiry, and the current retry set is not yet filtered by permanence — tracked in issue #109. Don't "fix" the classification here without reading it.

## 3. References

| Open when | File |
|-----------|------|
| Wiring Firebase up: ADC init, `fcm.*` properties, per-profile YAML, env vars, IAM role, Gradle placement | `references/setup.md` |
| Writing or changing send logic: `Message` building, batch/multicast/topic, error-code mapping, token cleanup flow | `references/sending.md` |
| Writing tests, or adding a Konsist rule for FCM | `references/testing.md` |
