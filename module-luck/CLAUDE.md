# luck

Owns 행운 액션 — the per-category actions that accompany a day's fortune, and whether the member completed them.

## Overview

Every day's generated fortune (`daily-fortune`) comes with short, actionable suggestions per category that a member can act on. This domain owns only the creation/lookup of those suggestions (행운 액션) and tracking whether the member actually did them — the fortune's own interpretation, and when actions get created, is `daily-fortune`'s call.

Terms this domain uses:

- **행운 액션 (lucky action)**: `LuckAction` — an append-only record with a score, title, and content, created once per member·date·category (`FortuneCategory`, a `shared` value type).
- **서비스일 (service day)**: the day boundary is KST 06:00, not midnight (`shared`'s `currentFortuneServiceDate`) — see "Decisions & Traps" below. `daily-fortune` shares this concept too.

## Module Structure

**Packages**
- `luck-domain`: entities/exceptions at the root, `port.inbound`/`port.outbound` — representative: `LuckAction`, `LuckActionRepository`
- `luck-application`: `*Service` at the root — representative: `GetLuckActionsService`, `ToggleLuckActionService`
- `luck-adapter-in`: `adapter.web` — representative: `LuckActionController`, `LuckActionApi`
- `luck-adapter-out`: `adapter.persistence` (JPA), notification adapter — representative: `LuckActionRepositoryAdapter`, `LuckyActionNotificationAdapter` (→ implements `LuckyActionNotificationPort`, consumed by `notification`)

**Core Domain Model**
```
LuckAction (one per memberId + fortuneCategory + fortuneDate combination)
    - score/title/content: fixed at creation (append-only)
    - achieved: the only mutable field, changed only via toggle()
```
A single aggregate — it references no other entity, and `memberId` is a plain UUID. The complexity is not in relationships but in "what's immutable vs. what's mutable."

**Must-Read Files**
- `LuckAction.kt` — shows that `toggle()` is the sole mutation point touching `achieved`, and that neither `create` nor `reconstitute` has a path that touches any other field
- `ToggleLuckActionService.kt` — the single write path that confirms why ownership of the `achieved` toggle lives only in this domain (→ Responsibility Boundary)

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| `LuckAction` records, their scores and summaries | The fortune the actions belong to → `daily-fortune` |
| The `achieved` toggle | When actions get created → `daily-fortune` decides |
| Supplying reminder content | Sending the reminder push → `notification` |

Records are **append-only except `achieved`**. Score, title and content are fixed at creation because the history feeds statistics; `achieved` is the single field a member may flip back and forth.

## Cross-Domain Contracts

Provides — all four implemented in `luck-adapter-out`, since each is a row read or write with no use-case logic:

| Port | Consumed by |
|------|-------------|
| `CreateLuckActionPort` | `daily-fortune` |
| `GetLuckActionScoresPort` | `daily-fortune` |
| `GetLuckActionSummariesPort` | `daily-fortune` |
| `LuckyActionNotificationPort` | `notification` |

Consumes nothing from any other domain — **a pure provider**.

**Three of the four go to `daily-fortune` alone, and that relationship runs both ways.** `daily-fortune` creates this domain's records inside its own `saveIfAbsent` transaction, then reads their scores back to derive the day's score. So a `LuckAction` is born inside `daily-fortune`'s transaction but belongs here from that moment on — most importantly the `achieved` toggle, which `daily-fortune` must never write.

## Decisions & Traps

- **The service day rolls over at 06:00, not midnight.** Records are dated with `currentFortuneServiceDate` (in `shared`): before 06:00 KST, "today" means the previous calendar day. Reach for `currentDate` only when you genuinely mean wall-clock date. Mixing the two makes records look a day off for exactly six hours every night — a bug that only reproduces between midnight and dawn.
