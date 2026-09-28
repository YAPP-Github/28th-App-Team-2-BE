---
name: external-harness
description: Load when using the externally installed skill suites (gstack / superpowers / compound-engineering / mattpocock-skills). Phase-by-phase routing, which external skills are banned because this repo's own harness wins, and project-specific hazards.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# External Harness Stack

Four external skill suites are installed alongside this repo's own harness. They overlap **substantially** with each other, and with our own `.claude/` skills. This document is the **single routing authority**: it adopts exactly one skill per phase and bans the rest.

## Core Rule

> **Wherever our own harness already has a skill, our harness always wins.** The external suites don't know our hexagonal boundaries, Konsist rules, `CommonResponse` envelope, or our Korean commit convention. Adopt an external skill only where we have a genuine gap.

## Installed Stack

| Suite | Source | Scope | Prefix |
|-------|--------|-------|--------|
| mattpocock-skills | `claude-plugins-official` | per-user | `/grill-me`, `/wayfinder`, … |
| superpowers | `obra/superpowers` (`superpowers-dev`) | per-user | auto-invoked skill names |
| compound-engineering | `EveryInc/compound-engineering-plugin` | per-user | `/ce-*`, `/lfg` |
| gstack | `~/.claude/skills/gstack` (vendored clone) | per-user | `/plan-*-review`, `/review`, … |

**These are user-scoped — git doesn't share them.** A teammate who clones this repo gets our `.claude/`, but not these four. Each teammate runs the following once:

```bash
claude plugin marketplace add obra/superpowers
claude plugin marketplace add EveryInc/compound-engineering-plugin
claude plugin install superpowers@superpowers-dev
claude plugin install compound-engineering@compound-engineering-plugin
claude plugin install mattpocock-skills@claude-plugins-official

# gstack is not a marketplace plugin — it's self-vendored
brew install bun   # gstack needs Bun v1.0+
git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git \
  ~/.claude/skills/gstack && cd ~/.claude/skills/gstack && ./setup
```

> **These commands run third-party code under your user account.** Plugin installs pull a pinned version through the marketplace, but the gstack line clones a mutable default branch and runs `./setup` immediately — no commit pinning, signing, or checksum verification at all. `./setup` writes to `~/.claude/settings.json` (it registers a Stop hook). Before running it, either read the upstream `setup` script, or pin a reviewed revision with `--branch <tag-or-sha>` instead of the default branch. Treat a gstack upgrade the same way.

Until a teammate does this, none of the external skills below are available to them — our own harness needs to be self-sufficient on its own.

### Session Token Cost

Every activated skill's name and description load into **every** session regardless of whether it's used (`claude plugin details <name>` shows per-plugin figures).

| Suite | Skills | Active | Always loaded | Per-skill toggle? |
|-------|-------:|--------:|----------:|-------------------|
| gstack | 54 | **5** | **~450 tokens** | ✅ possible |
| compound-engineering | 36 | 36 | ~2,990 tokens | ❌ not possible |
| mattpocock-skills | 25 | 25 | ~1,610 tokens | ❌ not possible |
| superpowers | 15 | 15 | ~840 tokens | ❌ not possible |
| **Total** | **130** | **81** | **~5,890 tokens** | |

**`/skills` only toggles skills under `~/.claude/skills/`.** gstack qualifies (it's a vendored clone, not a plugin), so its 49 unused skills are switched off — that alone saves ~5,200 tokens. The other three are plugin-provided, so `/skills` doesn't expose them individually; the only lever for those is `claude plugin disable <plugin>`, which turns off the whole suite, including the skills we route to.

Toggling doesn't touch any files — it only stops loading, so it's reversible and doesn't conflict with `/gstack-upgrade`. Never delete the skill directories for this reason.

**Current stance: eat the ~5.9k cost.** compound-engineering has the worst ratio — ~2,990 tokens for 2 skills. If session context gets tight, the next move is to disable that plugin and reimplement `/ce-compound` as this repo's own `compound` skill — one more application of the "our harness wins" rule.

Toggles are **user-scoped and not stored in this repo** — they apply across that person's every project, and `git clone` doesn't carry them (searching `~/.claude/`, `~/.claude.json`, `~/.config/`, `~/Library/` found no local file that records this — it's kept per-account, not per-repo). **That's why this table is the shareable artifact**: each teammate runs `/skills` once and matches it.

Of gstack's skills, keep only the following on and switch off the other 49:

```
_gstack-command   gstack   plan-eng-review   plan-ceo-review   review
```

(`_gstack-command` and `gstack` are routers — without them, the three review skills won't load.) Don't trust the UI; verify with a one-off session:

```bash
claude -p "Do not use any tools. Output ONLY the names of skills whose source is gstack, one per line."
```

The three plugin suites have no toggle, so their skills stay loaded whether we route to them or not. What we route to: `ce-compound`, `ce-plan`; `grill-me`, `wayfinder`; `test-driven-development`, `subagent-driven-development`, `systematic-debugging`, `verification-before-completion` (the last one is included because `systematic-debugging` hands off to it directly). **Everything else in those three suites is banned by the routing table below, not by a switch** — so that table has to be read and enforced.

## Phase-by-Phase Routing

| # | Phase | Use | Why |
|---|-------|-----|-----|
| 1 | Clarifying ambiguous requirements | `/grill-me` (mattpocock) | **Gap.** We had no tool to interrogate intent before design |
| 1b | Work spanning multiple sessions | `/wayfinder` (mattpocock) | A decision-ticket map on the issue tracker |
| 2 | Writing a plan | `/ce-plan` | Produces per-task file paths + verification steps |
| 3 | **Plan review** | `/plan-eng-review` → `/plan-ceo-review` (gstack) wrapped in `/goal` | **Gap.** Catches transaction-boundary/port-contract mistakes *before* code is written. `/goal` is the mechanism that forces the revise-and-re-review loop to actually converge — see below |
| 4 | Implementation | `test-driven-development` (superpowers) | Enforces RED-GREEN-REFACTOR |
| 4b | A plan with many independent tasks | `subagent-driven-development` (superpowers) | A fresh subagent per task, reviewed each time |
| 4c | Debugging a defect | `systematic-debugging` (superpowers) | **Gap.** Our `test-validator` only diagnoses *failing tests*, not open-ended bugs. Don't also run gstack `/investigate` — same role |
| 5 | Writing test code | **our `test-writer` agent** | Only ours knows the per-layer patterns + TestContainer |
| 6 | Code review | **our `code-reviewer` agent** first, then `/review` (gstack) | Ours enforces hexagonal/Konsist; gstack adds a production-bug perspective |
| 7 | Pre-PR verification | **our `/run-checks`** | Deterministic script, not LLM judgment |
| 8 | Commit / push / PR | **our `commit-push-pr`** | Korean `[#issue] type:` convention |
| 9 | Applying PR feedback | **our `resolve-review`** | |
| 10 | Recording what was learned | `/ce-compound` → `docs/solutions/` | **Gap.** Repo-local, so the whole team inherits it |

Phases 3 and 10 are what justify this whole stack. Phase 1 is next.

## Closing the Plan-Review Loop with `/goal`

Phase 3 isn't a single step — it's a **loop**: review finds problems → plan gets revised → review again. Nothing forces this loop to actually repeat, and the failure mode is quiet — one review, a few findings acknowledged but not fixed, and implementation starts on a plan that never actually converged.

`/goal <condition>` is the mechanism that enforces this. A Claude Code built-in feature that registers a **session-scoped Stop hook**: the session can't end until the condition is met, and it auto-releases once it is.

Set it **before** the first review:

```
/goal until plan-eng-review and plan-ceo-review pass with no blocking findings, and every finding raised has been folded into the plan
```

### Writing a Condition That Can Actually Close

The condition has to be something whose satisfaction can be objectively declared. A vague condition produces a session that never stops.

| | |
|---|---|
| ❌ | `until the plan is good` — no observable end state |
| ❌ | `until every risk is gone` — unsatisfiable |
| ✅ | `until both reviews pass with no blocking findings` |
| ✅ | `until every review finding is either folded into the plan or explicitly dismissed` |

**The escape hatch:** if a condition turns out to be unreachable, don't fight the hook — explicitly release the goal. Don't weaken the plan to satisfy a badly written condition; rewrite the condition instead.

### Scope Limits

- **Session-scoped.** It disappears on restart. Work spanning multiple sessions belongs to `/wayfinder` (phase 1b) instead of `/goal`; if you need that kind of persistent ledger, there's `omc ultragoal`, which layers on top of `/goal`.
- **Works alongside our Stop hooks.** Both `stop-format.sh` (`ktlintFormat`) and gstack's timeline hook still run on every exit attempt — `/goal` doesn't replace them, it just adds a third gate.
- **Phase 3 only.** Don't wrap TDD in `/goal`: superpowers already enforces RED-first, and phase 7's deterministic gate is `/run-checks`. Stacking another Stop hook onto a phase that already has a strong gate only makes it harder to escape a failure.

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

gstack's 49 unused skills are suppressed **mechanically**, not by instruction:

```bash
gstack-config set proactive false   # already applied
```

With `PROACTIVE=false`, gstack never auto-invokes or proactively suggests a skill — an explicit `/plan-eng-review` still works exactly as before. This is the "determinism over judgment" rule in practice: one config flag beats a paragraph asking the agent to hold back.

The other three suites have **no equivalent lever** — no config flag, and `/skills` can't toggle plugin skills. So superpowers `brainstorming` (*"MUST be used before any creative work"*) and the model-invoked `ce-*` skills stay loaded and can fire even mid-`/new-domain` or `/new-feature`.

As a result, these are controlled by instruction alone: if one activates outside the routing table above, **stop and follow the table instead** — the table is the authority, not the skill's own description. This is the weakest link in this whole setup, because it's the one rule with no mechanism backing it up.

## Project-Specific Hazards

### 1. The ktlint hook conflicts with the TDD loop

`PostToolUse(Edit|Write)` runs `ktlintFormat` on **every edit**. This affects superpowers TDD in two ways:

- Every RED→GREEN step pays a Gradle-invocation cost — **~1 second**, down from ~2.7s. The hook resolves the Gradle project from the edited file's path and formats only that module instead of all 51, but most of the remaining time is Gradle's own startup/config cost (even a bare `gradlew help` takes ~1.0s here), so it's close to the floor — the fix is fewer invocations, not a faster one. When the path can't be resolved (e.g. `buildSrc/`), it falls back to formatting the whole root.
- **An edit that only adds an `import` has that import deleted** — ktlint judges it unused. Always put the import and its first use in the **same** edit. Module scoping only shrinks the blast radius; it doesn't remove the problem, which is inherent to formatting mid-edit.

### 2. iCloud `' 2'` duplicates break the build

This repo lives under `~/Documents`, which syncs via iCloud. Before an external skill runs a build:

```bash
find . \( -name "* 2.*" -o -name "* 2" \)        # list first
find . \( -name "* 2.*" -o -name "* 2" \) -exec rm -rf {} +
```

### 3. The Iron Law needs an exception here

superpowers TDD states *"never write production code without a failing test."* The following are legitimate exceptions in this repo — write them directly:

- `*JpaEntity` Java classes (schema declarations, no behavior)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
- `application*.yaml` profile config
- Konsist rules under `:architecture-test` (they *are* the tests)

Domain entities, use-case services, and adapters have **no exception** — tests come first.

### 4. Language

All external suites answer in English by default. This repo's user-facing output is entirely in Korean.

### 5. gstack installs a user-level Stop hook

`./setup` adds a `timeline-stop-hook` to `~/.claude/settings.json` (backing it up to `settings.json.bak.<ts>` first). This repo's `.claude/settings.json` is **not** touched — verified. So two Stop hooks now run: ours (`stop-format.sh` → `ktlintFormat`) and gstack's. They don't conflict, they just add up.

To remove gstack's:

```bash
~/.claude/skills/gstack/bin/gstack-settings-hook remove-source --source gstack-timeline-stop
```

gstack also registers **50+ skills** into every session's context. If session token cost becomes a problem, that's the first thing to cut — `claude plugin disable` doesn't apply (gstack is a vendored clone, not a plugin), so remove `~/.claude/skills/gstack` instead.

### 6. Plan review needs our context

`/plan-eng-review` knows nothing about our constraints. When invoking it, state them up front: hexagonal module boundaries, `@CommandService`/`@QueryService` transaction semantics, `shared` ports for cross-domain access, `Uuid.generateV7()` PKs. Otherwise it just reviews as a generic Spring app.

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

## `docs/solutions/`

`/ce-compound` records what was learned here, repo-local. This is deliberately **separate** from the per-user memory under `~/.claude/` — that's invisible to teammates. What earns a spot here is reasoning that **doesn't survive in the final code and tests**: why a boundary sits where it does, which approach failed and why, a framework behavior that surprised us.

Past examples that would have belonged here: `@CommandService` (REQUIRED propagation) silently defeating the `TransactionalStore` pattern; keeping an AI call outside the transaction via the orchestrator + `<Aggregate>TransactionalStore` split.

Don't compound routine changes the diff already explains on its own.
