# chat

Owns the AI conversation — conversations, messages, suggestions, and the daily free-chat quota.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| Conversations, messages, message status, suggestions | 명식 and its interpretation → `saju` |
| The daily free-chat quota | Member profile → `member` |
| Prompt context assembly | Notification delivery → `notification`; this domain only asks |

## Cross-Domain Contracts

Provides no `shared` port — **a pure consumer**. Consumes `GetSajuChartPort` (`saju`), `GetMemberFortuneProfilePort` (`member`), and `SendNotificationPort` (`notification`).

## A Streaming Turn Is Three Transactions

`StreamChatAnswerService` is a plain `@Service` on purpose: a streaming AI call must not sit inside a transaction, so one turn is split across three short ones.

```
PrepareChatTurnService    @CommandService
  reserve quota → get or create conversation → save user message
  + assistant placeholder → build prompt context
        ↓
   AI streaming           no transaction, no connection held
        ↓
CompleteChatTurnService   @CommandService — finalize the assistant message
   ── or ──
FailChatTurnService       @CommandService — mark it failed AND refund the quota
```

- **Quota is reserved up front and refunded on failure**, never charged on success. `FailChatTurnService` refunds even when the message row has vanished (the conversation was deleted mid-stream) or the save itself throws — a refund must never be skipped because the bookkeeping failed. Keep that ordering if you touch it.
- **The quota lives in Redis, separate from conversation storage, and resets at midnight.** That is *midnight*, not the 06:00 service-day rollover `luck` and `daily-fortune` use. The two calendars differ; do not unify them without checking both sides.

## Decisions & Traps

- **A streaming failure is not an HTTP failure.** The controller opens the SSE stream *before* this service runs, so even preparation errors — quota exhausted, conversation missing, permission denied — arrive as an `error` event on the stream rather than as a status code. A 200 does not mean the turn started. This is the accepted trade-off of synchronous SSE with a separate generation thread, so do not "fix" it by moving validation ahead of the stream without redesigning the contract with clients.
