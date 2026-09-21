# daily-fortune

Owns 오늘의 운세 — one record per member per day, generated from that day's 일진 and the member's 명식.

Not to be confused with its siblings: `day-fortune` scores **a date the member picked** for a purpose (택일), `year-fortune` covers **a chosen year**. This is the only one of the three that generates on a schedule rather than on request.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Daily fortune generation (scheduled batch and on-demand), history | 명식 and 일진 → `saju` |
| The day's score, derived from the member's 행운 액션 scores | 행운 액션 definition, scoring, summaries → `luck` |
| Serving today's fortune to the push pipeline | Who to push and when → `notification` |

Records are **append-only** — a day's fortune is never edited once written, because the history feeds statistics.

## Cross-Domain Contracts

Provides:

| Port | Consumed by | What the consumer acts on |
|------|-------------|---------------------------|
| `CreateDailyFortunePort` | `auth` | generates the member's first fortune during signup |
| `DailyFortuneNotificationPort` | `notification` | reads today's fortune to build the push payload |

Consumes `GetSajuChartPort` and `GetDailyPillarPort` (`saju`), `GetMemberFortuneProfilePort` and `GetMemberIdsPort` (`member`, to enumerate the batch), and `CreateLuckActionPort`, `GetLuckActionScoresPort`, `GetLuckActionSummariesPort` (`luck`).

**This domain deliberately does not subscribe to `SajuChartChangedEvent`.** A chart change must not rewrite fortunes already issued — they are historical record — and the `TODAY_FORTUNE` cache is keyed `memberId:fortuneDate`, so it rotates on its own with nothing to evict. `year-fortune` makes the opposite call for good reasons; see its `CLAUDE.md`.

## Decisions & Traps

- **Generation runs outside the transaction** via the orchestrator + `DailyFortuneTransactionalStore` split (→ `architecture` skill). Specific here: the lock is on `(memberId, fortuneDate)`, and `saveIfAbsent` writes the fortune **and** its per-category `LuckAction` rows in one transaction, so a fortune never exists without its actions.
- **The batch chunk size is pinned to 1, and must stay there.** `CreateDailyFortunePort.create` already commits per member in its own transaction; a larger chunk would bundle several members' commits into one Step transaction and destroy per-member commit isolation. See `GenerateDailyFortunesJobConfig`.
- **Failure handling is per member, never per batch.** Transient AI failures retry up to `AI_CALL_RETRY_LIMIT` and then skip that member. Circuit-open and timeout skip immediately — retrying while the breaker is open only burns calls. Generation-already-in-progress (#90) also skips immediately. A skipped member is filled by the next batch or self-heals on the home screen via `GetTodayFortuneService`.
- **`generate` and `regenerate` differ only in their JobInstance key, and that is the whole design.** `generate` keys on `fortuneDate` alone, so an already-completed date is filtered out by `JobInstanceAlreadyCompleteException`. `regenerate` adds `requestedAt` so every call is a new instance and can run regardless of prior COMPLETED/FAILED state — safe because `create` is idempotent, so a rerun only fills the members that are missing.
- The scheduler (`DailyFortuneScheduler`, 03:00 KST) is a **thin trigger only**. Member selection and generation belong to the use case; do not grow logic into the adapter.
