---
name: new-domain
description: Scaffold a new domain across the 4 modules of the nested hexagonal architecture
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Scaffold the new domain '$ARGUMENTS' per the nested hexagonal architecture rules.

> **The canonical code templates are in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md).**
> Follow that document verbatim for package rules, build.gradle, source-file templates, and core rules. The `domain-scaffolder` agent references the same document.

## Procedure

1. **Add the 4 modules to `settings.gradle.kts`** — include the nested path with the layer name as leaf (`{domain}:domain`), then map the container and each child to its `{domain}-{layer}` directory (the leaf drops the `{domain}-` prefix, so each child needs an explicit `projectDir`):
   ```kotlin
   include(
       "{domain}:domain",
       "{domain}:application",
       "{domain}:adapter-in",
       "{domain}:adapter-out",
   )
   // Only the outer wrapper dir carries the `module-` prefix. The Gradle leaf uses just the layer name while the directory keeps `{domain}-{layer}` → map each child explicitly.
   project(":{domain}").projectDir = file("module-{domain}")
   project(":{domain}:domain").projectDir = file("module-{domain}/{domain}-domain")
   project(":{domain}:application").projectDir = file("module-{domain}/{domain}-application")
   project(":{domain}:adapter-in").projectDir = file("module-{domain}/{domain}-adapter-in")
   project(":{domain}:adapter-out").projectDir = file("module-{domain}/{domain}-adapter-out")
   ```
2. **Create each module's `build.gradle.kts`** — exactly as in the "build.gradle.kts (per module)" section of `domain-scaffold.md`.
3. **Add `implementation` for the 4 modules to `bootstrap/build.gradle.kts`.**
4. **Add `testImplementation` for the 4 modules to `architecture-test/build.gradle.kts`.**
5. **Create directories** — make all 4 module directories at once with `./.claude/scripts/new-module.sh {domain}`.
6. **Create initial source files** — generate them by substituting the domain name into the per-module templates in `domain-scaffold.md` (entity · port · ErrorCode · exception · UseCase · Service · JpaEntity · Adapter · Api · Controller · DTO).
7. **Verify** — `./gradlew ktlintFormat` → `./gradlew :architecture-test:test`.

Domain name: $ARGUMENTS