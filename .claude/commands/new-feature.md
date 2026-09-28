---
name: new-feature
description: Add a new feature (UseCase → Service → Api → Controller) to an existing domain
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Add a new feature to an existing domain.

Feature description: $ARGUMENTS

> Follow the code patterns in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md) and the `architecture` skill.

## Steps

1. **Define the inbound port** (`{domain}-application`) — `{FeatureName}UseCase.kt`.
2. **Implement the service** (`{domain}-application`) — `@CommandService` for mutations, `@QueryService` for reads. Add to an existing `*Service` or create a new one. Never put `@Transactional` directly on a method.
3. **API interface** (`{domain}-adapter-in`) — add the endpoint to the existing `{Domain}Api.kt`. Return `ResponseEntity<CommonResponse<T>>`. Use only `@Operation(summary, description)` + `@Parameter`; unauthenticated APIs get `@DisableSwaggerSecurity`. (Responses are auto-documented from the return type, so `@ApiResponses` isn't needed.)
4. **Create DTOs** (`{domain}-adapter-in`) — add `*Request`/`*Response` as needed. `CommonResponse` handles the envelope.
5. **Implement the controller** (`{domain}-adapter-in`) — implement it in the existing `{Domain}Controller.kt`. Wrap with `CommonResponse.success/created/retrieved/...`.
6. **Verify** — `./gradlew ktlintCheck` → `./gradlew :architecture-test:test`.

## Principles

- When changing an outbound port, also update the `*-domain` interface.
- Domain logic belongs in domain entity methods, not in services.
- Controllers handle only routing and DTO conversion.
