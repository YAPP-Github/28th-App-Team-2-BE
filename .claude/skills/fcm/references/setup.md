# FCM Setup Reference

Detailed reference for the `fcm` skill — Firebase initialization, properties, per-profile config, IAM, and Gradle placement. The placement rules and failure-classification policy live in the skill body.

Working implementation: `module-notification/notification-adapter-out/.../adapter/fcm/config/FcmConfig.kt` and `.../adapter/fcm/properties/FcmProperties.kt`.

---

## Initialization (ADC)

Initialize `FirebaseApp` once and expose `FirebaseMessaging` as a bean **from the adapter-out module**. Gate the whole `@Configuration` on `fcm.enabled` so nothing Firebase-related is constructed when push is off.

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
        // Idempotent: prevents a duplicate init on context refresh.
        FirebaseApp.getApps().firstOrNull()?.let { return it }
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())   // ADC — no key file
                .setProjectId(fcmProperties.projectId)
                .build()
        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)
}
```

**Why the `getApps()` guard**: `FirebaseApp.initializeApp` throws if the default app already exists. A context refresh (common in tests and in `@DirtiesContext` runs) would otherwise fail on the second pass.

```kotlin
// {domain}-adapter-out : .adapter.fcm.properties
@ConfigurationProperties(prefix = "fcm")
data class FcmProperties(
    val enabled: Boolean = false,
    // Firebase project = GCP project (reuses GCP_PROJECT_ID). Unused when enabled=false, hence nullable.
    val projectId: String? = null,
)
```

`projectId` is **nullable on purpose**. Making it non-null would force every local/test environment to define `GCP_PROJECT_ID` even with push disabled, which defeats the no-op design.

## The no-op twin

Two adapters implement the same port, selected by the same property from opposite sides:

| Adapter | Condition | Behavior |
|---------|-----------|----------|
| `FcmPushNotificationAdapter` | `havingValue = "true"` | Real send |
| `NoOpPushNotificationAdapter` | `havingValue = "false", matchIfMissing = true` | Returns `success = true` without sending |

So **exactly one bean always exists** — the in-app inbox, notification settings and every other feature keep working with no Firebase credentials present. `matchIfMissing = true` is what makes "no config at all" resolve to the no-op rather than to a missing-bean failure.

**Diagnostic trap**: when sends "succeed" but nothing arrives on the device, check `fcm.enabled` before anything else — a no-op send is indistinguishable from a successful one in the response.

## Per-profile configuration

```yaml
# application-local.yaml
fcm:
  enabled: ${FCM_ENABLED:false}   # false → no Firebase bean (no-op) → local run needs no Firebase
  project-id: ${GCP_PROJECT_ID}

# application-dev.yaml / application-prod.yaml
fcm:
  enabled: ${FCM_ENABLED:true}
  project-id: ${GCP_PROJECT_ID}
```

The default flips per environment: **off locally, on in dev/prod**. Connection-style settings go in each profile file even when the value is identical (root `CLAUDE.md`, Profiles).

| Env var | Purpose |
|---------|---------|
| `FCM_ENABLED` | Initializes Firebase only when `true`. Leave unset/`false` locally if you don't need push. |
| `GCP_PROJECT_ID` | GCP/Firebase project ID — shared with the existing GCS config, not a new secret |

Neither is a secret, so neither belongs in a vault; ADC supplies the credentials. Never commit `.env` (`code-style` skill).

## IAM

The service account needs the **Firebase Cloud Messaging API** enabled and a **custom role containing only `cloudmessaging.messages.create`**. Use `roles/firebase.admin` only when the account genuinely needs broader Firebase permissions — it grants far more than sending.

Credential sources, in the order ADC resolves them:

| Environment | Credential |
|-------------|-----------|
| GKE / Cloud Run / GCE | Workload Identity or the attached service account — nothing to configure |
| Other servers | `GOOGLE_APPLICATION_CREDENTIALS` pointing at an SA key file |
| Local | `gcloud auth application-default login` (your own user credentials) |

## Gradle

Add the dependency **only** to `{domain}-adapter-out`:

```kotlin
// {domain}-adapter-out/build.gradle.kts
dependencies {
    implementation(libs.firebase.admin)
}
```

Never add Firebase to the domain or application module — that is what keeps the "no Firebase imports outside the adapter" rule mechanically true rather than merely stated. The version lives in `gradle/libs.versions.toml` (root `CLAUDE.md`: versions have a single source).
