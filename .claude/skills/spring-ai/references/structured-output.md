# Structured Output Reference

Detailed reference for the `spring-ai` skill. Open it when writing or debugging a typed model response. The rules are in the skill body; this file holds the mechanics and the traps.

---

## The Basic Shape

```kotlin
chatClient.prompt()
    .options(JSON_RESPONSE_OPTIONS)          // force JSON at the provider level — see below
    .system(systemPrompt(today))             // grounding facts go HERE, not in the user block
    .user { it.text(USER_TEMPLATE).param("saju", context.render()) }
    .call()
    .entity(GeneratedCompatibility::class.java)   // mapping target lives in the adapter module
```

- `.entity(T::class.java)` makes Spring AI inject schema instructions into the prompt and deserialize the JSON for you via `BeanOutputConverter`.
- Collections need `ParameterizedTypeReference`: `.entity(object : ParameterizedTypeReference<List<T>>() {})`.
- `.entity(...)` can return **null**. Every adapter here converts that into a domain `*EmptyResponseException` rather than returning a null to the use case.

## Where the Mapping Type Lives

The `entity()` target is an **adapter-module type** (`GeneratedCompatibility`, `RawChatAction`), not the domain entity:

- The LLM's shape is driven by prompt convenience and changes with prompt tuning; the domain entity is driven by invariants. Coupling them makes every prompt tweak a domain change.
- The adapter converts the mapping type → domain type. That is ordinary DTO↔domain mapping, which the `architecture` skill confines to the adapter layer.
- Keep the mapping type `private`/internal to the adapter package unless a test needs it.

## Forcing JSON at the Provider Level

Prompt instructions alone do **not** guarantee syntactically valid JSON — Gemini can return a truncated object (e.g. a missing closing `}`), which fails `BeanOutputConverter` parsing. So set the provider option:

```kotlin
private val JSON_RESPONSE_OPTIONS =
    VertexAiGeminiChatOptions.builder()
        .responseMimeType("application/json")
        .build()
```

### The nullable-field trap (why the two adapters differ)

Two existing adapters deliberately disagree about how far to force the schema, and the difference is load-bearing:

| Adapter | Forces | Why |
|---------|--------|-----|
| `VertexAiCompatibilityAdapter` | `responseMimeType` **+ `responseSchema`** matching the `entity()` target | All fields are always present, so a strict schema is safe and cuts parse failures further |
| `VertexAiChatAdapter` | `responseMimeType` **only** | `RawChatAction` is a nullable structure (`hasAction = false` leaves the rest empty) |

The mechanism behind the chat exception: for a nullable field, `BeanOutputConverter` generates `"type": ["string", "null"]`. Vertex's `Schema` proto accepts only a **single** type, so it cannot parse that schema and **every call fails** — not intermittently, always. Forcing JSON syntax without forcing the schema avoids it.

**Rule of thumb:** force the schema only when the mapping type has no nullable fields. If you add a nullable field to a type whose adapter forces `responseSchema`, you must drop the schema forcing in the same change.

## Prompt Hygiene

- **Grounding facts belong in the system prompt.** The model has no clock; without a reference date it will answer with a year from its training data (observed: "2024" treated as the current year). `VertexAiChatAdapter` therefore takes `today` as a parameter and renders it into the *system* prompt.
- **Never put a grounding fact in the user data block.** The user block is untrusted input and a prompt-injection attempt can override it. System prompt vs user block is a trust boundary, not a formatting preference.
- Pass variable content with `.param(...)` placeholders rather than string interpolation, so the template stays readable and injection surface stays explicit.

## Failure Handling

Every failure path ends in a domain exception carrying a `*ErrorCode` (`error-handling` skill). The four-exception family each AI domain defines:

| Condition | Exception | Notes |
|-----------|-----------|-------|
| Circuit breaker open | `*CircuitOpenException` | Caller should skip immediately, not retry |
| Timeout exceeded | `*TimeoutException` | Per-call limit, see `references/resilience.md` |
| Any other call/parse failure | `*GenerationFailedException` | Wraps the cause |
| Call succeeded but `entity()` was null | `*EmptyResponseException` | Not a transport failure — the model returned nothing usable |

Never let a raw `RuntimeException`, `error(...)`, or `IllegalStateException` escape the adapter: `GlobalExceptionHandler` turns those into a generic `INTERNAL_ERROR` 500 and the domain code is lost.

## Debugging a Parse Failure

1. Is `responseMimeType("application/json")` set? Without it, assume invalid JSON is possible.
2. Does the mapping type have a nullable field **and** `responseSchema` forcing? That combination fails 100% of the time — drop the schema forcing.
3. Log the raw content before `.entity(...)` once, locally only. Don't leave response-body logging in — prompts and responses can contain member data.
4. If the model returns prose around the JSON, the system prompt is competing with the schema instructions; tighten the system prompt rather than post-processing the string.
