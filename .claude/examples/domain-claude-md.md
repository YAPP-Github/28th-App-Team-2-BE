# Per-Domain `CLAUDE.md` Template (canonical)

The template for `module-{domain}/CLAUDE.md` — the context that auto-loads for an agent working inside that domain module.

Lives in the **domain wrapper** (`module-saju/CLAUDE.md`), not inside a layer module — that way it covers all 4 layers at once.

## What Belongs Here

Only what is **non-obvious, domain-specific, and spans multiple files**. A domain `CLAUDE.md` answers *what this bounded context is and why it's shaped this way* — never *how the architecture works*.

The test: **could a competent agent read one file and derive this itself?** If so, cut it. The exception is a **summary that requires synthesizing multiple files** — a single class's role can be read from that one file, but how a domain's core aggregates relate to each other requires synthesizing several files and their KDoc.

| Don't write here | Already lives |
|-------------------|---------------------|
| The generic pattern behind module structure (4 layers, dependency direction, package naming rules) | `architecture` skill |
| Ports & adapters pattern, transaction-boundary rules, DTO↔domain mapping, Swagger, response envelope | `architecture` skill |
| Naming, formatting, ktlint | `code-style` skill |
| Exception hierarchy, `ResponseCode` | `error-handling` skill |
| Per-layer test strategy, TestContainer | `testing` skill |
| Global constraints (UUIDv7 PKs, `@CommandService`, forbidden imports) | root `CLAUDE.md` |
| A rule already stated in a class's KDoc | that class |
| A full list of classes/files, method signatures, field names | the code itself |

The last pair matters most in practice. `MemberSajuLink`'s KDoc already explains why ownership is split from the chart — repeating it here doubles the update points, and they will eventually drift. The "Module Structure" section below is **curated, not exhaustive**, for the same reason: `saju-domain` alone has 47 files — list them all and it goes stale at the next refactor.

## Template

Written in **English**, like every other harness artifact in this repo. Korean domain terms (명식, 일진, 오행, etc.) stay in Korean inline — translating them would lose precision.

```markdown
# {domain}

{One line: what this bounded context is responsible for.}

## Overview

{Why this domain exists — its role from a product/business perspective. One or two paragraphs.}

{Define, at minimum, any Korean domain-specific terms this domain uses that another domain's contributor might not know. Do not redefine a term another domain's doc already owns (e.g. 명식·일진·연주 are defined by `saju`) — reference it instead.}

## Module Structure

**Packages**
- `{domain}-domain`: {1–2 subpackages} — representative: `{ClassA}`, `{ClassB}`
- `{domain}-application`: {subpackages} — representative: `{ServiceA}`
- `{domain}-adapter-in`: {if any — representative controller/DTO}
- `{domain}-adapter-out`: {subpackages} — representative: `{AdapterA}`

**Core domain model**
```
{Entity A} ← {Entity B}({relationship}) → {Entity C}
```

**Must-read files** (3–5, layer-agnostic — the files you need to actually understand this domain)
- `{File}` — {one line: why read this first}

## Responsibility Boundary

| Owns | Does not own |
|------|--------------|
| {concept} | {concept} → `{other domain}` |

## Cross-Domain Contracts

| Port | Consumed by | What the consumer branches or acts on |
|------|-------------|---------------------------------------|
| `{X}Port` | `{domain}` | {why they need it — the decision or action it feeds} |

{State where this domain's ports are implemented — `-application` vs `-adapter-out` — and the rule for choosing.}

{If the domain publishes or consumes `shared.event.*`, record that too — it is a separate channel from ports, and which one a new integration should use is the question this section has to answer.}

> A plain read needs no port — the client calls that domain's `*UseCase` directly (root `CLAUDE.md`, cross-domain criteria).

## Decisions & Traps

- **{Title}** — {what went wrong or was decided, and why the current shape follows}
```

The three sections after the title stay in this order. If a domain genuinely has nothing for a section, cut the whole section — an empty heading is worse than no heading. There is no fixed line-count target — domain size varies too widely (`saju` has 100+ files, `terms` has 37); the "only what needs synthesis, only what's needed" principle keeps length in check on its own.

## Worked Example

`module-saju/CLAUDE.md` is the reference implementation — don't keep a copy here, read that file directly so the two never drift apart.

Worth reading for three reasons:

- **The "Overview" section defines the Korean terms saju supplies to every other domain** (명식, 일진, 연주, 만세력, etc.), so the other 9 domain docs reference this definition instead of redefining it.
- **"Module Structure" doesn't enumerate all 47 files in `saju-domain`** — it names a handful of must-read files (`SajuChart`, `MemberSajuLink`, `SajuCalculator`) and the relationship between them, drawn as an arrow diagram.
- **The "Does not own" column** names the domain that owns each excluded concept, so the boundary is actionable information rather than a mere disclaimer.
- **The contract section states a pattern, not just a list**: saju implements all 8 `shared` ports in `-application` rather than `-adapter-out`, which contradicts the root `CLAUDE.md` example — so it states the rule for choosing between the two.
- **The trap entries are the kind recorded nowhere else**: a doc section superseded by the actual implementation, a generated data file that must never be hand-edited, and a year range that's a data limitation rather than a validation preference.

Also notice what's omitted: no module tree, no entity list, no port signatures, no restatement of `MemberSajuLink`'s invariants (that's the class's own KDoc), and no `MAX_PARTNER_COUNT` — a named constant living in one file, which fails the "derivable from one file" test.

## Writing Order

1. Write Overview first — the reason a domain exists is the premise every later boundary call rests on.
2. Fill Module Structure by actually looking at the code: run `find module-{domain} -name "*.kt"` to get the big picture first, then pick only the 3–5 that genuinely matter. Don't paste in the full listing.
3. Write Responsibility Boundary — the "Does not own" column is what stops scope creep into a neighboring domain, and it's the column people forget most.
4. Fill Cross-Domain Contracts from the actual `shared.*Port` implementations, not memory. Verify with:
   ```bash
   grep -rln "{X}Port" --include="*.kt" module-*/
   ```
5. Add to Decisions & Traps only what has actually bitten someone. An empty list is a fine outcome for a young domain.
6. After writing, delete anything already covered by the root `CLAUDE.md` or a skill.

## Staying Current

This doc staying in sync with the code relies on a human remembering to update it each time — whether a class is "core" enough to belong here is a judgment call that can't be enforced by a script. Instead, right before opening a PR, the `commit-push-pr` skill judges whether this branch's per-domain changes are structural, and if so and this doc wasn't updated alongside them, it asks the user (see `commit-push-pr` skill, "Check domain docs before opening the PR").
