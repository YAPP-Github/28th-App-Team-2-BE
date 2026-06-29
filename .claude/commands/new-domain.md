---
name: new-domain
description: Scaffold a new domain across the 4 modules of the nested hexagonal architecture
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Scaffold the new domain '$ARGUMENTS' per the nested hexagonal architecture rules.

> **The canonical code templates are in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md).**
> Follow that document verbatim for package rules, build.gradle, source-file templates, and core rules. The `domain-scaffolder` agent references the same document.

## Procedure

1. **Add the 4 modules to `settings.gradle.kts`** — include the nested path (`{domain}:`), then map the outer wrapper to its `module-` prefixed directory (children resolve under it automatically):
   ```kotlin
   include(
       "{domain}:{domain}-domain",
       "{domain}:{domain}-application",
       "{domain}:{domain}-adapter-in",
       "{domain}:{domain}-adapter-out",
   )
   // 외부 래퍼 디렉터리에만 `module-` 접두사 → 내부 레이어 모듈은 module-{domain}/ 하위로 자동 해석
   project(":{domain}").projectDir = file("module-{domain}")
   ```
2. **Create each module's `build.gradle.kts`** — exactly as in the "build.gradle.kts (per module)" section of `domain-scaffold.md`.
3. **Add `implementation` for the 4 modules to `bootstrap/build.gradle.kts`.**
4. **Add `testImplementation` for the 4 modules to `architecture-test/build.gradle.kts`.**
5. **Create directories** — make all 4 module directories at once with `./.claude/scripts/new-module.sh {domain}`.
6. **Create initial source files** — generate them by substituting the domain name into the per-module templates in `domain-scaffold.md` (entity · port · ErrorCode · exception · UseCase · Service · JpaEntity · Adapter · Api · Controller · DTO).
7. **Verify** — `./gradlew ktlintFormat` → `./gradlew :architecture-test:test`.

Domain name: $ARGUMENTS