---
name: external-harness
description: Load when using the externally installed skill suites (gstack / superpowers / compound-engineering / mattpocock-skills). Phase-by-phase routing, which external skills are banned because this repo's own harness wins. Installation, token budget, hazards and the /goal loop live in references/.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# External Harness Stack

Four external skill suites are installed alongside this repo's own harness. They overlap **substantially** with each other, and with our own `.claude/` skills. This document is the **single routing authority**: it adopts exactly one skill per phase and bans the rest.

## Core Rule

> **Wherever our own harness already has a skill, our harness always wins.** The external suites don't know our hexagonal boundaries, Konsist rules, `CommonResponse` envelope, or our Korean commit convention. Adopt an external skill only where we have a genuine gap.

They are also **user-scoped — `git clone` doesn't carry them.** A teammate who hasn't run the install steps has none of the skills below, so our own harness must stay self-sufficient (→ `references/installation.md`).

## Phase-by-Phase Routing

| # | Phase | Use | Why |
|---|-------|-----|-----|
| 1 | Clarifying ambiguous requirements | `/grill-me` (mattpocock) | **Gap.** We had no tool to interrogate intent before design |
| 1b | Work spanning multiple sessions | `/wayfinder` (mattpocock) | A decision-ticket map on the issue tracker |
| 2 | Writing a plan | `/ce-plan` | Produces per-task file paths + verification steps |
| 3 | **Plan review** | `/plan-eng-review` → `/plan-ceo-review` (gstack) wrapped in `/goal` | **Gap.** Catches transaction-boundary/port-contract mistakes *before* code is written. `/goal` forces the revise-and-re-review loop to converge → `references/goal-loop.md` |
| 4 | Implementation | `test-driven-development` (superpowers) | Enforces RED-GREEN-REFACTOR |
| 4b | A plan with many independent tasks | `subagent-driven-development` (superpowers) | A fresh subagent per task, reviewed each time |
| 4c | Debugging a defect | `systematic-debugging` (superpowers) | **Gap.** Our `test-validator` only diagnoses *failing tests*, not open-ended bugs. Don't also run gstack `/investigate` — same role |
| 5 | Writing test code | **our `test-writer` agent** | Only ours knows the per-layer patterns + TestContainer |
| 6 | Code review | **our `code-reviewer` agent** first, then `/review` (gstack) | Ours enforces hexagonal/Konsist; gstack adds a production-bug perspective |
| 7 | Pre-PR verification | **our `/run-checks`** | Deterministic script, not LLM judgment |
| 8 | Commit / push / PR | **our `commit-push-pr`** | Korean `[#issue] type:` convention |
| 9 | Applying PR feedback | **our `resolve-review`** | |
| 10 | Recording what was learned | `/ce-compound` → `docs/solutions/` | **Gap.** Repo-local, so the whole team inherits it → `references/compounding.md` |

Phases 3 and 10 are what justify this whole stack. Phase 1 is next.

## Banned External Skills

Don't invoke these, and don't let them auto-trigger either.

| Banned | Conflicts with | Reason |
|--------|---------------|--------|
| `ce-commit`, `ce-commit-push-pr`, gstack `/ship` | our `commit-push-pr` | Writes commit messages in English and ignores `[#issue] type:` |
| gstack `/land-and-deploy`, `/canary` | our GitOps pipeline | Deployment is Argo CD, pull-based from the GitOps repo — don't let a skill deploy |
| `ce-code-review` | our `code-reviewer` | Duplicate perspective, and doesn't know the Konsist rules |
| `ce-resolve-pr-feedback` | our `resolve-review` | |
| `ce-brainstorm`, superpowers `brainstorming`, gstack `/office-hours` | `/grill-me` | Four skills doing the same job — pick one |
| `ce-work` | superpowers TDD | `ce-work` just executes the plan, with no RED-first loop |
| superpowers `using-git-worktrees` | — | A worktree under `~/Documents` risks iCloud `' 2'` duplication |
| gstack `/qa`, `/browse`, `/design-*`, `/scrape`, `/ios-*` | — | Browser/design/iOS skills; this repo is backend-only |
| gstack `/investigate` | superpowers `systematic-debugging` | Duplicate — use one debugging methodology |

### Suppressing Auto-Triggers

**Suppression is per-user, so this table is the shareable part.** The suites are user-scoped installs, which means a teammate's inventory differs from yours — plugin enablement and the per-skill switches therefore live in `.claude/settings.local.json` (per-user, not committed), and only repo-level facts go in `.claude/settings.json`. Each person applies the list below once on their own machine; `references/token-budget.md` has the exact switches and the measured cost of each suite.

- **gstack: keep 6 of 54.** The rest are switched off per skill. `gstack-config set proactive false` additionally stops gstack from auto-invoking or proactively suggesting a skill; an explicit `/plan-eng-review` still works exactly as before.
- **The three suites we route to can't be trimmed per skill.** `skillOverrides` **does not apply to plugin skills**, so superpowers, mattpocock-skills and compound-engineering are all-or-nothing — and we route to two skills in each, so they stay on. That means superpowers `brainstorming` (*"MUST be used before any creative work"*) and the model-invoked `ce-*` skills stay loaded and can fire even mid-`/new-domain` or `/new-feature`.

So if one of those three activates a skill outside the routing table above, **stop and follow the table instead** — the table is the authority, not the skill's own description. That instruction is the only enforcement here, which is why it's in the body of this skill rather than a reference.

## Standard Feature-Development Flow

```
/grill-me                 → requirements become clear
  ↓
/ce-plan                  → a plan with file paths + verification steps
  ↓
/goal <pass condition>     ┐ Stop hook — keeps the session from ending before the condition
/plan-eng-review          │ transaction boundaries, port contracts, module placement
/plan-ceo-review          │ scope: is this the smallest shippable unit?
  ↺ revise & re-review     ┘ /goal auto-releases once the condition is met
  ↓
superpowers TDD            → RED → GREEN → REFACTOR
  + test bodies from our test-writer agent
  ↓
our code-reviewer agent    → hexagonal / Konsist / security
/review (gstack)          → production-bug perspective
  ↓
/run-checks               → ktlintFormat → ktlintCheck → architecture-test → test
  ↓
our commit-push-pr         → [#issue] type: description
  ↓
/ce-compound               → docs/solutions/<slug>.md
```

Feel free to skip phases for small changes. A one-line fix only needs `/run-checks` and `commit-push-pr`.

## References

| Open when | File |
|-----------|------|
| Setting up a machine, onboarding a teammate, or upgrading gstack (incl. pinning a reviewed revision) | `references/installation.md` |
| Session context is tight, or deciding whether a suite earns its cost | `references/token-budget.md` |
| An external skill is about to run a build, write production code, or open a worktree | `references/hazards.md` |
| Starting phase 3 — writing a `/goal` condition that can actually close | `references/goal-loop.md` |
| Phase 10 — deciding whether a lesson belongs in `docs/solutions/` | `references/compounding.md` |
