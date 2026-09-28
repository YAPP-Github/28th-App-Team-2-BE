---
name: spring-ai
description: Load when integrating Spring AI / Vertex AI (Gemini). Hexagonal placement (*AiPort/adapter), structured output, pgvector VectorStore, config & env vars, testing, Konsist verification.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Spring AI Rules

Integrate Google Vertex AI (Gemini) via Spring AI. The core use cases are **structured output** (mapping an LLM response to a domain type) and **pgvector-based RAG/embeddings**.

| Item | Decision |
|------|----------|
| Framework | Spring AI (Spring Boot 4.x-compatible release, version managed via BOM) |
| Provider | Google Vertex AI — Gemini (`spring-ai-starter-model-vertex-ai-gemini`) |
| Embedding | Vertex AI Embedding (`text-embedding-005`) |
| VectorStore | pgvector (`spring-ai-starter-vector-store-pgvector`) — reuses the existing PostgreSQL extension, no separate infra needed |
| Auth | Service account key (`GOOGLE_APPLICATION_CREDENTIALS`) |

---

## 1. Hexagonal Placement (most important)

AI is an **external system**, so it's treated as an outbound adapter. The domain/use-case layer **never imports Spring AI types** (`ChatClient`, `VectorStore`, `ChatModel`, etc.).

```
{domain}-domain        →  *AiPort interface (pure Kotlin, domain types in and out)
{domain}-application    →  use case calls *AiPort
{domain}-adapter-out    →  Spring AI adapter (implements the port, uses ChatClient/VectorStore)
```

| Type | Naming | Location (package) |
|------|--------|--------------------|
| Outbound port | `*AiPort` (e.g. `ReviewSummaryAiPort`) | `com.yapp.todakun.{domain}` (domain) |
| Adapter implementation | `VertexAi*Adapter` (e.g. `VertexAiReviewSummaryAdapter`) | `com.yapp.todakun.{domain}.adapter.ai` |
| VectorStore adapter | `*VectorStoreAdapter` | `com.yapp.todakun.{domain}.adapter.ai` |

- Use the new tech package `adapter.ai` (per the `architecture` skill's `.adapter.{tech}` rule). pgvector search is semantically AI search too, so it also lives in `adapter.ai`.
- The port only handles domain types. Prompt strings, the `ChatClient` call, and JSON parsing all end inside the adapter.

```kotlin
// {domain}-domain : pure port (no Spring AI import)
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

Always receive the response **as a domain type (or an adapter-only mapping type)**. Don't let raw `String` parsing leak into the use case.

- Receive it with `ChatClient.call().entity(Xxx::class.java)`. Spring AI injects the schema instructions into the prompt and deserializes the JSON.
- If the mapping target's shape differs from the domain entity, receive it as a **separate class inside the adapter module** and convert to the domain type in the adapter (DTO ↔ domain mapping is an adapter-layer concern, `architecture` skill).
- For collections, use `ParameterizedTypeReference`: `.entity(object : ParameterizedTypeReference<List<Xxx>>() {})`.
- On a mapping failure (schema mismatch), convert and throw an `AppException` subclass from the adapter. Never throw `RuntimeException` directly (`error-handling` skill).

---

## 3. pgvector VectorStore

- Enable the `vector` extension on the existing PostgreSQL instance: `CREATE EXTENSION IF NOT EXISTS vector;` (include it in a migration).
- `dimensions` must exactly match the embedding model (`text-embedding-005` = 768).
- VectorStore access also goes through a port. The use case calls a domain port (`*SearchPort`, etc.), not `VectorStore` directly.
- Converting search-result metadata → domain types happens in the adapter.

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

## 4. Configuration & Environment Variables

Manage via `.env`, commit only `.env.example` (`code-style` skill). Never commit secrets (service account keys).

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

| Env var | Purpose |
|----------------------|---------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to the service account JSON (Vertex AI auth) |
| `VERTEX_AI_PROJECT_ID` | GCP project ID |
| `VERTEX_AI_LOCATION` | Region (e.g. `us-central1`) |
| `VERTEX_AI_CHAT_MODEL` | Gemini chat model |
| `VERTEX_AI_EMBEDDING_MODEL` | Embedding model |

- Unify Gradle dependency versions via the Spring AI BOM (`spring-ai-bom`), and add the starter only to `{domain}-adapter-out`'s `build.gradle.kts`. Don't add it to the domain/application modules.

---

## 5. Testing

| Layer | Target | Approach |
|-------|--------|------|
| `*-application` | The use case calls `*AiPort` correctly | Mock with `mockk<*AiPort>()` (no real LLM call) |
| `*-adapter-out` (AI) | Prompt construction + structured-mapping logic | Stub `ChatModel`/`ChatClient` with MockK and verify the response mapping |
| `*-adapter-out` (pgvector) | Storing embeddings and similarity search | TestContainer (shared `pgvector/pgvector:pg17`) + `vector` extension |

- **Never call real Vertex AI in a test.** Banned for network/cost/non-determinism reasons. Inject the LLM response as a fixed stub.
- pgvector integration tests need no separate container — the shared `TestContainersConfig`'s PostgreSQL container is already `pgvector/pgvector:pg17` (backward-compatible with plain postgres), so reuse it (`testing` skill).
- Every spec uses `DescribeSpec`, mocking uses MockK, assertions use Kotest matchers (`testing` skill).

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

Compatible with the `konsist` skill's rules. Additionally guarantees:

- `org.springframework.ai..` imports are only allowed in `.adapter` packages (forbidden in domain/application).
- `*AiPort` interfaces only exist in the `*-domain` package (`..{domain}`, excluding `.application`/`.adapter`).
- `VertexAi*Adapter` / `*VectorStoreAdapter` only exist in the `.adapter.ai` package.

Add new rules as a `@Test` in `architecture-test/ArchitectureTest.kt` (see the `konsist` skill for how to add a rule).
