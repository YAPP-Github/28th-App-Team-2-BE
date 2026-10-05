# todakun (토닥운)

Backend server for YAPP 28th App Team 2 (targeting AOS/iOS clients). Domain-oriented nested multi-module + hexagonal architecture, designed with a future MSA migration in mind. Base package: `com.yapp.todakun`

> **Language**: All user-facing responses MUST be written in Korean, without exception (code, identifiers, logs, etc. are excluded).

## Agent Harness Design

This `.claude/` config is the team's **harness** — an *executable* single source of truth that programs the agent's behavior, not mere documentation. Treat it like source code: layered, single-responsibility, reviewed. It raises the team's floor — every member runs the top performer's workflow via one command.

**Component → layered-architecture mapping** (design each artifact like the layer it maps to):

| Harness artifact | Maps to | Responsibility |
|------------------|---------|----------------|
| `CLAUDE.md` | `package.json` / manifest | Small, stable **router**: what exists + when to load it — never the details. |
| User-invoked skills (`/new-domain`, `/new-feature`, `/run-checks`) | Controller | Entry point for a user-triggered procedure. `disable-model-invocation: true` — only a human starts them. |
| Sub-agents (`domain-scaffolder`, `code-reviewer`, `test-*`) | Service | Orchestrate multi-step work in an isolated context. |
| `.claude/skills/<name>/SKILL.md` | SRP component | One knowledge domain = the single source for its rules. Body ≤ 100 lines; detail lives in its `references/`. |
| MCP servers | Adapter / infra | Abstract external systems (GitHub, Notion, …). |
| `.claude/scripts/*` (`check-all.sh`) | Deterministic core | Conventions/verification that must NOT vary — run as code, not LLM judgment. |

**Operating principles** (derived from the layers above):

- **필요성 원칙 — need-to-know / progressive disclosure**: load only the context the current task needs. The *Per-Task Loading* table below is the concrete enforcement — don't dump the whole rulebook up front.
- **Facade**: `SKILL.md` is the entry point and stays **≤ 100 lines** — judgment rules, a checklist, and an index of which `references/` file to open when. Tables, long code and edge cases go into `references/`·`examples/`, which cost nothing until opened. Avoid the *God Skill* / *Spaghetti CLAUDE.md* anti-patterns.
- **Load only on the matching path**: a skill that applies to specific files declares `paths:` in its frontmatter, so it auto-loads only while those files are in play (`konsist` → `module-architecture-test/**`, `fcm` → `module-notification/**`, …). Workflow skills with side effects declare `disable-model-invocation: true` instead.
- **Deterministic → script, judgment → LLM**: conventions (ktlint, EOF newline, layer rules) live in scripts/Konsist; only non-deterministic decisions are left to the agent.
- **"Exception → Question"**: hard-to-reverse or outward-facing actions (delete, deploy, force-push, publish) get a confirmation, never a silent guess. A good agent knows *when to ask*.

## Per-Task Loading (Pre-Task)

Rules, procedures and their long-form detail all live in **skills** (`.claude/skills/<name>/SKILL.md` + its `references/`); code templates shared by several skills live in **examples** (`.claude/examples/`). A skill auto-loads when one of the tasks below is detected — and, for a path-scoped skill, only while its `paths:` match the files in play. Manual invocation: `/<name>`.

| Task type | Load |
|-----------|------|
| **Working inside any `module-{domain}/`** | that domain's `CLAUDE.md` (auto-loaded) — read before changing its code |
| Architecture/module design, ports & adapters, transaction boundaries, DTO mapping, response format, Swagger | `architecture` |
| Writing Kotlin code, naming/formatting, ktlint | `code-style` |
| Writing tests (Kotest/MockK/TestContainer) | `testing` |
| Exception/error handling | `error-handling` |
| Adding/verifying architecture rules (Konsist) | `konsist` |
| Spring AI / Vertex AI (Gemini) · pgvector | `spring-ai` |
| FCM push notifications (Firebase Admin SDK) | `fcm` |
| Branch/commit/PR conventions | `git-workflow` |
| Commit · push · create PR | `commit-push-pr` |
| Creating GitHub issues | `create-issue` |
| Applying PR review comments & replying | `resolve-review` |
| Scaffolding a new domain | `/new-domain` (+ `examples/domain-scaffold.md`) |
| Adding a feature to an existing domain | `/new-feature` |
| Full pre-PR verification | `/run-checks` |
| Using an external skill suite (gstack / superpowers / compound-engineering / mattpocock) | `external-harness` |

> Diagnosing failing tests (without fixing) is delegated to the `test-validator` agent, code review to `code-reviewer`, test writing to `test-writer`, and domain scaffolding to the `domain-scaffolder` agent.
>
> **External suites are user-scoped** — `git clone` does not bring them. `external-harness` is the single routing authority over them: it decides which external skill runs in each phase and which are banned because our own harness already owns that phase.

## Critical Constraints

### ❌ Forbidden

| Constraint | Reason |
|------------|--------|
| Importing `org.springframework.*` / `jakarta.persistence.*` in domain entities | Pollutes the pure domain, violates hexagonal architecture |
| Importing the `.adapter.` package from `*-application` | Inverts the dependency direction |
| Direct references between domain entities | Must go through a `shared` port instead (e.g. `shared.GetMemberIdPort`) |
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
| Follow the module directory ↔ Gradle path naming rules — the rule itself lives in the `architecture` skill (`references/module-layout.md`) | Keeps module folders grouped at the repo root (prevents dispersion) + mirrors the domain boundary in the Gradle project graph |
| Register the 4 modules under the `:{domain}` container in `settings.gradle.kts` when adding a new domain (`include("{domain}:domain")`, …) | Prevents missing modules |
| Manage all versions in `gradle/libs.versions.toml`, reference via `libs.*` | Single source of truth (SSOT) for versions |

## Tech Stack

| Area | Technology |
|------|------------|
| WAS | Spring Boot 4.1.0 / JDK 25 / Kotlin 2.3.21 / Gradle 9.5.1 |
| DB / Cache | PostgreSQL 17.10 (JPA·Hibernate, pgvector extension) / Redis 7.2 |
| AI | Spring AI / Google Vertex AI (Gemini) / pgvector |
| Push | FCM (Firebase Cloud Messaging) / Firebase Admin SDK (ADC auth) |
| Test & Lint | Kotest / MockK / TestContainer / Ktlint / Konsist |
| Auth | OAuth 2.0 (Kakao/Google/Apple) + JWT / Refresh Token → Redis |

## Project Structure

Each domain module keeps a bounded-context boundary so it can later be extracted as an independent service. Directory ↔ Gradle-path naming, per-module roles, and package rules all live in the `architecture` skill.

```
todakun/
├── module-bootstrap/           # (:bootstrap) Spring Boot entry point, Security config (jwt/gateway modes)
├── module-common/              # (:common) AppException, ResponseCode, @CommandService/@QueryService
├── module-common-web/          # (:common-web) CommonResponse, GlobalExceptionHandler, @DisableSwaggerSecurity
├── module-common-persistence/  # (:common-persistence) BaseEntity (JPA, Java)
├── module-common-logging/      # (:common-logging) @Loggable + its KSP processor
├── module-shared/              # (:shared) cross-domain ports, domain events, shared value types
├── module-{domain}/            # (:{domain}) one per bounded context; each is 4 nested modules
│   ├── {domain}-domain/        # (:{domain}:domain) pure Kotlin entities & ports
│   ├── {domain}-application/   # (:{domain}:application) UseCase services (@CommandService/@QueryService)
│   ├── {domain}-adapter-in/    # (:{domain}:adapter-in) REST controller, DTO, Swagger Api
│   └── {domain}-adapter-out/   # (:{domain}:adapter-out) JPA(Java), OAuth, JWT, Redis adapters
└── module-architecture-test/   # (:architecture-test) Konsist architecture-rule verification
```

**Dependency direction:** `adapter-in`/`adapter-out` → `application` → `domain`; all modules → `common`/`shared`; `bootstrap` integrates everything.

**Every domain carries its own `module-{domain}/CLAUDE.md`** — that context's overview, module structure, boundary, cross-domain contracts, and domain-specific decisions and traps. It loads automatically when working inside the module, so read it before changing anything there rather than inferring the boundary from code. Template and authoring rules: `.claude/examples/domain-claude-md.md`.

**Cross-domain references:** 다른 도메인의 데이터를 단순 조회하는 게 아니라, 그 데이터로 **자기 도메인이 분기·실행**해야 할 때만 `shared` 포트를 거친다. 단순 조회라면 포트를 만들지 말고 상대 도메인의 `*UseCase`를 클라이언트가 직접 호출한다. 포트 구현을 `-application`에 둘지 `-adapter-out`에 둘지는 **유스케이스 로직의 유무**로 갈리며, 양쪽 실례와 판단 기준은 `module-saju/CLAUDE.md`(application 쪽)와 `module-member/CLAUDE.md`(adapter-out 쪽)에 있다.

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
