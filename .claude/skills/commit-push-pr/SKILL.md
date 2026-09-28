---
name: commit-push-pr
description: Load when committing per project conventions and (on request) pushing and creating a PR. Auto-applies commit message format, branch strategy, PR rules.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Commit · Push · PR

Perform commit → push → PR creation per project convention (`git-workflow`).

## 1. Pre-commit Verification (required)

```bash
./gradlew ktlintFormat   # auto-fix style
./gradlew ktlintCheck    # verify it passes
```

- **Never commit `.env`**. Check staged files with `git status`.
- **Keep commits small and atomic** — one concern per commit. Split by `type` and by module/layer, and use `git add -p` when a file mixes concerns. See the "Commit granularity" section of the `git-workflow` skill for details.

## 2. Commit

Format: `[#issue-number] type: description` (**in Korean**). Example: `[#122] feat: 멤버 기능 추가`
Follow the `git-workflow` skill for the type list and branch strategy.

## 3. Push (only when the user explicitly requests it)

- **Never push until the user explicitly asks for it.**
- **Never use `git push --force`.**
- New features are worked on in a `feat/#issue-number` branch cut from `develop`. Never push directly to `main`/`develop`.
- The pre-push gate (ktlintCheck + Konsist) runs automatically on push — it blocks on failure, so pass it first.

## 4. Pre-PR Module Documentation Check

Before creating the PR, catch the case where this branch changed a domain module's structure but that domain's `module-{domain}/CLAUDE.md` was left untouched.

1. Mechanically check which domain modules changed in this branch:
   ```bash
   git diff develop...HEAD --name-only -- 'module-*/'
   ```
2. For each domain, judge whether this change is **structural** — a new entity/aggregate, an added or removed `shared` port, a shift in the responsibility boundary (owns/does-not-own), or a new decision/trap worth recording (a lock strategy, a transaction-boundary change, etc.). A change that's only internal logic, a new private method, a bug fix, or added tests alone doesn't count.
3. If the change looks structural and the corresponding `module-{domain}/CLAUDE.md` isn't among this diff's changed files, confirm with the user before creating the PR: "Should I draft `module-{domain}/CLAUDE.md` now, or create the PR as-is?" Proceed based on the user's choice — never skip this silently or force it through.

Template and authoring rules: `.claude/examples/domain-claude-md.md`.

## 5. Create the PR

```bash
gh pr create --base develop --title "[#issue-number] [Type] description" --body "..."
```

- **The PR always targets `develop`** (never `main`).
- PR title format: `[#issue-number] [Type] description` — the `[Type] description` part uses the working branch's **issue title verbatim** (just prepend `[#issue-number] `). This is **different** from the commit message format — the type is an uppercase word in brackets (`[Feature]`, `[Fix]`, `[Refactor]`, `[Chore]`, `[Docs]`, `[Test]`, `[Performance]`), not the `type:` form. See the `git-workflow` skill for the full tag → commit-type mapping. Example: issue `[Feature] JwtAuthenticationFilter 추가 및 Security 설정` → PR `[#4] [Feature] JwtAuthenticationFilter 추가 및 Security 설정`.
- The body follows the `.github/PULL_REQUEST_TEMPLATE.md` structure: ✅ PR type / ✏️ Description of work / 🔗 Related issue (`closes #issue-number`) / 💡 Additional notes.
- **Attaching a screenshot or test results is required** (per the template). If you can't attach one, at least leave the test-pass log in the body.
- **Never merge your own PR** — it needs code review. Create it only, don't merge.
