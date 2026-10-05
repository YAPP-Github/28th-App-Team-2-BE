# Session Token Budget

Detailed reference for the `external-harness` skill. Read this when session context gets tight, or when deciding whether a suite is worth its cost.

---

## How the Skill Listing Costs Tokens

Every activated skill's **name** loads into every session. Descriptions are budgeted:

- The listing budget is roughly **1% of the model's context window**.
- When the listing exceeds it, Claude Code **drops descriptions starting with the least-invoked skills**, so the ones you use most keep their full text. A dropped description takes the matching keywords with it, which is how a skill silently stops being selected.
- Per entry, `description` + `when_to_use` is truncated to **1,536 characters** regardless of budget — so put the primary use case first.
- Knobs: `skillListingBudgetFraction` (e.g. `0.02` = 2%), `skillListingMaxDescChars`, or the `SLASH_COMMAND_TOOL_CHAR_BUDGET` env var for a fixed character count. `skillOverrides` set to `"name-only"` keeps a low-priority skill listed without its description.
- Measure, don't guess: `/doctor` reports the listing's estimated context cost and its biggest contributors, `/context`'s Skills row reports the post-budget size (what the model actually receives), and `/skill-doctor` finds unused skills.

For scale: this repo's own 12 skill descriptions total ~2,200 characters. The external suites below are an order of magnitude more.

---

## Measured Cost per Suite

| Suite | Skills | Active | Always loaded | Per-skill toggle? |
|-------|-------:|--------:|----------:|-------------------|
| gstack | 54 | **6** | **~450 tokens** | ✅ possible |
| compound-engineering | 36 | 36 | ~2,990 tokens | ❌ not possible |
| mattpocock-skills | 25 | 25 | ~1,610 tokens | ❌ not possible |
| superpowers | 15 | 15 | ~840 tokens | ❌ not possible |
| **Total** | **130** | **82** | **~5,890 tokens** | |

`claude plugin details <name>` shows per-plugin figures.

**Current stance: eat the ~5.9k cost.** compound-engineering has the worst ratio — ~2,990 tokens for the 2 skills we actually route to. If session context gets tight, the next move is to disable that plugin and reimplement `/ce-compound` as this repo's own `compound` skill — one more application of the "our harness wins" rule.

---

## Which gstack Skills Stay On

Only these six; the other 48 are switched off:

```
_gstack-command   gstack   gstack-upgrade   plan-eng-review   plan-ceo-review   review
```

`_gstack-command` and `gstack` are routers — without them, the three review skills won't load. `gstack-upgrade` stays so the suite can be updated without re-enabling everything.

**Those 48 off-switches live in this repo's `.claude/settings.json` under `skillOverrides`**, so they are committed and apply to every teammate on clone. This is a change from the earlier setup, where `/skills` wrote them to `.claude/settings.local.json` — a per-user file that `git clone` doesn't carry, which meant each teammate had to re-toggle 48 skills by hand.

Verify what's actually loaded with a one-off session rather than trusting the UI:

```bash
claude -p "Do not use any tools. Output ONLY the names of skills whose source is gstack, one per line."
```

---

## Toggle Mechanics and Their Limits

| Mechanism | Applies to | Scope |
|-----------|-----------|-------|
| `skillOverrides` in `.claude/settings.json` | personal + project skills, by name | committed, team-wide |
| `/skills` menu | skills under `~/.claude/skills/` | writes `settings.local.json` (per-user) |
| `claude plugin disable <plugin>` | a whole plugin suite | per-user, all-or-nothing |
| `gstack-config set proactive false` | gstack's auto-suggestion behavior | per-user |

**`skillOverrides` does not apply to plugin skills** — that's a documented limitation, and it's why compound-engineering, mattpocock-skills, and superpowers can't be trimmed per skill. Their only lever is `claude plugin disable`, which also removes the skills we route to.

Toggling never touches files — it only stops loading, so it's reversible and doesn't conflict with `/gstack-upgrade`. **Never delete a skill directory to disable it.**
