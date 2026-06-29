---
name: commit-push-pr
description: Load when committing per project conventions and (on request) pushing and creating a PR. Auto-applies commit message format, branch strategy, PR rules.
---

# Commit · Push · PR

Perform commit → push → PR creation per the project conventions (`git-workflow`).

## 1. Pre-Commit Verification (required)

```bash
./gradlew ktlintFormat   # auto-tidy style
./gradlew ktlintCheck    # confirm it passes
```

- **Never commit `.env`**. Check staged files with `git status`.
- Stage changes split into logical units.

## 2. Commit

Format: `[#issue-number] type: description` (**in Korean**). e.g. `[#122] feat: 멤버 기능 추가`
The type list and branch strategy follow the `git-workflow` skill.

## 3. Push (only when the user explicitly requests it)

- **Never push until the user requests a push.**
- **Never use `git push --force`.**
- Work on a `feat/#issue-number` branch forked from `develop` for new features. Do not push directly to `main`/`develop`.
- On push, the pre-push gate (ktlintCheck + Konsist) runs automatically — it blocks on failure, so pass it first.

## 4. Create PR

```bash
gh pr create --base develop --title "[#issue-number] type: description" --body "..."
```

- **The PR target is always `develop`** (not main).
- Make the PR title identical to the commit message format.
- The body follows the `.github/PULL_REQUEST_TEMPLATE.md` structure: ✅ PR type / ✏️ work done / 🔗 related issue (`closes #issue-number`) / 💡 additional notes.
- **Attaching screenshots or test results is required** (per the template). If you cannot attach them, at least leave a test-pass log in the body.
- **Do not merge your own PR** — code review required. Only create it; do not merge.