# FCM Testing & Verification Reference

Detailed reference for the `fcm` skill — test strategy per layer and the Konsist rules that keep Firebase contained.

Working example: `module-notification/notification-adapter-out/src/test/.../adapter/fcm/FcmPushNotificationAdapterTest.kt`.

---

## Per-layer strategy

| Layer | Target | Approach |
|-------|--------|----------|
| `*-application` | The use case sends via `PushNotificationPort` and deletes expired tokens through the token repository | Mock both with `mockk<...>()` — no real FCM |
| `*-adapter-out` (fcm) | `Message` construction + error → `PushResult` mapping | Stub `FirebaseMessaging` with MockK; fabricate a `FirebaseMessagingException` per error code |
| `*-adapter-out` (persistence) | Device-token CRUD, including both delete scopes | TestContainer (shared `pgvector/pgvector:pg17`) — see the `testing` skill |

**Never send a real push from a test.** It costs money, needs credentials, and is non-deterministic. Stub `FirebaseMessaging`.

All specs use `DescribeSpec`, MockK for mocking, Kotest matchers for assertions, and `afterTest { clearMocks(...) }` with no `relaxed = true` (`testing` skill).

## What the adapter test must cover

The adapter is where classification policy becomes behavior, so each branch needs its own case:

| Case | Expected |
|------|----------|
| `send` with `UNREGISTERED` | `tokenExpired = true`, `success = false` — **not** an exception |
| `send` with any other error (e.g. `INTERNAL`) | throws `PushSendFailedException` |
| `send` success | `success = true` |
| `sendAll` with one `UNREGISTERED` response | that entry `tokenExpired = true`, and `errorCode` left **null** |
| `sendAll` with a non-expiry failure | `success = false` **and** `errorCode` populated |
| `sendAll` with an empty list | returns empty without calling Firebase |

The last two are the ones worth guarding deliberately: they encode "a batch reports failures instead of throwing" and "the cause must survive in `errorCode`". A refactor that makes `sendAll` throw will pass a naive happy-path test and fail these.

```kotlin
class FcmPushNotificationAdapterTest : DescribeSpec({
    val firebaseMessaging = mockk<FirebaseMessaging>()
    val adapter = FcmPushNotificationAdapter(firebaseMessaging)

    afterTest { clearMocks(firebaseMessaging) }

    describe("send") {
        context("등록 해제된 토큰이면(UNREGISTERED)") {
            it("실패가 아니라 정리 대상(tokenExpired)으로 보고한다") {
                val exception = mockk<FirebaseMessagingException>()
                every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
                every { firebaseMessaging.send(any()) } throws exception

                val result = adapter.send(PushNotification("stale", "t", "b"))

                result.tokenExpired shouldBe true
                result.success shouldBe false
            }
        }
    }
})
```

`FirebaseMessagingException` has no public constructor, so build it with `mockk<>()` and stub only `messagingErrorCode` (and `errorCode` when testing the fallback). For batch tests, a small `mockBatchResponse(responses)` helper keeps each case readable.

## Konsist rules (these already exist)

`module-architecture-test/.../ArchitectureTest.kt` enforces FCM containment:

| Test (Korean name as written) | Guarantees |
|------|-----------|
| `Firebase 타입은 adapter 패키지에서만 임포트한다` | `com.google.firebase..` imports appear only under `.adapter` — forbidden in domain/application |
| `Fcm 어댑터는 adapter_fcm 패키지에만 위치한다` | A class named `Fcm*Adapter` resides in `..adapter.fcm..` |

The first one checks **files, not classes** — on purpose. A class-level filter misses files that declare only an `object`/`interface` (such as `FcmConfig`), which is exactly where a stray Firebase import hides. Preserve that if you touch the rule.

Port placement (`PushNotificationPort` in `..port.outbound`) is already covered by the generic `*Port`/`*Repository` location rules — there is no FCM-specific rule for it, and none is needed.

To add a rule, append a `@Test` to `ArchitectureTest.kt` with a Korean test name (`konsist` skill).
