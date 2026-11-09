# notification

Owns notification **delivery and the in-app inbox** — push transport, device tokens, the member's notification list, dispatch scheduling, and notices. It owns almost none of the content it sends.

## Overview

The single channel through which the various content this service generates (오늘의 운세, 행운 액션 reminders, AI chat-answer completion, admin notices) actually reaches a member. It doesn't produce content itself — it only decides "when, to whom, and via which channel (in-app vs. FCM push)" to send it. That's what lets the dispatch logic stay unified even though content providers differ by domain.

Terms this domain uses:

- **`NotificationType`** (`shared`): the kind of notification — `FORTUNE` (morning report), `AI_COMPLETE` (토닥이's answer is ready), `LUCKY_ACTION` (행운 액션 reminder), `NOTICE` (admin notice). `NotificationSetting.isPushEnabledFor(type)` gates push per type (`NOTICE` always sends regardless of any toggle).
- **Setting vs. consent**: see the paragraph below — the boundary most often confused, precisely because the names sound alike.

## Module Structure

**Packages**
- `notification-domain`: entities such as `Notification`/`DeviceToken`/`NotificationSetting`/`NoticeDispatchHistory` at the root, `PushNotificationPort`/`DispatchLockPort` in `port.outbound` — representative: `Notification`, `NotificationSetting`, `PushNotificationPort`
- `notification-application`: `*Service` + dispatchers at the root — representative: `SendNotificationService`, `NotificationDispatchService`, `RetryFailedNotificationsService`
- `notification-adapter-in`: 4 controllers (notifications/settings/device tokens/admin notices) + scheduler/runner — representative: `NotificationController`, `NotificationScheduler`, `AdminNoticeController`
- `notification-adapter-out`: FCM adapters (real + `NoOp`), 5 JPA adapters, Postgres advisory lock — representative: `FcmPushNotificationAdapter`, `NoOpPushNotificationAdapter`, `PostgresDispatchLockAdapter`

**Core Domain Model**
```
Notification (memberId + type + isRead) — the in-app inbox record, always written
DeviceToken (memberId ↔ FCM token, reassign() re-binds a device)
NotificationSetting (3 per-member toggles + osPushPermission; isPushEnabledFor(type) is the real gating logic)
```
The three entities reference nothing else, linked only by `memberId` — the logic deciding "who gets pushed" lives entirely in `NotificationSetting.isPushEnabledFor`.

**Must-Read Files** (layer-agnostic)
- `Notification.kt` — its KDoc explains why the originally planned schema (a separate `notification` + `member_notification`) was merged into one per-member record
- `NotificationSetting.kt` — `isPushEnabledFor()` is what "setting" actually means. Note that only `NOTICE` is always `true` regardless of any toggle
- `NotificationDispatchService.kt` — the actual implementation of the transaction-free dispatch path described under "Dispatch Design" below

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Push transport, device tokens, delivery-failure records | FCM mechanics — init, token cleanup, error classification → `fcm` skill |
| The in-app inbox (`Notification`) and read state | Notification **content** → pulled from `daily-fortune` and `luck` through optional ports |
| Per-member notification settings, OS permission sync | **Night-push consent (`NIGHT_PUSH`) → `terms`** |
| Dispatch scheduling, retry, notice publication | Member enumeration → `member` |

Consent living in `terms` is the boundary most often gotten wrong. A notification **setting** ("do I want morning reports") is this domain's; an **agreement** is not — `terms` owns `NIGHT_PUSH` and this domain only reads it through `GetPushConsentPort`.

Two rules decide what actually goes out, and they are easy to conflate:

- **The in-app inbox always gets the record.** Only the FCM push is gated — by the member's setting toggle and, at night, by consent.
- **Night consent applies to `MARKETING` only.** `SERVICE` notifications send at night regardless. So "the member declined night push" never means "suppress everything after hours".

## Cross-Domain Contracts

Provides `SendNotificationPort`, implemented in **`notification-application`** (`SendNotificationService`) — it is a use case rather than a row write, so it follows `saju`'s placement rule, not `member`'s.

Consumes `GetMemberIdsPort` (`member`), `GetPushConsentPort` (`terms`), and two **content** ports: `DailyFortuneNotificationPort` (`daily-fortune`) and `LuckyActionNotificationPort` (`luck`).

**The content ports are injected optionally, and that is the extension point.** If no implementing bean exists, or the port yields no content for a member, that member is skipped rather than the dispatch failing. This domain must stay shippable with zero content providers — a new notification kind is added by supplying a provider on the other side, never by branching here.

## Dispatch Design

Three schedules, all Asia/Seoul: morning reports every 30 minutes (`0 0,30 * * * *`), lucky-action reminders at 20:00, failed-send retries **every minute**.

- **None of the dispatch path is transactional.** `NotificationDispatchService` and `RetryFailedNotificationsService` hold no transaction, so per-member sends commit independently and FCM I/O stays outside the database — the same principle as the orchestrator split (→ `architecture` skill).
- **Total concurrency is capped at 4 by one shared dispatcher.** Both services deliberately share `notificationDispatchDispatcher`; separate caps would sum and could exceed the Hikari pool, which also serves web requests. Do not give either service a dispatcher of its own.
- **Every send catches its own exceptions inside its coroutine** (#81), so nothing escapes to `awaitAll()` and one member's failure cannot cancel its siblings. Only an *uncaught* exception would propagate. `day-fortune` instead relies on a cancellation-propagation contract — do not carry that pattern across, and do not move this `try`/`catch` outward on the assumption the two domains work alike.
- **Retry is bounded**: 3 attempts, 1m → 5m → 30m backoff. The retry set is the intersection of the transiently-failed tokens with the member's *current* device tokens, so tokens lost to logout drop out and already-delivered ones are never resent.

## Decisions & Traps

- **Notice publication carries two independent idempotency guards, and both are load-bearing.** The advisory lock (`DispatchLockPort`) serializes concurrent instances during a Blue/Green switchover; the `NoticeDispatchHistory` unique constraint catches repeated calls at *different* times. The second was added once the admin API meant operational scripts were no longer the only entry point. Neither guard covers the other's case — removing one reopens a real duplicate-send path.
- **Push is off by default.** `NoOpPushNotificationAdapter` is selected unless `fcm.enabled=true` (`matchIfMissing = true`), so a local or misconfigured environment silently sends nothing instead of failing. When sends "succeed" but nothing arrives, check this flag before anything else.
