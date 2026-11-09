# pgvector / VectorStore Reference

Detailed reference for the `spring-ai` skill. Open it **before introducing embeddings or vector search for the first time**.

---

## Current State: Not Used

As of now this repo has **no VectorStore usage at all**:

- no `VectorStore` reference in any Kotlin source
- no `spring.ai.vectorstore.*` block in any profile
- no embedding model configured (`VERTEX_AI_CHAT_MODEL` is the only model env var; there is no embedding counterpart)
- no `vector` extension migration in the app's schema management

So treat everything below as **the rules for the first implementation**, not a description of existing practice. Don't cite this file as evidence that RAG exists here. The one place pgvector already shows up is the **test container image** (`pgvector/pgvector:pg17`), chosen so that adding pgvector later needs no infra change — see `testing`.

## Decisions Already Made

| Item | Decision | Why |
|------|----------|-----|
| Vector store | pgvector (`spring-ai-starter-vector-store-pgvector`) | Reuses the existing PostgreSQL instance — no new infrastructure to run, monitor or pay for |
| Container image | `pgvector/pgvector:pg17` | Already the shared test image, backward-compatible with plain postgres |
| Schema ownership | Migrations, not the app | `initialize-schema: false`; `ddl-auto` is `validate` repo-wide, so letting Spring AI create tables would contradict it |

## If You Introduce It

1. **Enable the extension through a migration**, not by hand and not by the app:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
   The test container already does this via `withInitScript("init-pgvector.sql")`, so local tests stay consistent.

2. **Pin `dimensions` to the embedding model exactly.** A mismatch is not a startup error — it surfaces as wrong or failing similarity queries later. Record the model→dimension pair in the same commit as the config:
   ```yaml
   spring:
     ai:
       vectorstore:
         pgvector:
           initialize-schema: false   # migrations own the schema
           dimensions: 768            # must equal the embedding model's output dimension
   ```

3. **Add the embedding model env var alongside the chat one** (`VERTEX_AI_EMBEDDING_MODEL`) in `.env.example` and every profile. Don't hardcode a model id.

4. **Go through a port like any other external system.** The use case calls a domain port (`*SearchPort`), never `VectorStore`:
   ```kotlin
   // {domain}-domain : port.outbound
   interface DocumentSearchPort {
       fun search(query: String, topK: Int): List<DocumentMatch>   // domain types only
   }
   ```
   ```kotlin
   // {domain}-adapter-out : .adapter.ai
   @Component
   class DocumentVectorStoreAdapter(
       private val vectorStore: VectorStore,
   ) : DocumentSearchPort {
       override fun search(query: String, topK: Int): List<DocumentMatch> =
           vectorStore
               .similaritySearch(SearchRequest.builder().query(query).topK(topK).build())
               .map { DocumentMatch(id = UUID.fromString(it.id), content = it.text) }
   }
   ```
   pgvector search is semantically AI search, so the adapter lives in `adapter.ai` — not `adapter.persistence` — even though the data sits in PostgreSQL.

5. **Convert result metadata to domain types in the adapter.** A `Document`'s metadata map must not reach the use case; that's DTO↔domain mapping, which belongs to the adapter layer (`architecture`).

6. **Wrap it in resilience too.** Embedding calls hit Vertex AI over the network just like chat calls, so they need an `instanceName` registered in `ai-resilience.retries`/`timeLimiters` (`references/resilience.md`). Similarity search against local PostgreSQL does not.

7. **Index before it matters.** An exact-scan similarity query is fine at small volume and silently degrades as rows grow; decide the index (HNSW / IVFFlat) and its parameters in the migration rather than after a latency incident.

## Testing

No separate container is needed — the shared `TestContainersConfig` PostgreSQL container is already the pgvector image with the extension enabled, so reuse it with `@Import(TestContainersConfig::class)` (`testing`). Embedding generation itself must still be stubbed; never call real Vertex AI in a test.
