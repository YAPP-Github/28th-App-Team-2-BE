# Spring AI Setup Reference

Detailed reference for the `spring-ai` skill. Open it when wiring configuration, env vars, Gradle dependencies, or adding a Konsist rule for AI code.

---

## Configuration (per profile)

`spring.ai.*` is declared in **each** per-profile file (`application-local.yaml`, `-dev.yaml`, `-prod.yaml`) rather than the shared `application.yaml`, matching the root `CLAUDE.md` rule that connection/credential settings branch per environment:

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${GCP_PROJECT_ID}
          location: ${VERTEX_AI_LOCATION}
          chat:
            options:
              model: ${VERTEX_AI_CHAT_MODEL}
```

Also present per profile: `ai-resilience.*` (retries / timeLimiters keyed by instance name) — see `references/resilience.md`.

## Environment Variables

Only three env vars are involved, and one is shared with other GCP integrations:

| Env var | Purpose |
|---------|---------|
| `GCP_PROJECT_ID` | **Reused** — the same GCP project as GCS and FCM. There is no separate `VERTEX_AI_PROJECT_ID` |
| `VERTEX_AI_LOCATION` | Region (e.g. `us-central1`) |
| `VERTEX_AI_CHAT_MODEL` | Gemini chat model id |

**Auth is ADC** — the same mechanism as GCS and FCM, so **no service-account key file and no `GOOGLE_APPLICATION_CREDENTIALS`**:

- Server: GCP SA key / Workload Identity picked up automatically by ADC.
- Local: `gcloud auth application-default login` once.
- The service account needs the Vertex AI API enabled and a role granting Vertex AI prediction access. Prefer a custom role with just that permission over a broad `roles/aiplatform.admin`.

Fill `.env` from `.env.example`; commit only `.env.example` and never a key file (`code-style`).

## Transport Configuration

`VertexAiTransportConfig` + `VertexAiTransportProperties` (`module-bootstrap`) tune the underlying transport. Read them before changing client-level timeouts — an operation timeout set here interacts with the per-call `TimeLimiter` and the adapter's own `Duration` constants, and the **tightest one wins**. Changing only one of the three and expecting a different ceiling is a common mistake.

## Gradle Placement

- Version via the Spring AI **BOM**; never pin a Spring AI version in a module's `build.gradle.kts` (root `CLAUDE.md`: all versions live in `gradle/libs.versions.toml`).
- Add the starter **only** to `{domain}-adapter-out`. Adding it to `{domain}-domain` or `-application` lets `org.springframework.ai..` imports compile there, which Konsist then rejects — and the compile error you get is far less obvious than the architecture violation it represents.
- resilience4j is consumed through `module-common` (`AiResilienceSupport`), so an adapter module does not declare it directly.

## Checklist for a New AI Adapter

1. `*AiPort` in `{domain}-domain/.../port/outbound/` — domain types only, no reactive types.
2. Four domain exceptions + `*ErrorCode` entries: `*CircuitOpenException`, `*TimeoutException`, `*GenerationFailedException`, `*EmptyResponseException`.
3. `VertexAi*Adapter` in `{domain}-adapter-out/.../adapter/ai/`, `@Component`, implementing the port.
4. Pick an `instanceName` (`{domain}-ai`) and **add it to both `ai-resilience.retries` and `ai-resilience.timeLimiters` in every profile** — otherwise those stages are silently skipped (`references/resilience.md`).
5. Decide schema forcing: `responseMimeType` always; `responseSchema` only if the mapping type has no nullable fields (`references/structured-output.md`).
6. Starter dependency added to the adapter-out module only.
7. Adapter test with a stubbed `ChatClient` + a circuit-open case. No real Vertex call.
8. If the use case writes to the DB around the call, split orchestrator / `TransactionalStore` so the AI call sits outside the transaction (`architecture`).

## Konsist Verification

The existing rules in `ArchitectureTest.kt` already cover AI code through the generic layer rules:

- `*Port` interfaces (so `*AiPort` too) only in `.port.outbound`
- `*Adapter` classes (so `VertexAi*Adapter` too) only in `.adapter` packages
- `*-application` may not import `.adapter.`

If you want AI-specific guarantees beyond those — e.g. "`org.springframework.ai..` imports appear only in `.adapter` packages" — add a `@Test` to `ArchitectureTest.kt` following the `konsist` skill. Check whether such a rule already exists before adding it; the rule table in that skill is the current list.
