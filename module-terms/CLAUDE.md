# terms

Owns the terms catalogue and each member's agreements — including the one that gates night-time pushes.

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| The terms catalogue — `SERVICE`, `PRIVACY`, `MARKETING`, `NIGHT_PUSH` | Notification settings and delivery → `notification` |
| Member agreements and their validation | Member profile → `member` |

## Cross-Domain Contracts

Provides `GetPushConsentPort`, implemented in `terms-adapter-out` — a row read, so `adapter-out` is the right home. Consumes nothing.

**Scope it precisely.** The port answers *"has this member agreed to `NIGHT_PUSH`"* — not *"may we push this member at all"*. The everyday on/off switch is a notification setting and belongs to `notification`; consent is an agreement and belongs here. `notification` consults this only when a send falls in night hours, and only for `MARKETING`-class notifications.

The adapter's KDoc names the MSA path deliberately: when `terms` is split out, only this implementation is swapped for an HTTP adapter and `notification` stays untouched. The port is the stable contract — keep the implementation behind it thin enough that this remains true.

## Decisions & Traps

- **Submission invariants live in `TermsAgreementPolicy`, in the domain — not in the service, adapter, or controller.** It enforces three rules: no duplicate terms IDs within one request, every submitted ID must exist in the catalogue, and every required term must be agreed. Re-checking any of these in a controller duplicates the rule and will drift; extending agreement rules means extending the policy.
