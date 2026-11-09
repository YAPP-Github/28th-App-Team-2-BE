# Web API: Swagger, Response Envelope, Request Validation

The detailed reference for the `architecture` skill. Open it when writing a controller, an `*Api` Swagger interface, the response envelope, or request validation.

---

## Swagger Patterns

Swagger annotations are handled **only in the `*Api` interface**. `*Controller` merely implements it and never attaches `@Operation`, `@Parameter`, or `@Tag` directly. Konsist enforces that every `*Controller` implements a `*Api`.

**Why the interface split**: it keeps the controller readable as code (what it calls, what it returns) and the documentation readable as documentation, and it makes the API contract reviewable in one file without scrolling past orchestration logic. It also means a controller refactor cannot silently drop documentation.

Structure the docs so **① the API description and ② the parameters** are visible. Since every response is wrapped in the common envelope `CommonResponse` (below), **springdoc auto-documents success/failure responses from the return type `CommonResponse<T>` plus `GlobalExceptionHandler`** — so do not write `@ApiResponses` by hand. Hand-written response lists go stale against the handler and start lying about which error codes an endpoint can actually return.

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

### Rules

- **Unauthenticated APIs get `@DisableSwaggerSecurity`** (`com.yapp.todakun.web.openapi.annotation`) on the `*Api` method. `bootstrap`'s springdoc `OperationCustomizer` strips that method's security requirement (the lock icon) from the docs. Without it, the docs tell clients to send a token to an endpoint that ignores it.
- **Write both `summary` and `description`** on `@Operation`. `summary` is what shows in the endpoint list; `description` is where the actual contract goes.
- **Attach `@Parameter(description, example)`** to parameters — equally for `@PathVariable`, `@RequestParam`, and `@RequestBody`.
- **Hide server-injected parameters** (a recurring review point). Anything the server fills from the `SecurityContext` — `@AuthenticationPrincipal memberId`, a `@BearerToken` access token — must carry `@Parameter(hidden = true)`. Otherwise Swagger UI's "Try it out" shows it as a required input and clients conclude they must send it themselves, which is both wrong and a security-shaped misunderstanding.
- **Give enum / whitelist string fields an `example`** (a recurring review point). For `*Request` fields constrained to a fixed value set (`birthTime`, `calendarType`, `gender`, `job`, `relationshipStatus`, …), a regex and a validation message do not tell the client which values are valid — add `@Schema(example = "...")` on the field. Clients otherwise discover the allowed set by trial and 400.

The customizer that implements `@DisableSwaggerSecurity` is a bean in `bootstrap`'s springdoc configuration; read that file rather than this sketch if you need its exact behavior:

```kotlin
@Bean
fun disableSecurityCustomizer() = OperationCustomizer { operation, handlerMethod ->
    if (handlerMethod.hasMethodAnnotation(DisableSwaggerSecurity::class.java)) operation.security(emptyList())
    operation
}
```

---

## Response Format

**Every response uses the common envelope `CommonResponse<T>` (common-web)** — the same shape for success and failure, so a client parses one structure:

```json
{ "success": true, "code": "COMMON-200", "message": "Retrieval complete",
  "data": { ... }, "timestamp": "2026-06-21T10:00:00" }
```

The controller wraps the `*Response` DTO with a factory and returns a `ResponseEntity`:

| Operation | Factory |
|-----------|---------|
| Read | `CommonResponse.retrieved(dto)` |
| Create | `CommonResponse.created(dto)` |
| Update | `CommonResponse.updated()` |
| Delete | `CommonResponse.deleted()` |
| Generic | `CommonResponse.success(dto)` |

- **The HTTP status comes from the code**, not from the controller — the factory reads `CommonSuccessCode.status` (e.g. 201 on create). Don't pass a status separately; that is how a 200 ends up on a create.
- `*Response` itself knows nothing about the envelope. It is a plain DTO with a `from(domain)` factory, which keeps it reusable and keeps envelope concerns in one place.
- **Error responses use the same envelope** (`success:false`) and are produced by `GlobalExceptionHandler` — see the `error-handling` skill for codes, the `AppException` hierarchy, and per-field validation errors.
- `data` is omitted from serialization when null (NON_NULL), so a no-payload success is `{ "success": true, "code": …, "message": …, "timestamp": … }`.

---

## Request Validation

- Declare input validation with **Bean Validation on the `*Request` DTO** and put `@Valid` on the controller parameter. No `@Valid`, no validation — the annotations are silently inert, which is the single most common way validation "stops working".
- In Kotlin, **specify the annotation target explicitly**: `@field:NotBlank`, `@field:Size(...)`. Without `field:`, the annotation may land on the constructor parameter instead of the backing field and be ignored by the validator.
- A validation failure raises `MethodArgumentNotValidException`, which `GlobalExceptionHandler` converts into the common error format with a per-field `reason` map (`error-handling` skill).
- **Format validation in the DTO, domain-rule validation in the domain entity / use case.** "Is this a well-formed nickname" is a DTO concern; "is this nickname already taken" is a domain concern. Mixing them puts business rules in the adapter layer where they can't be unit-tested without the web stack.

---

## OSIV

`spring.jpa.open-in-view=false` is required. Do not use lazy loading in a controller — by the time the controller runs, the transaction is closed. Resolve everything the response needs inside the `*-application` service (→ `persistence.md`).
