# Per-Layer Test Strategy Reference

Detailed reference for the `testing` skill. Open it when writing a test for a specific layer. Copy-paste skeletons are in [`.claude/examples/testing-patterns.md`](../../../examples/testing-patterns.md); this file explains *what to assert and why*, plus a real in-repo example to read for each layer.

---

## `adapter-in` — Web Layer

**The question: is the endpoint's contract enforced?** Not "does the feature work" — that's the service test.

Assert:
- `401 Unauthorized` without a valid JWT.
- `403 Forbidden` when an authenticated member requests someone else's resource. Ownership checks are the most commonly missed test here.
- Request validation: a `*Request` field violating Bean Validation returns `400` with the per-field `reason` map (`error-handling`), not a 500.
- The response is wrapped in the `CommonResponse` envelope with the expected `code`.

Approach:
- `@WebMvcTest({Controller}::class)` + MockMvc, with the use case replaced by `@MockkBean`. The use case is *stubbed*, so no DB or container is involved.
- Server-injected parameters (`@AuthenticationPrincipal memberId`, `@BearerToken`) are supplied by the security test setup, not by the request body — mirroring `@Parameter(hidden = true)` on the `*Api` interface.
- A full-stack variant (`*ControllerIntegrationTest`) boots the app with `@Import(TestContainersConfig::class)` when you need the real filter chain and DB together.

Read: `AuthControllerIntegrationTest` (auth + validation + envelope in one place).

Don't:
- Re-test business branching through MockMvc. It makes the suite slow and the failure message tells you nothing about the rule that broke.
- Assert on raw JSON strings when you can assert on the envelope fields.

## `application` — Use-Case Layer

**The question: does the business logic branch and fail correctly?**

Assert:
- Each branch of the use case, with ports replaced by `mockk()`.
- The **specific** domain exception on a rule violation: `shouldThrow<MemberNotFoundException> { ... }`. Asserting a generic `Exception` passes even when the wrong rule fires.
- Collaborator interaction where order or arguments matter: `verify(exactly = 1) { port.call(expected) }`, `verifyOrder { ... }`. Order matters most in orchestration (e.g. withdrawal's six steps, orchestrator + `TransactionalStore` splits).
- Idempotency where the design claims it — call twice, assert one write.

Approach:
- Pure `DescribeSpec({ ... })` with `val` mocks and `afterTest { clearMocks(...) }`. No Spring context, so these are the fastest tests in the repo — put most of your coverage here.
- Coroutine code: `coEvery`/`coVerify`, and `runTest` for suspending bodies.
- A transaction-boundary claim (AI call outside the transaction, per-member commit isolation) needs a real context — that's an `*IntegrationTest`, not this layer. Example: `CreateYearSelectionFortuneTransactionBoundaryIntegrationTest`.

Read: `GetTodayFortuneServiceTest` (branching + exception), `SendNotificationServiceTest` (gating logic with several ports).

Don't:
- Mock the domain entity. Construct a real one through its `*Fixture`; entities hold the invariants you want exercised.
- Stub what you're asserting. If a test stubs the port and then verifies the same stub, it asserts nothing about the service.

## `adapter-out` (JPA) — Persistence Layer

**The question: does the query return what the port promises?**

Assert:
- Round-trip: save through the adapter, read back, compare domain fields (not JPA entity fields).
- Sorting, paging, filtering and uniqueness-constraint behavior — the things a query can silently get wrong.
- Mapping both ways: `toDomain()` / `fromDomain()` preserve every field, including nullable ones and enums with DB check constraints.

Approach:
- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestContainersConfig::class)`. Composition, not a base class.
- **Real PostgreSQL via TestContainer; H2 is forbidden** — H2 diverges on JSON, check constraints, `uuid`, and locking, which is exactly where these bugs live.
- Advisory-lock behavior (`DispatchLockPort`, the `*TransactionalStore` locks) only reproduces on real PostgreSQL; assert it here rather than in a service test.

Read: `MemberSajuLinkRepositoryAdapterTest`, `CreateLuckActionAdapterTest`, `CreateMemberAdapterTest`.

Don't:
- Assert through the `*JpaRepository` directly when the port is the contract under test.
- Re-verify business rules the service enforces.

## `adapter-out` (AI / external) — Integration Layer

**The question: do we build the request right and interpret the response right?** Never whether the provider works.

Assert:
- Response → domain mapping, including a null/empty response becoming `*EmptyResponseException`.
- Failure classification: circuit-open surfaces as the **domain** `*CircuitOpenException`, not `CallNotPermittedException`; timeout as `*TimeoutException`.
- For FCM: `UNREGISTERED` sets `tokenExpired`, while other error codes don't (`fcm` skill).

Approach:
- Stub `ChatClient`/`ChatModel` or `FirebaseMessaging` with MockK. Build resilience4j registries directly instead of booting Spring (see `VertexAiChatAdapterTest`).
- To exercise an open circuit, construct a registry with a custom `CircuitBreakerConfig` and force the named instance open.

Read: `VertexAiChatAdapterTest` (stubbed model + circuit-open case).

## `domain` — Pure Kotlin

**The question: do the invariants hold?**

Assert factory rules (`create` vs `reconstitute`), state transitions (`toggle`, `complete`/`fail`), and validation that throws a domain exception. No Spring, no container, no mocks — construct and assert. These tests run in milliseconds, so cover edge cases exhaustively here rather than higher up.

Read: `SajuChartTest`, `BirthTimeTest`.

## Choosing a Layer When It's Ambiguous

| Claim you want to pin | Test at |
|-----------------------|---------|
| "A member can't read another member's data" | `adapter-in` (403) |
| "This use case retries N times then skips" | `application` |
| "The AI call happens outside the transaction" | integration (`*IntegrationTest`) |
| "This date range is rejected" | `domain` if the entity validates it, else `adapter-in` |
| "Two concurrent requests create one row" | `adapter-out` / integration (needs real locks) |
