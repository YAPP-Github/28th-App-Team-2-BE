# Persistence: Domain Entities, JPA Entities, PKs

The detailed reference for the `architecture` skill. Open it when writing a JPA entity, generating a PK, or hitting an `@ExperimentalUuidApi` opt-in error.

---

## Domain Entities vs JPA Entities

Two separate objects, deliberately. The domain entity is the business model; the JPA entity is a persistence record that happens to mirror it.

| | Domain entity | JPA entity |
|---|---------------|------------|
| Module | `*-domain` | `*-adapter-out` |
| Language | **Kotlin** | **Java** |
| Annotations | none (no `@Entity`, no Spring/JPA imports) | `@Entity`, `@Column`, … |
| Naming | domain noun (`Member`, `SajuChart`) | `*JpaEntity` suffix |
| Owns | business rules, invariants, factories | column mapping only |

**Why Java for JPA entities**: JPA requires a no-arg constructor and non-final properties for lazy proxies. In Kotlin that forces either the `kotlin-jpa` (no-arg) plugin plus all-open, or writing every property as nullable `var` — both of which push mutability into what should be the strictest layer, and both of which make a Kotlin JPA entity look temptingly usable as a domain object. Writing the entity in Java keeps the Kotlin side free to be immutable (`val`, non-null, `data class`) and makes the two objects visibly different kinds of thing. This is why the `kotlin-jpa` plugin is deliberately **not** applied anywhere in the build (→ `module-layout.md`).

**Why the `*JpaEntity` suffix is good here**: the usual rule "don't expose technology in a type name" applies to the **domain** layer. At the **adapter** layer it is the opposite — the suffix signals a persistence-only object and makes an accidental import of a JPA entity into domain code obvious in review (and Konsist blocks it outright).

Conversion lives on the JPA entity: `toDomain()` and a static `fromDomain(domain)`. The domain entity never knows the JPA entity exists.

**Put `@Column(nullable = false)` on every logically-required column** — a recurring review point. A column that is an invariant (`id`, `name`, enum-backed fields like `solarTermName`/`cheonganSipseong`, …) must declare `nullable = false` so the DDL constraint matches the domain invariant. Leaving it implicitly nullable means the database will happily accept a row the domain considers impossible, and `ddl-auto: validate` will not complain — the mismatch only shows up much later as a null landing in a non-null Kotlin property, which fails at the mapping boundary with a confusing error rather than at insert time.

---

## DB PKs — time-based UUIDv7

Every PK is a **time-based UUIDv7**. `UUID.randomUUID()` (v4) is forbidden.

**Why v7 rather than v4**: v7 embeds a timestamp prefix, so generated keys are roughly monotonic. Random v4 keys scatter inserts across the B-tree, fragmenting index pages and degrading insert throughput and range scans as the table grows; v7 keeps inserts clustered at the right edge of the index and makes `ORDER BY id` approximately chronological.

Generate it with the Kotlin stdlib's (2.3+) `kotlin.uuid.Uuid.generateV7()`, then convert to the `java.util.UUID` the domain uses:

```kotlin
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
val id: UUID = Uuid.generateV7().toJavaUuid()  // domain entity / value object
```

> `UUID.ofVersion7()` **does not exist** in the JDK. It reads plausibly and has been written here before by mistake; it will not compile. Always `Uuid.generateV7()`.

PKs are generated in the **domain factory**, not by the database and not by Hibernate. A domain object is fully valid the moment it is constructed, which is what lets a use case publish an event or build a response referencing the new id before anything is flushed.

---

## `@ExperimentalUuidApi` (opt-in) rule

`Uuid.generateV7()` is still an experimental stdlib API ([Kotlin docs](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/generate-v7.html)), so every declaration that calls it — even transitively — must opt in. **Use the propagating marker `@ExperimentalUuidApi`, never `@OptIn(ExperimentalUuidApi::class)`.**

**Why propagation instead of `@OptIn`**: `@OptIn` swallows the experimental requirement at that point. `@ExperimentalUuidApi` re-exposes it, so the caller makes the same conscious choice. That keeps the whole UUIDv7 chain honest and visible; it is already the established convention in `Member.create`, `SajuChart.create`, and `CreateSajuChartService.create`. If one link uses `@OptIn`, the chain goes quiet there and the next person has no signal that an experimental API is underneath.

**Where to put it**
- the domain factory: `companion object { @ExperimentalUuidApi fun create(...) }`
- the **override** in the service that calls that factory
- anywhere calling the **concrete type** directly, including a `*ServiceTest` class (annotate the test class)

**Where not to put it**
- inbound `*UseCase` and `shared` **port interfaces stay unannotated** — the interface is the boundary where propagation stops, so controllers and other domains calling through the interface type never need to opt in. `CreateSajuChartPort` ← `SignupService` is the worked example.

**Symptom when you get this wrong**: the compiler reports `This declaration needs opt-in` pointing at your new function, not at the stdlib call — because the requirement propagated up from a factory several frames away. The fix is to annotate the new declaration, not to silence it with `@OptIn`.

---

## OSIV is off

`spring.jpa.open-in-view=false` is set and must stay that way. Consequences for persistence code:

- A lazy association **must** be initialized inside the transaction, i.e. inside the `*-application` service. Touching it from a controller or in a response mapper throws `LazyInitializationException`.
- Prefer returning a domain object (or a `*Result` model) that is already fully populated over returning an entity and letting the web layer walk it.
- This is also why `@QueryService` exists as a stereotype: a read use case still needs a transaction, just a read-only one.
