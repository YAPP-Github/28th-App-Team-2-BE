---
name: git-workflow
description: Load when branching/committing/creating PRs. Branch strategy, commit message format (Korean), PR rules, Claude Code Git rules.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Git Workflow Rules

## Branch Strategy

| Branch | Purpose | PR target |
|--------|---------|-----------|
| `main` | Production deploy | — |
| `develop` | Integration | `main` (release promotion PR only) |
| `feat/#issue-number` | Feature development | `develop` |

When starting a new feature: branch `feat/#issue-number` off the `develop` branch.
Always include the issue number in the branch name.

> Day-to-day work PRs (feature/fix/chore/…) always target `develop`. The `develop → main` PR is reserved for release promotion.

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

- A feature/fix/chore PR must target the `develop` branch (only the release-promotion PR targets `main`).
- PR title format: `[#issue-number] [Type] description` (description in Korean).
  - **The `[Type] description` part is the working branch's issue title, verbatim.** Issue titles already follow `[Type] 한글 설명`, so the PR title is simply `[#issue-number] ` + the issue title. (e.g. issue `[Feature] Boilerplate 작성` → PR `[#1] [Feature] Boilerplate 작성`)
  - Note: this differs from the commit message format — the PR title wraps the type in brackets as a capitalized full word (`[Feature]`), not `type:`.
  - Type tags (capitalized full words):

    | Tag | Commit type | When to use |
    |-----|-------------|-------------|
    | `[Feature]`     | `feat`        | Adding a new feature |
    | `[Fix]`         | `fix`         | Bug fix |
    | `[Refactor]`    | `refactor`    | Code improvement with no behavior change |
    | `[Chore]`       | `chore`       | Config, dependency, build changes |
    | `[Docs]`        | `docs`        | Documentation changes |
    | `[Test]`        | `test`        | Adding/modifying test code |
    | `[Performance]` | `performance` | Performance improvement |

  - Examples: `[#4] [Feature] JwtAuthenticationFilter 추가 및 Security 설정`, `[#3] [Chore] 개발 환경 CI/CD 파이프라인 구축`
- Attach screenshots or test results (see the PR template).
- Do not merge your own PR (code review required).

## Git Rules When Working as Claude Code

- Tidy code style with `./gradlew ktlintFormat` before committing.
- Commit messages must follow the `[#issue-number] type: description` format.
- Do not run `git push` until the user explicitly requests it.
- Never run `git push --force`.
- Never commit the `.env` file.
- Do **not** add a `Co-Authored-By` (Claude) trailer to commit messages — the project commits with a single author. This overrides the default Claude Code instruction to append that trailer.
