# Module & Package Layout

The detailed reference for the `architecture` skill, and the **canonical source** for this project's package structure — other skills point here instead of restating it. Open it when adding or renaming a module, writing `build.gradle.kts`/`settings.gradle.kts`, or deciding which package a declaration belongs in.

---

## Module Directory Naming

Top-level module **directories** use the `module-{module-name}` prefix (`module-common`, `module-bootstrap`, …) — so that module folders stay grouped at the repo root instead of scattered among config directories. For **nested domains**, only the outer wrapper directory gets the prefix (`module-{domain}/`); the inner layer modules keep plain names (`{domain}-domain`, `{domain}-adapter-in`, …).

Gradle **project paths** drop the `module-` prefix: top-level modules stay flat (`:common`), while a domain's layer modules are **nested** under a sourceless `:{domain}` container, with only the layer name at the leaf (`:auth:domain`, `:auth:adapter-in`).

`settings.gradle.kts` maps each path explicitly:

```kotlin
project(":auth:domain").projectDir = file("module-auth/auth-domain")
```

So the leaf `:auth:domain` lives in the directory `module-auth/auth-domain`. Every table and path in this skill uses the Gradle project-path form.

**Why two naming schemes at all**: the directory prefix is for humans browsing the repo root; the nested Gradle path is so the domain boundary shows up in the project graph (you can run `:auth:domain:test` and know you tested exactly one bounded context's core). Dropping either one costs something: without the prefix, module folders interleave with `.github/`, `gradle/`, `docs/`; without nesting, `:auth-domain` and `:auth-application` are just two of 50 flat siblings with no structural relationship.

**When adding a new domain, register all four modules at once** in `settings.gradle.kts` (`include("{domain}:domain")`, `…:application`, `…:adapter-in`, `…:adapter-out`) plus the `projectDir` mapping for each. A missing `include` fails late and confusingly — the module compiles standalone but is absent from `bootstrap`'s dependency graph, so the app starts without those beans.

---

## Top-level Modules

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `bootstrap` | Spring Boot entry point, integrated security config | All |
| `common` | `AppException`·`ResponseCode`, common exceptions, `@CommandService`/`@QueryService`, utilities | spring-context, spring-tx (lightweight, **no spring-web**) |
| `common-web` | `CommonResponse` (response envelope), `GlobalExceptionHandler`, `CommonErrorCode`/`CommonSuccessCode`, `@DisableSwaggerSecurity` | spring-web, spring-webmvc |
| `common-persistence` | `BaseEntity` (JPA, Java) | spring-data-jpa |
| `common-logging` | `@Loggable` + its KSP processor | — |
| `shared` | Cross-domain ports, domain events, shared value types (UserId, OAuthProvider, UserAuthPort) | None |
| `architecture-test` | Konsist architecture-rule verification | None |

`common-web`'s base package is `com.yapp.todakun.web` (not `…common.web`). `*-adapter-in` modules depend on both `common` and `common-web`.

**Why `common` and `common-web` stay separate even though `common-web` is small**: the split is a compile-time boundary. `common` is on the classpath of every module including `*-domain`; if spring-web lived there, a domain class could import `@RestController` and the "pure domain" rule would become unenforceable by anything except review. Keeping web types in a module that `*-domain` never depends on makes the violation impossible rather than merely discouraged.

---

## Domain Modules (nested as `{domain}/{domain}-*`)

Each domain (`auth`, `member`, `saju`, …) has these four modules.

| Module | Role | Spring dependency |
|--------|------|-------------------|
| `{domain}-domain` | Pure Kotlin domain entities, inbound port interfaces (`*UseCase` + its command/result models, `port.inbound`) and outbound port interfaces (`*Port`, `port.outbound`) | None |
| `{domain}-application` | **Implementations** of the use-case services for `port.inbound` interfaces (`@CommandService`/`@QueryService`) | spring-tx/context (via common), `todakun.spring` convention |
| `{domain}-adapter-in` | REST controllers, DTOs, Swagger interfaces | spring-web, springdoc |
| `{domain}-adapter-out` | JPA (Java), OAuth, JWT, Redis adapters | spring-data-jpa, security, etc. |

---

## Build Conventions (buildSrc convention plugins)

Shared module setup (Kotlin/JVM, ktlint, JDK 25 toolchain, Spring BOM, testing) is applied via **convention plugins in `buildSrc`**. Each module's `build.gradle.kts` applies exactly one convention plugin through a **declarative `plugins {}` block** instead of `apply(plugin = ...)`, and `dependencies {}` adds only that module's own dependencies. Imperative `apply(...)` at root/subprojects is forbidden — it defeats configuration-cache reuse and hides which plugins a module actually has.

| Convention plugin | Composition | Applied to |
|-------------------|-------------|-----------------------|
| `todakun.kotlin-common` | Kotlin/JVM + ktlint + toolchain + BOM + testing | The base for every module (`*-domain`, `common`, `common-web`, `shared`, `architecture-test`) |
| `todakun.spring` | `kotlin-common` + `kotlin-spring` (all-open) | `*-application`; the base for the two adapter plugins below |
| `todakun.adapter-web` | `todakun.spring` + `common-web` + web/security/validation/springdoc | Inbound web adapters (`*-adapter-in`) |
| `todakun.adapter-persistence` | `todakun.spring` + `todakun.lombok` + `common-persistence` + spring-data-jpa + Testcontainers/postgresql | JPA outbound adapters (`*-adapter-out`) — `auth-adapter-out` is the **exception** (Redis/JWT, no JPA → applies `todakun.spring` directly) |
| `todakun.spring-boot` | `todakun.spring` + Spring Boot plugin | The `bootstrap` entry point |
| `todakun.lombok` | `kotlin-lombok` + Lombok (compileOnly/annotationProcessor) | Java JPA entity modules (`common-persistence`; composed into `adapter-persistence`) |

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

**An adapter module applies only its own role plugin** — `todakun.adapter-web` (adapter-in) or `todakun.adapter-persistence` (JPA adapter-out) — and declares only **its own** `project(...)` dependencies (`{domain}:domain`, `{domain}:application`, and `:shared` **only when the module actually references a shared type**), plus **domain-specific** libraries (`spring-ai`, `firebase-admin`, …). Don't re-list the shared web/JPA/Testcontainers stack per module; it lives in the plugin. Re-listing it is how versions drift between modules.

External plugin versions are managed in one place, `buildSrc/build.gradle.kts`. When a new external plugin is needed, add its classpath dependency there and then declare it in the convention plugin. Library versions go in `gradle/libs.versions.toml` and are referenced as `libs.*` — hardcoding a version in a module or convention plugin creates a second source of truth that no one updates.

**The `kotlin-jpa` (no-arg) plugin is not used.** Since JPA entities are written in **Java** (`*JpaEntity.java`), entities don't need no-arg/all-open (→ `persistence.md`). `kotlin-spring` (all-open) is applied not for entities but for **Kotlin beans that get CGLIB-proxied** (`@CommandService`/`@QueryService`, `@Repository` adapters, `@SpringBootApplication`); `*-adapter-out` gets the same proxy support via `todakun.adapter-persistence`, which composes `todakun.spring`. Symptom if a module is missing it: `@Transactional` silently does nothing because the class is `final` and cannot be proxied.

---

## Package Structure

| Module | Package |
|--------|---------|
| `common` | `com.yapp.todakun.common` |
| `common-web` | `com.yapp.todakun.web` |
| `*-domain` | `com.yapp.todakun.{domain}` |
| `*-application` | `com.yapp.todakun.{domain}.application` |
| `*-adapter-in` | `com.yapp.todakun.{domain}.adapter.web` |
| `*-adapter-out` | `com.yapp.todakun.{domain}.adapter.{tech}` |

Package names are always lowercase, no underscores.

### Port subpackages (`*-domain`)

`*-domain` splits port interfaces by direction — an adapted form of the traditional hexagonal `port.in`/`port.out` naming. The adaptation is forced: `in` is a Kotlin reserved word, so a `port.in` package (or `` port.`in` ``) also violates ktlint's `standard:package-name` rule.

| Subpackage | Purpose | Example classes |
|-------------|---------|---------------|
| `.port.inbound` | Inbound ports: `*UseCase` interfaces + their command/result models (a use case's input/output contract lives with the interface, not in `*-application`) | `LoginUseCase`, `LoginCommand`, `LoginResult` |
| `.port.outbound` | Outbound ports: interfaces the domain requires from the outside, implemented by `*-adapter-out` (a JWT filter-type case is implemented by `*-adapter-in` instead) | `AccessTokenPort`, `OAuthPort` |

`*-application` holds only `*Service` classes implementing `port.inbound` interfaces — port interfaces and command/result models do not live there.

**One inbound port per file, and never mix a Query port and a Command port in the same file.** The `@QueryService`/`@CommandService` (CQRS) split must be visible in the file structure, not buried inside a shared `*UseCases.kt` (`GetXUseCase` and `ReadXUseCase` → 2 files). Likewise, split exceptions and DTOs one per file. Full rationale and the recurring review comments behind it: the `code-style` skill's "File Organization" section.

### Adapter subpackages (`*-adapter-out`)

| Subpackage | Purpose | Example classes |
|-------------|---------|---------------|
| `.adapter.persistence` | JPA adapters, JPA entities | `UserJpaAdapter`, `UserJpaEntity` |
| `.adapter.oauth` | OAuth client adapters | `GoogleOAuthAdapter` |
| `.adapter.jwt` | JWT issuance/verification | `JwtProvider` |
| `.adapter.redis` | Redis (cache) adapters | `RedisTokenStore` |

When adding a new technology adapter, create a new subpackage named after that technology. The technology name in the package is intentional at this layer — it tells you at a glance which external system a failure came from.

---

## What Konsist Enforces Here

Package placement is not a convention you have to remember; most of it is a test. `module-architecture-test/src/test/kotlin/com/yapp/todakun/architecture/ArchitectureTest.kt` is the authoritative list — read that file rather than trusting a copy, since it drifts the moment a rule is added. The `konsist` skill explains how to add a rule.

Enforced today (summary, not a substitute for the file): `*UseCase` must live under `..port.inbound..`; `*Port` must live under `..port.outbound..` (except `shared`'s cross-domain ports); `*JpaEntity`/`*Adapter`/`*Api`/`@RestController`/`*Request`/`*Response` must live under `.adapter`; `@CommandService`/`@QueryService` only under `.application`; `*-application` may not import `.adapter.`.
