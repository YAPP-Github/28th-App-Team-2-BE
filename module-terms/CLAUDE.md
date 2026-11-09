# terms

Owns the terms catalogue and each member's agreements — including the one that gates night-time pushes.

## Overview

Manages the terms this service requires (terms of service / privacy / marketing / night-time promotional messaging) and whether each member has agreed to each one. It's both the basis for enforcing required-terms agreement during signup/onboarding, and the sole source `notification` consults to gate night-time marketing sends — without this domain, neither function has anything to stand on.

Korean terms this domain defines (other domain docs reference this definition rather than redefining it):

- **`TermsType`**: four values — `SERVICE` (terms of service, required), `PRIVACY` (personal-data collection/use, required), `MARKETING` (marketing opt-in, optional), `NIGHT_PUSH` (night-time promotional messaging, optional). A stable code for identifying a specific terms entry without hardcoding a UUID.

## Module Structure

**Packages**
- `terms-domain`: entities/policy at the root — representative: `Terms`, `MemberTermsAgreement`, `TermsAgreementPolicy`
- `terms-application`: `*Service` at the root — representative: `GetTermsService`, `SaveTermsAgreementService`
- `terms-adapter-in`: `adapter.web` — representative: `TermsController`, `TermsApi`
- `terms-adapter-out`: JPA + `shared`-port implementation — representative: `MemberTermsAgreementRepositoryAdapter`, `GetPushConsentAdapter`

**Core Domain Model**
```
Terms (type: TermsType, required: Boolean) — a terms-catalogue entry
    ↑ referenced by termsId
MemberTermsAgreement (memberId + termsId + agreed) — per-member agreement/non-agreement record
```
`agreed = false` is stored explicitly too (distinct from no row at all) — that's what lets a later query accurately filter "members who declined marketing."

**Must-Read Files** (layer-agnostic)
- `Terms.kt` / `TermsType.kt` — the catalogue's 4 types and the `required` flag
- `MemberTermsAgreement.kt` — its KDoc explains why non-agreement is recorded explicitly too
- `TermsAgreementPolicy.kt` — where the three submission invariants are enforced (see "Decisions & Traps" below)

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
