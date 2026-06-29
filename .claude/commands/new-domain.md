---
name: new-domain
description: Scaffold a new domain across the 4 modules of the nested hexagonal architecture
---

Scaffold the new domain '$ARGUMENTS' per the nested hexagonal architecture rules.

> **The canonical code templates are in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md).**
> Follow that document verbatim for package rules, build.gradle, source-file templates, and core rules. The `domain-scaffolder` agent references the same document.

## Procedure

1. **Add the 4 modules to `settings.gradle.kts`** — include the nested path (`{domain}:`):
   ```kotlin
   "{domain}:{domain}-domain",
   "{domain}:{domain}-application",
   "{domain}:{domain}-adapter-in",
   "{domain}:{domain}-adapter-out",
   ```
2. **Create each module's `build.gradle.kts`** — exactly as in the "build.gradle.kts (per module)" section of `domain-scaffold.md`.
3. **Add `implementation` for the 4 modules to `bootstrap/build.gradle.kts`.**
4. **Add `testImplementation` for the 4 modules to `architecture-test/build.gradle.kts`.**
5. **Create directories** — make all 4 module directories at once with `./.claude/scripts/new-module.sh {domain}`.
6. **Create initial source files** — generate them by substituting the domain name into the per-module templates in `domain-scaffold.md` (entity · port · ErrorCode · exception · UseCase · Service · JpaEntity · Adapter · Api · Controller · DTO).
7. **Verify** — `./gradlew ktlintFormat` → `./gradlew :architecture-test:test`.

Domain name: $ARGUMENTS