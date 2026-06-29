---
name: spring-ai
description: Load when integrating Spring AI / Vertex AI (Gemini). Hexagonal placement (*AiPort/adapter), structured output, pgvector VectorStore, config & env vars, testing, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Spring AI Rules

Use Spring AI to integrate Google Vertex AI (Gemini). The core use cases are **structured output** (mapping LLM responses to domain types) and **pgvector-based RAG/embeddings**.

| Item | Decision |
|------|----------|
| Framework | Spring AI (a Spring Boot 4.x-compatible release, version-managed via BOM) |
| Provider | Google Vertex AI — Gemini (`spring-ai-starter-model-vertex-ai-gemini`) |
| Embedding | Vertex AI Embedding (`text-embedding-005`) |
| VectorStore | pgvector (`spring-ai-starter-vector-store-pgvector`) — uses the existing PostgreSQL extension, no separate infra |
| Auth | Service-account key (`GOOGLE_APPLICATION_CREDENTIALS`) |

---

## 1. Hexagonal Placement (most important)

Since AI is an **external system**, treat it as an outbound adapter. The domain/use cases **never import** Spring AI types (`ChatClient`, `VectorStore`, `ChatModel`, etc.).

```
{domain}-domain        →  *AiPort interface (pure Kotlin, domain-type I/O)
{domain}-application    →  use case calls *AiPort
{domain}-adapter-out    →  Spring AI adapter (implements the port, uses ChatClient/VectorStore)
```

| Type | Naming | Location (package) |
|------|--------|--------------------|
| Outbound port | `*AiPort` (e.g. `ReviewSummaryAiPort`) | `com.yapp.todakun.{domain}` (domain) |
| Adapter implementation | `VertexAi*Adapter` (e.g. `VertexAiReviewSummaryAdapter`) | `com.yapp.todakun.{domain}.adapter.ai` |
| VectorStore adapter | `*VectorStoreAdapter` | `com.yapp.todakun.{domain}.adapter.ai` |

- Use a new technology package `adapter.ai` (per the `architecture` skill's `.adapter.{tech}` rule). pgvector search is semantically AI search too, so put it in `adapter.ai`.
- The port deals only in domain types. Prompt strings, `ChatClient` calls, and JSON parsing are all finished inside the adapter.

```kotlin
// {domain}-domain : pure port (no Spring AI imports)
interface ReviewSummaryAiPort {
    fun summarize(reviews: List<String>): ReviewSummary   // returns a domain type
}
```

```kotlin
// {domain}-adapter-out : .adapter.ai
@Component
class VertexAiReviewSummaryAdapter(
    private val chatClient: ChatClient,
) : ReviewSummaryAiPort {
    override fun summarize(reviews: List<String>): ReviewSummary =
        chatClient.prompt()
            .user { it.text("Summarize the following reviews:\n{reviews}").param("reviews", reviews.joinToString("\n")) }
            .call()
            .entity(ReviewSummary::class.java)   // structured output
}
```

---

## 2. Structured Output

Always **receive the response as a domain type (or an adapter-only mapping type)**. Do not let raw `String` parsing leak into the use cases.

- Receive via `ChatClient.call().entity(Xxx::class.java)`. Spring AI injects schema instructions into the prompt and deserializes the JSON.
- If the mapping target has a different shape than the domain entity, receive into **a separate class inside the adapter module** and convert to the domain type in the adapter (DTO ↔ domain mapping is the adapter layer, `architecture` skill).
- For collections, use `ParameterizedTypeReference`: `.entity(object : ParameterizedTypeReference<List<Xxx>>() {})`.
- On mapping failure (schema mismatch), convert to an `AppException` subclass in the adapter and throw. Throwing `RuntimeException` directly is forbidden (`error-handling` skill).

---

## 3. pgvector VectorStore

- Enable the `vector` extension on the existing PostgreSQL: `CREATE EXTENSION IF NOT EXISTS vector;` (include it in the migration).
- The `dimensions` must match the embedding model exactly (`text-embedding-005` = 768).
- VectorStore access also goes through a port. The use case calls a domain port (`*SearchPort`, etc.), not `VectorStore`.
- Converting search-result metadata → domain type is done in the adapter.

```kotlin
// {domain}-adapter-out : .adapter.ai
@Component
class DocumentVectorStoreAdapter(
    private val vectorStore: VectorStore,
) : DocumentSearchPort {
    override fun search(query: String, topK: Int): List<DocumentMatch> =
        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build())
            .map { DocumentMatch(id = UUID.fromString(it.id), content = it.text) }
}
```

---

## 4. Config & Environment Variables

Manage via `.env` and commit only `.env.example` (`code-style` skill). Never commit secrets (the service-account key).

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID}
          location: ${VERTEX_AI_LOCATION}
          chat:
            options:
              model: ${VERTEX_AI_CHAT_MODEL}
    vectorstore:
      pgvector:
        initialize-schema: false   # schema is managed by migrations, no app auto-creation
        dimensions: 768
```

| Environment variable | Purpose |
|----------------------|---------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to the service-account JSON (Vertex AI auth) |
| `VERTEX_AI_PROJECT_ID` | GCP project ID |
| `VERTEX_AI_LOCATION` | Region (e.g. `us-central1`) |
| `VERTEX_AI_CHAT_MODEL` | Gemini chat model |
| `VERTEX_AI_EMBEDDING_MODEL` | Embedding model |

- For Gradle dependencies, unify versions with the Spring AI BOM (`spring-ai-bom`) and add only the starters to `{domain}-adapter-out`'s `build.gradle.kts`. Do not add them to the domain/application modules.

---

## 5. Testing

| Layer | Target | Method |
|-------|--------|--------|
| `*-application` | Whether the use case calls `*AiPort` correctly | Mock with `mockk<*AiPort>()` (no real LLM calls) |
| `*-adapter-out` (AI) | Prompt-construction and structured-mapping logic | Stub `ChatModel`/`ChatClient` with MockK and verify response mapping |
| `*-adapter-out` (pgvector) | Embedding storage and similarity search | TestContainer (shared `pgvector/pgvector:pg17`) + `vector` extension |

- **Never call the real Vertex AI in tests.** Forbidden due to network/cost/non-determinism. Inject LLM responses as fixed stubs.
- pgvector integration tests need no separate container. The shared `TestContainersConfig`'s PostgreSQL container is already `pgvector/pgvector:pg17` (superset-compatible with postgres), so use it as-is (`testing` skill).
- All Specs are `DescribeSpec`, mocking is MockK, assertions are Kotest matchers (`testing` skill).

```kotlin
class ReviewSummaryServiceTest : DescribeSpec({
    val reviewSummaryAiPort = mockk<ReviewSummaryAiPort>()
    val service = ReviewSummaryService(reviewSummaryAiPort)

    afterTest { clearMocks(reviewSummaryAiPort) }

    describe("summarize") {
        context("when reviews are given") {
            it("delegates summarization to the AI port") {
                val summary = ReviewSummary(/* ... */)
                every { reviewSummaryAiPort.summarize(any()) } returns summary

                service.summarize(listOf("Nice")) shouldBe summary
                verify(exactly = 1) { reviewSummaryAiPort.summarize(any()) }
            }
        }
    }
})
```

---

## 6. Architecture Verification (Konsist)

Compatible with the `konsist` skill rules. Additionally guarantee the following.

- `org.springframework.ai..` imports are allowed only in the `.adapter` package (forbidden in domain/application).
- `*AiPort` interfaces live only in the `*-domain` package (`..{domain}`, excluding `.application`/`.adapter`).
- `VertexAi*Adapter` / `*VectorStoreAdapter` live only in the `.adapter.ai` package.

Add new rules to `architecture-test/ArchitectureTest.kt` as `@Test` (see the `konsist` skill for how to add rules).