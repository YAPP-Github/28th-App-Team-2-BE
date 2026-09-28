# Domain Scaffolding Template (canonical)

Code template for scaffolding a new domain across its 4 nested hexagonal modules. Substitute `{Domain}`/`{domain}`/`{DOMAIN}` with the domain name.
Procedure (order, verification) follows the `/new-domain` command. This document holds **only the output template**.

## Package Rules

| Module | Package |
|--------|---------|
| `{domain}-domain` | `com.yapp.todakun.{domain}` (entities); `com.yapp.todakun.{domain}.port.inbound` (`*UseCase` + command/result models); `com.yapp.todakun.{domain}.port.outbound` (`*Repository`/`*Port`) |
| `{domain}-application` | `com.yapp.todakun.{domain}.application` (only `*Service` implementations of `port.inbound` interfaces — no port interfaces or command/result models here) |
| `{domain}-adapter-in` | `com.yapp.todakun.{domain}.adapter.web` |
| `{domain}-adapter-out` | `com.yapp.todakun.{domain}.adapter.{tech}` (JPA uses `.adapter.persistence`) |

> `in`/`out` is used instead of the traditional hexagonal naming (`inbound`/`outbound`) is actually reversed here — `in` is a reserved keyword in Kotlin, and even a backtick-escaped `` port.`in` `` package fails ktlint's `standard:package-name` rule — so we use `port.inbound`/`port.outbound` instead.

## build.gradle.kts (per module)

> Shared module configuration is applied via `buildSrc` convention plugins. Imperative `apply(plugin = ...)` is forbidden — apply exactly one convention plugin via a declarative `plugins {}` block (→ architecture skill, "Build Conventions").
>
> `:common` is **auto-injected by the `todakun.kotlin-common` convention plugin** (since every module applies that plugin either directly or via `todakun.spring`), so it is **not declared** in the per-module lists below.

**{domain}-domain:** (pure Kotlin — base convention only)
```kotlin
plugins {
    id("todakun.kotlin-common")
}
dependencies {
    implementation(project(":shared"))
}
```

**{domain}-application:** (applies `todakun.spring` (kotlin-spring) for the `@Transactional` proxy behind `@CommandService`/`@QueryService`)
```kotlin
plugins {
    id("todakun.spring")
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":{domain}:domain"))
}
```

**{domain}-adapter-in:** (applies `todakun.adapter-web` — bundles `common-web` + web/security/validation/springdoc; declare only your own project dependencies)
```kotlin
plugins {
    id("todakun.adapter-web")
}
dependencies {
    implementation(project(":shared")) // only if the adapter actually references shared types
    implementation(project(":{domain}:domain"))
    implementation(project(":{domain}:application"))
}
```

**{domain}-adapter-out:** (applies `todakun.adapter-persistence` — bundles `common-persistence` + spring-data-jpa + Lombok + Testcontainers/postgresql. JPA entities are Java, so `kotlin-jpa` isn't needed; the composed `todakun.spring` provides Kotlin `@Repository` adapter proxying. Add only domain-specific libraries here, e.g. `spring-ai`/`firebase-admin`.)
```kotlin
plugins {
    id("todakun.adapter-persistence")
}
dependencies {
    implementation(project(":{domain}:domain"))
    implementation(project(":shared")) // only if the adapter actually references shared types
}
```

## {domain}-domain (`com.yapp.todakun.{domain}`)

`{Domain}.kt` — Kotlin data class (no Spring/JPA imports). PK is a time-based UUIDv7:
```kotlin
package com.yapp.todakun.{domain}

import java.util.UUID
        import kotlin.uuid.ExperimentalUuidApi
        import kotlin.uuid.Uuid
        import kotlin.uuid.toJavaUuid

        @ExperimentalUuidApi  // UUIDv7 is an experimental stdlib API → use the propagating @ExperimentalUuidApi, not @OptIn (see architecture skill)
        data class {Domain}(
val id: UUID = Uuid.generateV7().toJavaUuid(),
// domain fields
)
```

`{Domain}Repository.kt` — outbound port (`com.yapp.todakun.{domain}.port.outbound`):
```kotlin
package com.yapp.todakun.{domain}.port.outbound

import com.yapp.todakun.{domain}.{Domain}
import java.util.UUID

interface {Domain}Repository {
    fun findById(id: UUID): {Domain}?
    fun save({domain}: {Domain}): {Domain}
}
```

`{Domain}ErrorCode.kt` — domain response code (implements `ResponseCode`, see `error-handling` skill):
```kotlin
package com.yapp.todakun.{domain}

import com.yapp.todakun.common.code.ResponseCode

        enum class {Domain}ErrorCode(
override val code: String,
override val message: String,
override val status: Int,
) : ResponseCode {
    {DOMAIN}_NOT_FOUND("{DOMAIN}-404", "{Domain}을(를) 찾을 수 없습니다", 404),
}
```

`{Domain}NotFoundException.kt` — domain exception (extends the common `NotFoundException`):
```kotlin
package com.yapp.todakun.{domain}

import com.yapp.todakun.common.exception.NotFoundException

class {Domain}NotFoundException : NotFoundException({Domain}ErrorCode.{DOMAIN}_NOT_FOUND)
```

## {domain}-domain — inbound ports (`com.yapp.todakun.{domain}.port.inbound`)

`*UseCase` interfaces and their command/result models live together here (as the port's contract) — not in `{domain}-application`.

`Create{Domain}UseCase.kt`:
```kotlin
package com.yapp.todakun.{domain}.port.inbound

import java.util.UUID

interface Create{Domain}UseCase {
    fun create(command: Create{Domain}Command): UUID
}

data class Create{Domain}Command(/* fields */)
```

`Get{Domain}UseCase.kt`:
```kotlin
package com.yapp.todakun.{domain}.port.inbound

import com.yapp.todakun.{domain}.{Domain}
import java.util.UUID

interface Get{Domain}UseCase {
    fun getById(id: UUID): {Domain}
}
```

## {domain}-application (`com.yapp.todakun.{domain}.application`)

Only the `*Service` **implementations** of `port.inbound` interfaces live here — no port interfaces, no command/result models. Split by responsibility — mutations: `@CommandService`, reads: `@QueryService`. Do not attach `@Transactional` to individual methods. Throw `{Domain}NotFoundException` on a failed lookup (never throw `RuntimeException` directly).

`Create{Domain}Service.kt`:
```kotlin
package com.yapp.todakun.{domain}.application

import com.yapp.todakun.common.annotation.CommandService
        import com.yapp.todakun.{domain}.{Domain}
import com.yapp.todakun.{domain}.port.inbound.Create{Domain}Command
import com.yapp.todakun.{domain}.port.inbound.Create{Domain}UseCase
import com.yapp.todakun.{domain}.port.outbound.{Domain}Repository
        import java.util.UUID

        @CommandService
        class Create{Domain}Service(
        private val {domain}Repository: {Domain}Repository,
) : Create{Domain}UseCase {
    override fun create(command: Create{Domain}Command): UUID =
    {domain}Repository.save({Domain}()).id
}
```

`Get{Domain}Service.kt`:
```kotlin
package com.yapp.todakun.{domain}.application

import com.yapp.todakun.common.annotation.QueryService
        import com.yapp.todakun.{domain}.{Domain}
import com.yapp.todakun.{domain}.{Domain}NotFoundException
import com.yapp.todakun.{domain}.port.inbound.Get{Domain}UseCase
        import com.yapp.todakun.{domain}.port.outbound.{Domain}Repository
        import java.util.UUID

        @QueryService
        class Get{Domain}Service(
        private val {domain}Repository: {Domain}Repository,
) : Get{Domain}UseCase {
    override fun getById(id: UUID): {Domain} =
    {domain}Repository.findById(id) ?: throw {Domain}NotFoundException()
}
```

## {domain}-adapter-out (`com.yapp.todakun.{domain}.adapter.persistence`)

`{Domain}JpaEntity.java` — a Java class (Kotlin immutability/JPA compatibility issue). The id is generated as a UUIDv7 in the domain and passed in, so no separate generator annotation is used:
```java
package com.yapp.todakun.{domain}.adapter.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "{domain}s")
public class {Domain}JpaEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    protected {Domain}JpaEntity() {}

    public {Domain}JpaEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }

    public com.yapp.todakun.{domain}.{Domain} toDomain() {
        return new com.yapp.todakun.{domain}.{Domain}(id);
    }

    public static {Domain}JpaEntity from(com.yapp.todakun.{domain}.{Domain} {domain}) {
        return new {Domain}JpaEntity({domain}.getId());
    }
}
```

`{Domain}JpaRepository.kt`:
```kotlin
package com.yapp.todakun.{domain}.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
        import java.util.UUID

interface {Domain}JpaRepository : JpaRepository<{Domain}JpaEntity, UUID>
```

`{Domain}JpaAdapter.kt`:
```kotlin
package com.yapp.todakun.{domain}.adapter.persistence

import com.yapp.todakun.{domain}.{Domain}
import com.yapp.todakun.{domain}.port.outbound.{Domain}Repository
        import org.springframework.stereotype.Repository
        import java.util.UUID

        @Repository
        class {Domain}JpaAdapter(
        private val jpaRepository: {Domain}JpaRepository,
) : {Domain}Repository {

    override fun findById(id: UUID): {Domain}? =
    jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun save({domain}: {Domain}): {Domain} =
    jpaRepository.save({Domain}JpaEntity.from({domain})).toDomain()
}
```

## {domain}-adapter-in (`com.yapp.todakun.{domain}.adapter.web`)

`{Domain}Api.kt` — interface dedicated to Swagger annotations. Documents the description (`@Operation`) and parameters (`@Parameter`); responses are wrapped in `CommonResponse` (springdoc auto-documents success/failure responses from the return type). Attach `@DisableSwaggerSecurity` to methods that require no authentication:
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.web.openapi.annotation.DisableSwaggerSecurity
        import com.yapp.todakun.web.response.CommonResponse
        import io.swagger.v3.oas.annotations.Operation
        import io.swagger.v3.oas.annotations.Parameter
        import io.swagger.v3.oas.annotations.tags.Tag
        import org.springframework.http.ResponseEntity
        import org.springframework.web.bind.annotation.*
        import java.util.UUID

        @Tag(name = "{Domain}", description = "{Domain} API")
        interface {Domain}Api {

    @Operation(summary = "Create {Domain}", description = "Creates a new {Domain}.")
    @PostMapping
    fun create(
        @RequestBody request: Create{Domain}Request,
    ): ResponseEntity<CommonResponse<{Domain}Response>>

        @Operation(summary = "Get a single {Domain}", description = "Retrieves a {Domain} by ID.")
        @GetMapping("/{id}")
        fun getById(
            @Parameter(description = "{Domain} ID", example = "018f...")
            @PathVariable id: UUID,
        ): ResponseEntity<CommonResponse<{Domain}Response>>

    // Attach @DisableSwaggerSecurity to any public API method that requires no authentication.
}
```

`{Domain}Controller.kt` — implements `{Domain}Api` (does not attach Swagger annotations directly):
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.web.response.CommonResponse
        import com.yapp.todakun.{domain}.port.inbound.Create{Domain}UseCase
        import com.yapp.todakun.{domain}.port.inbound.Get{Domain}UseCase
        import org.springframework.http.ResponseEntity
        import org.springframework.web.bind.annotation.*
        import java.util.UUID

        @RestController
        @RequestMapping("/{domain}s")
        class {Domain}Controller(
        private val create{Domain}UseCase: Create{Domain}UseCase,
private val get{Domain}UseCase: Get{Domain}UseCase,
) : {Domain}Api {

    override fun create(request: Create{Domain}Request): ResponseEntity<CommonResponse<{Domain}Response>> {
    val id = create{Domain}UseCase.create(request.toCommand())
    return CommonResponse.created({Domain}Response(id = id))
}

    override fun getById(id: UUID): ResponseEntity<CommonResponse<{Domain}Response>> =
    CommonResponse.retrieved({Domain}Response.from(get{Domain}UseCase.getById(id)))
}
```

`Create{Domain}Request.kt` — request DTO (domain conversion happens in the adapter layer):
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.{domain}.port.inbound.Create{Domain}Command

        data class Create{Domain}Request(
// request fields
) {
    fun toCommand() = Create{Domain}Command(/* mapping */)
}
```

`{Domain}Response.kt` — response DTO (with a `from(domain)` factory):
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.{domain}.{Domain}
import java.util.UUID

        data class {Domain}Response(
val id: UUID,
) {
    companion object {
    fun from({domain}: {Domain}) = {Domain}Response(id = {domain}.id)
}
}
```

## Key Rules

- `*UseCase` interfaces (+ command/result models) live in `{domain}-domain`'s `port.inbound`; `*Repository`/`*Port` interfaces live in `port.outbound`. `{domain}-application` holds only `*Service` implementations — both placements are enforced by Konsist (`module-architecture-test/.../ArchitectureTest.kt`)
- JPA entities must be **Java classes**; domain entities must never import Spring/JPA
- DB PKs are **time-based UUIDv7** (`Uuid.generateV7().toJavaUuid()`); no separate UUID generator annotation on JPA entities
- Response codes are domain `*ErrorCode`s (implementing `ResponseCode`); exceptions must be **subclasses of `AppException`**, like the common `NotFoundException` (never throw `RuntimeException` directly)
- Declare transactions via `@CommandService` (writes) / `@QueryService` (reads) — no method-level `@Transactional`
- Every controller response uses the `CommonResponse` envelope (`ResponseEntity<CommonResponse<T>>`)
- Attach `@DisableSwaggerSecurity` to APIs that require no authentication; `*Api`'s Swagger holds only the description/parameters (responses are auto-documented from the `CommonResponse<T>` return type — don't write `@ApiResponses` directly)
- Prefer the Kotlin DSL, keep comments minimal
