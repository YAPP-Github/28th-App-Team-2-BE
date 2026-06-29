---
name: git-workflow
description: Load when branching/committing/creating PRs. Branch strategy, commit message format (Korean), PR rules, Claude Code Git rules.
---

# Git Workflow Rules

## Branch Strategy

| Branch | Purpose | PR target |
|--------|---------|-----------|
| `main` | Production deploy | — |
| `develop` | Integration | main |
| `feat/#issue-number` | Feature development | develop |

When starting a new feature: branch `feat/#issue-number` off the `develop` branch.
Always include the issue number in the branch name.

## Commit Messages

Format: `[#issue-number] type: description`

| type          | When to use |
|---------------|-------------|
| `feat`        | Adding a new feature |
| `fix`         | Bug fix |
| `refactor`    | Code improvement with no behavior change |
| `chore`       | Config, dependency, build changes |
| `docs`        | Documentation changes |
| `test`        | Adding/modifying test code |
| `performance` | Performance improvement |

Write commit messages in Korean.
You must pass `./gradlew ktlintCheck` before committing.

## PR Rules

- A PR must target the `develop` branch.
- Write the PR title in the same format as the commit message.
- Attach screenshots or test results (see the PR template).
- Do not merge your own PR (code review required).

## Git Rules When Working as Claude Code

- Tidy code style with `./gradlew ktlintFormat` before committing.
- Commit messages must follow the `[#issue-number] type: description` format.
- Do not run `git push` until the user explicitly requests it.
- Never run `git push --force`.
- Never commit the `.env` file.