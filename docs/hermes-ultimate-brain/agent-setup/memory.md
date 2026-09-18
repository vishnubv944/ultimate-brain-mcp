# Memory: MEMORY.md / USER.md vs. Ultimate Brain

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
[../pi-setup-audit.md](../pi-setup-audit.md) (boundary flagged as open there),
[../../hermes/PURPOSE.md](../../hermes/PURPOSE.md). Resolves the open question: what
should Hermes remember itself, vs. what must always be looked up live from Ultimate
Brain.

## How Hermes's native memory actually works

- **`MEMORY.md`/`USER.md` are injected into the system prompt from token 1** — no tool
  call, no latency, no query. This is exactly why it's tempting to put UB-derived facts
  there: it's the cheapest, fastest-available context. That temptation is the risk.
- Mid-session edits persist to disk immediately but only enter the *system prompt* on the
  **next** session — the current session keeps working from what it started with.
- **`session_search`** (SQLite + FTS5 over `~/.hermes/state.db`) backfills anything not in
  active memory by searching raw past messages, no summarization.
- Entries are security-scanned before acceptance (blocks injection/exfiltration patterns,
  invisible Unicode) since they land directly in the prompt.
- **External memory providers** (Honcho, Mem0, Supermemory, 5 others) can replace/augment
  this with deeper reasoning — dialectic user modeling, semantic search, automatic fact
  extraction. Only one external provider can be active at a time; built-in memory stays
  active alongside whichever one is chosen.
- An open GitHub feature request (#10835) proposes exposing Hermes's own memory via an
  MCP server so external clients could read/write it — not built yet, just a forward
  pointer, not actionable now.

## The boundary rule

**MEMORY.md/USER.md are for "how to work with me." Ultimate Brain is for "what is true
about my life."** Never let a fact that has a live truth-value tracked in UB get written
into Hermes's own memory — the moment UB changes, that memory entry is now wrong, and
Hermes would confidently answer from stale, diverged state instead of checking. That's
the exact failure PURPOSE.md is designed to prevent, just moved into the memory layer
instead of the planning layer.

## What belongs in Hermes's native memory

- **Interaction preferences Hermes has itself observed** — what tone lands, what kind of
  nudge gets ignored vs. acted on, whether Telegram or CLI gets a faster real response,
  timing quirks in how the user actually engages (not data UB already tracks — behavior
  about the *conversation*, not the *life*).
- **Distilled, already-reviewed insights**, never raw data — e.g. "off-checkpoint replan
  attempts have clustered on Wednesday afternoons" is fine to remember *after* a Review
  session has actually looked at the log and concluded that; the raw log entries
  themselves stay in UB (the `Hermes Log` note type), not duplicated here.
- **Working-relationship facts that have no UB home at all** — e.g. how directly to phrase
  a redirect, whether the user wants the Sunday planning nudge or prefers to open it
  themselves. This is genuinely "how to work with me," not life data.

## What must always be looked up live, never cached here

- Any Task/Project/Goal state — status, due dates, completion. This is UB's entire job as
  system of record; caching it anywhere else recreates the two-sources-of-truth problem
  this whole project exists to eliminate.
- The deviation/replan log itself (raw entries) — lives in UB as `Hermes Log` notes, per
  the MCP roadmap decision. Memory can hold a *conclusion* drawn from it, never the log.
- People/relationship data — `Check-In` / `Last Check-In` dates live in the People
  database; memorizing "haven't talked to X in a while" anywhere else risks it going
  stale the moment a real check-in happens through UB directly.
- Anything Review mode surfaces — recompute from UB each time a pattern question comes
  up; don't let a one-time computed pattern silently calcify into a permanent "fact."

## External memory providers: defer

Given the whole point of this project is enforcing *one* source of truth, turning on a
second deep user-modeling layer (Honcho et al.) right now multiplies exactly the risk
this doc exists to close — Honcho explicitly models "preferences, communication style,
goals, and patterns," which overlaps directly with what UB's Goals/Tags/PARA structure is
already meant to own. Built-in `MEMORY.md`/`USER.md`, scoped to the boundary above, is
enough. Revisit only if a concrete gap shows up that the built-in system genuinely can't
cover.

## Status

Boundary decided. Not yet implemented — nothing has actually been written into
`MEMORY.md`/`USER.md` on the Pi yet reflecting this rule.
