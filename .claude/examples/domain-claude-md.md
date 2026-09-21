# Per-Domain `CLAUDE.md` Template (canonical)

The template for `module-{domain}/CLAUDE.md` — the context that loads automatically when the agent works inside that domain's modules.

Place it at the **domain wrapper** (`module-saju/CLAUDE.md`), not inside a layer module, so it covers all four layers at once.

## What Belongs Here

Only what is **non-obvious, domain-specific, and spans more than one file**. A domain `CLAUDE.md` answers *what this bounded context is and why it is shaped this way* — never *how the architecture works*.

The test: **could a competent agent derive this by reading one file?** If yes, leave it out.

| Do NOT write here | It already lives in |
|-------------------|---------------------|
| Module structure, dependency direction, package rules | `architecture` skill |
| Ports & adapters pattern, transaction-boundary rules, DTO↔domain mapping, Swagger, response envelope | `architecture` skill |
| Naming, formatting, ktlint | `code-style` skill |
| Exception hierarchy, `ResponseCode` | `error-handling` skill |
| Test strategy per layer, TestContainer | `testing` skill |
| Global constraints (UUIDv7 PKs, `@CommandService`, forbidden imports) | root `CLAUDE.md` |
| A rule already stated in a class's KDoc | that class |
| Entity lists, method signatures, field names | the code |

That last pair matters most in practice. `MemberSajuLink`'s KDoc already explains why ownership is split from the chart — repeating it here creates two places to update, and they will drift.

## Template

Write it in **English**, like every other harness artifact in this repo (root `CLAUDE.md`, the skills, `module-{domain}/docs/*`). Korean domain vocabulary (명식, 일진, 오행) stays in Korean inline — translating it loses precision.

```markdown
# {domain}

{One line: what this bounded context is responsible for.}

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

Three sections after the title, in this order. Drop a section entirely when a domain has nothing real to say in it — an empty heading is worse than no heading. Aim for **40–70 lines**; past that, the detail belongs in a skill or in `docs/solutions/`.

## Worked Example

`module-saju/CLAUDE.md` is the reference implementation — read it rather than a copy kept here, so the two cannot drift.

It is worth reading for three things it does:

- The **"Does not own" column** names the domain that owns each excluded concept, so the boundary is actionable rather than a disclaimer.
- The **contracts section states a pattern**, not just a list: saju implements all eight of its `shared` ports in `-application` rather than `-adapter-out`, which contradicts the root `CLAUDE.md` example — so it gives the rule for choosing between them.
- The **traps are the kind nothing else records**: a doc section that the implementation superseded, a generated data file that must not be hand-edited, and a year range that is a data limit rather than a validation preference.

And note what it omits: no module tree, no entity list, no port signatures, no restatement of `MemberSajuLink`'s invariants (its KDoc owns those), and no `MAX_PARTNER_COUNT` — that one is a named constant in a single file, so it fails the derive-from-one-file test.

## Authoring Order

1. Write the Responsibility Boundary first — the "Does not own" column is what prevents scope creep into neighbouring domains, and it is the column people forget.
2. Fill Cross-Domain Contracts from the actual `shared.*Port` implementations, not from memory. Verify with:
   ```bash
   grep -rln "{X}Port" --include="*.kt" module-*/
   ```
3. Add Decisions & Traps only for things that actually bit us. An empty list is a valid outcome for a young domain.
4. After writing, delete anything the root `CLAUDE.md` or a skill already says.
