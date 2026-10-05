# Closing the Plan-Review Loop with `/goal`

Detailed reference for the `external-harness` skill, phase 3. Read this before the first plan review.

---

## Why a Loop Needs a Mechanism

Phase 3 isn't a single step — it's a **loop**: review finds problems → plan gets revised → review again. Nothing forces this loop to actually repeat, and the failure mode is quiet: one review, a few findings acknowledged but not fixed, and implementation starts on a plan that never actually converged.

`/goal <condition>` is the mechanism that enforces it. A Claude Code built-in that registers a **session-scoped Stop hook**: the session can't end until the condition is met, and it auto-releases once it is.

Set it **before** the first review:

```
/goal until plan-eng-review and plan-ceo-review pass with no blocking findings, and every finding raised has been folded into the plan
```

---

## Writing a Condition That Can Actually Close

The condition has to be something whose satisfaction can be objectively declared. A vague condition produces a session that never stops.

| | |
|---|---|
| ❌ | `until the plan is good` — no observable end state |
| ❌ | `until every risk is gone` — unsatisfiable |
| ✅ | `until both reviews pass with no blocking findings` |
| ✅ | `until every review finding is either folded into the plan or explicitly dismissed` |

**The escape hatch:** if a condition turns out to be unreachable, don't fight the hook — explicitly release the goal. Don't weaken the plan to satisfy a badly written condition; rewrite the condition instead.

---

## Scope Limits

- **Session-scoped.** It disappears on restart. Work spanning multiple sessions belongs to `/wayfinder` (phase 1b) instead of `/goal`; if you need that kind of persistent ledger, there's `omc ultragoal`, which layers on top of `/goal`.
- **Works alongside our Stop hooks.** Both `stop-format.sh` (`ktlintFormat` + EOF-newline fixup) and gstack's timeline hook still run on every exit attempt — `/goal` doesn't replace them, it just adds a third gate.
- **Phase 3 only.** Don't wrap TDD in `/goal`: superpowers already enforces RED-first, and phase 7's deterministic gate is `/run-checks`. Stacking another Stop hook onto a phase that already has a strong gate only makes it harder to escape a failure.
