---
name: architecture
description: Load when designing hexagonal architecture — module roles, dependency direction, ports & adapters, transaction boundaries, cross-domain access, DTO↔domain mapping. Module/package layout, persistence, web-API and AI-transaction details live in references/.
user-invocable: false
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Architecture Rules

## Dependency Direction

```
*-adapter-in  ──→  *-application  ──→  *-domain
*-adapter-out ──→  *-application  ──→  *-domain
all modules   ──→  common, shared
*-adapter-in  ──→  common-web   (response envelope · global handler · Swagger annotations)
bootstrap     ──→  integrates all modules
```

Never add a reverse-direction dependency. `*-domain` depends on no external framework.

| Layer | Owns | Never contains |
|-------|------|----------------|
| `{domain}-domain` | Entities, `*UseCase` + its command/result models (`port.inbound`), `*Port` (`port.outbound`) | Spring/JPA imports, `*Request`/`*Response` |
| `{domain}-application` | `*Service` implementing `port.inbound`, the transaction boundary | Port interfaces, DTOs, `.adapter.` imports |
| `{domain}-adapter-in` | REST controller, `*Api` (Swagger), `*Request`/`*Response` | Business rules |
| `{domain}-adapter-out` | JPA (Java) / OAuth / JWT / Redis adapters, `*JpaEntity` | Business rules |

## Transaction Boundaries

Transactions live **only on use-case services in `*-application`** — never on controllers or domain entities. Declare them with a CQRS stereotype (`com.yapp.todakun.common.annotation`), never a method-level `@Transactional`:

| Annotation | Composition | For |
|------------|-------------|-----|
| `@CommandService` | `@Service` + `@Transactional` | Mutating (create/update/delete) use cases |
| `@QueryService` | `@Service` + `@Transactional(readOnly = true)` | Read use cases |

One service has exactly one responsibility: Command or Query. OSIV is off, so **finish every lazy load inside the transaction**.

**A use case that calls an AI model splits into two beans** — a transaction-less `@Service` orchestrator plus a `@CommandService` store — so the AI call happens *between* short transactions instead of inside one long one. Giving the orchestrator `@CommandService` silently collapses the split under `REQUIRED` propagation and surfaces only as connection-pool exhaustion under load → `references/transaction-patterns.md`.

## Cross-Domain Access

Pick the channel by who needs to know:

| Situation | Use |
|-----------|-----|
| Your use case must **branch or act** on another domain's data | `shared` port |
| The caller only needs to **display** the other domain's data | No port — the client calls that domain's `*UseCase` directly |
| Consequences the publisher should not enumerate (cache eviction, notification) | `shared.event.*` domain event |

Direct references between domain entities are forbidden.

Where a `shared` port is *implemented* depends on whether use-case logic is involved: a pure row read/write belongs in `-adapter-out`, while a real use case (calculate-and-store, role-filtered read) belongs in `-application`. Neither is the default. Worked examples of both: `module-saju/CLAUDE.md` (application side, 8 ports) and `module-member/CLAUDE.md` (adapter-out side, 5 ports).

Once a domain is split out for MSA, only the adapter behind the port is swapped for an HTTP client — the consuming `*-application` code does not change, because the port is the stable contract.

## DTO ↔ Domain Mapping

Mapping happens **only in the adapter layer**. The domain never imports `*Request`/`*Response`; it *does* own the port's own `*Command`/`*Result` models in `port.inbound`, since those are the port's contract rather than adapter DTOs.

- Response: a `from(domain)` factory in `*Response`'s `companion object`; the controller wraps it in `CommonResponse`
- Request: a `toCommand()` on `*Request` that builds the `port.inbound` command type
- Persistence: `toDomain()` / `fromDomain(domain)` on `*JpaEntity`

## References

Open only the one the current task needs — each is detail you otherwise have to re-derive from several files:

| File | Open when |
|------|-----------|
| `references/module-layout.md` | Adding/renaming a module, writing `build.gradle.kts`/`settings.gradle.kts`, or deciding which package a declaration belongs in |
| `references/persistence.md` | Writing a JPA entity, generating a PK, or hitting an `@ExperimentalUuidApi` opt-in error |
| `references/web-api.md` | Writing a controller, an `*Api` Swagger interface, the response envelope, or request validation |
| `references/transaction-patterns.md` | Any use case that calls an AI model, or debugging lock / connection-pool contention |
