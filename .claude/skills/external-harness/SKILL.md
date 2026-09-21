---
name: external-harness
description: Load when using the externally installed skill suites (gstack / superpowers / compound-engineering / mattpocock-skills). Phase-by-phase routing, which external skills are banned because this repo's own harness wins, and project-specific hazards.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# External Harness Stacks

Four external skill suites are installed alongside this repo's own harness. They overlap heavily — with each other **and** with our `.claude/` skills. This document is the **single routing authority**: for each phase, exactly one skill is adopted and the rest are banned.

## Core Rule

> **Our harness wins wherever it already has a skill.** External suites do not know hexagonal boundaries, Konsist rules, the `CommonResponse` envelope, or our Korean commit convention. Adopt an external skill only where we have a genuine gap.

## Installed Stacks

| Suite | Source | Scope | Prefix |
|-------|--------|-------|--------|
| mattpocock-skills | `claude-plugins-official` | user | `/grill-me`, `/wayfinder`, … |
| superpowers | `obra/superpowers` (`superpowers-dev`) | user | auto-invoked skill names |
| compound-engineering | `EveryInc/compound-engineering-plugin` | user | `/ce-*`, `/lfg` |
| gstack | `~/.claude/skills/gstack` (vendored clone) | user | `/plan-*-review`, `/review`, … |

**These are user-scoped — git does NOT share them.** A teammate cloning this repo gets our `.claude/` but none of the four. Each teammate runs this once:

```bash
claude plugin marketplace add obra/superpowers
claude plugin marketplace add EveryInc/compound-engineering-plugin
claude plugin install superpowers@superpowers-dev
claude plugin install compound-engineering@compound-engineering-plugin
claude plugin install mattpocock-skills@claude-plugins-official

# gstack is not a marketplace plugin — it vendors itself
brew install bun   # gstack requires Bun v1.0+
git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git \
  ~/.claude/skills/gstack && cd ~/.claude/skills/gstack && ./setup
```

Until a teammate does this, every external skill below is simply unavailable to them — our own harness must remain sufficient on its own.

### Session token cost

Every enabled skill's name and description load into **every** session, whether used or not (`claude plugin details <name>` prints the figure for a plugin).

| Suite | Skills | Enabled | Always-on | Toggle per skill? |
|-------|-------:|--------:|----------:|-------------------|
| gstack | 54 | **5** | **~450 tok** | ✅ yes |
| compound-engineering | 36 | 36 | ~2,990 tok | ❌ no |
| mattpocock-skills | 25 | 25 | ~1,610 tok | ❌ no |
| superpowers | 15 | 15 | ~840 tok | ❌ no |
| **Total** | **130** | **81** | **~5,890 tok** | |

**`/skills` only toggles skills that live in `~/.claude/skills/`.** gstack qualifies (it is a vendored clone, not a plugin), so its 49 unused skills are off — that alone cut ~5,200 tok. The other three are plugin-provided and `/skills` does not expose them individually; for those the only lever is `claude plugin disable <plugin>`, which takes the whole suite including the skills we route to.

The toggle leaves files untouched — it only stops them loading, so it is reversible and does not fight `/gstack-upgrade`. **Never** delete skill directories to achieve this.

**Remaining stance: pay the ~5.9k.** The worst ratio left is compound-engineering — ~2,990 tok for two skills. If session context ever gets tight, the move is to disable that plugin and reimplement `/ce-compound` as a `compound` skill in this repo, which is the "our harness wins" rule applied one step further.

The toggle is **user-scoped and not stored in this repo** — it applies across all of that person's projects, and `git clone` does not carry it (a search of `~/.claude/`, `~/.claude.json`, `~/.config/`, and `~/Library/` found no local file recording it, so it is held per-account rather than per-repo). **This table is therefore the shareable artifact**: each teammate runs `/skills` once and matches it.

Leave exactly these gstack skills on and turn the other 49 off:

```
_gstack-command   gstack   plan-eng-review   plan-ceo-review   review
```

(`_gstack-command` and `gstack` are the routers — the three review skills do not load without them.) Verify the result with a throwaway session rather than trusting the UI:

```bash
claude -p "Do not use any tools. Output ONLY the names of skills whose source is gstack, one per line."
```

For the three plugin suites nothing can be toggled, so their skills stay loaded whether we route to them or not. We route to: `ce-compound`, `ce-plan`; `grill-me`, `wayfinder`; `test-driven-development`, `subagent-driven-development`, `systematic-debugging`, `verification-before-completion` (this last one because `systematic-debugging` hands off to it directly). **Everything else in those three suites is banned by the routing table below, not by a switch** — which is why that table has to be enforced by reading it.

## Phase Routing

| # | Phase | Use | Why |
|---|-------|-----|-----|
| 1 | Sharpening vague requirements | `/grill-me` (mattpocock) | **Gap.** We had nothing that interrogates intent before design |
| 1b | Work spanning multiple sessions | `/wayfinder` (mattpocock) | Decision-ticket map on the issue tracker |
| 2 | Writing the plan | `/ce-plan` | Produces file paths + verification steps per task |
| 3 | **Reviewing the plan** | `/goal` wrapping `/plan-eng-review` → `/plan-ceo-review` (gstack) | **Gap.** Catches transaction-boundary / port-contract mistakes *before* code. `/goal` is what forces the revise-and-recheck loop to actually converge — see below |
| 4 | Implementing | `test-driven-development` (superpowers) | RED-GREEN-REFACTOR, enforced |
| 4b | Plan with many independent tasks | `subagent-driven-development` (superpowers) | Fresh subagent per task, review between each |
| 4c | Debugging a defect | `systematic-debugging` (superpowers) | **Gap.** Our `test-validator` only diagnoses *failing tests*, not open-ended bugs. Do not also run gstack `/investigate` — same job |
| 5 | Writing the tests themselves | **our `test-writer` agent** | Only ours knows per-layer patterns + TestContainer |
| 6 | Code review | **our `code-reviewer` agent** first, then `/review` (gstack) | Ours enforces hexagonal/Konsist; gstack adds a production-bug lens |
| 7 | Pre-PR verification | **our `/run-checks`** | Deterministic script, never LLM judgment |
| 8 | Commit / push / PR | **our `commit-push-pr`** | Korean `[#issue] type:` convention |
| 9 | Applying PR feedback | **our `resolve-review`** | |
| 10 | Recording the learning | `/ce-compound` → `docs/solutions/` | **Gap.** Repo-local, so the team inherits it |

Phases 3 and 10 are the two that justify this whole stack. Phase 1 is the third.

## Closing the Plan-Review Loop with `/goal`

Phase 3 is a **loop**, not a step: review finds problems → revise the plan → review again. Nothing makes that loop repeat on its own, and the failure mode is silent — one review pass, a few findings acknowledged but not fixed, and implementation starts on a plan that never actually converged.

`/goal <condition>` is the forcing function. It is a Claude Code built-in that registers a **session-scoped Stop hook**: the session cannot end until the condition holds, and it auto-clears once satisfied.

Set it **before** the first review:

```
/goal plan-eng-review와 plan-ceo-review가 blocking finding 없이 통과하고, 지적된 항목이 모두 계획에 반영될 때까지
```

### Writing a condition that terminates

The condition must be something you can objectively declare satisfied. Vague conditions produce a session that refuses to stop.

| | |
|---|---|
| ❌ | `계획이 좋아질 때까지` — no observable end state |
| ❌ | `모든 리스크가 사라질 때까지` — unsatisfiable |
| ✅ | `두 리뷰가 blocking finding 없이 통과할 때까지` |
| ✅ | `리뷰 지적사항이 모두 계획에 반영되거나 명시적으로 기각될 때까지` |

**Escape hatch:** if a condition turns out to be unreachable, clear the goal explicitly rather than fighting the hook. Do not weaken the plan just to satisfy a badly written condition — rewrite the condition.

### Scope limits

- **Session-scoped.** A restart loses it. Work spanning multiple sessions belongs to `/wayfinder` (phase 1b) instead; `omc ultragoal` exists to add a durable ledger on top of `/goal` if we ever need that.
- **It composes with our Stop hooks.** `stop-format.sh` (`ktlintFormat`) and gstack's timeline hook both still run on every stop attempt — `/goal` adds a third gate, it does not replace them.
- **Phase 3 only.** Do not wrap TDD in `/goal`: superpowers already enforces RED-first, and `/run-checks` is the deterministic gate for phase 7. Stacking a Stop hook on a step that already has a hard gate just makes failures harder to exit.

## Banned External Skills

Do **not** invoke these, and do not let them auto-trigger.

| Banned | Collides with | Reason |
|--------|---------------|--------|
| `ce-commit`, `ce-commit-push-pr`, gstack `/ship` | our `commit-push-pr` | They write English commit messages and ignore `[#issue] type:` |
| gstack `/land-and-deploy`, `/canary` | our GitOps pipeline | Deployment is Argo CD pull-based from the GitOps repo — never let a skill deploy |
| `ce-code-review` | our `code-reviewer` | Duplicate lens, unaware of Konsist rules |
| `ce-resolve-pr-feedback` | our `resolve-review` | |
| `ce-brainstorm`, superpowers `brainstorming`, gstack `/office-hours` | `/grill-me` | Four skills for one job — pick one |
| `ce-work` | superpowers TDD | `ce-work` executes plans without the RED-first loop |
| superpowers `using-git-worktrees` | — | Worktrees under `~/Documents` trigger the iCloud `' 2'` duplication hazard |
| gstack `/qa`, `/browse`, `/design-*`, `/scrape`, `/ios-*` | — | Browser/design/iOS skills; this is a backend-only repo |
| gstack `/investigate` | superpowers `systematic-debugging` | Duplicate — one debugging methodology only |

### Suppressing auto-triggers

gstack's 51 unused skills are suppressed **mechanically**, not by instruction:

```bash
gstack-config set proactive false   # already applied
```

With `PROACTIVE=false`, gstack never auto-invokes or proactively suggests a skill — explicit `/plan-eng-review` still works exactly as before. This is the deterministic-over-judgment rule: a config flag beats a paragraph asking the agent to restrain itself.

The other three suites have **no equivalent** — no config flag, and `/skills` cannot toggle plugin skills. So superpowers `brainstorming` (*"You MUST use this before any creative work"*) and the model-invoked `ce-*` skills stay loaded and will still fire during `/new-domain` and `/new-feature`.

That leaves them governed by instruction alone: when one activates outside the routing table above, **stop and follow the table instead** — the table is the authority, not the skill's own description. This is the weakest link in the whole setup, because it is the only rule here with no mechanism behind it.

## Project-Specific Hazards

### 1. The ktlint hook fights the TDD loop

`PostToolUse(Edit|Write)` runs `ktlintFormat` on **every single edit**. Two consequences for superpowers TDD:

- Each RED→GREEN step pays a Gradle invocation — **~0.95s**, of which ~1.0s is Gradle's configuration floor. The hook resolves the edited file's Gradle project from its path and formats only that module; formatting all 51 modules took ~2.7s. Unresolvable paths (e.g. `buildSrc/`) fall back to a root format.
- **An edit that adds only an `import` gets it deleted** — ktlint sees it unused. Always put the import and its first usage in the **same** edit. Module scoping narrows the blast radius but does not remove this; it is inherent to formatting mid-edit.

### 2. iCloud `' 2'` duplicates break the build

This repo lives under `~/Documents` (iCloud-synced). Before any external skill runs a build:

```bash
find . -name "* 2.*" -o -name "* 2"        # inspect the list FIRST
find . -name "* 2.*" -o -name "* 2" -exec rm -rf {} +
```

### 3. The Iron Law needs exceptions here

superpowers TDD states *"NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST"*. In this repo the following are legitimate exceptions — write them directly:

- `*JpaEntity` Java classes (schema declarations, no behavior)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
- `application*.yaml` profile config
- Konsist rules in `:architecture-test` (they *are* the test)

Domain entities, use-case services, and adapters have **no exception** — test first.

### 4. Language

Every external suite answers in English by default. All user-facing output in this repo is Korean.

### 5. gstack installs a user-level Stop hook

`./setup` appends a `timeline-stop-hook` to `~/.claude/settings.json` (it backs the file up as `settings.json.bak.<ts>` first). It does **not** touch this repo's `.claude/settings.json` — verified. So two Stop hooks now run: ours (`stop-format.sh` → `ktlintFormat`) and gstack's. They are additive, not conflicting.

To remove gstack's:

```bash
~/.claude/skills/gstack/bin/gstack-settings-hook remove-source --source gstack-timeline-stop
```

gstack also registers **50+ skills** into every session's context. If session token cost becomes a problem, that is the first thing to cut — `claude plugin disable` does not apply (gstack is a vendored clone, not a plugin), so remove `~/.claude/skills/gstack` instead.

### 6. Plan review needs our context

`/plan-eng-review` knows nothing about our constraints. When invoking it, state up front: hexagonal module boundaries, `@CommandService`/`@QueryService` transaction semantics, `shared` ports for cross-domain access, and `Uuid.generateV7()` PKs. Otherwise it reviews a generic Spring app.

## Standard Feature Flow

```
/grill-me                 → requirements are unambiguous
  ↓
/ce-plan                  → plan with file paths + verification
  ↓
/goal <통과 조건>          ┐ Stop hook — 세션이 조건 전에 끝나지 못하게 막는다
/plan-eng-review          │ transaction boundaries, port contracts, module placement
/plan-ceo-review          │ scope: is this the smallest thing that ships?
  ↺ revise & re-review    ┘ 조건 충족 시 /goal 자동 해제
  ↓
superpowers TDD           → RED → GREEN → REFACTOR
  + our test-writer agent for the test bodies
  ↓
our code-reviewer agent   → hexagonal / Konsist / security
/review (gstack)          → production-bug lens
  ↓
/run-checks               → ktlintFormat → ktlintCheck → architecture-test → test
  ↓
our commit-push-pr        → [#issue] type: 설명
  ↓
/ce-compound              → docs/solutions/<slug>.md
```

Skip phases freely for small changes. A one-line fix needs `/run-checks` and `commit-push-pr`, nothing else.

## `docs/solutions/`

`/ce-compound` writes repo-local learnings here. This is deliberately **not** the same place as the per-user memory under `~/.claude/` — that one is invisible to teammates. A learning qualifies when the reasoning is **absent from the final code and tests**: why a boundary sits where it does, which approach failed and why, a framework behavior that surprised us.

Past examples that would have qualified: `@CommandService` (REQUIRED propagation) silently defeating a `TransactionalStore` pattern; keeping AI calls outside the transaction via an orchestrator + `<Aggregate>TransactionalStore`.

Do not compound routine fixes whose diff already explains itself.
