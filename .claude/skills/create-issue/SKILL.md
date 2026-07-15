---
name: create-issue
description: Load when creating a GitHub issue per the project ISSUE_TEMPLATE. Auto-applies per-type template mapping and title/label conventions.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Creating GitHub Issues

Create issues following the form templates in `.github/ISSUE_TEMPLATE/`.

## Type → Template Mapping

| Type | Template file | Title prefix | Label |
|------|---------------|--------------|-------|
| Feature | `✨_feature.yaml` | `[Feature] ` | `✨ feature` |
| Bug | `🐛_bug.yaml` | `[Bug] ` | `🐛 bug` |
| Refactor | `♻️_refactor.yaml` | `[Refactor] ` | `♻️ refactor` |
| Chore | `⚙️_chore.yaml` | `[Chore] ` | `⚙️ chore` |
| Performance | `⚡_performance.yaml` | `[Performance] ` | `⚡ performance` |
| Documentation | `📃_documentation.yaml` | `[Documentation] ` | `📃 documentation` |

> For the exact title prefix/label, Read the template file's `title`/`labels` fields and follow them verbatim.

## Procedure

1. **Determine the type** from the user's description; if ambiguous, confirm which type it is.
2. Together with the user, compose the content to fill that template's fields (e.g. feature = `설명` (description) / `할 일 목록(TODO)` (TODO list)). Required fields (`required: true`) must be filled.
3. Create the issue:
   ```bash
   gh issue create \
     --title "[Feature] <summary>" \
     --label "✨ feature" \
     --body "$(cat <<'EOF'
   ## 📌 이슈 내용 설명
   <description>

   ## ✅ TODO
   - [ ] Task 1
   - [ ] Task 2
   EOF
   )"
   ```
4. Tell the user the created issue number — it is used afterward in branches (`feat/#issue-number`) and commits (`[#issue-number] ...`) (`git-workflow`).

## Principles

- Do not arbitrarily change the template structure (section titles, required fields).
- Write the body in Korean.
