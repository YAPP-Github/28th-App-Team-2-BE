---
name: new-domain
description: Scaffold a new domain across the 4 modules of the nested hexagonal architecture
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Scaffold the new domain '$ARGUMENTS' per the nested hexagonal architecture rules.

> **The canonical code templates are in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md)**, and the `module-{domain}/CLAUDE.md` template is in [`.claude/examples/domain-claude-md.md`](../examples/domain-claude-md.md).
> Follow those documents verbatim for package rules, build.gradle, source-file templates, and core rules. The `domain-scaffolder` agent references the same documents.

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
7. **Create `module-{domain}/CLAUDE.md`** — follow `.claude/examples/domain-claude-md.md`. A brand-new domain has no history yet, so write only what is already true:
   - the title line and **Responsibility Boundary**, filled from what the user said this domain is for. The "Does not own" column matters most — name the domain that owns each adjacent concept, since that is what stops the new module from absorbing its neighbours' work.
   - **omit Cross-Domain Contracts and Decisions & Traps** when there are none. The template is explicit that an empty section is worse than a missing one; both get added as real ports and real scars appear.

   Ask the user for the boundary if the domain's purpose is not clear from `$ARGUMENTS` — do not invent one. A wrong boundary written down is worse than no file.
8. **Verify** — `./gradlew ktlintFormat` → `./gradlew :architecture-test:test`.

Domain name: $ARGUMENTS
