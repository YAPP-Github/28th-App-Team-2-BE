# External Suite Installation

Detailed reference for the `external-harness` skill. Read this when setting up a new machine, onboarding a teammate, or upgrading gstack. The routing and ban tables live in the skill body.

---

## Installed Stack

| Suite | Source | Scope | Prefix |
|-------|--------|-------|--------|
| mattpocock-skills | `claude-plugins-official` | per-user | `/grill-me`, `/wayfinder`, … |
| superpowers | `obra/superpowers` (`superpowers-dev`) | per-user | auto-invoked skill names |
| compound-engineering | `EveryInc/compound-engineering-plugin` | per-user | `/ce-*`, `/lfg` |
| gstack | `~/.claude/skills/gstack` (vendored clone) | per-user | `/plan-*-review`, `/review`, … |

**These are user-scoped — git doesn't share them.** A teammate who clones this repo gets our `.claude/`, but not these four. Each teammate runs the following once:

```bash
claude plugin marketplace add obra/superpowers
claude plugin marketplace add EveryInc/compound-engineering-plugin
claude plugin install superpowers@superpowers-dev
claude plugin install compound-engineering@compound-engineering-plugin
claude plugin install mattpocock-skills@claude-plugins-official

# gstack is not a marketplace plugin — it's self-vendored
brew install bun   # gstack needs Bun v1.0+
git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git \
  ~/.claude/skills/gstack && cd ~/.claude/skills/gstack && ./setup
```

Until a teammate does this, none of the external skills are available to them — our own harness needs to be self-sufficient on its own.

---

## Third-Party Code Warning

**These commands run third-party code under your user account.** Plugin installs pull a pinned version through the marketplace, but the gstack line clones a mutable default branch and runs `./setup` immediately — no commit pinning, signing, or checksum verification at all. `./setup` writes to `~/.claude/settings.json` (it registers a Stop hook).

Before running it, either read the upstream `setup` script, or pin a reviewed revision instead of the default branch:

| To pin | Command |
|--------|---------|
| A tag | `git clone --branch <tag> --single-branch --depth 1 <url> ~/.claude/skills/gstack` |
| An exact commit SHA | `git clone <url> ~/.claude/skills/gstack`, then `git fetch origin <sha> && git checkout <sha>` (detached HEAD) |

`--branch` accepts a branch or tag name only — it does **not** accept an arbitrary commit SHA, so a SHA needs the separate fetch + detached-checkout path. Treat a gstack upgrade the same way.

---

## gstack Installs a User-Level Stop Hook

`./setup` adds a `timeline-stop-hook` to `~/.claude/settings.json` (backing it up to `settings.json.bak.<ts>` first). This repo's `.claude/settings.json` is **not** touched — verified. So two Stop hooks now run: ours (`stop-format.sh` → `ktlintFormat`) and gstack's. They don't conflict, they just add up.

To remove gstack's:

```bash
~/.claude/skills/gstack/bin/gstack-settings-hook remove-source --source gstack-timeline-stop
```

To remove gstack entirely, delete `~/.claude/skills/gstack` — `claude plugin disable` doesn't apply, because gstack is a vendored clone rather than a plugin.
