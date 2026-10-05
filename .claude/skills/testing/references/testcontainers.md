# TestContainer Reference

Detailed reference for the `testing` skill. Open it when setting up containers for an integration test, or when a container-backed test fails for environmental reasons. The `TestContainersConfig` code itself is in [`.claude/examples/testing-patterns.md`](../../../examples/testing-patterns.md).

---

## Images (must match the deployed environment)

| Infra | Image | Note |
|-------|-------|------|
| PostgreSQL | `pgvector/pgvector:pg17` | Includes the pgvector extension and is backward-compatible with plain postgres |
| Redis | `redis:7.2` | |

**Use the pgvector image even for plain JPA tests.** Branching the image per test type means the day someone introduces embeddings, half the suite can't see the extension. Pin it with `asCompatibleSubstituteFor("postgres")` so Testcontainers accepts it as a PostgreSQL container, and enable the extension with `withInitScript` (`CREATE EXTENSION IF NOT EXISTS vector;`).

## Composition, Not Inheritance

- `@TestConfiguration(proxyBeanMethods = false)` holding the containers, mixed into a spec with `@Import(TestContainersConfig::class)`.
- **`@ServiceConnection` wires the connection properties automatically.** Don't hand-map with `@DynamicPropertySource` — that's the pattern `@ServiceConnection` replaced, and a stale manual mapping is a silent misconfiguration.
- A base class would force every integration test into one context shape; `@Import` lets a spec take only what it needs and keeps Spring's context cache effective.

## Singleton + Reuse

- Start the container once in a `companion object` and keep it with `withReuse(true)`.
- **Never call `stop()`.** With reuse enabled Ryuk doesn't clean the container up, and stopping it defeats cross-run reuse.
- Local reuse needs `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`.
- **Reuse is disabled in CI** — each run starts fresh containers, which is why CI is slower and more memory-sensitive than your machine.

## Symptoms and Remedies

| Symptom | Cause | Remedy |
|---------|-------|--------|
| `Could not find a valid Docker environment` | Docker/Colima not running locally | Start Docker, or run only the non-container modules (`./gradlew :{domain}:domain:test :{domain}:application:test`) |
| `Connection to localhost:<port> refused` mid-suite | The container died or was never started for that context | Check Docker memory limits; look for an earlier container-start failure in the log — the refusal is a symptom, not the cause |
| `HikariPool-N - Connection is not available, request timed out` | Pool exhausted, or the DB container is gone | Usually downstream of the row above; don't raise the pool size to "fix" it |
| `OutOfMemoryError: Java heap space` while creating a Spring bean | Many distinct Spring contexts cached at once (CI) | Reduce context variants: reuse the same `@Import`/`@MockkBean` combination so contexts are shared. Seen in CI on the full 51-module run, not a product bug |
| Tests pass locally, fail in CI | Reuse on locally, off in CI | Run the suspect module with reuse disabled locally before blaming CI |
| `extension "vector" is not available` | Plain postgres image, or `withInitScript` missing | Use `pgvector/pgvector:pg17` + the init script |

**Context-cache hygiene is the main lever on suite cost.** Every distinct combination of annotations (`@MockkBean` sets, `@Import`s, active profiles, properties) creates a *separate* Spring context that is cached for the whole run. Two specs that could share a context but differ by one stray property each pay full boot cost twice and hold both in memory.

## Integration-Test Checklist

1. Name it `*IntegrationTest` (controller: `{Controller}ControllerIntegrationTest`).
2. `@Import(TestContainersConfig::class)` — don't start your own container.
3. Use the shared PostgreSQL/Redis containers; a new container type needs a deliberate decision, not a local `GenericContainer` in one spec.
4. Keep the annotation set identical to sibling integration tests so they share a cached context.
5. Clean state between tests explicitly (delete rows you inserted, or rely on a transactional rollback) — containers are reused, so leftovers leak into later specs.
6. Don't assert on auto-generated timestamps or UUIDs; use `*Fixture` fixed IDs (`references/fixtures.md`).

## When You Don't Need a Container

Most tests don't. Domain and application tests use plain Kotlin + MockK and run in milliseconds. Reach for a container only when the claim under test is about **real infrastructure behavior**: SQL correctness, DB constraints, advisory locks, Redis TTL/expiry, or transaction boundaries. Adding `@Import(TestContainersConfig::class)` to a test that doesn't need it is the most common way this suite gets slow.
