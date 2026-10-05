---
name: spring-ai
description: Load when calling an LLM through Spring AI / Vertex AI (Gemini) — *AiPort placement, structured output, resilience (timeout/circuit breaker), prompt hygiene, testing without a real model call.
paths:
  - "**/adapter/ai/**"
  - "**/*Ai*.kt"
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Spring AI Rules

Google Vertex AI (Gemini) via Spring AI. Five domains already ship an adapter — read the closest one before writing a new one (list at the bottom).

| Item | Decision |
|------|----------|
| Provider | Vertex AI Gemini (`spring-ai-starter-model-vertex-ai-gemini`), version via Spring AI BOM |
| Auth | **ADC** — same as GCS/FCM. No service-account key file. `GCP_PROJECT_ID` is reused |
| Resilience | resilience4j CircuitBreaker + per-call timeout, always through `AiResilienceSupport` (`common`) |
| pgvector / VectorStore | **Not used anywhere yet.** No embedding model is configured. See `references/vectorstore.md` before introducing one |

---

## 1. Hexagonal Placement

AI is an external system → outbound adapter. The domain/application layer **never imports `org.springframework.ai..`** (`ChatClient`, `ChatModel`, `VectorStore`, …), and never depends on reactive types (`Mono`/`Flux`) either — streaming is converted to a callback at the port boundary.

| Type | Naming | Package |
|------|--------|---------|
| Outbound port | `*AiPort` | `com.yapp.todakun.{domain}.port.outbound` |
| Adapter | `VertexAi*Adapter` | `com.yapp.todakun.{domain}.adapter.ai` |
| Prompt context / history types | `*PromptContext`, `*HistoryTurn` | next to the port, in `port.outbound` |

- The port speaks domain types only. Prompt strings, `ChatClient` calls, JSON parsing, retries and timeouts all end **inside** the adapter.
- Streaming: the port takes an `onDelta: (String) -> Unit` callback and returns the completed text (see `ChatAiPort`), so `Flux` never escapes the adapter.

## 2. Non-Negotiables

- [ ] Receive the response as a **typed object**, not a `String` to be parsed later — `.entity(T::class.java)`, or `ParameterizedTypeReference` for collections.
- [ ] Wrap every failure in an `AppException` subclass with a domain `*ErrorCode` (`error-handling`). A bare `throw`/`error(...)` becomes a generic 500 and loses the domain code.
- [ ] Route **every** model call through `AiResilienceSupport` with a per-call timeout. An unguarded call can hang a request thread for minutes.
- [ ] Distinguish *circuit-open* (`CallNotPermittedException`) from *transient failure* — the caller skips immediately on the former instead of burning retries (`daily-fortune` batch relies on this).
- [ ] **Never call real Vertex AI in a test.** Stub the `ChatClient`/`ChatModel`; cost, network and non-determinism all disqualify it.
- [ ] Put the reference date and any other grounding fact in the **system prompt**, never in the user data block — a user block is untrusted and can be overridden by prompt injection.
- [ ] Keep the AI call **outside the transaction** — orchestrator + `<Aggregate>TransactionalStore` (`architecture` skill).

## 3. References

| Open it when | File |
|--------------|------|
| Writing or debugging structured output — schema forcing, parse failures, the nullable-field trap | `references/structured-output.md` |
| Adding resilience to a new adapter, or tuning timeout/circuit-breaker values | `references/resilience.md` |
| Introducing pgvector/embeddings for the first time (nothing exists yet) | `references/vectorstore.md` |
| Wiring config, env vars, Gradle deps, or adding a Konsist rule for AI | `references/setup.md` |

## 4. Real Implementations

Prefer reading one of these over inventing a shape. They are the source of truth; this document only states the rules.

| Domain | Port | Adapter |
|--------|------|---------|
| `chat` (streaming + action extraction) | `ChatAiPort` | `VertexAiChatAdapter` |
| `daily-fortune` | `DailyFortuneAiPort` | `VertexAiDailyFortuneAdapter` |
| `day-fortune` (택일) | `DaySelectionFortuneAiPort` | `VertexAiDaySelectionFortuneAdapter` |
| `year-fortune` | `YearSelectionFortuneAiPort` | `VertexAiYearSelectionFortuneAdapter` |
| `compatibility` (궁합) | `CompatibilityAiPort` | `VertexAiCompatibilityAdapter` |

Shared infrastructure: `AiResilienceSupport` (`module-common`), `AiResilienceConfig`/`AiResilienceProperties` and `VertexAiTransportConfig`/`VertexAiTransportProperties` (`module-bootstrap`).
