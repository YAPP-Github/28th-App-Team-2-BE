---
name: new-feature
description: Add a new feature (UseCase → Service → Api → Controller) to an existing domain
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

Add a new feature to an existing domain.

Feature description: $ARGUMENTS

> Follow the code patterns in [`.claude/examples/domain-scaffold.md`](../examples/domain-scaffold.md) and the `architecture` skill.

## Work Order

1. **Define the inbound port** (`{domain}-application`) — `{FeatureName}UseCase.kt`.
2. **Implement the service** (`{domain}-application`) — mutating: `@CommandService`, query: `@QueryService`. Add to an existing `*Service` or create a new one. Do not attach `@Transactional` directly on methods.
3. **API interface** (`{domain}-adapter-in`) — add the endpoint to the existing `{Domain}Api.kt`. Return `ResponseEntity<CommonResponse<T>>`. Only `@Operation(summary, description)` + `@Parameter`; for unauthenticated APIs, `@DisableSwaggerSecurity`. (Responses are auto-documented from the return type — no `@ApiResponses`.)
4. **Create DTOs** (`{domain}-adapter-in`) — add `*Request`/`*Response` as needed. The envelope is handled by `CommonResponse`.
5. **Implement the controller** (`{domain}-adapter-in`) — implement in the existing `{Domain}Controller.kt`. Wrap with `CommonResponse.success/created/retrieved/...`.
6. **Verify** — `./gradlew ktlintCheck` → `./gradlew :architecture-test:test`.

## Principles

- When changing an outbound port, also modify the `*-domain` interface.
- Domain logic belongs in domain entity methods, not in the service.
- The Controller only handles routing and DTO conversion.
