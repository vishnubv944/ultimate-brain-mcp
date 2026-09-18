# Skills System — Fusing Ultimate Brain's Process Knowledge Into Hermes

Research + recommendation, 2026-09-18. Covers how Hermes Agent's Skills System actually
works and which of the `docs/hermes/` design docs should become real Skills, so that
running a UB ritual pulls from Hermes's own on-demand expertise instead of feeling like a
lookup into an attached system's documentation.

## How the Skills System actually works

- **Location & format**: `skills/<category>/<name>/SKILL.md` (or `optional-skills/...`),
  compatible with the open **agentskills.io** standard (the same format Claude, Codex,
  Gemini CLI, Cursor, and 20+ other platforms use — a skill written once works
  everywhere).
- **Frontmatter**: starts at byte 0 with `---`, closes with `\n---\n`. Required: `name`,
  `description`. Optional: `version`, `author`, `license`, `platforms`, and a
  `metadata.hermes` block for `tags`, `related_skills`, `config`.
- **Body structure**: conventionally `When to Use`, `Procedure`, `Pitfalls`,
  `Verification` sections.
- **Progressive disclosure**: only the frontmatter (`name` + `description`, a few dozen
  tokens) sits in context by default. The full body loads only when the agent judges the
  skill relevant to the current moment — this is the mechanism that lets Hermes carry a
  large amount of UB-specific process knowledge without it bloating every single
  conversation.
- **Supporting files**: `references/`, `templates/`, `scripts/`, `assets/` subdirectories
  ship alongside `SKILL.md` and install together.
- **Self-authoring**: Hermes can also write its own skills from experience (and, per a
  June 2026 update, capture a workflow as a skill via `/learn` without hand-writing
  `SKILL.md`) — relevant later if Hermes starts noticing its own repeated UB patterns, but
  the initial fusion work should be hand-authored, not left to emerge.

## Why this is the right mechanism for the native-fusion goal

A skill isn't documentation Hermes *looks up* — it's closer to trained-in procedural
knowledge that surfaces exactly when the situation calls for it, the same way a person
doesn't consciously "check a manual" to know how to run a familiar routine. Putting the
Plan/Execute/Review mechanics here (rather than, say, permanently in SOUL.md or AGENTS.md)
is what makes invoking "let's plan today" feel like Hermes reaching into its own
expertise about *your* life, not querying a bolted-on system's separate docs.

## Recommended skill breakdown

One skill per ritual/mechanism already designed, not one giant "ultimate-brain" skill —
matches progressive disclosure (only the relevant ritual's detail loads) and matches how
`docs/hermes/` is already split by concern.

| Skill | `name` | Maps to | Loads when |
|---|---|---|---|
| Daily planning | `ub-daily-plan` | `daily-plan-phase.md` | "let's plan today," morning trigger |
| Inbox processing | `ub-inbox-processing` | `inbox-processing.md` | capturing/triaging inbox items, referenced by the two skills below |
| End-of-day review | `ub-eod-review` | `eod-review.md` | "let's wrap up," evening trigger |
| Planning session | `ub-planning-session` | `planning-mode.md` | "let's plan this project/goal," brainstorming, Area-backlog pickup |
| Mode discipline | `ub-mode-contract` | `MODES.md` (+ the mode/trigger split) | any moment Hermes needs to judge which mode it's in and whether a boundary is being crossed |

`ub-mode-contract` is the one that should carry `metadata.hermes.related_skills` pointing
at all four ritual skills — it's the meta-rule ("which mode is this, and is Execution
quietly turning into Planning") that the other four all operate under, so it should be
the thing that pulls the right ritual skill in, not a peer to them.

`ub-inbox-processing` is referenced by both `ub-daily-plan` (Plan starts from
whatever's already processed) and `ub-eod-review` (which runs the same flow as its first
half) — use `related_skills` rather than duplicating the GTD decision tree in three
places.

## What does *not* become a skill

- **PURPOSE.md** — this is identity/motivation, not a procedure. It belongs distilled
  into **SOUL.md** (why Hermes exists, what it's protecting against), not loaded
  on-demand as a skill. A skill answers "how do I run this," not "who am I and why does
  this matter" — that has to be always-present, which is exactly what SOUL.md's slot #1
  position is for.
- **mcp-roadmap.md** — this is a build plan for the MCP itself, not agent behavior at
  runtime. No skill needed; it's implementation work tracked in this repo, not something
  Hermes needs to "know how to do" in conversation.

## Authoring recommendation

Each `SKILL.md`'s `description` field is what Hermes uses to decide relevance before
loading the body — write it the way the existing `docs/hermes/` files already open
("the detailed flow for the morning Plan step of My Day..."), not a generic one-liner,
since that's the only part of the skill visible before it's chosen. The body should be
adapted from the corresponding `docs/hermes/*.md` file (including its Mermaid flow) rather
than rewritten from scratch — the design work is already done; this is a packaging step,
not a redesign.

## Open item

Whether `ub-mode-contract` should be `always: true` / auto-loaded every session
(guaranteeing the boundary-discipline is never skipped) versus purely on-demand like the
others is a real tradeoff — always-loading it defeats some of the token-saving point of
skills, but making the *one rule that prevents the whole failure pattern* optional-to-load
is risky. Needs the actual Hermes `metadata.hermes.config` options checked against the
live docs before deciding — not resolved here.

## Sources

- [Skills System | Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-guide/features/skills/)
- [Hermes Agent Skill Authoring](https://hermes-agent.nousresearch.com/docs/user-guide/skills/bundled/software-development/software-development-hermes-agent-skill-authoring)
- [Creating Skills | Hermes Agent](https://hermes-agent.nousresearch.com/docs/developer-guide/creating-skills)
- [Skills System | NousResearch/hermes-agent | DeepWiki](https://deepwiki.com/NousResearch/hermes-agent/8-skills-system)
- [agentskills/agentskills — GitHub](https://github.com/agentskills/agentskills)
- [Nous Research Adds /learn to Hermes Agent's Skills System — MarkTechPost](https://www.marktechpost.com/2026/06/24/nous-research-adds-learn-to-hermes-agents-skills-system-capturing-workflows-as-slash-commands-without-hand-writing-skill-md/)

## Status

Research + recommendation, not yet authored. Next: write the actual five `SKILL.md`
files once SOUL.md/AGENTS.md content (identity vs. instructions split) is settled, so
skill bodies aren't duplicating what those files already establish.
