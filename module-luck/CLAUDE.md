# luck

Owns 행운 액션 — the per-category actions that accompany a day's fortune, and whether the member completed them.

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
