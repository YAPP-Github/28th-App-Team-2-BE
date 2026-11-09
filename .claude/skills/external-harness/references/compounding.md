# Compounding Learnings into `docs/solutions/`

Detailed reference for the `external-harness` skill, phase 10. Read this when deciding whether something that was just learned is worth recording.

---

## Why Repo-Local

`/ce-compound` records what was learned here, repo-local. This is deliberately **separate** from the per-user memory under `~/.claude/` — that memory is invisible to teammates, so a lesson stored there is re-learned by everyone else the hard way.

## What Earns a Spot

Reasoning that **doesn't survive in the final code and tests**:

- why a boundary sits where it does
- which approach was tried and failed, and why
- a framework behavior that surprised us

Past examples that would have belonged here:

- `@CommandService` (`REQUIRED` propagation) silently defeating the `TransactionalStore` pattern
- keeping an AI call outside the transaction via the orchestrator + `<Aggregate>TransactionalStore` split
- an FCM `INVALID_ARGUMENT` not meaning token expiry, because treating it as expiry deletes healthy tokens

## What Doesn't

Routine changes the diff already explains on its own. If a reader can reconstruct the reasoning from the commit and the code, compounding it only adds a second place to keep in sync.

## Relationship to the Other Doc Surfaces

| Surface | Holds |
|---------|-------|
| `docs/solutions/` | Reasoning that left no trace in code — failed approaches, surprising framework behavior |
| `module-{domain}/CLAUDE.md` | That bounded context's boundary, contracts, and standing traps |
| `.claude/skills/*` | Rules that apply to a whole class of work |
| KDoc on the class | Invariants of that one declaration |

When a lesson fits more than one, pick the narrowest surface that a future reader would actually open.
