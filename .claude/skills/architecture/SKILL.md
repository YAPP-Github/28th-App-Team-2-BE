---
name: architecture
description: Load when designing hexagonal architecture. Module roles/dependency direction, ports & adapters, domain vs JPA entities, OSIV, transaction boundaries, DTO↔domain mapping, response format, request validation, Swagger patterns.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Architecture Rules

## Module Structure and Dependency Direction

```
*-adapter-in  ──→  *-application  ──→  *-domain
*-adapter-out ──→  *-application  ──→  *-domain
all modules   ──→  common, shared
*-adapter-in  ──→  common-web   (response envelope · global handler · Swagger annotations)
bootstrap     ──→  integrates all modules
```

Never add a reverse-direction dependency. `*-domain` depends on no external framework.

> **Module directory naming**: top-level module **directories** use the `module-{module-name}` prefix (`module-common`, `module-bootstrap`, …) — so that module folders stay grouped at the repo root instead of scattered among config directories. For **nested domains**, only the outer wrapper directory gets the prefix (`module-{domain}/`); the inner layer modules keep plain names (`{domain}-domain`, `{domain}-adapter-in`, …). Gradle **project paths** drop the `module-` prefix: top-level modules stay flat (`:common`), while a domain's layer modules are **nested** under a sourceless `:{domain}` container, with only the layer name at the leaf (`:auth:domain`, `:auth:adapter-in`). `settings.gradle.kts` maps each path with `project(":path").projectDir = file("module-...")` (leaf `:auth:domain` → directory `module-auth/auth-domain`). The tables/paths below use this Gradle project-path form.

## Each Module's Role

### Top-level modules

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `bootstrap` | Spring Boot entry point, integrated security config | All |
| `common` | `AppException`·`ResponseCode`, common exceptions, `@CommandService`/`@QueryService`, utilities | spring-context, spring-tx (lightweight, **no spring-web**) |
| `common-web` | `CommonResponse` (response envelope), `GlobalExceptionHandler`, `CommonErrorCode`/`CommonSuccessCode`, `@DisableSwaggerSecurity` | spring-web, spring-webmvc |
| `shared` | UserId, OAuthProvider, UserAuthPort | None |
| `architecture-test` | Konsist architecture-rule verification | None |

> `common-web`'s base package is `com.yapp.todakun.web`. `*-adapter-in` modules depend on both `common` and `common-web`.

### Domain modules (nested as `{domain}/{domain}-*`)

Each domain (`auth`, `user`, ...) has these 4 modules.

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `{domain}-domain` | Pure Kotlin domain entities, inbound port interfaces (`*UseCase` + its command/result models, `port.inbound`) and outbound port interfaces (`*Port`, `port.outbound`) | None |
| `{domain}-application` | **Implementations** of the use-case services for `port.inbound` interfaces (`@CommandService`/`@QueryService`) | spring-tx/context (via common), `todakun.spring` convention |
| `{domain}-adapter-in` | REST controllers, DTOs, Swagger interfaces | spring-web, springdoc |
| `{domain}-adapter-out` | JPA (Java), OAuth, JWT, Redis adapters | spring-data-jpa, security, etc. |

## Build Conventions (buildSrc convention plugins)

Shared module setup (Kotlin/JVM, ktlint, JDK 25 toolchain, Spring BOM, testing) is applied via **convention plugins in `buildSrc`**. Each module's `build.gradle.kts` applies exactly one convention plugin through a **declarative `plugins {}` block** instead of `apply(plugin = ...)`, and `dependencies {}` adds only that module's own dependencies. (Imperative `apply(...)` at root/subprojects is forbidden.)

| Convention plugin | Composition | Applied to |
|-------------------|-------------|-----------------------|
| `todakun.kotlin-common` | Kotlin/JVM + ktlint + toolchain + BOM + testing | The base for every module (`*-domain`, `common`, `common-web`, `shared`, `architecture-test`) |
| `todakun.spring` | `kotlin-common` + `kotlin-spring` (all-open) | `*-application`; the base for the two adapter plugins below |
| `todakun.adapter-web` | `todakun.spring` + `common-web` + web/security/validation/springdoc | Inbound web adapters (`*-adapter-in`) |
| `todakun.adapter-persistence` | `todakun.spring` + `todakun.lombok` + `common-persistence` + spring-data-jpa + Testcontainers/postgresql | JPA outbound adapters (`*-adapter-out`) — `auth-adapter-out` is the **exception** (Redis/JWT, no JPA → applies `todakun.spring` directly) |
| `todakun.spring-boot` | `todakun.spring` + Spring Boot plugin | The `bootstrap` entry point |
| `todakun.lombok` | `kotlin-lombok` + Lombok (compileOnly/annotationProcessor) | Java JPA entity modules (`common-persistence`; composed into `adapter-persistence`) |

> **The `kotlin-jpa` (no-arg) plugin is not used.** Since JPA entities are written in **Java** (`*JpaEntity.java`), entities don't need no-arg/all-open (→ see "Domain Entities vs JPA Entities"). `kotlin-spring` (all-open) is applied not for entities but for **Kotlin beans that get CGLIB-proxied** (`@CommandService`/`@QueryService`, `@Repository` adapters, `@SpringBootApplication`); `*-adapter-out` gets the same proxy support too, via `todakun.adapter-persistence`, which composes `todakun.spring`.

```kotlin
// e.g. {domain}-application/build.gradle.kts
plugins {
  id("todakun.spring")
}
dependencies {
  // :common is auto-injected by the todakun.kotlin-common convention plugin (no need to declare it per module).
  implementation(project(":shared"))
  implementation(project(":{domain}:domain"))
}
```

> **An adapter module applies only its own role plugin** — `todakun.adapter-web` (adapter-in) or `todakun.adapter-persistence` (JPA adapter-out) — and declares only **its own** `project(...)` dependencies (`{domain}:domain`, `{domain}:application`, and `:shared` **only when the module actually references a shared type**), plus **domain-specific** libraries (`spring-ai`, `firebase-admin`, …). Don't re-list the shared web/JPA/Testcontainers stack per module — it lives in the plugin. `auth-adapter-out` is the exception (Redis/JWT, no JPA → `todakun.spring`).
>
> External plugin versions are managed in one place, `buildSrc/build.gradle.kts`. When a new external plugin is needed, add its classpath dependency there and then declare it in the convention plugin.

## Package Structure

| Module | Package |
|--------|---------|
| `common` | `com.yapp.todakun.common` |
| `common-web` | `com.yapp.todakun.web` |
| `*-domain` | `com.yapp.todakun.{domain}` |
| `*-application` | `com.yapp.todakun.{domain}.application` |
| `*-adapter-in` | `com.yapp.todakun.{domain}.adapter.web` |
| `*-adapter-out` | `com.yapp.todakun.{domain}.adapter.{tech}` |

`*-domain` splits port interfaces by direction (an adapted form of the traditional hexagonal `port.in`/`port.out` naming — since `in` is a Kotlin reserved word, a `port.in` or `` port.`in` `` package would as-is also violate ktlint's `standard:package-name` rule).

| Subpackage | Purpose | Example classes |
|-------------|---------|---------------|
| `.port.inbound` | Inbound ports: `*UseCase` interfaces + their command/result models (a use case's input/output contract lives with the interface, not in `*-application`) | `LoginUseCase`, `LoginCommand`, `LoginResult` |
| `.port.outbound` | Outbound ports: interfaces the domain requires from the outside, implemented by `*-adapter-out` (a JWT filter-type case is implemented by `*-adapter-in` instead) | `AccessTokenPort`, `OAuthPort` |

`*-application` holds only `*Service` classes implementing `port.inbound` interfaces — port interfaces or command/result models don't live here.

> **One inbound port per file, and never mix a Query port and a Command port in the same file.** The `@QueryService`/`@CommandService` (CQRS) split must be visible in the file structure, not buried inside a shared `*UseCases.kt` (`GetXUseCase` and `ReadXUseCase` → 2 files). Likewise, split exceptions and DTOs one per file. (See the `code-style` skill's "File Organization" section for details.)

`*-adapter-out` splits into subpackages by technology.

| Subpackage | Purpose | Example classes |
|-------------|---------|---------------|
| `.adapter.persistence` | JPA adapters, JPA entities | `UserJpaAdapter`, `UserJpaEntity` |
| `.adapter.oauth` | OAuth client adapters | `GoogleOAuthAdapter` |
| `.adapter.jwt` | JWT issuance/verification | `JwtProvider` |
| `.adapter.redis` | Redis (cache) adapters | `RedisTokenStore` |

When adding a new technology adapter, create a new subpackage named after that technology.

> Konsist enforces both: `*UseCase` interfaces must live under `..port.inbound..`, and `*Port` interfaces (except `shared`'s cross-domain ports, e.g. `UserAuthPort`) must live under `..port.outbound..` (`module-architecture-test/.../ArchitectureTest.kt`).

## Domain Entities vs JPA Entities

- **Domain entities** (`*-domain`, Kotlin): own the business rules, no `@Entity`, no Spring/JPA imports
- **JPA entities** (`*-adapter-out`, Java): use `@Entity`, `*JpaEntity` suffix, work around Kotlin immutability/JPA proxy compatibility issues
- **Put `@Column(nullable = false)` on every logically-required column** (a recurring review point). A column that's an invariant (`id`, `name`, enum-backed fields like `solarTermName`/`cheonganSipseong`, …) must declare `nullable = false` so the DDL constraint matches the domain invariant — don't leave it implicitly nullable.
- The `*JpaEntity` naming is deliberate: exposing the persistence technology in the name is fine — encouraged, even — at the **adapter** layer. It signals a persistence-only object and helps prevent a JPA entity from being accidentally used in the domain layer. (The "don't expose technology in the name" rule applies to the **domain** layer, not adapters.)

## DB PKs

Every PK is a **time-based UUIDv7**. Using `UUID.randomUUID()` (v4) is forbidden.
Generate it with the Kotlin stdlib's (2.3+) `kotlin.uuid.Uuid.generateV7()`, then convert to the `java.util.UUID` the domain uses.
```kotlin
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
val id: UUID = Uuid.generateV7().toJavaUuid()  // domain entity / value object
```
> `UUID.ofVersion7()` does not exist in the JDK (don't use it). Always use `Uuid.generateV7()`.

### `@ExperimentalUuidApi` (opt-in) rule

`Uuid.generateV7()` is still an experimental stdlib API ([Kotlin docs](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/generate-v7.html)), so every declaration that calls it (even transitively) must opt in. **Use the propagating marker `@ExperimentalUuidApi`, not `@OptIn(ExperimentalUuidApi::class)`.**

- **Why propagation instead of `@OptIn`**: `@OptIn` swallows the experimental requirement at that point, while `@ExperimentalUuidApi` re-exposes it so the caller makes the same conscious choice. This keeps the whole UUIDv7 chain honest, and it's already an established convention (`Member.create`, `SajuChart.create`, `CreateSajuChartService.create`).
- **Where to put it**: on the domain factory (`companion object { @ExperimentalUuidApi fun create(...) }`), on the **override** in the service that calls it, and anywhere that calls the **concrete type** directly like a `*ServiceTest` class (annotate the test class).
- **Where not to put it**: leave inbound `*UseCase`/`shared` **port interfaces unannotated** — this is the boundary of propagation, so controllers/other domains that call through the interface type don't need to opt in (see `CreateSajuChartPort` ← `SignupService`).

## Cross-Domain References

- Direct references between domain entities are forbidden
- Going through a `shared` port is only warranted when a domain must **branch or act** on another domain's data within its own use case — not when it merely needs to display it. If the caller's logic doesn't depend on the result at all, let the client call the other domain's `*UseCase` directly instead of coupling the two domains on the backend.
- Example: `LoginService` must branch between issuing a token and issuing an onboarding token depending on whether the member already exists, so it goes through `shared.GetMemberPort` ← implemented by `member-adapter-out` (`GetMemberAdapter`), not `member-application` — a port implementation is an adapter, not a use-case service.
- Currently (monolith), `GetMemberAdapter` queries `MemberRepository` via JPA, but once `member` is split out for MSA, only that adapter gets swapped for an HTTP client — `auth-application`'s code doesn't change, because the port is the stable contract.
- Counter-example: "show the member their own profile screen" needs no branching in another domain, so no cross-domain port is needed — the client calls member's `*UseCase` directly.

## Swagger Patterns

Swagger annotations are handled **only in the `*Api` interface**. `*Controller` merely implements it and never attaches Swagger annotations (`@Operation`, `@Parameter`, `@Tag`) directly.

Structure the docs so **① the API description and ② the parameters** are visible. Since the response is wrapped in the common envelope `CommonResponse` (→ see "Response Format" below), **springdoc auto-documents success/failure responses from the return type `CommonResponse<T>` and `GlobalExceptionHandler`**, so don't write `@ApiResponses` yourself.

```kotlin
// *-adapter-in module (com.yapp.todakun.{domain}.adapter.web)
@Tag(name = "User", description = "User API")
interface UserApi {
  @Operation(
    summary = "Get my info",
    description = "Returns the authenticated user's own profile.",
  )
  @GetMapping("/me")
  fun getMe(
    @Parameter(hidden = true) userId: UserId,
  ): ResponseEntity<CommonResponse<UserResponse>>

  @Operation(summary = "Check nickname availability", description = "A public API callable without authentication.")
  @DisableSwaggerSecurity // No auth required → removes the lock icon from the Swagger docs
  @GetMapping("/nickname/check")
  fun checkNickname(
    @Parameter(description = "Nickname to check", example = "todak")
    @RequestParam nickname: String,
  ): ResponseEntity<CommonResponse<Boolean>>
}

@RestController
@RequestMapping("/users")
class UserController(
  private val getUserUseCase: GetUserUseCase,
) : UserApi {
  override fun getMe(userId: UserId): ResponseEntity<CommonResponse<UserResponse>> =
    CommonResponse.retrieved(UserResponse.from(getUserUseCase.getUser(userId)))
}
```

Rules:
- **For APIs that don't require authentication**, put `@DisableSwaggerSecurity` (`com.yapp.todakun.web.openapi.annotation`) on the `*Api` method. bootstrap's springdoc `OperationCustomizer` removes that method's security requirement (lock icon) from the docs.
- Write both `summary` and `description` on `@Operation`.
- Attach `@Parameter(description, example)` to parameters for description/examples. (Applies equally to `@PathVariable`/`@RequestParam`/`@RequestBody`.)
- Response schema/examples are auto-generated from the return type `CommonResponse<T>`, so don't write `@ApiResponses` yourself.
- **Hide server-injected parameters** (a recurring review point). A parameter the server fills in from the `SecurityContext` — `@AuthenticationPrincipal memberId`, a `@BearerToken` access token — must carry `@Parameter(hidden = true)`. Otherwise it looks like an input the client must send in Swagger UI / "Try it out", leading clients to think they actually need to send it.
- **Give enum / whitelist string fields an `example`** (a recurring review point). For `*Request` fields constrained to a fixed value set (`birthTime`, `calendarType`, `gender`, `job`, `relationshipStatus`, …), a regex/validation message alone doesn't tell the client the valid values — add `@Schema(example = "...")` on the field.

> springdoc `OperationCustomizer` skeleton (bootstrap):
> ```kotlin
> @Bean
> fun disableSecurityCustomizer() = OperationCustomizer { operation, handlerMethod ->
>     if (handlerMethod.hasMethodAnnotation(DisableSwaggerSecurity::class.java)) operation.security(emptyList())
>     operation
> }
> ```

## OSIV

`spring.jpa.open-in-view=false` is required. Don't use lazy loading in a Controller.

## Transaction Boundaries

Apply transactions **only on use-case services in `*-application`**. (Forbidden on Controllers and domain entities.)
Declare the transaction by attaching a **CQRS stereotype** (`com.yapp.todakun.common.annotation`) to the service class.

| Annotation | Composition | Purpose |
|------------|-------------|---------|
| `@CommandService` | `@Service` + `@Transactional` | Mutating (create/update/delete) use cases |
| `@QueryService` | `@Service` + `@Transactional(readOnly = true)` | Read use cases |

```kotlin
// CreateUserUseCase/GetUserUseCase come from com.yapp.todakun.{domain}.port.inbound in {domain}-domain
@CommandService
class CreateUserService(...) : CreateUserUseCase { ... }

@QueryService
class GetUserService(...) : GetUserUseCase { ... }
```

- Don't put `@Transactional` on service methods individually (the stereotype applies at the class level). One service has exactly one responsibility: Command or Query.
- These annotations need a `@Transactional` proxy (CGLIB all-open), so `*-application` modules apply the **`todakun.spring` convention plugin** (which includes `kotlin-spring`).
- Since OSIV is off, **always finish lazy loading inside the transaction (the application layer)**.

### Long external calls (AI) — orchestrator + TransactionalStore

An AI call takes several seconds. Holding a transaction — and the row lock inside it — for that whole time exhausts the connection pool and serializes concurrent requests on the same key. So a generation use case splits into **two beans**:

| Bean | Stereotype | Transaction | Role |
|------|-----------|-------------|------|
| `Create{Aggregate}Service` (orchestrator) | **`@Service`** | None | Steps through the stages in order; calls the AI **between** transactions |
| `{Aggregate}TransactionalStore` | `@CommandService` | One per method | Short lock-and-read, lock-and-save transactions |

```
findExistingWithLock()   ← short transaction: acquire lock, read; lock released on commit
        ↓
    AI call              ← no transaction, no lock, no connection held
        ↓
saveIfAbsent()           ← short transaction: re-acquire lock, re-check, save only if still absent
```

Both store methods acquire the lock and re-check before writing, so a concurrent request that finished creating first is **detected** instead of colliding with the unique constraint. Idempotency comes from the re-check in `saveIfAbsent`, not from the lock.

> **The orchestrator must never carry `@CommandService` or `@Transactional`.** Under `REQUIRED` propagation, the store's methods would join the orchestrator's transaction instead of opening their own, silently collapsing this split back into one long transaction holding a lock across the entire AI call — exactly the failure this pattern exists to prevent. Nothing fails loudly when this happens; it only shows up as pool exhaustion under load.

Used by `daily-fortune`, `day-fortune`, `year-fortune`, `compatibility`, `notification`. Each domain's `CLAUDE.md` should not re-explain the pattern itself — only note what's domain-specific (which key the lock uses, what the save transaction shares with).

## DTO ↔ Domain Mapping

- Mapping happens **only in the adapter layer**. The domain knows nothing about `*Request`/`*Response` (no import in `*-domain`/`*-application`) — though the UseCase's own input/output models (`*Command`/`*Result` in `port.inbound`) are owned by the domain, since they're the port's contract, not an adapter DTO.
- Response: a `from(domain)` factory in `*Response`'s `companion object`. The controller wraps it in `CommonResponse` (`*Response` itself knows nothing about the envelope).
- Request: give `*Request` a `toCommand()` / domain-conversion function that builds the `port.inbound` command type.
- Persistence: `toDomain()` / `fromDomain(domain)` on `*JpaEntity` (adapter-out).

```kotlin
data class UserResponse(val id: UUID, val nickname: String) {
  companion object {
    fun from(user: User) = UserResponse(id = user.id, nickname = user.nickname)
  }
}
```

## Response Format

- **Every response uses the common envelope `CommonResponse<T>` (common-web)** (same shape for success/failure).
  ```json
  { "success": true, "code": "COMMON-200", "message": "Retrieval complete",
    "data": { ... }, "timestamp": "2026-06-21T10:00:00" }
  ```
- The controller wraps the `*Response` DTO with a `CommonResponse` factory and returns a `ResponseEntity`.
  - Read `CommonResponse.retrieved(dto)` · create `CommonResponse.created(dto)` · update `CommonResponse.updated()` · delete `CommonResponse.deleted()` · generic `CommonResponse.success(dto)`
  - The HTTP status is decided by the factory from the code (`CommonSuccessCode.status`) (e.g. 201 on create).
- Error responses use the same envelope (`success:false`) and are produced by `GlobalExceptionHandler` (`error-handling` skill).
- `data` is omitted from serialization when null (NON_NULL).

## Request Validation

- Declare input validation with **Bean Validation on the `*Request` DTO** and put `@Valid` on the Controller parameter.
- In Kotlin, specify the annotation target explicitly: `@field:NotBlank`, `@field:Size(...)`, etc.
- A validation failure (`MethodArgumentNotValidException`) is converted to the common error format in the global handler.
- Keep format validation in the DTO, and **domain-rule validation in the domain entity/use case** (separation of concerns).
