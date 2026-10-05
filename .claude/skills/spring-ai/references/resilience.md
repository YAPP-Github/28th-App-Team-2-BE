# AI Call Resilience Reference

Detailed reference for the `spring-ai` skill. Open it when adding resilience to a new AI adapter or tuning timeout / circuit-breaker values.

An LLM call is slow, occasionally hangs, and fails in bursts. Every model call in this repo therefore goes through `AiResilienceSupport` (`module-common`), which composes **CircuitBreaker + Retry + TimeLimiter** from resilience4j.

---

## Using It From an Adapter

```kotlin
@Component
class VertexAiCompatibilityAdapter(
    private val chatClient: ChatClient,
    private val resilience: AiResilienceSupport,
) : CompatibilityAiPort {

    override fun generate(context: CompatibilityPromptContext): GeneratedCompatibility {
        val generated = resilience.execute(
            instanceName = AI_RESILIENCE_INSTANCE_NAME,          // e.g. "compatibility-ai"
            onCircuitOpen = { CompatibilityCircuitOpenException(it) },
            onTimeout = { CompatibilityTimeoutException(it) },
            onFailure = { CompatibilityGenerationFailedException(it) },
        ) {
            // the actual ChatClient call
        }
        return generated ?: throw CompatibilityEmptyResponseException()
    }
}
```

- The three `on*` lambdas map infrastructure failures to **domain** exceptions, so the use case never sees a resilience4j type. This is what keeps `CallNotPermittedException` from leaking past the adapter.
- `instanceName` is a per-domain string (`chat-ai`, `compatibility-ai`, …). It is the key for both the circuit-breaker instance and the config lookup.
- A `null` result is *not* an infrastructure failure, so handle it outside `execute` with `*EmptyResponseException`.

## Configuration (`ai-resilience.*`)

Bound by `AiResilienceProperties` (`module-bootstrap`), wired by `AiResilienceConfig`.

| Key | Shape | Notes |
|-----|-------|-------|
| `ai-resilience.circuitBreaker` | single shared block | All five instances currently share one registry default. Per-instance tuning is deliberately deferred until there's a real need |
| `ai-resilience.executor` | single block | The executor backing `TimeLimiter`; one executor per instance name, created lazily |
| `ai-resilience.retries` | **map keyed by instance name** | `chat-ai: { ... }` |
| `ai-resilience.timeLimiters` | **map keyed by instance name** | `chat-ai: { ... }` |

Current circuit-breaker defaults (`CircuitBreakerSettings`): sliding window 20, minimum calls 10, failure-rate threshold 50%, open-state wait 30s, half-open permitted calls 5.

### The silent-skip trap

`retries` and `timeLimiters` are **maps keyed by instance name**. If your new adapter's `instanceName` is missing from a map, `AiResilienceConfig` never registers that name in the corresponding registry, and `AiResilienceSupport.execute` **silently skips that stage**.

Consequences of forgetting:

- missing from `timeLimiters` → **no timeout at all**; a hung Vertex call holds the thread until the transport gives up
- missing from `retries` → no retry; a single transient blip surfaces as a user-visible failure

Nothing fails loudly, so this only shows up in production latency. **When you add an adapter, add its instance name to both maps in the same change**, and grep the per-profile YAML to confirm all five (plus yours) are present.

## Per-Call Timeouts Inside the Adapter

Some adapters also pin an operation-level timeout next to the call, independent of the `TimeLimiter`:

```kotlin
private val STREAM_TIMEOUT = Duration.ofSeconds(60)   // VertexAiChatAdapter: streaming answer
private val ACTION_TIMEOUT = Duration.ofSeconds(30)   // VertexAiChatAdapter: action extraction
```

Streaming and one-shot extraction have very different acceptable latencies, so they get different values. Keep them as named constants at the top of the adapter rather than inline magic numbers.

## How Callers Must React

The distinction between *circuit-open* and *transient failure* is the contract this layer exists to provide:

| Caller | Behavior |
|--------|----------|
| `daily-fortune` batch | Transient failure → retry up to `AI_CALL_RETRY_LIMIT`, then skip that member. **Circuit-open and timeout → skip immediately** — retrying while the breaker is open only burns calls |
| `chat` (SSE) | Failure arrives as an `error` event on an already-open stream, not as an HTTP status (`module-chat/CLAUDE.md`) |
| Fortune/compatibility on-demand | Propagates as the domain exception → `GlobalExceptionHandler` maps it to the `*ErrorCode`'s status |

## Testing Resilience

`VertexAiChatAdapterTest` builds the registries directly rather than booting Spring:

- `CircuitBreakerRegistry.ofDefaults()` for the happy path.
- To exercise the open state, construct a registry with a custom `CircuitBreakerConfig`, force the named instance open, then assert the adapter throws the **domain** `*CircuitOpenException` — not `CallNotPermittedException`. That assertion is what pins the mapping contract.
- Never call real Vertex AI; stub the `ChatClient`/`ChatModel`.
