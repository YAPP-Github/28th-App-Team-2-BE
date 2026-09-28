---
name: testing
description: Load when writing or modifying tests. Kotest/MockK/TestContainer, DescribeSpec style, per-layer test strategy, fixtures, isolation mode, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Testing Rules

## Test Libraries

| Purpose | Library |
|---------|---------|
| Test framework / assertions | Kotest |
| Mocking | MockK |
| Integration test infra | TestContainer |

- Use **Kotest** for the test framework and assertions. Use Kotest matchers (`shouldBe`, `shouldThrow`, etc.) instead of JUnit's `assertEquals`.
- Use **MockK** for mocking. Mockito is forbidden.
  - Mocking a port interface: `mockk<UserRepository>()`
  - Stubbing: `every { ... } returns ...`; for coroutines, `coEvery`
  - Verifying calls: `verify { ... }`; for coroutines, `coVerify`
  - When you need to mock a Spring bean, use `@MockkBean` (springmockk) — since Spring injects it as a field after construction, it needs to be a **class property with `lateinit var`** (`val` won't work). Use `val` for constructor-injected / plain `mockk()` objects.
  - **Use strict mocks by default**: use the default `mockk()` so an unstubbed call fails. `relaxed = true` is forbidden; only for `Unit`-returning methods whose return value is meaningless, use `every { ... } just Runs`.

### Spec style — standardize on `DescribeSpec`

For consistency, **use only `DescribeSpec` at every layer**. Don't use other spec styles (`BehaviorSpec`, `StringSpec`, `FunSpec`, etc.).

Fix the roles of `describe` / `context` / `it` as follows.

| Keyword | Role | Writing rule |
|---------|------|--------------|
| `describe` | The unit under test (method/feature) | The target's name (e.g. `"findByEmail"`, `"사용자 조회"`) |
| `context` | Condition/situation | `"~이면"` form (e.g. `"존재하지 않는 ID이면"`) |
| `it` | Expected outcome | `"~한다"` form (e.g. `"사용자를 반환한다"`) |

**Where to put the DSL:** default to the **constructor lambda** (`DescribeSpec({ ... })`). Use `DescribeSpec()` + `init { ... }` only when you need a class property, such as for field injection like `@MockkBean`. In other words, **use `init { }` only when `@MockkBean` is present**.

> For full per-layer `DescribeSpec` examples, see [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md).

### Isolation (IsolationMode) and mock cleanup

- Stick with Kotest's default, `IsolationMode.SingleInstance` (one spec instance). It's the default that's compatible with Spring context caching.
- Under this mode, a `val mock` in the spec body is shared across every `it`, so **`afterTest { clearMocks(...) }` is required** to prevent state leaking between tests. Clean up several mocks at once with `clearMocks(a, b, c)`.
- Set `isolationMode = IsolationMode.InstancePerLeaf` at the top of a class only for a spec that genuinely needs full isolation between tests (exceptional, expensive).

## Per-Layer Test Scope

### API/Controller layer

Focus on verifying JWT authentication/authorization behavior.

- Verify that `401 Unauthorized` is returned when requesting without a valid JWT
- Verify that `403 Forbidden` is returned when an unauthorized user requests
- Use `@WebMvcTest` + MockMvc

### Repository layer

Verify that JPA methods return the expected results.

- Verify query results for lookups, sorting, paging, filtering, etc.
- Test against real PostgreSQL via TestContainer (H2 forbidden)

### Service layer

Verify that the business logic executes correctly.

- Replace port interfaces with MockK (`mockk`)
- Verify that the right exception is thrown on a domain-rule violation (Kotest `shouldThrow`)
- Verify collaborator call order and parameters (MockK `verify` / `verifyOrder`)

## TestContainer Setup

Must use the same version as the deployed environment.

| Infra | Image |
|-------|-------|
| PostgreSQL | `pgvector/pgvector:pg17` (includes the pgvector extension, postgres-compatible) |
| Redis | `redis:7.2` |

Compose containers via the **`@ServiceConnection` + `@TestConfiguration` combination**. Instead of inheriting a base class, mix it in only where needed with `@Import(TestContainersConfig::class)` (riding on the context Kotest's `SpringExtension` brings up).

- **Singleton + reuse**: start it once in a `companion object` and reuse it with `withReuse(true)`. Don't call `stop()`, since Ryuk doesn't clean it up. (Locally, `~/.testcontainers.properties` needs `testcontainers.reuse.enable=true`; **reuse is disabled in CI**.)
- **Automate connection setup with `@ServiceConnection`**. Avoid manual `@DynamicPropertySource` mappings.
- **Use `pgvector/pgvector:pg17` for the PostgreSQL container from the start.** Since it's backward-compatible with plain postgres, don't branch the image for pgvector — use it for JPA tests too. Pin it with `asCompatibleSubstituteFor("postgres")` and enable the `vector` extension via `withInitScript`.

> For the full `TestContainersConfig` code, see [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md).

## Test Fixtures

Since PKs are UUIDv7, using a random value every time makes assertions flaky. Unify creation logic with a **per-domain `*Fixture` object** and use **fixed UUIDs** in tests.

- Build it as a factory function with defaults, and override only the values a given test needs via named arguments.
- Put `*Fixture` in the `fixture` package under each module's `src/test`. (Prefer explicit fixed values over a random-data library.)
- **Don't repeat `X.create(...)` inline in every test** (a recurring review point). Route creation logic through the domain's `*Fixture` (or a local helper function) to keep setup code DRY and maintainable.
- **Hoist hardcoded UUIDs into a `private val` at the top of the spec file** (`MEMBER_ID`, `PARTNER_SAJU_ID`, …) and reuse them, instead of scattering UUID string literals through the test body.

## Kotest Global Configuration

Register `SpringExtension` globally (once per `src/test`) via `AbstractProjectConfig` (no need to declare it per spec), and explicitly pin the default isolation mode.

> For the `*Fixture` and `KotestProjectConfig` code, see [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md).

## Lint / Architecture Verification

```bash
./gradlew ktlintCheck                 # Verify Kotlin code style
./gradlew :architecture-test:test     # Verify compliance with architecture-layer rules (Konsist, run via JUnit)
```

Example Konsist rules: automatically verifies architectural constraints such as whether domain modules have no Spring dependency, whether a Controller implements `*Api`, etc.

## Principles

- Tests must run independently (no shared state between tests, `afterTest { clearMocks() }`).
- Generate test data via `*Fixture`, overriding only the values that change via named arguments.
- Test class names: `*Test`; integration tests: `*IntegrationTest` (composed with `@Import(TestContainersConfig::class)`). **Controller integration tests follow `{Controller}ControllerIntegrationTest`** — e.g. `NotificationControllerIntegrationTest`, not `NotificationIntegrationTest` — matching the naming of sibling tests (a recurring review point).
