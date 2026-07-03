# Domain-Scaffolding Templates (canonical)

The code templates used when scaffolding a new domain across the 4 nested-hexagonal modules. Replace `{Domain}`/`{domain}`/`{DOMAIN}` with the domain name.
The procedure (order, verification) follows the `/new-domain` command. This document holds **only the output templates**.

## Package Rules

| Module | Package |
|--------|---------|
| `{domain}-domain` | `com.yapp.todakun.{domain}` |
| `{domain}-application` | `com.yapp.todakun.{domain}.application` |
| `{domain}-adapter-in` | `com.yapp.todakun.{domain}.adapter.web` |
| `{domain}-adapter-out` | `com.yapp.todakun.{domain}.adapter.{tech}` (JPA uses `.adapter.persistence`) |

## build.gradle.kts (per module)

> Shared module config is applied via `buildSrc` convention plugins. Imperative `apply(plugin = ...)` is forbidden — apply a single convention plugin via the declarative `plugins {}` block (→ architecture skill, "Build Conventions").
>
> `:common` is **auto-injected by the `todakun.kotlin-common` convention plugin** (which every module applies, directly or via `todakun.spring`), so it is **not** declared per module below.

**{domain}-domain:** (pure Kotlin — base convention only)
```kotlin
plugins {
    id("todakun.kotlin-common")
}
dependencies {
    implementation(project(":shared"))
}
```

**{domain}-application:** (apply `todakun.spring` (kotlin-spring) for the `@Transactional` proxy of `@CommandService`/`@QueryService`)
```kotlin
plugins {
    id("todakun.spring")
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":{domain}:domain"))
}
```

**{domain}-adapter-in:**
```kotlin
plugins {
    id("todakun.spring")
}
dependencies {
    implementation(project(":common-web"))
    implementation(project(":shared"))
    implementation(project(":{domain}:domain"))
    implementation(project(":{domain}:application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")
}
```

**{domain}-adapter-out:** (JPA entities are Java, so `kotlin-jpa` is unnecessary. Apply `todakun.spring` for the Kotlin `@Repository` adapter proxy)
```kotlin
plugins {
    id("todakun.spring")
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":{domain}:domain"))
    implementation(project(":{domain}:application"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.testcontainers:postgresql")
}
```

## {domain}-domain (`com.yapp.todakun.{domain}`)

`{Domain}.kt` — Kotlin data class (no Spring/JPA imports). PK is time-based UUIDv7:
```kotlin
package com.yapp.todakun.{domain}

import java.util.UUID
        import kotlin.uuid.ExperimentalUuidApi
        import kotlin.uuid.Uuid
        import kotlin.uuid.toJavaUuid

        @OptIn(ExperimentalUuidApi::class)
        data class {Domain}(
val id: UUID = Uuid.generateV7().toJavaUuid(),
// domain fields
)
```

`{Domain}Repository.kt` — outbound port:
```kotlin
package com.yapp.todakun.{domain}

import java.util.UUID

interface {Domain}Repository {
    fun findById(id: UUID): {Domain}?
    fun save({domain}: {Domain}): {Domain}
}
```

`{Domain}ErrorCode.kt` — domain response code (implements `ResponseCode`, `error-handling` skill):
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

## {domain}-application (`com.yapp.todakun.{domain}.application`)

Split UseCase implementations **by responsibility** — mutation: `@CommandService`, query: `@QueryService`. Do not attach `@Transactional` separately on methods. On retrieval failure, throw `{Domain}NotFoundException` (no direct `RuntimeException`).

`Create{Domain}UseCase.kt`:
```kotlin
package com.yapp.todakun.{domain}.application

import java.util.UUID

interface Create{Domain}UseCase {
    fun create(command: Create{Domain}Command): UUID
}

data class Create{Domain}Command(/* fields */)
```

`Get{Domain}UseCase.kt`:
```kotlin
package com.yapp.todakun.{domain}.application

import com.yapp.todakun.{domain}.{Domain}
import java.util.UUID

interface Get{Domain}UseCase {
    fun getById(id: UUID): {Domain}
}
```

`Create{Domain}Service.kt`:
```kotlin
package com.yapp.todakun.{domain}.application

import com.yapp.todakun.common.annotation.CommandService
        import com.yapp.todakun.{domain}.{Domain}
import com.yapp.todakun.{domain}.{Domain}Repository
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
        import com.yapp.todakun.{domain}.{Domain}Repository
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

`{Domain}JpaEntity.java` — Java class (Kotlin immutability/JPA compatibility issue). The id is generated as UUIDv7 in the domain and passed in, so no separate generator annotation is used:
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
import com.yapp.todakun.{domain}.{Domain}Repository
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

`{Domain}Api.kt` — the interface dedicated to Swagger annotations. Document descriptions (`@Operation`) and parameters (`@Parameter`), and wrap responses in `CommonResponse` (success/failure responses are auto-documented by springdoc from the return type). Attach `@DisableSwaggerSecurity` to methods that need no authentication:
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

    // Attach @DisableSwaggerSecurity to methods for public APIs that need no authentication.
}
```

`{Domain}Controller.kt` — the `{Domain}Api` implementation (no Swagger annotations attached directly):
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.web.response.CommonResponse
        import com.yapp.todakun.{domain}.application.Create{Domain}UseCase
        import com.yapp.todakun.{domain}.application.Get{Domain}UseCase
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

`Create{Domain}Request.kt` — request DTO (domain conversion in the adapter layer):
```kotlin
package com.yapp.todakun.{domain}.adapter.web

import com.yapp.todakun.{domain}.application.Create{Domain}Command

        data class Create{Domain}Request(
// request fields
) {
    fun toCommand() = Create{Domain}Command(/* mapping */)
}
```

`{Domain}Response.kt` — response DTO (`from(domain)` factory):
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

## Core Rules

- JPA entities must be **Java classes**; absolutely no Spring/JPA imports in domain entities
- DB PKs are **time-based UUIDv7** (`Uuid.generateV7().toJavaUuid()`); no separate UUID-generator annotation on JPA entities
- Response codes are domain `*ErrorCode` (implementing `ResponseCode`); exceptions are **subclasses of `AppException`** such as the common `NotFoundException` (no direct `RuntimeException`)
- Declare transactions with `@CommandService` (write)/`@QueryService` (read) — no method-level `@Transactional`
- All controller responses use the `CommonResponse` envelope (`ResponseEntity<CommonResponse<T>>`)
- Unauthenticated APIs get `@DisableSwaggerSecurity`; Swagger on `*Api` has only descriptions/parameters (responses are auto-documented from the `CommonResponse<T>` return type — no hand-written `@ApiResponses`)
- Prefer Kotlin DSL, minimize comments