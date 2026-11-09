# Transaction Patterns: Long External Calls (AI)

The detailed reference for the `architecture` skill. Open it for any use case that calls an AI model, or when debugging lock / connection-pool contention.

---

## The Problem

An AI call takes several seconds. If it happens inside a transaction, that transaction — and every row lock it holds — stays open for the whole call. Two things follow:

- **The connection pool drains.** Each in-flight generation pins one Hikari connection for seconds. The pool also serves web requests, so a burst of generations starves ordinary traffic.
- **Concurrent requests on the same key serialize.** The second caller blocks on the first caller's lock for the full duration of the first caller's AI call, instead of discovering "someone already made this" in milliseconds.

Neither failure is visible in a unit test or at low traffic. Both appear under load, as timeouts far away from the code that caused them.

---

## The Split

A generation use case is **two beans**:

| Bean | Stereotype | Transaction | Role |
|------|-----------|-------------|------|
| `Create{Aggregate}Service` (orchestrator) | **`@Service`** | None | Steps through the stages in order; calls the AI **between** transactions |
| `{Aggregate}TransactionalStore` | `@CommandService` | One per method | Short lock-and-read, lock-and-save transactions |

```
findExistingWithLock()   ← short transaction: acquire lock, read; lock released on commit
        ↓
    AI call              ← no transaction, no lock, no connection held
        ↓
saveIfAbsent()           ← short transaction: re-acquire lock, re-check, save only if still absent
```

Both store methods acquire the lock and re-check before writing, so a concurrent request that finished creating first is **detected** rather than colliding with the unique constraint.

**Idempotency comes from the re-check in `saveIfAbsent`, not from the lock.** The lock only narrows the window; the re-read is what makes a duplicate request return the existing row instead of failing. This distinction matters when someone proposes "we have a lock, we can skip the second read" — that change reintroduces unique-constraint errors under concurrency.

The pre-read also makes a repeat request **cheap**, not merely correct: if the row already exists, the AI call is skipped entirely.

---

## The Trap

> **The orchestrator must never carry `@CommandService` or `@Transactional`.**

Under `REQUIRED` propagation, the store's methods would **join** the orchestrator's transaction instead of opening their own. The split silently collapses back into one long transaction that holds a lock across the entire AI call — exactly the failure the pattern exists to prevent.

**Nothing fails loudly when this happens.** Tests still pass, behavior is still correct, and the only symptom is pool exhaustion under load. This has actually happened in this repo before: `@CommandService` on a signup orchestrator defeated the same pattern, and the fix was to make the orchestrator a plain `@Service` again.

How to check a suspect use case:
1. Does the orchestrator class carry `@Service` only (no `@CommandService`, no `@Transactional`)?
2. Is the AI port called from the orchestrator, never from inside a store method?
3. Does each store method do its own lock → read/write → return, with nothing long-running in between?

If a transaction must *not* join an outer one for some other reason, the explicit tool is `@Transactional(propagation = REQUIRES_NEW)` — but prefer keeping the orchestrator transaction-less, since that makes the boundary obvious from the stereotype alone rather than from a propagation attribute.

---

## Related: post-commit work

The same "don't hold a transaction across something slow" reasoning drives two neighboring patterns:

- **`@TransactionalEventListener(AFTER_COMMIT)`** for consequences the publisher shouldn't enumerate — cache eviction, notifications. Running after commit avoids evicting on a transaction that later rolls back, and avoids racing a concurrent read that would refill the cache from pre-change data. Note that Spring does **not** swallow exceptions thrown from an `AFTER_COMMIT` listener: they propagate to the caller of an already-committed transaction, so such a listener must absorb its own failures and log them.
- **External HTTP after commit** — e.g. a provider-side token revoke during withdrawal. The local transaction deletes local state; the remote call runs after commit, because it can take seconds, can't be rolled back, and would otherwise pin a DB connection for its duration.

---

## Where This Pattern Is Used

`daily-fortune`, `day-fortune`, `year-fortune`, `compatibility`, `notification`.

Each of those domains' `CLAUDE.md` does **not** re-explain the pattern — it only records what is domain-specific: which key the lock uses, and what the save transaction writes together. Read the domain doc for the specifics, this file for the mechanism.
