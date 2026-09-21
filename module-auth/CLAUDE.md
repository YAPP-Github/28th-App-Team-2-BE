# auth

Owns authentication: OAuth sign-in (Kakao/Google/Apple), token issuance, and the re-signup restriction registry. It creates the member record but does not own it.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| OAuth provider exchange; Apple credential storage | The member record and its profile fields → `member` |
| Access / refresh / onboarding token issuance and revocation (Redis) | 명식 → `saju` |
| The re-signup restriction registry — hashed SNS id, 90-day TTL | Withdrawal itself → `member` |
| | The admin role — `auth` asks via `IsAdminMemberPort` |

Withdrawal splits across both domains: **`member` orchestrates it, `auth` owns the restriction registry.** The hash lives here because the restriction is enforced at login, and login is this domain's gate.

## Cross-Domain Contracts

Provides (implemented in `auth-adapter-out`):

| Port | Consumed by | What the consumer acts on |
|------|-------------|---------------------------|
| `RevokeMemberTokensPort` | `member` | revokes tokens during withdrawal |
| `RevokeOauthTokenPort` | `member` | revokes the provider-side grant during withdrawal |

Consumes `CreateMemberPort`, `GetMemberIdPort`, `IsAdminMemberPort`, `RegisterWithdrawnAccountPort` (`member`), `CreateSajuChartPort` (`saju`), and `CreateDailyFortunePort` (`daily-fortune`). Publishes `shared.event.MemberSignedUpEvent`, consumed by its own listener.

## Signup Choreography

Read this before changing anything in the signup path — the shape is load-bearing.

```
SignupService                    @Service — deliberately NO transaction
  ├─ SignupTransactionService    @CommandService — member + SELF 명식 in ONE tx
  │     └─ publishes MemberSignedUpEvent
  │           └─ MemberSignedUpEventListener   @TransactionalEventListener(AFTER_COMMIT)
  │                 └─ hands off to a dedicated worker thread → daily fortune (AI)
  └─ issues tokens
```

Three properties depend on it, each of which has broken before:

- **The account is final before the response returns.** A client timeout is harmless — re-login is handled as an ordinary existing-member login.
- **A duplicate signup succeeds idempotently** instead of returning 409 (#87).
- **The signup response never waits on an AI call** (#90); the listener returns immediately and the request thread that committed is not held.

**`SignupService` must stay a plain `@Service`.** Giving it `@CommandService` folds `SignupTransactionService`'s transaction into it under `REQUIRED` propagation, which puts the AI call back inside the transaction — the failure mode the split exists to prevent (→ `architecture` skill).

Fortune generation failing does **not** roll back a completed signup; `GetTodayFortuneService` or the next batch fills the gap.

## Decisions & Traps

- **`AppleOauthCredentialJpaEntity` is the only JPA entity in this domain**, and its presence is not a claim on account data. Apple requires a stored authorization code to revoke the grant later; the other providers need nothing persisted.
- **Admin is not this domain's concept.** `Role` lives on `Member`; auth asks through `IsAdminMemberPort` rather than reading or caching a role of its own.
