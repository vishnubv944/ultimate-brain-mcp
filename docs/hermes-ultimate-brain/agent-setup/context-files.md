# Context Files — Where the Operational Detail Lives

Context: [soul-md.md](soul-md.md), [../../hermes/PURPOSE.md](../../hermes/PURPOSE.md) and
the rest of `docs/hermes/`. Concrete recommendation for AGENTS.md content, not a feature
description.

## The boundary, precisely

Hermes auto-discovers `.hermes.md`, `AGENTS.md`, `CLAUDE.md`, `SOUL.md`, `.cursorrules`.
Nous Research's own guidance draws the line cleanly: **SOUL.md is who Hermes is and how
it talks — stable, identity-only. AGENTS.md is what Hermes actually does — project
instructions, task-specific behavior, workflow detail, the stuff that's expected to
change as the work evolves.** Their own example: "if your SOUL.md says 'always confirm
before acting' but in this specific project you trust the agent to run tests
aggressively, AGENTS.md is where you say so." Same pattern applies here directly — SOUL.md
carries the *general* rule about autonomy (act on routine/reversible things, escalate
structural ones); AGENTS.md carries the *specific* operational mechanics of how that
plays out inside Ultimate Brain.

## What belongs in AGENTS.md, concretely

Everything already designed in `docs/hermes/` is operational content, not identity — it
all belongs here, either inlined or referenced:

- **The three modes** (Execution / Planning / Review) and the rule that Hermes must
  classify which mode a moment belongs to before acting — from
  [MODES.md](../../hermes/MODES.md).
- **The lock mechanism**: a committed plan is locked until the next checkpoint; an
  off-checkpoint change request gets named as a re-plan attempt and logged, not silently
  honored — from [PURPOSE.md](../../hermes/PURPOSE.md).
- **The Planning-session flow**: open discussion is fully unlocked, convergence is the
  user's call alone (Hermes may only *surface* a long/circling session, never push to
  close it), classification into Goal/Project/Note/priority-update/stays-as-is, then
  lock + stated checkpoint — from [planning-mode.md](../../hermes/planning-mode.md).
- **The daily Plan mechanism**: pull today's existing shape (routines as recurring
  tasks + overdue + already-committed), compute free gaps, draft a full schedule,
  propose once, patch don't renegotiate, single `bulk_update_tasks` commit — from
  [daily-plan-phase.md](../../hermes/daily-plan-phase.md).
- **Inbox processing**: the GTD decision-tree routing for every captured item, with
  Someday/Snoozed treated as legitimate holding pens, not failures — from
  [inbox-processing.md](../../hermes/inbox-processing.md).
- **End-of-day review**: process-then-reflect, sequential not blended, nightly log
  surfaced plainly with deep pattern analysis reserved for the weekly cadence — from
  [eod-review.md](../../hermes/eod-review.md).
- **Tool-usage patterns**: PARA hierarchy as the mental model (Tags→Goals→Projects→Tasks),
  `bulk_update_tasks` for 3+ task changes, the `Hermes Log` Note type for deviation
  logging, which tools are first-class (Tasks/Projects/Notes/Tags/Goals/Milestones/Work
  Sessions) vs. generic-only today (People/Books/Recipes/etc.) — from
  [mcp-roadmap.md](../../hermes/mcp-roadmap.md).

## Recommended structure for AGENTS.md

Rather than copy-pasting the full content of every `docs/hermes/*.md` file into
AGENTS.md (which would bloat the system prompt and drift out of sync with the source
docs), reference them and pull out only the operational rules that need to be active on
every turn:

```markdown
# AGENTS

## Mental model
Everything is Tags/Areas → Goals → Projects → Tasks. Classify what mode a moment is in
(Execution / Planning / Review) before acting — see docs/hermes/MODES.md.

## The lock
A committed plan is locked until its stated checkpoint. An off-checkpoint change request
gets named as a re-plan attempt, logged as a Hermes Log note, and redirected — never
silently honored. Daily check-ins are progress/blockers only; the weekly window is where
plan edits are actually welcome.

## Planning sessions
Fully open discussion. Convergence is the user's call, not mine — I may name a long or
circling session once, plainly, but never propose closing it. On "let's finalize,"
classify the outcome (Goal / Project / Note / priority update / stays-as-is) and commit.

## Daily Plan
Pull today's shape (routines + overdue + already-committed) via daily_review_snapshot,
compute free gaps, draft one complete schedule, propose it in full, patch on request,
commit with a single bulk_update_tasks call.

## Inbox processing
Run the GTD decision tree per captured item. Someday/Snoozed are correct outcomes, not
gaps to re-scan for later.

## Tools
Use bulk_update_tasks for 3+ task changes in one workflow. [full tool reference:
docs/hermes/mcp-roadmap.md and TOOLS.md]
```

This keeps AGENTS.md as the living, detailed operational layer, and SOUL.md as the small,
stable identity layer — matching the separation Hermes's own docs recommend, and matching
why `docs/hermes/` was built as a set of separate, focused files rather than one giant
spec in the first place.

## Sources

- [Which File Does What — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/which-file-does-what)
- [Use SOUL.md with Hermes — guide](https://hermes-agent.nousresearch.com/docs/guides/use-soul-with-hermes)
- [SOUL.md, AGENTS.md, USER.md and MEMORY.md explained — LumaDock](https://lumadock.com/tutorials/hermes-soul-agents-user-memory-files)

## Status

Concrete recommendation ready for review. AGENTS.md does not yet exist on the Pi — needs
to be created at `~/.hermes/AGENTS.md` (or the relevant project-scoped location) and
kept in sync with `docs/hermes/` as those files evolve, rather than treated as a
one-time copy.
