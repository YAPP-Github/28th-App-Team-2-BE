---
name: git-workflow
description: Load when branching/committing/creating PRs. Branch strategy, commit message format (Korean), PR rules, Claude Code Git rules.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Git Workflow Rules

## Branch Strategy

| Branch | Purpose | PR target |
|--------|---------|-----------|
| `main` | Production deployment | — |
| `develop` | Integration | `main` (release-promotion PRs only) |
| `feat/#issue-number` | Feature development | `develop` |

When starting a new feature: cut a `feat/#issue-number` branch from `develop`.
Always include the issue number in the branch name.

> Ordinary work PRs (feature/fix/chore/…) always target `develop`. A `develop → main` PR is release-promotion only.

## Commit Messages

Format: `[#issue-number] type: description`

| type          | When to use |
|---------------|-------------|
| `feat`        | Add a new feature |
| `fix`         | Bug fix |
| `refactor`    | Code improvement with no behavior change |
| `chore`       | Config, dependency, or build changes |
| `docs`        | Documentation changes |
| `test`        | Add/modify test code |
| `performance` | Performance improvement |

Write commit messages in Korean.
`./gradlew ktlintCheck` must pass before committing.

## Commit Granularity (keep them small and atomic)

Prefer several small, single-purpose commits over one large commit mixing several concerns. Each commit should be **atomic**: one logical change, complete on its own, and able to build/pass tests by itself.

- **One concern per commit.** Don't mix a feature, a refactor, a config change, and a formatting fix into one commit — split by `type` (`feat`/`fix`/`refactor`/`chore`/`docs`/`test`).
- **Split by module/layer.** In a hexagonal structure, it's usually better to split a domain change, its adapter, and its test into separate commits (`domain` → `application` → `adapter-in`/`adapter-out`), except for pieces too small to stand on their own.
- When a single file mixes concerns, **stage it partially** with `git add -p` so each hunk lands in the right commit.
- **Each commit must independently compile and pass `ktlintCheck`** — never commit a broken intermediate state.
- **Rule of thumb:** if the commit description needs an "and," it probably should be two commits.

## PR Rules

- feature/fix/chore PRs must always target the `develop` branch (only release-promotion PRs target `main`).
- PR title format: `[#issue-number] [Type] description` (description in Korean).
  - **The `[Type] description` part uses the working branch's issue title verbatim.** Since the issue title already follows the `[Type] Korean description` format, the PR title is simply `[#issue-number] ` + the issue title. (E.g. issue `[Feature] Boilerplate 작성` → PR `[#1] [Feature] Boilerplate 작성`)
  - Note: this differs from the commit message format — the PR title's type is an uppercase word in brackets (`[Feature]`), not the `type:` form.
  - Type tags (uppercase words):

    | Tag | Commit type | When to use |
    |-----|-------------|-------------|
    | `[Feature]`     | `feat`        | Add a new feature |
    | `[Fix]`         | `fix`         | Bug fix |
    | `[Refactor]`    | `refactor`    | Code improvement with no behavior change |
    | `[Chore]`       | `chore`       | Config, dependency, or build changes |
    | `[Docs]`        | `docs`        | Documentation changes |
    | `[Test]`        | `test`        | Add/modify test code |
    | `[Performance]` | `performance` | Performance improvement |

  - Examples: `[#4] [Feature] JwtAuthenticationFilter 추가 및 Security 설정`, `[#3] [Chore] 개발 환경 CI/CD 파이프라인 구축`
- Attach a screenshot or test results (see the PR template).
- Never merge your own PR (needs code review).

## Git Rules When Working with Claude Code

- Run `./gradlew ktlintFormat` to clean up code style before committing.
- Commit messages must follow the `[#issue-number] type: description` format.
- Never run `git push` until the user explicitly asks for it.
- Never run `git push --force`.
- Never commit `.env` files.
- **Do not** add a `Co-Authored-By` (Claude) trailer to commit messages — this project commits under a single author. This overrides Claude Code's default instruction to add that trailer.
