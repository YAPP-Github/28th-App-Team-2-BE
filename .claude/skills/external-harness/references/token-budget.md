# Session Token Budget

Detailed reference for the `external-harness` skill. Read this when session context gets tight, or when deciding whether a suite is worth its cost.

Scope: this repo's own skills plus the four suites the team shares — **gstack, superpowers, mattpocock-skills, compound-engineering**. Anything else a given machine has loaded is personal tooling and out of scope here; measure it with `/context` and decide per machine.

---

## How the Skill Listing Costs Tokens

Every activated skill's **name** loads into every session. Descriptions are budgeted:

- The listing budget is roughly **1% of the model's context window**.
- When the listing exceeds it, Claude Code **drops descriptions starting with the least-invoked skills**, so the ones you use most keep their full text. A dropped description takes the matching keywords with it, which is how a skill silently stops being selected.
- Per entry, `description` + `when_to_use` is truncated to **1,536 characters** regardless of budget — so put the primary use case first.
- Knobs: `skillListingBudgetFraction` (e.g. `0.02` = 2%), `skillListingMaxDescChars`, or the `SLASH_COMMAND_TOOL_CHAR_BUDGET` env var for a fixed character count. `skillOverrides` set to `"name-only"` keeps a low-priority skill listed without its description.
- Measure, don't guess: `/doctor` reports the listing's estimated context cost and its biggest contributors, `/context`'s Skills row reports the post-budget size (what the model actually receives), and `/skill-doctor` finds unused skills.
- Agent definitions are a **separate** always-loaded cost from the skill listing — `/context` reports them on their own row. Worth checking, because it's easy to optimize the skill listing and miss a larger agent cost sitting next to it.

---

## Measured Cost (`/context`, 2026-10-05, Opus 5 / 1M window)

| Source | Skills | Listing cost | Agents | Routed by us? |
|--------|-------:|------:|---|---|
| this repo | 15 | **~240** (only 3 listed) | 4 (~335) | — |
| compound-engineering | 27 | ~540 | none | ✓ 2 (`ce-plan`, `ce-compound`) |
| superpowers | 15 | ~300 | none | ✓ 2 (TDD, systematic-debugging) |
| mattpocock-skills | 11 | ~250 | none | ✓ 2 (`grill-me`, `wayfinder`) |
| gstack | 54 → **6 on** | ~150 | none | ✓ 3 review skills |
| **shared total** | | **~1,480** | **~335** | |

**Our own 15 skills cost ~240 tokens, because `paths` scopes them out.** Their descriptions total ~2,650 characters, but a `/context` taken while editing docs listed only three — `architecture`, `external-harness`, `git-workflow`. The twelve carrying a `paths:` or `disable-model-invocation` flag were absent from the listing entirely. That is the measured answer to a question the official docs leave open: **`paths` does not merely gate auto-invocation, it keeps the entry out of the listing while the paths don't match.**

**None of the four shared suites ships agent definitions**, so the whole agent cost in this repo is our own four (`code-reviewer`, `domain-scaffolder`, `test-validator`, `test-writer`).

An earlier version of this file estimated **~5,890 tokens** for four suites and named compound-engineering the worst offender. Measurement says otherwise: the four shared suites together are **~1,480 tokens**, and compound-engineering's ~540 buys the two skills we route to in phases 2 and 10 — a fine ratio. The old figure also had the skill counts wrong (compound-engineering 36 → 27, mattpocock-skills 25 → 11). Treat any number in this file as stale until you've re-run `/context`.

---

## Which gstack Skills Stay On

Only these six; the other 48 are switched off:

```
_gstack-command   gstack   gstack-upgrade   plan-eng-review   plan-ceo-review   review
```

`_gstack-command` and `gstack` are routers — without them, the three review skills won't load. `gstack-upgrade` stays so the suite can be updated without re-enabling everything.

Those 48 off-switches sit in `.claude/settings.local.json`, which `git clone` does not carry — gstack is a per-user vendored clone, so a teammate who never installed it has nothing to switch off. That's why the list above is written down here: **the documentation is the shareable artifact, not the switch state.** `/skills` writes the same file, so toggling through the menu and editing the file by hand are equivalent.

Verify what's actually loaded with a one-off session rather than trusting the UI:

```bash
claude -p "Do not use any tools. Output ONLY the names of skills whose source is gstack, one per line."
```

---

## Toggle Mechanics and Their Limits

| Mechanism | Applies to | Scope |
|-----------|-----------|-------|
| `paths:` / `disable-model-invocation:` frontmatter | one of **our own** skills | committed — the only suppression that travels with the repo, and it keeps the entry out of the listing entirely |
| `skillOverrides` in `.claude/settings.json` | **bundled** skills, by name | committed; safe to share because every install has the same bundled set |
| `skillOverrides` in `.claude/settings.local.json` | per-user skills (gstack among them), by name | per-user |
| `enabledPlugins: {"<plugin>@<marketplace>": false}` in `.claude/settings.local.json` | a whole plugin suite, **including any agents it ships** | per-user, this repo only |
| `/skills` menu | skills under `~/.claude/skills/` | writes `settings.local.json` — same file as the rows above |
| `claude plugin disable <plugin>` | a whole plugin suite | per-user, **every** project |
| `gstack-config set proactive false` | gstack's auto-suggestion behavior | per-user |

### Where each switch belongs

**Don't commit a switch for software the team doesn't have.** The suites are per-user installs, so a committed `enabledPlugins` entry can name a plugin — or a marketplace — that a teammate never installed: dead config in a shared file at best. The same goes for `skillOverrides` on per-user skills. Keep those in `settings.local.json` and let this document carry the recommendation instead.

Bundled skills are the exception, and the only `skillOverrides` block in the committed `settings.json`: they ship with Claude Code, so every teammate has exactly the same set, and "this backend repo doesn't need chart, artifact or browser skills, and talks to Vertex AI rather than the Claude API" is a fact about the repo rather than about one person's toolbox.

Two further limits:

- **`skillOverrides` does not apply to plugin skills** (documented limitation), so a plugin can only be trimmed whole, never per skill. That's why gstack — a vendored clone, not a plugin — can keep 6 of 54 while the three plugin suites are all-or-nothing.
- **`enabledPlugins` in `settings.local.json` beats `claude plugin disable`** for a repo-specific choice: it only affects sessions in this repo instead of every project, and it reverts by flipping one boolean. Either way, plugin enablement is read at startup, so a running session keeps the old set until restart — `/context` won't reflect the change until then.

Toggling never touches files — it only stops loading, so it's reversible and doesn't conflict with `/gstack-upgrade`. **Never delete a skill directory to disable it.**
