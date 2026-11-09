# Fixtures & Global Config Reference

Detailed reference for the `testing` skill. Open it when creating test data or touching the global Kotest configuration. The `*Fixture` and `KotestProjectConfig` code is in [`.claude/examples/testing-patterns.md`](../../../examples/testing-patterns.md).

---

## Why Fixtures Exist Here

PKs are **time-based UUIDv7** (`Uuid.generateV7()`), so every `create(...)` produces a different id. Two consequences:

1. An assertion that compares ids fails unpredictably unless the id is pinned.
2. Ordering by id is ordering by creation time, so a test that creates rows in a loop gets an order that depends on timing.

So: **tests use fixed UUIDs, never generated ones.**

## The `*Fixture` Pattern

- One `object {Domain}Fixture` per domain, in the `fixture` package under that module's `src/test`.
- A factory function with **defaults for every parameter**, so a test overrides only what it cares about via named arguments:
  ```kotlin
  val member = MemberFixture.create(nickname = "탈퇴예정")   // everything else defaulted
  ```
- Expose the fixed ids as constants on the fixture (`FIXED_ID`, …) so assertions and stubs reference the same value.
- Prefer explicit fixed values over a random-data library. Random data makes a failure non-reproducible, which is the opposite of what a regression test is for.

### Two recurring review points

- **Don't repeat `X.create(...)` inline in every test.** Route creation through the `*Fixture` (or a local helper in the spec) so a constructor change touches one place.
- **Hoist hardcoded UUIDs to a `private val` at the top of the spec file** (`MEMBER_ID`, `PARTNER_SAJU_ID`, …) instead of scattering `UUID.fromString("…")` literals through the body. A literal repeated three times in one file is three places to update and an invitation to a typo that silently changes which row you assert on.

### Fixtures for domains with a creation invariant

Some entities only make sense via a specific factory — `MemberSajuLink.self(...)` vs `.partner(...)`, `ChatMessage.createAssistantPlaceholder(...)`. Expose **one fixture function per valid factory** rather than a single `create` with a `role` flag; a flag lets a test construct a combination the domain forbids (e.g. `SELF` with a `relationshipType`), which then "passes" while being impossible in production.

Entities restored from persistence use `reconstitute(...)`. Use that path in adapter tests when you need a specific id, and the `create` path when you're testing the creation rules themselves — the distinction matters because `create` derives fields (`SajuChart` computes 오행/십성 distribution) that `reconstitute` takes as given.

## Global Kotest Configuration

One `AbstractProjectConfig` per `src/test` source set:

```kotlin
class KotestProjectConfig : AbstractProjectConfig() {
    override val isolationMode = IsolationMode.SingleInstance
    override fun extensions() = listOf(SpringExtension)
}
```

- **Register `SpringExtension` globally**, not per spec. A spec that declares it again is redundant; a spec that forgets it (in a module whose config is missing) fails confusingly with un-injected constructor parameters.
- **Pin `isolationMode` explicitly** even though `SingleInstance` is Kotest's default — it's the mode Spring's context caching is compatible with, and pinning it documents that the `afterTest { clearMocks(...) }` requirement follows from this choice rather than from habit.
- Adding a new module with tests means adding its `KotestProjectConfig`; otherwise `SpringExtension` isn't registered there.

## Mock Cleanup, Restated

Because `SingleInstance` reuses one spec instance:

```kotlin
class XServiceTest : DescribeSpec({
    val port = mockk<XPort>()
    val service = XService(port)

    afterTest { clearMocks(port) }   // required, not optional
    …
})
```

Without `clearMocks`, a `verify(exactly = 1)` in the second `it` can see the first `it`'s invocation and fail — or worse, a stale `every { … } returns` makes a later test pass for the wrong reason. Clear several at once: `clearMocks(a, b, c)`.

`@MockkBean` fields are cleared by springmockk between tests by default, but the Spring **context** is cached across specs, so any state you mutate on a real bean persists. Don't rely on bean state for test setup.

## Checklist

- [ ] New domain → `{Domain}Fixture` in `src/test/.../fixture/`
- [ ] Every fixture parameter has a default; fixed ids are constants
- [ ] No `UUID.randomUUID()` anywhere in test code
- [ ] UUID literals hoisted to the top of the spec, not inline
- [ ] One fixture function per valid domain factory, no "mode" flags
- [ ] `afterTest { clearMocks(...) }` present wherever a `val mockk()` is shared
- [ ] New test source set → `KotestProjectConfig` added
