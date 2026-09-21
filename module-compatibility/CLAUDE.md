# compatibility

Owns 궁합 — comparing the member's own 명식 against a partner's. One record per `(member, own chart, partner chart)`, created idempotently.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| The compatibility aggregate: score, narrative, 오행 breakdown | 명식 and partner registration → `saju` |
| The deterministic 오행 calculation | What a relationship label means → `saju` owns `MemberSajuLink.relationshipType` |

`relationshipType` on the aggregate is a **snapshot taken at comparison time**, not a live reference. Changing the partner link later does not rewrite past results, and it should not.

## Cross-Domain Contracts

Provides no `shared` port — **a pure consumer**. Nothing else branches on a compatibility result, so clients call this domain's `*UseCase` directly.

Consumes `GetSajuChartsForCompatibilityPort` and `GetSajuChartNamePort` (`saju`).

## Decisions & Traps

- **The aggregate is deliberately half AI, half arithmetic.** Score, headline, subheadline, summary and 총운 come from the AI. `ohaengs` does **not** — `CompatibilityOhaengCalculator` computes it from both charts. Never fold the 오행 percentages into the AI prompt: they must be reproducible and sum to exactly 100, and a model cannot guarantee either.
- **Percentages are normalised with the largest-remainder method** so the five values total exactly 100 after flooring. Rounding each value independently breaks that invariant — the numbers will occasionally total 99 or 101.
- **Unknown 오행 codes are rejected, not dropped.** The calculator raises `CompatibilityOhaengElementMismatchException` rather than treating an unrecognised key as zero. A silent drop is the dangerous alternative: the remaining percentages would still sum to 100 while quietly discarding part of a chart.
- **Generation follows the orchestrator + `SajuCompatibilityTransactionalStore` split** (→ `architecture` skill). Note the asymmetry between lock and lookup: the lock is taken on the **chart pair** `(myChartId, partnerChartId)`, while the row is looked up by `(memberId, myChartId, partnerChartId)`. The pre-read skips the AI call entirely when a result already exists, which is what makes a repeated request cheap rather than merely correct.
