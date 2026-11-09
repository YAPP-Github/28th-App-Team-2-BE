---
name: testing
description: Load when writing or changing tests — what to test at each layer, DescribeSpec style, strict mocks and cleanup, naming. TestContainer setup, per-layer strategy and fixtures live in references/.
paths:
  - "**/src/test/**"
  - "**/*Test.kt"
  - "**/*Fixture.kt"
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Testing Rules

| Purpose | Library | Forbidden alternative |
|---------|---------|----------------------|
| Framework / assertions | Kotest (`shouldBe`, `shouldThrow`, …) | JUnit `assertEquals` |
| Mocking | MockK (`every`/`verify`, `coEvery`/`coVerify`) | Mockito |
| Integration infra | TestContainer | H2 |

## What to Test at Each Layer

The judgment this skill exists for — each layer has a different question, and testing the wrong one duplicates coverage without adding confidence.

| Layer | Test this | Don't test this |
|-------|-----------|-----------------|
| `adapter-in` | That auth is enforced (401 without a valid JWT, 403 for the wrong member) and that request validation rejects bad input | Business rules — they belong to the service test |
| `application` | Business logic: branching, the exception thrown on a domain-rule violation, collaborator call order/arguments | Framework wiring, real DB behavior |
| `adapter-out` (JPA) | That the query returns what you expect — sorting, paging, filtering, uniqueness | Logic the service already owns |
| `adapter-out` (AI/external) | Prompt assembly, response mapping, failure→domain-exception mapping | The external provider itself (**never call real Vertex AI / FCM**) |
| `domain` | Invariants and factory/transition rules, with no Spring context at all | Anything needing a container |

## Spec Style — `DescribeSpec` Only

Every layer uses `DescribeSpec`. No `BehaviorSpec`/`StringSpec`/`FunSpec`.

| Keyword | Role | Form |
|---------|------|------|
| `describe` | The unit under test | The target's name (`"findByEmail"`, `"사용자 조회"`) |
| `context` | Condition | `"~이면"` (`"존재하지 않는 ID이면"`) |
| `it` | Expected outcome | `"~한다"` (`"사용자를 반환한다"`) |

**DSL placement:** default to the constructor lambda `DescribeSpec({ ... })`. Use `DescribeSpec()` + `init { }` **only when `@MockkBean` is present** — Spring field-injects it after construction, so it must be a class property with `lateinit var`.

## Mocks and Isolation

- **Strict mocks by default.** Plain `mockk()`, so an unstubbed call fails loudly. `relaxed = true` is forbidden; for a `Unit` method whose return is meaningless use `every { ... } just Runs`.
- **`afterTest { clearMocks(a, b, c) }` is required.** Kotest's default `IsolationMode.SingleInstance` shares one spec instance, so a `val mock` leaks state across `it` blocks. Only pin `IsolationMode.InstancePerLeaf` for the rare spec that genuinely needs full isolation (expensive).
- Mocking a Spring bean → `@MockkBean` (springmockk) as `lateinit var`; constructor-injected or plain `mockk()` → `val`.

## Naming

- Unit: `*Test`. Integration: `*IntegrationTest`, composed with `@Import(TestContainersConfig::class)`.
- Controller integration tests follow `{Controller}ControllerIntegrationTest` — `NotificationControllerIntegrationTest`, not `NotificationIntegrationTest` (recurring review point).
- Fixtures: `*Fixture` in the `fixture` package under each module's `src/test`.

## Verification

```bash
./gradlew test                        # all tests
./gradlew :architecture-test:test     # Konsist architecture rules (incl. test-convention rules)
./gradlew ktlintCheck                 # style
```

## References

| Open when | File |
|-----------|------|
| Writing a test for a specific layer and you want the strategy + a real in-repo example to copy | `references/layer-strategies.md` |
| Setting up or debugging containers — reuse, `@ServiceConnection`, Docker-not-running, CI OOM | `references/testcontainers.md` |
| Creating test data, fixed UUIDs, or touching the global Kotest config | `references/fixtures.md` |

Copy-paste-ready `DescribeSpec` skeletons per layer, plus `TestContainersConfig` / `*Fixture` / `KotestProjectConfig` code, live in [`.claude/examples/testing-patterns.md`](../../examples/testing-patterns.md) — that file is the canonical code; the references above explain the judgment around it.
