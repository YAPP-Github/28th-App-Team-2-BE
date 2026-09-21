# notification

Owns notification **delivery and the in-app inbox** — push transport, device tokens, the member's notification list, dispatch scheduling, and notices. It owns almost none of the content it sends.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Push transport, device tokens, delivery-failure records | FCM mechanics — init, token cleanup, error classification → `fcm` skill |
| The in-app inbox (`Notification`) and read state | Notification **content** → pulled from `daily-fortune` and `luck` through optional ports |
| Per-member notification settings, OS permission sync | **Push consent → `terms`** |
| Dispatch scheduling, retry, notice publication | Member enumeration → `member` |

Push consent living in `terms` is the boundary most often gotten wrong. Consent is an agreement, so `terms` owns it and this domain only reads it through `GetPushConsentPort`. A notification setting ("do I want morning reports") is this domain's; consent ("may we push you at all") is not.

## Cross-Domain Contracts

Provides `SendNotificationPort`, implemented in **`notification-application`** (`SendNotificationService`) — it is a use case rather than a row write, so it follows `saju`'s placement rule, not `member`'s.

Consumes `GetMemberIdsPort` (`member`), `GetPushConsentPort` (`terms`), and two **content** ports: `DailyFortuneNotificationPort` (`daily-fortune`) and `LuckyActionNotificationPort` (`luck`).

**The content ports are injected optionally, and that is the extension point.** If no implementing bean exists, or the port yields no content for a member, that member is skipped rather than the dispatch failing. This domain must stay shippable with zero content providers — a new notification kind is added by supplying a provider on the other side, never by branching here.

## Dispatch Design

Three schedules, all Asia/Seoul: morning reports every 30 minutes (`0 0,30 * * * *`), lucky-action reminders at 20:00, failed-send retries **every minute**.

- **None of the dispatch path is transactional.** `NotificationDispatchService` and `RetryFailedNotificationsService` hold no transaction, so per-member sends commit independently and FCM I/O stays outside the database — the same principle as the orchestrator split (→ `architecture` skill).
- **Total concurrency is capped at 4 by one shared dispatcher.** Both services deliberately share `notificationDispatchDispatcher`; separate caps would sum and could exceed the Hikari pool, which also serves web requests. Do not give either service a dispatcher of its own.
- **Exceptions are isolated before `async`, not inside it** (#81) — catching inside the coroutine would let `awaitAll()` cancel sibling sends. This is the **opposite** arrangement from `day-fortune`, which relies on a cancellation-propagation contract. Do not carry that pattern across.
- **Retry is bounded**: 3 attempts, 1m → 5m → 30m backoff. The retry set is the intersection of the transiently-failed tokens with the member's *current* device tokens, so tokens lost to logout drop out and already-delivered ones are never resent.

## Decisions & Traps

- **Notice publication carries two independent idempotency guards, and both are load-bearing.** The advisory lock (`DispatchLockPort`) serializes concurrent instances during a Blue/Green switchover; the `NoticeDispatchHistory` unique constraint catches repeated calls at *different* times. The second was added once the admin API meant operational scripts were no longer the only entry point. Neither guard covers the other's case — removing one reopens a real duplicate-send path.
- **Push is off by default.** `NoOpPushNotificationAdapter` is selected unless `fcm.enabled=true` (`matchIfMissing = true`), so a local or misconfigured environment silently sends nothing instead of failing. When sends "succeed" but nothing arrives, check this flag before anything else.
