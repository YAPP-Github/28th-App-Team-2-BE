# year-fortune

Owns 연간 운세 — one record per member per chosen year, generated from that year's 연주 and the member's 명식.

Siblings: `daily-fortune` is today's fortune on a schedule; `day-fortune` scores candidate dates for a purpose (택일).

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Year fortune generation and lookup | 명식 and 연주 → `saju` |
| The lifetime of the `YEAR_FORTUNE` cache, including invalidating it | Member profile → `member` |
| | 오늘의 운세 → `daily-fortune`; 택일 → `day-fortune` |

## Cross-Domain Contracts

Provides **no** `shared` port — clients call this domain's `*UseCase` directly.

Consumes `GetSajuChartPort` and `GetYearPillarPort` (`saju`), and `GetMemberFortuneProfilePort` (`member`).

**This is the only fortune domain that subscribes to `shared.event.SajuChartChangedEvent`**, and the reason is cache lifetime, not importance. `YEAR_FORTUNE` has a **90-day TTL** (`CacheNames`), so a changed or deleted 명식 would keep serving stale answers for up to three months. `daily-fortune`'s `TODAY_FORTUNE` is 24 hours and keyed by date, so it rotates on its own and needs no listener. Use the same test when adding a cache anywhere: **TTL long enough to outlive the underlying data means you own an invalidation path.**

## Decisions & Traps

- **Generation runs outside the transaction** via the orchestrator + `YearSelectionFortuneTransactionalStore` split (→ `architecture` skill). The lock is on `(memberId, year)`.
- **Eviction is manual and cannot be an annotation.** The cache key is `memberId:year`, so `@CacheEvict` cannot clear all of one member's years at once. `SajuChartChangedEventListener` reads the years that member actually has and evicts those keys individually — deliberately not a broader wipe, which would take out other members' entries.
- **The listener must absorb its own failures.** It runs at `AFTER_COMMIT`, and Spring does **not** swallow afterCommit exceptions: a throw would propagate to the caller of an already-committed `DeleteMemberSajusService`/`ReplaceSelfSajuChartService` transaction. It also calls `cache.evict(...)` directly, which bypasses `RedisCacheConfig`'s fail-open `CacheErrorHandler` — that handler only covers the `@CacheEvict` advisor path. The full reasoning is in the listener's KDoc.
- **A failed evict is a human-visible incident, not a warning to bury.** It means the 명식 already changed while the cache keeps answering from the old one for up to 90 days. Log it so someone notices.
- `AFTER_COMMIT` is also what prevents two wrong outcomes: evicting on a transaction that later rolls back, and racing a concurrent read that would refill the cache from the pre-change 명식.
