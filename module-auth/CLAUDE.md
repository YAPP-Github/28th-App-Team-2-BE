# auth

Owns authentication: OAuth sign-in (Kakao/Google/Apple), token issuance, and the re-signup restriction registry. It creates the member record but does not own it.

## Overview

Auth identifies users through Kakao/Google/Apple social sign-in and, as a result, issues and revokes sessions (access/refresh tokens). This service has no separate signup form — the first login *is* the signup, which is why `auth` creates `member`'s `Member` record. It also owns the re-signup restriction registry for the same reason: the check that blocks a re-signup on the same SNS account after withdrawal happens at "login" time.

- **Onboarding token**: a short-lived token (`IssuedOnboardingToken`) used only in the step right before signup. It identifies a user whose OAuth profile has been confirmed but who hasn't yet submitted their service profile (birth date/time, etc.), and has a lifecycle separate from access/refresh tokens.
- **Re-signup restriction**: a policy that blocks re-signup on a withdrawn SNS identifier for 90 days. Registration is done by `member` (during withdrawal orchestration); the check is done by `auth` (at login) — see the "Withdrawal" section of `module-member/CLAUDE.md`.

## Module Structure

**Packages**
- `auth-domain`: use-case interfaces, command/result and issued-token value objects at the root, external-dependency ports in `port.outbound` — representative: `LoginUseCase`, `SignupUseCase`, `OauthPort`
- `auth-application`: `*Service` at the root — representative: `SignupService`, `SignupTransactionService`, `MemberSignedUpEventListener`
- `auth-adapter-in`: controller, JWT filter, security exception handler — representative: `AuthController`, `JwtAuthenticationFilter`
- `auth-adapter-out`: `adapter.oauth` (apple/google/kakao), `adapter.jwt.access`, `adapter.redis` (blacklist/onboarding/refresh/withdrawn), `adapter.persistence` (Apple credential) — representative: the Apple OAuth adapter family, `RefreshTokenAdapter`, `WithdrawnAccountAdapter`

**Core Domain Model**
```
OauthMemberProfile (result of OauthPort.fetchProfile: provider + identifier)
    → login if already a member, otherwise branch into the onboarding flow
IssuedOnboardingToken ──(profile submitted)──→ SignupTransactionService
                                                  → creates Member (member domain) + SajuChart (SELF, saju domain) together
IssuedAccessToken / IssuedRefreshToken (issued on successful login, stored in Redis)
AppleOauthCredential (Apple-only, stores the authorization code used for revoke)
```

**Must-read files**
- `SignupService.kt` — the conductor of the whole signup flow. The reason it must stay a plain `@Service` is the crux of "Decisions & Traps" below
- `SignupTransactionService.kt` — the actual commit point that bundles the member + SELF 명식 into one transaction
- `MemberSignedUpEventListener.kt` — the point where daily fortune generation is handed off to a dedicated worker thread after commit
- `WithdrawnAccountPort.kt` — the contract for the re-signup restriction check. Pairs with the withdrawal flow in `module-member/CLAUDE.md`

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
