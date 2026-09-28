# Shared Code Examples (examples)

The **single source of truth (SSOT) for large code templates** shared across multiple skills, commands, and agents.
Skills keep only rules (prose); long code blocks are referenced from here instead. No duplicate definitions.

| File | Contents | Referenced by |
|------|------|--------------|
| [domain-scaffold.md](domain-scaffold.md) | Scaffolding templates for a new domain's 4 modules (build.gradle · entity · port · use case · adapter · controller · DTO) | `/new-domain`, `domain-scaffolder` agent |
| [testing-patterns.md](testing-patterns.md) | Per-layer `DescribeSpec` examples, `TestContainersConfig`, `*Fixture`, `KotestProjectConfig` | `testing`·`spring-ai` skills, `test-writer` agent |
| [domain-claude-md.md](domain-claude-md.md) | `module-{domain}/CLAUDE.md` template — overview, module structure, bounded-context boundary, cross-domain port contracts, domain-specific decisions and traps, and what must *not* be duplicated there | `/new-domain` (step 7), `domain-scaffolder` agent |

> Short, illustrative snippets (3–8 lines) stay inline inside each skill; only full, compilable templates are collected here.
