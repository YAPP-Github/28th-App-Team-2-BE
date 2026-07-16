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

Never add dependencies in the reverse direction. `*-domain` does not depend on any external framework.

> **Module directory naming**: top-level module **directories** use the `module-{module-name}` prefix (`module-common`, `module-bootstrap`, …) so module folders stay grouped at the repo root instead of dispersing among config dirs. For a **nested domain**, only the outer wrapper dir is prefixed (`module-{domain}/`); inner layer modules keep plain names (`{domain}-domain`, `{domain}-adapter-in`, …). The Gradle **project path** drops the `module-` prefix: top-level modules stay flat (`:common`), but a domain's layer modules are **nested** under a source-less `:{domain}` container with just the layer name as leaf (`:auth:domain`, `:auth:adapter-in`). `settings.gradle.kts` maps each path via `project(":path").projectDir = file("module-...")` (leaf `:auth:domain` → dir `module-auth/auth-domain`). Tables/paths below use these Gradle project paths.

## Role of Each Module

### Root-level modules

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `bootstrap` | Spring Boot entry point, integrated Security config | Everything |
| `common` | `AppException`·`ResponseCode`, common exceptions, `@CommandService`/`@QueryService`, utils | spring-context, spring-tx (lightweight, **spring-web forbidden**) |
| `common-web` | `CommonResponse` (response envelope), `GlobalExceptionHandler`, `CommonErrorCode`/`CommonSuccessCode`, `@DisableSwaggerSecurity` | spring-web, spring-webmvc |
| `shared` | UserId, OAuthProvider, UserAuthPort | None |
| `architecture-test` | Konsist architecture-rule verification | None |

> The `common-web` base package is `com.yapp.todakun.web`. `*-adapter-in` modules depend on both `common` and `common-web`.

### Domain modules (nested as `{domain}/{domain}-*`)

Each domain (`auth`, `user`, ...) has the following 4 modules.

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `{domain}-domain` | Pure Kotlin domain entities, inbound port(`*UseCase` + its command/result models, `port.inbound`) and outbound port(`*Port`, `port.outbound`) interfaces | None |
| `{domain}-application` | Use-case service **implementations** (`@CommandService`/`@QueryService`) of the `port.inbound` interfaces | spring-tx/context (via common), `todakun.spring` convention |
| `{domain}-adapter-in` | REST Controller, DTO, Swagger interface | spring-web, springdoc |
| `{domain}-adapter-out` | JPA(Java), OAuth, JWT, Redis adapters | spring-data-jpa, security, etc. |

## Build Conventions (buildSrc convention plugins)

Shared module config (Kotlin/JVM, ktlint, JDK 25 toolchain, Spring BOM, tests) is applied via **convention plugins in `buildSrc`**. Each module's `build.gradle.kts` applies a single convention plugin through the **declarative `plugins {}` block** instead of `apply(plugin = ...)`, and adds only its module-specific dependencies to `dependencies {}`. (Imperative `apply(...)` in root/subprojects is forbidden.)

| Convention plugin | Composition | Modules it applies to |
|-------------------|-------------|-----------------------|
| `todakun.kotlin-common` | Kotlin/JVM + ktlint + toolchain + BOM + tests | Base for all modules (`*-domain`, `common`, `common-web`, `shared`, `architecture-test`) |
| `todakun.spring` | `kotlin-common` + `kotlin-spring` (all-open) | Modules needing Spring bean proxies (`*-application`, `*-adapter-in`, `*-adapter-out`) |
| `todakun.spring-boot` | `todakun.spring` + Spring Boot plugin | The entry point `bootstrap` |

> **The `kotlin-jpa` (no-arg) plugin is not used.** Since JPA entities are written in **Java** (`*JpaEntity.java`), entities need no no-arg/all-open (→ "Domain entity vs JPA entity"). `kotlin-spring` (all-open) is applied not for entities but for **Kotlin beans proxied by CGLIB** (`@CommandService`/`@QueryService`, `@Repository` adapters, `@SpringBootApplication`); `*-adapter-out` also uses `todakun.spring` for Kotlin `@Repository` adapter proxies.

```kotlin
// e.g. {domain}-application/build.gradle.kts
plugins {
  id("todakun.spring")
}
dependencies {
  // :common is auto-injected by the todakun.kotlin-common convention plugin (no per-module declaration needed).
  implementation(project(":shared"))
  implementation(project(":{domain}:domain"))
}
```

> External plugin versions are managed in one place, `buildSrc/build.gradle.kts`. If a new external plugin is needed, add its classpath dependency there and then declare it in a convention plugin.

## Package Structure

| Module | Package |
|--------|---------|
| `common` | `com.yapp.todakun.common` |
| `common-web` | `com.yapp.todakun.web` |
| `*-domain` | `com.yapp.todakun.{domain}` |
| `*-application` | `com.yapp.todakun.{domain}.application` |
| `*-adapter-in` | `com.yapp.todakun.{domain}.adapter.web` |
| `*-adapter-out` | `com.yapp.todakun.{domain}.adapter.{tech}` |

`*-domain` separates port interfaces by direction (traditional hexagonal `port.in`/`port.out` naming, adjusted since `in` is a hard keyword in Kotlin and a literal `port.in`/`` port.`in` `` package also fails ktlint's `standard:package-name` rule).

| Sub-package | Purpose | Example class |
|-------------|---------|---------------|
| `.port.inbound` | Inbound port: `*UseCase` interface + its command/result models (the use case's input/output contract lives with the interface, not in `*-application`) | `LoginUseCase`, `LoginCommand`, `LoginResult` |
| `.port.outbound` | Outbound port: interface the domain requires from the outside, implemented by `*-adapter-out` (or `*-adapter-in` for things like JWT filters) | `AccessTokenPort`, `OAuthPort` |

`*-application` holds only the `*Service` classes that implement `port.inbound` interfaces — no port interfaces or command/result models live there.

`*-adapter-out` separates sub-packages by technology.

| Sub-package | Purpose | Example class |
|-------------|---------|---------------|
| `.adapter.persistence` | JPA adapter, JPA entity | `UserJpaAdapter`, `UserJpaEntity` |
| `.adapter.oauth` | OAuth client adapter | `GoogleOAuthAdapter` |
| `.adapter.jwt` | JWT issuance/verification | `JwtProvider` |
| `.adapter.redis` | Redis (cache) adapter | `RedisTokenStore` |

When adding a new technology adapter, create a new sub-package named after the technology.

> Konsist enforces both: `*UseCase` interfaces must reside in `..port.inbound..`, and `*Port` interfaces (except cross-domain ports in `shared`, e.g. `UserAuthPort`) must reside in `..port.outbound..` (`module-architecture-test/.../ArchitectureTest.kt`).

## Domain Entity vs JPA Entity

- **Domain entity** (`*-domain`, Kotlin): owns business rules, no `@Entity`, Spring/JPA imports forbidden
- **JPA entity** (`*-adapter-out`, Java): uses `@Entity`, `*JpaEntity` suffix, works around Kotlin immutability/JPA proxy compatibility

## DB PK

All PKs are **time-based UUIDv7**. Using `UUID.randomUUID()` (v4) is forbidden.
Generate via Kotlin stdlib (2.3+) `kotlin.uuid.Uuid.generateV7()`, then convert to the `java.util.UUID` the domain uses.
```kotlin
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
val id: UUID = Uuid.generateV7().toJavaUuid()  // domain entity / value object
```
> `UUID.ofVersion7()` does not exist in the JDK (do not use it). Always use `Uuid.generateV7()`.

## Cross-Domain References

- Direct references between domain entities are forbidden
- Going through a `shared` port is only warranted when a domain's own use case must branch/act on another domain's data — not whenever data merely needs to be displayed. If nothing about the caller's logic depends on the result, let the client call the other domain's own `*UseCase` directly instead of coupling the two domains on the backend.
- Example: `LoginService` must branch between issuing tokens vs. an onboarding token depending on whether the member already exists, so it goes through `shared.GetMemberPort` ← implemented by `member-adapter-out` (`GetMemberAdapter`), not `member-application` — the port implementation is an adapter, not a use-case service.
- Today (monolith) `GetMemberAdapter` queries `MemberRepository` via JPA; once `member` is split out for MSA, only that adapter is swapped for an HTTP client — `auth-application`'s code doesn't change, because the port is the stable contract.
- Counter-example: "show the member's own profile screen" needs no branching in another domain, so it doesn't need a cross-domain port — the client calls member's own `*UseCase` directly.

## Swagger Patterns

Swagger annotations are **handled exclusively on the `*Api` interface**. `*Controller` only implements it and does not attach Swagger annotations (`@Operation`, `@Parameter`, `@Tag`) directly.

Structure the docs so that **① the API description and ② the parameters** are visible. Responses are wrapped in the common envelope `CommonResponse` (→ "Response Format" below); since **success/failure responses are auto-documented by springdoc from the return type `CommonResponse<T>` and the `GlobalExceptionHandler`**, do not write `@ApiResponses` by hand.

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
  @DisableSwaggerSecurity // API requiring no auth → removes the lock icon from the Swagger docs
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
- For **APIs requiring no authentication**, attach `@DisableSwaggerSecurity` (`com.yapp.todakun.web.openapi.annotation`) to the `*Api` method. bootstrap's springdoc `OperationCustomizer` removes that method's security requirement (lock icon) from the docs.
- Write both `summary` + `description` in `@Operation`.
- Annotate parameters with `@Parameter(description, example)` for descriptions/examples. (For `@PathVariable`/`@RequestParam`/`@RequestBody` alike.)
- Response schemas/examples are auto-generated from the return type `CommonResponse<T>`, so do not write `@ApiResponses` by hand.

> springdoc `OperationCustomizer` skeleton (bootstrap):
> ```kotlin
> @Bean
> fun disableSecurityCustomizer() = OperationCustomizer { operation, handlerMethod ->
>     if (handlerMethod.hasMethodAnnotation(DisableSwaggerSecurity::class.java)) operation.security(emptyList())
>     operation
> }
> ```

## OSIV

`spring.jpa.open-in-view=false` is required. Do not use lazy loading in the Controller.

## Transaction Boundaries

Transactions are applied **only on use-case services in `*-application`**. (Forbidden in Controllers and domain entities.)
Declare transactions by attaching a **CQRS stereotype** (`com.yapp.todakun.common.annotation`) to the service class.

| Annotation | Composition | Purpose |
|------------|-------------|---------|
| `@CommandService` | `@Service` + `@Transactional` | Mutating (create/update/delete) use cases |
| `@QueryService` | `@Service` + `@Transactional(readOnly = true)` | Query use cases |

```kotlin
// CreateUserUseCase/GetUserUseCase come from {domain}-domain's com.yapp.todakun.{domain}.port.inbound
@CommandService
class CreateUserService(...) : CreateUserUseCase { ... }

@QueryService
class GetUserService(...) : GetUserUseCase { ... }
```

- Do not attach `@Transactional` separately on service methods (the stereotype applies at class level). A single service has only one responsibility: Command or Query.
- These annotations need a `@Transactional` proxy (CGLIB all-open), so apply the **`todakun.spring` convention plugin** (which includes `kotlin-spring`) to `*-application` modules.
- Since OSIV is off, **always finish lazy loading inside the transaction (application layer)**.

## DTO ↔ Domain Mapping

- Mapping happens **only in the adapter layer**. The domain knows nothing of `*Request`/`*Response` (no importing them in `*-domain`/`*-application`) — but it does own the UseCase's own input/output models (`*Command`/`*Result` in `port.inbound`), since those are the port's contract, not adapter DTOs.
- Response: a `from(domain)` factory in the `companion object` of `*Response`. The controller wraps it in `CommonResponse` (`*Response` itself knows nothing of the envelope).
- Request: a `toCommand()` / domain-conversion function on `*Request`, building the `port.inbound` command type.
- Persistence: `toDomain()` / `fromDomain(domain)` on `*JpaEntity` (adapter-out).

```kotlin
data class UserResponse(val id: UUID, val nickname: String) {
  companion object {
    fun from(user: User) = UserResponse(id = user.id, nickname = user.nickname)
  }
}
```

## Response Format

- **All responses use the common envelope `CommonResponse<T>` (common-web)** (same shape for success and failure).
  ```json
  { "success": true, "code": "COMMON-200", "message": "Retrieval complete",
    "data": { ... }, "timestamp": "2026-06-21T10:00:00" }
  ```
- The controller wraps the `*Response` DTO with a `CommonResponse` factory and returns it as a `ResponseEntity`.
  - Retrieve `CommonResponse.retrieved(dto)` · create `CommonResponse.created(dto)` · update `CommonResponse.updated()` · delete `CommonResponse.deleted()` · generic `CommonResponse.success(dto)`
  - The HTTP status is decided by the factory from the code (`CommonSuccessCode.status`) (e.g. 201 for creation).
- Error responses use the same envelope (`success:false`) and are produced by `GlobalExceptionHandler` (`error-handling` skill).
- `data` is omitted from serialization when it has no value (NON_NULL).

## Request Validation

- Declare input validation as **Bean Validation on the `*Request` DTO** and attach `@Valid` to the Controller parameter.
- In Kotlin, specify the annotation target: `@field:NotBlank`, `@field:Size(...)`, etc.
- Validation failures (`MethodArgumentNotValidException`) are converted to the common error format in the global handler.
- Format validation goes in the DTO; **domain-rule validation goes in the domain entity/use case** (separation of roles).
