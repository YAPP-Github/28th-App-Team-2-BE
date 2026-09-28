---
name: code-style
description: Load when writing or modifying Kotlin code. Naming conventions, package structure, project-specific review points. Official Kotlin formatting/idiom details live in references/kotlin-conventions.md.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Code Style Rules

Passing ktlint is required: `./gradlew ktlintCheck` / auto-fix: `./gradlew ktlintFormat`

> Official Kotlin naming, formatting, and idioms (class headers/functions/properties/annotations/control flow/lambdas/chained calls/trailing commas/doc comments, etc.) live in `references/kotlin-conventions.md` — `ktlintFormat` auto-fixes most of it, so you rarely need to look it up; open it only for a judgment call ktlint can't make (e.g. choosing between `if`/`when` expression forms, null-handling idioms).

---

## 1. Naming — project-specific rules

| Type | Rule | Example |
|------|------|---------|
| Domain entity (Kotlin) | Domain noun | `User` |
| JPA entity (Java) | `*JpaEntity` | `UserJpaEntity` |
| Inbound port | `*UseCase` | `GetUserUseCase` |
| Outbound port | `*Repository` / `*Port` | `UserRepository` |
| Adapter implementation | `*Adapter` | `UserJpaAdapter` |
| Swagger interface | `*Api` | `UserApi` |
| Controller | `*Controller` | `UserController` |
| Request DTO | `*Request` | `UpdateUserRequest` |
| Response DTO | `*Response` | `UserResponse` |
| Response code enum | `*ErrorCode` / `*SuccessCode` (implements `ResponseCode`) | `UserErrorCode` |
| Command service | `@CommandService` + `*Service` | `CreateUserService` |
| Query service | `@QueryService` + `*Service` | `GetUserService` |

### Naming consistency (a recurring PR review point)

These come up over and over in review, so apply them from the start.

- **Put the target entity's noun in the name.** The use case that withdraws a *member* is `WithdrawMemberUseCase`, not bare `WithdrawUseCase` — match the existing sibling class (`UpdateMemberUseCase`). The same noun must run through the entire vertical slice.
- **When one name changes, change the whole family in the same commit.** Renaming one name means renaming its siblings and tests too: `*UseCase` · `*Command` · `*Request` · `*Service` · `*ServiceTest`. A half-renamed slice (`WithdrawMemberService` while `WithdrawRequest` stays as-is) is exactly what a reviewer will catch.
- **Name `shared` ports with a concrete resource/entity noun, never a vague one.** `DeleteMemberSajusPort` (concrete `Sajus`), not `DeleteMemberSajuDataPort` (vague `Data`) — follow the sibling ports (`CreateSajuChartPort`, `RevokeMemberTokensPort`, `CreateMemberPort`).
- **Use the domain's actual id name.** A member id is `memberId`, never a generic `userId`, even inside a `shared` port signature — stay consistent with the rest of the codebase.
- **Pick the verb prefix by meaning.** `Create*` is pure new-creation (`CreateMemberUseCase`). `Save*` is upsert / create-or-update (`SaveTermsAgreementUseCase`: the first submission inserts, a resubmission updates). Don't default to `Create` for an operation that also updates.

---

## 2. Packages & Modules

**Top-level modules**
```
com.yapp.todakun.common            # AppException, ResponseCode, @CommandService/@QueryService
com.yapp.todakun.web               # common-web: CommonResponse, GlobalExceptionHandler, @DisableSwaggerSecurity
com.yapp.todakun.shared            # UserId, UserAuthPort
com.yapp.todakun.bootstrap         # AppApplication (entry point)
```

**Domain modules** (nested as `{domain}/{domain}-*`)
```
com.yapp.todakun.{domain}                    # {domain}-domain
com.yapp.todakun.{domain}.application        # {domain}-application
com.yapp.todakun.{domain}.adapter.web        # {domain}-adapter-in
com.yapp.todakun.{domain}.adapter.{tech}     # {domain}-adapter-out
```

Example (the auth domain):
```
com.yapp.todakun.auth                  # auth-domain
com.yapp.todakun.auth.application      # auth-application
com.yapp.todakun.auth.adapter.web      # auth-adapter-in
com.yapp.todakun.auth.adapter.jwt      # auth-adapter-out (JWT)
com.yapp.todakun.auth.adapter.oauth    # auth-adapter-out (OAuth)
com.yapp.todakun.auth.adapter.redis    # auth-adapter-out (Redis)
```

- Package names are always **lowercase**, no underscores
- When adding a new domain, register its 4 modules in `settings.gradle.kts` together

### File organization — one public type per file (a recurring PR review point)

Reviewers consistently ask for this split — do it this way from the start instead of merging later.

- **One `*Request`/`*Response` DTO per file**, filename == class name. Don't dump `RegisterDeviceTokenRequest` + `UnregisterDeviceTokenRequest` into a single `DeviceTokenRequests.kt`.
- **One `*UseCase` per file.** Don't merge `GetNotificationSettingUseCase` + `UpdateNotificationSettingUseCase` into `NotificationSettingUseCases.kt`.
- **Never mix a Query port and a Command port in one file.** A read port (`GetNotificationsUseCase`) and a command port (`ReadNotificationUseCase`) go in separate files — the `@QueryService`/`@CommandService` (CQRS) boundary must be visible in the file structure, not hidden inside a shared `*UseCases.kt`.
- **One exception per file too.** Split domain exceptions one per file (`NotificationNotFoundException.kt`, `NotificationAccessDeniedException.kt`, `PushSendFailedException.kt`) — don't collect them into one `*Exceptions.kt`. Follow the existing `member`/`saju`/`terms` structure.

---

## 3. Environment Variables & Gradle

- Manage secrets via `.env`; commit only `.env.example`
- Use `build.gradle.kts` (Kotlin DSL). Groovy DSL is forbidden.
