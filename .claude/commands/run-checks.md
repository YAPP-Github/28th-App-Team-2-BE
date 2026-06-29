---
name: run-checks
description: Run the full pre-PR verification in order (ktlintFormat → ktlintCheck → architecture-test → test)
---

Run the full pre-PR verification in order. (Same order as `./.claude/scripts/check-all.sh`.)

1. `./gradlew ktlintFormat` — auto-fix code style
2. `./gradlew ktlintCheck` — verify code style
3. `./gradlew :architecture-test:test` — verify architecture rules (Konsist)
4. `./gradlew test` — all tests

If any step fails, analyze the cause, fix it, and re-run. When all steps pass, summarize the results.