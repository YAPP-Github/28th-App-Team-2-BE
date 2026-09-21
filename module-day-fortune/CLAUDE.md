# day-fortune

Owns 택일 — scoring candidate dates the member picked for a stated purpose. One record per `(member, purpose, date)`.

Siblings: `daily-fortune` is today's fortune generated on a schedule; `year-fortune` covers a chosen year. This domain is the only one that fans out over **several dates in one request**.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| 택일 운세 generation and lookup, `DaySelectionPurpose` | 명식 and 일진 → `saju` |
| Candidate-date fan-out and its concurrency | Member profile → `member` |
| | 오늘의 운세 → `daily-fortune`; 연간 운세 → `year-fortune` |

## Cross-Domain Contracts

Provides **no** `shared` port — nothing else branches on a 택일 result, so clients call this domain's `*UseCase` directly.

Consumes `GetSajuChartPort` and `GetDailyPillarPort` (`saju`), and `GetMemberFortuneProfilePort` (`member`).

No cache and no event subscription. Like `daily-fortune` it does not react to `SajuChartChangedEvent`; unlike `year-fortune` it has no cached read path either, so there is nothing that could go stale.

## Decisions & Traps

- **Generation runs outside the transaction** via the orchestrator + `DaySelectionFortuneTransactionalStore` split (→ `architecture` skill). The lock is on `(memberId, purpose, targetDate)`.
- **Orchestration is two-level.** `CreateDaySelectionFortuneService` sorts and de-duplicates the candidate dates, then fans them out to `CreateOneDaySelectionFortuneService` with coroutines (`async`/`awaitAll`). The dominant cost is one AI call per date, so running them sequentially is what the fan-out exists to avoid.
- **Parallel fan-out is deadlock-free only because each transaction holds exactly one lock.** `createOne`'s lock-read and lock-save each take a single advisory lock and release it at commit, so no transaction ever holds two — there is no circular wait to form. **Preserve this property.** If a change ever makes one transaction hold two locks, concurrent dates can deadlock, and the failure will be load-dependent and hard to reproduce.
- **Sorting is no longer about lock ordering.** It once was; now it only makes the returned results deterministically date-ordered. De-duplication avoids redundant lock/transaction round-trips and wasted AI calls. Do not "simplify" either away on the assumption they are cosmetic.
- Per-date commits are independent — one date failing does not roll back the others.
