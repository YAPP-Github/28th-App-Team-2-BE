---
name: new-domain
description: Scaffold a new domain across the 4 modules of the nested hexagonal architecture. User-invoked only.
argument-hint: "[domain]"
disable-model-invocation: true
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Scaffold a new domain '$ARGUMENTS' following the nested hexagonal architecture rules.

> **The canonical code template is [`.claude/examples/domain-scaffold.md`](../../examples/domain-scaffold.md)**, and the `module-{domain}/CLAUDE.md` template is [`.claude/examples/domain-claude-md.md`](../../examples/domain-claude-md.md).
> Follow those documents exactly for package rules, build.gradle, source-file templates, and key rules. The `domain-scaffolder` agent references the same documents.

## Procedure

1. **Add the 4 modules to `settings.gradle.kts`** — include the nested paths with the layer name as the leaf (`{domain}:domain`), and map the container and each child to `{domain}-{layer}` directories (since the leaf drops the `{domain}-` prefix, each child needs an explicit `projectDir`):
   ```kotlin
   include(
       "{domain}:domain",
       "{domain}:application",
       "{domain}:adapter-in",
       "{domain}:adapter-out",
   )
   // Only the outer wrapper directory carries the `module-` prefix. The Gradle leaf uses only the layer name, but the directory keeps `{domain}-{layer}`, so each child is mapped explicitly.
   project(":{domain}").projectDir = file("module-{domain}")
   project(":{domain}:domain").projectDir = file("module-{domain}/{domain}-domain")
   project(":{domain}:application").projectDir = file("module-{domain}/{domain}-application")
   project(":{domain}:adapter-in").projectDir = file("module-{domain}/{domain}-adapter-in")
   project(":{domain}:adapter-out").projectDir = file("module-{domain}/{domain}-adapter-out")
   ```
2. **Generate each module's `build.gradle.kts`** — exactly as in the "build.gradle.kts (per module)" section of `domain-scaffold.md`.
3. **Add `implementation` on all 4 modules in `bootstrap/build.gradle.kts`.**
4. **Add `testImplementation` on all 4 modules in `architecture-test/build.gradle.kts`.**
5. **Create the directories** — run `./.claude/scripts/new-module.sh {domain}` to create all 4 module directories at once.
6. **Generate the initial source files** — instantiate `domain-scaffold.md`'s per-module templates (entity · port · ErrorCode · exception · UseCase · Service · JpaEntity · Adapter · Api · Controller · DTO) with the domain name.
7. **Create `module-{domain}/CLAUDE.md`** — follow `.claude/examples/domain-claude-md.md`. A new domain has no history yet, so write only what's already true:
   - Fill the title line and the **Responsibility Boundary** with the domain's purpose as stated by the user. The "Does not own" column matters most — naming the domain that owns each adjacent concept is what keeps the new module from absorbing its neighbors' work.
   - **Omit** Cross-Domain Contracts and Decisions & Traps if there's nothing yet. The template states explicitly that an empty section is worse than a missing one; fill them in once real ports and real scars exist.

   If `$ARGUMENTS` doesn't make the domain's purpose clear, don't invent it — ask the user for the boundary. Writing down the wrong boundary is worse than having no file.
8. **Verify** — `./gradlew ktlintFormat` → `./gradlew :architecture-test:test`.

Domain name: $ARGUMENTS
