# todakun (토닥운)

Backend server for YAPP 28th App Team 2 (targeting AOS/iOS clients). Domain-oriented nested multi-module + hexagonal architecture, designed with a future MSA migration in mind. Base package: `com.yapp.todakun`

> **Language**: All user-facing responses MUST be written in Korean, without exception (code, identifiers, logs, etc. are excluded).

## Per-Task Loading (Pre-Task)

Detailed rules live in **skills** (`.claude/skills/<name>/SKILL.md`), procedures in **commands** (`.claude/commands/`), and shared code templates in **examples** (`.claude/examples/`). When one of the tasks below is detected, the skill is **auto-loaded** (manual invocation: `/<name>`).

| Task type | Load |
|-----------|------|
| Architecture/module design, ports & adapters, transaction boundaries, DTO mapping, response format, Swagger | `architecture` |
| Writing Kotlin code, naming/formatting, ktlint | `code-style` |
| Writing tests (Kotest/MockK/TestContainer) | `testing` |
| Exception/error handling | `error-handling` |
| Adding/verifying architecture rules (Konsist) | `konsist` |
| Spring AI / Vertex AI (Gemini) · pgvector | `spring-ai` |
| Branch/commit/PR conventions | `git-workflow` |
| Commit · push · create PR | `commit-push-pr` |
| Creating GitHub issues | `create-issue` |
| Applying PR review comments & replying | `resolve-review` |
| Scaffolding a new domain | `/new-domain` (+ `examples/domain-scaffold.md`) |
| Adding a feature to an existing domain | `/new-feature` |
| Full pre-PR verification | `/run-checks` |

> Diagnosing failing tests (without fixing) is delegated to the `test-validator` agent, code review to `code-reviewer`, test writing to `test-writer`, and domain scaffolding to the `domain-scaffolder` agent.

## Critical Constraints

### ❌ Forbidden

| Constraint | Reason |
|------------|--------|
| Importing `org.springframework.*` / `jakarta.persistence.*` in domain entities | Pollutes the pure domain, violates hexagonal architecture |
| Importing the `.adapter.` package from `*-application` | Inverts the dependency direction |
| Direct references between domain entities | Forces going through ports such as `shared.UserAuthPort` |
| Throwing `RuntimeException` directly | Use only subclasses of `AppException` (+ `ResponseCode`) (`error-handling`) |
| `UUID.randomUUID()` (v4) / `UUID.ofVersion7()` (a fake API) | PKs use `Uuid.generateV7().toJavaUuid()` (time-based v7) |
| Depending on spring-web in `common` | Web-common code belongs in the `common-web` module (`common` only has spring-tx/context) |
| Importing `*Request`/`*Response` in domain/use-case layers | DTO mapping belongs only in the adapter layer |
| Committing `.env`, hardcoding secrets | Commit only `.env.example`, inject via `${ENV_VAR}` |
| Hardcoding dependency/plugin versions in `build.gradle.kts` or convention plugins | Pollutes the single version source (`gradle/libs.versions.toml`), causes multi-source management |

### ✅ Required

| Practice | Reason |
|----------|--------|
| Run `./gradlew ktlintFormat` before committing (auto-run by the Stop hook) | Consistent code style |
| Declare transactions on application services via `@CommandService`/`@QueryService` | OSIV off, clear CQRS boundaries |
| Wrap all responses in the `CommonResponse<T>` envelope (common-web) | Consistent client contract |
| `*Controller` implements the `*Api` interface (Swagger on `*Api`; use `@DisableSwaggerSecurity` for unauthenticated APIs) | Konsist-enforced rule |
| JPA entities in Java (`*JpaEntity`), domain entities in Kotlin | Immutability/proxy compatibility |
| Commit messages `[#issue-number] type: description` (in Korean) | Convention (`git-workflow`) |
| Register the 4 modules in `settings.gradle.kts` when adding a new domain | Prevents missing modules |
| Manage all versions in `gradle/libs.versions.toml`, reference via `libs.*` | Single source of truth (SSOT) for versions |

## Tech Stack

| Area | Technology |
|------|------------|
| WAS | Spring Boot 4.1.0 / JDK 25 / Kotlin 2.3.21 / Gradle 9.5.1 |
| DB / Cache | PostgreSQL 17.10 (JPA·Hibernate, pgvector extension) / Redis 7.2 |
| AI | Spring AI / Google Vertex AI (Gemini) / pgvector |
| Test & Lint | Kotest / MockK / TestContainer / Ktlint / Konsist |
| Auth | OAuth 2.0 (Kakao/Google/Apple) + JWT / Refresh Token → Redis |

## Project Structure

Each domain module maintains a bounded-context boundary so it can be extracted as an independent service (detailed package rules in the `architecture` and `code-style` skills).

```
todakun/
├── bootstrap/        # Spring Boot entry point, Security config (jwt/gateway modes)
├── common/           # AppException, ResponseCode, @CommandService/@QueryService (spring-tx/context only)
├── common-web/       # CommonResponse, GlobalExceptionHandler, @DisableSwaggerSecurity (com.yapp.todakun.web)
├── shared/           # Cross-domain sharing (UserId, OAuthProvider, UserAuthPort)
├── {domain}/         # auth, user, ... each domain = 4 modules
│   ├── {domain}-domain/        # Pure Kotlin entities & ports
│   ├── {domain}-application/   # UseCase services (@CommandService/@QueryService)
│   ├── {domain}-adapter-in/    # REST Controller, DTO, Swagger Api
│   └── {domain}-adapter-out/   # JPA(Java), OAuth, JWT, Redis adapters
└── architecture-test/ # Konsist architecture-rule verification
```

**Dependency direction:** `adapter-in`/`adapter-out` → `application` → `domain`; all modules → `common`/`shared`; `bootstrap` → integrates everything.
**Cross-domain references:** `auth-application` → `shared.UserAuthPort` ← implemented by `user-application` (on MSA migration, swap the implementation for an HTTP client).

## Core Development Principles (decisions not covered by skills)

- **Security mode**: `security.mode=jwt` (monolith) → `security.mode=gateway` (after MSA migration, trust the `X-User-Id` header)
- **OSIV disabled**: `spring.jpa.open-in-view=false`
- **AI integration**: `*AiPort` in the domain, Spring AI adapter in adapter-out, VectorStore is pgvector (`spring-ai` skill)
- DDD-based, designed per bounded context. Domain logic belongs in domain entity methods, not in services.

## Profiles

Three per-environment profiles (`local`/`dev`/`prod`). Common config in `application.yaml`, per-environment values split into `application-{profile}.yaml`.

| Property nature | Location |
|-----------------|----------|
| Same value regardless of environment (`jpa.open-in-view`, `ddl-auto: validate`, `dialect`) | `application.yaml` |
| Per-environment values (`datasource`, `data.redis`, `security.mode`, `jwt`, `oauth2`, `logging.level`) | `application-{profile}.yaml` |

- Active profile: `--spring.profiles.active={profile}` or `SPRING_PROFILES_ACTIVE`.
- **Connection info (DB/Redis URLs) goes in each profile file even when the value is identical** (independent per-environment branching). Never hardcode secrets; inject via `${ENV_VAR}` (`.env` must not be committed).

## Commands

```bash
./gradlew clean build                 # Full build (includes tests)
./gradlew test                        # All tests
./gradlew test --tests "GetUserServiceTest"   # Specific class/pattern
./gradlew :architecture-test:test     # Architecture-rule verification (Konsist)
./gradlew ktlintFormat                # Auto-fix code style (verify only: ktlintCheck)
./gradlew :bootstrap:bootRun          # Run locally (default: local profile)
./gradlew :bootstrap:bootRun --args='--spring.profiles.active=dev'   # Specify a profile
```

> Fill in `.env` before running (see `.env.example`). `bootRun` only works from the **`bootstrap` module**, which holds the entry point.
> For full pre-PR verification, use `/run-checks` or `./.claude/scripts/check-all.sh`.