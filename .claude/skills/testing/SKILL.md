---
name: testing
description: Load when writing or modifying tests. Kotest/MockK/TestContainer, DescribeSpec style, per-layer test strategy, fixtures, isolation mode, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Test Rules

## Test Libraries

| Purpose | Library |
|---------|---------|
| Test framework / assertions | Kotest |
| Mocking | MockK |
| Integration-test infra | TestContainer |

- Use **Kotest** for the test framework and assertions. Use Kotest matchers (`shouldBe`, `shouldThrow`, etc.) instead of JUnit `assertEquals`.
- Use **MockK** for mocking. Mockito is forbidden.
  - Mocking port interfaces: `mockk<UserRepository>()`
  - Stubs: `every { ... } returns ...`; for coroutines, `coEvery`
  - Call verification: `verify { ... }`; for coroutines, `coVerify`
  - When you need to mock a Spring bean, use `@MockkBean` (springmockk) — since Spring injects via fields after construction, a **class property + `lateinit var`** is required (`val` not allowed). For constructor injection / plain `mockk()` objects, use `val`.
  - **Strict mock by default**: use `mockk()` by default and fail on un-stubbed calls. `relaxed = true` is forbidden; only handle `Unit` methods with meaningless return values via `every { ... } just Runs`.

### Spec style — standardize on `DescribeSpec`

For consistency, **use only `DescribeSpec` across all layers**. Do not use other Spec styles (`BehaviorSpec`, `StringSpec`, `FunSpec`, etc.).

Fix the roles of `describe` / `context` / `it` as follows.

| Keyword | Role | Writing rule |
|---------|------|--------------|
| `describe` | Subject under test (method/feature) | Subject name (e.g. `"findByEmail"`, `"사용자 조회"`) |
| `context` | Condition/situation | `"~이면"` form (e.g. `"존재하지 않는 ID이면"` / "when the ID does not exist") |
| `it` | Expected result | `"~한다"` form (e.g. `"사용자를 반환한다"` / "returns the user") |

**Where to place the DSL:** by default use the **constructor lambda** (`DescribeSpec({ ... })`). Use `DescribeSpec()` + `init { ... }` only when you need class properties (field injection), such as `@MockkBean`. In other words, **use `init { }` only when `@MockkBean` is present**.

> See [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md) for full per-layer `DescribeSpec` examples.

### Isolation (IsolationMode) and mock cleanup

- Use Kotest's default `IsolationMode.SingleInstance` as-is (one spec instance). It is the default that is compatible with Spring context caching.
- In this mode, a `val mock` in the spec body is shared across all `it`s, so **`afterTest { clearMocks(...) }` is mandatory** to prevent state leakage between tests. Clean multiple mocks at once with `clearMocks(a, b, c)`.
- Only for specs that truly need full isolation between tests, specify `isolationMode = IsolationMode.InstancePerLeaf` at the top of the class (exceptional, costly).

## Per-Layer Test Scope

### API/Controller layer

Focus on verifying JWT authentication/authorization behavior.

- Verify `401 Unauthorized` is returned when requesting without a valid JWT
- Verify `403 Forbidden` is returned when an unauthorized user requests
- Use `@WebMvcTest` + MockMvc

### Repository layer

Verify that JPA methods return the expected results.

- Verify query results for retrieval, sorting, paging, filtering, etc.
- Test against real PostgreSQL via TestContainer (H2 is forbidden)

### Service layer

Verify that business logic is performed correctly.

- Replace port interfaces with MockK (`mockk`)
- Verify the appropriate exception is thrown on a domain-rule violation (Kotest `shouldThrow`)
- Verify the call order and parameters of collaborators (MockK `verify` / `verifyOrder`)

## TestContainer Setup

You must use the same versions as the deployment environment.

| Infra | Image |
|-------|-------|
| PostgreSQL | `pgvector/pgvector:pg17` (includes pgvector extension, postgres-compatible) |
| Redis | `redis:7.2` |

Configure containers via **the `@ServiceConnection` + `@TestConfiguration` composition approach**. Rather than inheriting a base class, plug them only into the tests that need them via `@Import(TestContainersConfig::class)` (it rides on the context that Kotest's `SpringExtension` brings up).

- **Singleton + reuse**: start once in a `companion object` and reuse across runs with `withReuse(true)`. Since Ryuk does not clean it up, do not call `stop()`. (Local needs `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`; **reuse is disabled in CI**.)
- **Automate connection config with `@ServiceConnection`**. Avoid manual `@DynamicPropertySource` mapping.
- **Use `pgvector/pgvector:pg17` for the PostgreSQL container from the start.** It is a superset-compatible with regular postgres, so use it as-is for JPA tests rather than branching the image for pgvector. Pin it with `asCompatibleSubstituteFor("postgres")` and enable the `vector` extension via `withInitScript`.

> See [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md) for the full `TestContainersConfig` code.

## Test Fixtures

Because PKs are UUIDv7, random values every time make assertions unstable. Unify creation logic via a **per-domain `*Fixture` object**, and use **fixed UUIDs** in tests.

- Build them as factory functions with default values, overriding only the per-test values via named arguments.
- Place `*Fixture` in the `fixture` package under each module's `src/test`. (Prefer explicit fixed values over random-data libraries.)
- **Don't repeat `X.create(...)` inline across tests** (recurring review point). Route construction through the domain's `*Fixture` (or a local helper function) so setup stays DRY and maintainable.
- **Hoist hardcoded UUIDs to a `private val` at the top of the spec file** (`MEMBER_ID`, `PARTNER_SAJU_ID`, …) and reuse it, instead of scattering literal UUID strings through the test body.

## Kotest Global Config

With `AbstractProjectConfig` (one per `src/test`), register `SpringExtension` globally (to avoid declaring it per spec) and explicitly fix the default isolation mode.

> See [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md) for the `*Fixture` and `KotestProjectConfig` code.

## Lint / Architecture Verification

```bash
./gradlew ktlintCheck                 # verify Kotlin code style
./gradlew :architecture-test:test     # verify compliance with architecture-layer rules (Konsist, run via JUnit)
```

Example Konsist rules: automatically verify architecture constraints such as no Spring dependencies in domain modules, and Controllers must implement `*Api`.

## Principles

- Tests must run independently (no shared state between tests, `afterTest { clearMocks() }`).
- Generate test data with `*Fixture`, overriding only the mutable values via named arguments.
- Test class names: `*Test`; integration tests: `*IntegrationTest` (composed with `@Import(TestContainersConfig::class)`). **Controller integration tests follow `{Controller}ControllerIntegrationTest`** — e.g. `NotificationControllerIntegrationTest`, not `NotificationIntegrationTest` — to match the sibling test names (recurring review point).
