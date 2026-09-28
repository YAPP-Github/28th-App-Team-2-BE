# day-fortune

Owns 택일 — scoring candidate dates the member picked for a stated purpose. One record per `(member, purpose, date)`.

Siblings: `daily-fortune` is today's fortune generated on a schedule; `year-fortune` covers a chosen year. This domain is the only one that fans out over **several dates in one request**.

## Overview

Used when a member has a concrete purpose — a wedding, a move, signing a contract — and wants to know "is this date okay," picking several candidate dates and getting them scored in one go. Unlike `daily-fortune`, these aren't pre-built on a schedule; only as many are generated as the member requests, at the moment they request them — which is why this is the only one of the three with a "one request → many results" fan-out shape.

Terms this domain uses:
- **택일**: `DaySelectionFortune` — one per member/purpose(`DaySelectionPurpose`)/date combination, made of star ratings (`FortuneCategoryStar`, 1–3) for 3 categories plus a score and body.
- **Purpose (`DaySelectionPurpose`)**: an enum for why the member is picking a date (moving, marriage, etc.) — the same date can score differently depending on the purpose.

## Module Structure

**Packages**
- `day-fortune-domain`: entities, exceptions at the root — representative: `DaySelectionFortune`, `DaySelectionPurpose`, `DaySelectionFortuneAiPort`
- `day-fortune-application`: `*Service` at the root — representative: `CreateDaySelectionFortuneService` (orchestrator, fan-out), `CreateOneDaySelectionFortuneService` (handles one date), `DaySelectionFortuneTransactionalStore`
- `day-fortune-adapter-in`: `adapter.web` — representative: `DaySelectionFortuneController`
- `day-fortune-adapter-out`: JPA, Vertex AI — representative: `DaySelectionFortuneRepositoryAdapter`

**Core Domain Model**
```
DaySelectionFortune (1 per memberId + purpose + targetDate combination)
    fortuneCategories: exactly 3 FortuneCategoryStar, distinct categories, star rating 1–3
```

**Must-read files**
- `DaySelectionFortune.kt` — creation-time validation rules: no past dates, no duplicate categories among the 3
- `CreateDaySelectionFortuneService.kt` — the orchestrator that sorts and de-duplicates candidate dates before fanning them out via coroutines (see "Decisions & Traps" below)
- `CreateOneDaySelectionFortuneService.kt` — the actual per-date unit that does lock-read / AI call / lock-save

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
