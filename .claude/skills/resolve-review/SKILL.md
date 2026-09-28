---
name: resolve-review
description: Load when reading a GitHub PR's code-review comments, applying the feedback, and replying to each comment in Korean. PR review-resolution workflow.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Applying PR Review Feedback

Read the review comments on the current branch (or a specified PR), apply them, then reply to each comment.

## 1. Collect Review Comments

```bash
# find the current branch's PR number
gh pr view --json number,title,headRefName

# inline review comments (per file/line)
gh api repos/{owner}/{repo}/pulls/<PR-number>/comments \
  --jq '.[] | {id, path, line, body, user: .user.login}'

# general review comments (the review's overall body)
gh api repos/{owner}/{repo}/pulls/<PR-number>/reviews \
  --jq '.[] | select(.body != "") | {id, state, body, user: .user.login}'
```

## 2. Apply

- **Classify** each comment: ① apply immediately / ② needs discussion / ③ won't apply (with a reason).
- Code changes follow project rules — architecture (`architecture`), code style (`code-style`), testing (`testing`).
- Always verify after changing: `./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew :architecture-test:test` (`/run-checks`).

## 3. Commit

- Review-fix commit message: `[#issue-number] refactor: 리뷰 피드백 반영 - <summary>` (or a type matching the nature of the change).
- Follow the `git-workflow` skill for commit convention and push rules (**push only when the user explicitly asks**).

## 4. Reply

Reply to each inline comment **in Korean**. Explain how it was applied, or why it wasn't.

```bash
# reply to an inline comment
gh api repos/{owner}/{repo}/pulls/<PR-number>/comments/<comment_id>/replies \
  -f body='반영했습니다. <change summary>. (commit <sha>)'
```

- Reply tone: concise and polite. Forms like "Applied / Kept as-is for this reason / Split off into a separate issue."
- After processing every comment, report a summary table to the user of which comment was handled how.

## Principles

- If the reviewer's intent is ambiguous, confirm with the user rather than guessing.
- Never merge your own PR (`git-workflow`). Don't merge it.
