# member

Owns the member record, its profile fields, and withdrawal.

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
