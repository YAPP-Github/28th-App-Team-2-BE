---
name: error-handling
description: Load when handling exceptions/errors. AppException + ResponseCode, common exceptions, global handler, CommonResponse error format, Bean Validation conversion.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Error-Handling Rules

## Building Blocks

| Element | Location | Role |
|---------|----------|------|
| `ResponseCode` (interface) | `common` (`com.yapp.todakun.common.code`) | The `code`·`message`·`status` (HTTP) contract. Implemented by both success and error codes |
| `AppException` (abstract) | `common` (`...common.exception`) | Base for all business exceptions. Carries `errorCode: ResponseCode` |
| Common exceptions (`NotFoundException`, etc.) | `common` (`...common.exception`) | Thin wrappers by meaning: `BadRequest`/`Unauthorized`/`Forbidden`/`NotFound`/`Conflict` |
| Domain `*ErrorCode` (enum) | each `{domain}-domain` | Implements `ResponseCode`. Defines per-domain codes |
| `CommonErrorCode` (enum) | `common-web` | Domain-independent common error codes |
| `GlobalExceptionHandler` | `common-web` (`...web.exception.handler`) | `@RestControllerAdvice`. Converts every exception to a `CommonResponse` |

## Exception Hierarchy

```
AppException (common, errorCode: ResponseCode)
├── BadRequestException / UnauthorizedException / ForbiddenException / NotFoundException / ConflictException (common)
│     └── UserNotFoundException, etc. — domain-specific exceptions ({domain}-domain)
└── (a domain may extend AppException directly)
```

## Defining Codes (per-domain `*ErrorCode`)

Define domain codes as an enum implementing `ResponseCode` (the code carries the HTTP status).

```kotlin
// {domain}-domain : com.yapp.todakun.user
enum class UserErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    USER_NOT_FOUND("USER-404", "사용자를 찾을 수 없습니다", 404),
    DUPLICATE_NICKNAME("USER-409", "이미 사용 중인 닉네임입니다", 409),
}

class UserNotFoundException : NotFoundException(UserErrorCode.USER_NOT_FOUND)
```

## Global Exception Handler

`GlobalExceptionHandler` (`@RestControllerAdvice`) is placed **only once, in the common-web module**, and is registered globally via bootstrap's component scan.

- **Catch `AppException` alone** and decide the HTTP status from `errorCode.status` (no branching by exception type needed).
- Map framework exceptions to `CommonErrorCode` with the **correct 4xx status — never let a client error fall through to 500** (recurring review point):
  - `MethodArgumentNotValidException` (Bean Validation) → 400 `VALIDATION_ERROR`
  - `HttpMessageNotReadableException` (malformed/broken JSON body) → 400 — this also covers `MissingKotlinParameterException` (a required field absent), since that is a subtype thrown during Jackson deserialization. (Optional enhancement raised in review: give it a dedicated handler that reports the missing field via the same per-field `reason` map, using the exception's `parameter.name`.)
  - `NoResourceFoundException` (unknown URL) → 404 `NOT_FOUND`
  - `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` → 400
- **Per-field validation errors use the `reason: Map<String, String>` field** on `CommonResponse` (field → message); `message` is omitted (`String?`) when `reason` is present, so multiple simultaneous field failures are each reported.
- Log 4xx as `warn`, 5xx as `error` with the stack trace.

Response format — the same `CommonResponse` envelope as success (`success:false`):
```json
{ "success": false, "code": "USER-404", "message": "사용자를 찾을 수 없습니다", "timestamp": "2026-06-21T10:00:00" }
```

## Principles

- Do not throw `RuntimeException` directly. Always use `AppException` (or a common/domain subclass).
- **Never let an exception escape to Spring's default error handling** (recurring review point). A bare `throw`, `error(...)`, or `IllegalStateException` from a domain/adapter bypasses `GlobalExceptionHandler` and returns Spring's default body instead of the `CommonResponse` envelope — breaking the client contract. Wrap the failure in an `AppException` subclass with a domain `*ErrorCode` (e.g. resource load failure in the manseryeok converter → `SajuCalculationException` / `SAJU_CALCULATION_FAILED`).
- Express the HTTP status **only via `ResponseCode.status`**. Do not hardcode the status in the handler.
- Write exception messages in Korean.
- Do not catch business exceptions directly in the Controller layer.
- Format-validation failures (Bean Validation) are also converted to the same `CommonResponse` format in the global handler (the `code` is `CommonErrorCode.VALIDATION_ERROR`).
