# member

Owns the member record, its profile fields, and withdrawal.

## Overview

Member is the ownership axis for essentially all domain data this service touches (명식, fortunes, conversations, etc.). This domain owns the member record itself (profile, role) and the procedure for when a member leaves the service (withdrawal) — there's an asymmetry where creating an account (signup) is `auth`'s job, while destroying one (withdrawal) is this domain's.

- **Withdrawal**: an immediate, final hard-delete policy (v1.0, no recovery grace period). See "Withdrawal — the ordering is the design" below.
- **De-identification**: removing anything that could identify the member (e.g. member ID) from the withdrawal reason log — VOC analysis stays possible, but who withdrew cannot be traced back.

## Module Structure

**Packages**
- `member-domain`: entities, use cases, value objects at the root — representative: `Member`, `MemberWithdrawalLog`
- `member-application`: `*Service` at the root — representative: `WithdrawMemberService`, `WithdrawMemberTransactionService`
- `member-adapter-in`: controller, DTOs — representative: `MemberController`, `MemberApi`
- `member-adapter-out`: JPA repository adapters, plus the 5 ports consumed by `auth`/`daily-fortune` — representative: `MemberRepositoryAdapter`, `MemberWithdrawalLogRepositoryAdapter`, `GetMemberIdAdapter`

**Core Domain Model**
```
Member (profile + Role(MEMBER|ADMIN))
    ──on withdrawal──→ MemberWithdrawalLog (reason only, no memberId — de-identified)
    ──on withdrawal──→ calls saju.DeleteMemberSajusPort,
                        calls auth.RevokeMemberTokensPort / RevokeOauthTokenPort / RegisterWithdrawnAccountPort
```

**Must-read files**
- `Member.kt` — the member entity. `update` draws a clean line between the fields that can be edited (birth info, interests) and the ones that can't (name, role, social-login identifier)
- `WithdrawMemberTransactionService.kt` — the actual order that bundles withdrawal's six steps into one transaction, the basis for "Withdrawal — the ordering is the design"
- `WithdrawMemberService.kt` — the follow-up step that calls the Apple revoke API outside the transaction

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| `Member`, profile fields, `Role` (`MEMBER`/`ADMIN`) | Authentication, tokens, OAuth → `auth` |
| Withdrawal orchestration and the de-identified withdrawal log | The re-signup restriction registry → `auth` (enforced at login) |
| | 명식 → `saju`; this domain asks it to delete or replace charts |

## Cross-Domain Contracts

Provides — and **all five are implemented in `member-adapter-out`**, not in `member-application`:

| Port | Consumed by |
|------|-------------|
| `CreateMemberPort` | `auth` (signup) |
| `GetMemberIdPort` | `auth` |
| `IsAdminMemberPort` | `auth` (admin authorization) |
| `GetMemberIdsPort` | `daily-fortune` (batch enumeration) |
| `GetMemberFortuneProfilePort` | `daily-fortune`, `day-fortune`, `year-fortune` |

They sit in `adapter-out` because each is a row read or write with no use-case logic. That is the deciding test, and `saju` lands on the other side of it — all eight of its ports live in `saju-application` because each is a real use case. Neither placement is the default; the question is always whether use-case logic is involved.

Consumes `DeleteMemberSajusPort` and `ReplaceSelfSajuChartPort` (`saju`), and `RevokeMemberTokensPort`, `RevokeOauthTokenPort`, `RegisterWithdrawnAccountPort` (`auth`).

## Withdrawal — the ordering is the design

Policy v1.0: immediate and final, **no recovery grace period**, hard delete. `WithdrawMemberTransactionService` commits six steps in one transaction, in this order:

1. write the de-identified reason log
2. hard-delete the member's 명식 (via `saju`)
3. revoke auth tokens
4. register the SNS identifier in the 90-day restriction list
5. (Apple only) delete the local OAuth credential
6. hard-delete the member

**The Apple revoke API call happens after that commit**, from `WithdrawMemberService`. Keeping it outside is deliberate on three counts: it is external HTTP taking up to 8 seconds, it cannot be undone if a later step rolls back, and inside the transaction it would pin a DB connection for its whole duration. The transaction deletes the *local* credential; the remote revoke is a separate post-commit call using the returned credential.

## Decisions & Traps

- **The withdrawal log holds no member identifier** — reason and detail only, for VOC analysis. Adding a `memberId` "for traceability" defeats the de-identification the policy requires.
- **Restriction registration is this domain's call but `auth`'s storage.** Do not add a local copy of the withdrawn-account list; the hash and its TTL belong to `auth`, which is where login checks them.
- Steps 1–6 being one transaction is what prevents a half-withdrawn account — a member deleted while their 명식 survives, or tokens still valid after deletion. Do not split it for readability.
