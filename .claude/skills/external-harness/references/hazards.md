# Project-Specific Hazards

Detailed reference for the `external-harness` skill. These are the places where an external suite's default behavior collides with this repo. Read the relevant entry before letting an external skill run a build, write production code, or open a worktree.

---

## 1. The ktlint Hook Conflicts with the TDD Loop

`PostToolUse(Edit|Write)` runs `ktlintFormat` on **every edit**. This affects superpowers TDD in two ways:

- Every RED→GREEN step pays a Gradle-invocation cost — **~1 second**, down from ~2.7s. The hook resolves the Gradle project from the edited file's path and formats only that module instead of all 51, but most of the remaining time is Gradle's own startup/config cost (even a bare `gradlew help` takes ~1.0s here), so it's close to the floor — the fix is fewer invocations, not a faster one. When the path can't be resolved (e.g. `buildSrc/`), it falls back to formatting the whole root.
- **An edit that only adds an `import` has that import deleted** — ktlint judges it unused. Always put the import and its first use in the **same** edit. Module scoping only shrinks the blast radius; it doesn't remove the problem, which is inherent to formatting mid-edit.

## 2. iCloud `' 2'` Duplicates Break the Build

This repo lives under `~/Documents`, which syncs via iCloud. iCloud resolves a conflict by writing a duplicate next to the original (`name 2.ext`), and a duplicated source file breaks `buildSrc`/Gradle compilation — the same class gets picked up twice.

Before an external skill runs a build:

```bash
find . \( -name "* 2.*" -o -name "* 2" \)        # list first
find . \( -name "* 2.*" -o -name "* 2" \) -exec rm -rf {} +
```

Both predicates must be wrapped in parentheses. Without them, `-exec` binds only to the trailing `-name "* 2"`, so `* 2.*` files get listed but never deleted — and the listing line still looks correct, which makes the failure hard to notice.

Deletion is hard to undo — before running the second command, eyeball the first command's output for anything that isn't actually a stale iCloud duplicate. If the build is already broken, `rm -rf build` and rebuild.

## 3. The Iron Law Needs an Exception Here

superpowers TDD states *"never write production code without a failing test."* The following are legitimate exceptions in this repo — write them directly:

- `*JpaEntity` Java classes (schema declarations, no behavior)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
- `application*.yaml` profile config
- Konsist rules under `:architecture-test` (they *are* the tests)

Domain entities, use-case services, and adapters have **no exception** — tests come first.

## 4. Language

All external suites answer in English by default. This repo's user-facing output is entirely in Korean.

## 5. gstack Installs a User-Level Stop Hook

Covered in `references/installation.md` — `./setup` registers a `timeline-stop-hook` in `~/.claude/settings.json`, so two Stop hooks run (ours plus gstack's). They add up rather than conflict.

## 6. Plan Review Needs Our Context

`/plan-eng-review` knows nothing about our constraints. When invoking it, state them up front:

- hexagonal module boundaries
- `@CommandService`/`@QueryService` transaction semantics
- `shared` ports for cross-domain access
- `Uuid.generateV7()` PKs

Otherwise it just reviews as a generic Spring app.

## 7. Worktrees Under `~/Documents`

superpowers `using-git-worktrees` is banned for the same reason as hazard 2: a worktree created under the iCloud-synced tree invites `' 2'` duplication across two checkouts of the same module, and the resulting build break is confusing to diagnose.
