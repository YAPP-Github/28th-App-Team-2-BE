---
name: resolve-review
description: Load when reading a GitHub PR's code-review comments, applying the feedback, and replying to each comment in Korean. PR review-resolution workflow.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Applying PR Reviews

Read the review comments of the current branch (or a specified PR), apply them, and reply to each comment.

## 1. Collect Review Comments

```bash
# Find the PR number for the current branch
gh pr view --json number,title,headRefName

# Inline review comments (per file/line)
gh api repos/{owner}/{repo}/pulls/<PR-number>/comments \
  --jq '.[] | {id, path, line, body, user: .user.login}'

# General review comments (full review bodies)
gh api repos/{owner}/{repo}/pulls/<PR-number>/reviews \
  --jq '.[] | select(.body != "") | {id, state, body, user: .user.login}'
```

## 2. Apply

- **Classify** each comment: ① apply immediately / ② needs discussion / ③ cannot apply (with a reason).
- Follow the project rules for code changes — architecture (`architecture`), code style (`code-style`), testing (`testing`).
- Always verify after changes: `./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew :architecture-test:test` (`/run-checks`).

## 3. Commit

- Review-resolution commit message: `[#issue-number] refactor: 리뷰 피드백 반영 - <summary>` (or a type matching the nature of the change).
- Commit conventions and push rules follow the `git-workflow` skill (**push only when the user explicitly requests it**).

## 4. Reply

Reply **in Korean** to each inline comment. If you applied it, say how; if you didn't, say why.

```bash
# Reply to an inline comment
gh api repos/{owner}/{repo}/pulls/<PR-number>/comments/<comment_id>/replies \
  -f body='반영했습니다. <change summary>. (commit <sha>)'
```

- Reply tone: concise and polite. Forms like "Applied / Kept for the following reason / Split off into a separate issue."
- After handling all comments, report a table summary to the user of which comment was handled and how.

## Principles

- If the reviewer's intent is ambiguous, do not decide arbitrarily — confirm with the user.
- Do not merge your own PR (`git-workflow`). Do not merge.
