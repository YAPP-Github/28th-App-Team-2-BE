# Shared Code Examples (examples)

The **single source of truth for the large code templates** shared by multiple skills, commands, and agents.
Skills keep only the rules (prose); long code blocks are referenced from here. No duplicate definitions.

| File | Contents | Referenced by |
|------|----------|---------------|
| [domain-scaffold.md](domain-scaffold.md) | Templates for scaffolding a new domain's 4 modules (build.gradle · entity · port · use case · adapter · controller · DTO) | `/new-domain`, the `domain-scaffolder` agent |
| [testing-patterns.md](testing-patterns.md) | Per-layer `DescribeSpec` examples, `TestContainersConfig`, `*Fixture`, `KotestProjectConfig` | the `testing` and `spring-ai` skills, the `test-writer` agent |

> Keep short illustrative snippets (3–8 lines) inline in each skill; collect only compilable, full templates here.