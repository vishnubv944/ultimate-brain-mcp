# Plugins & Hooks — Fusing UB into Hermes's Behavior Loop

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
[../../hermes/PURPOSE.md](../../hermes/PURPOSE.md),
[../../hermes/MODES.md](../../hermes/MODES.md). MCP registration already gives Hermes UB
*tools*. Hooks are the mechanism for going further — making UB state and rules part of
Hermes's behavior automatically, not just something available if the model remembers to
reach for it.

## What hooks actually offer (per official docs + source)

Plugins register hooks via `ctx.register_hook(name, callback)` in a `register()`
function, dropped into `~/.hermes/plugins/<name>/`. The two most relevant lifecycle
points:

- **`on_session_start`** — fires once when a new session begins.
- **`pre_llm_call`** — fires once per turn, before the tool-calling loop. If the callback
  returns a dict with a `"context"` key (or a plain string), Hermes **injects that text
  into the current turn** — the documented mechanism behind memory plugins, RAG, and
  guardrails.
- **`pre_tool_call`** / **`post_tool_call`** — fire around every individual tool
  invocation, with access to the tool name and arguments. Broader than the original
  4-hook list in our feature-surface doc — `hermes_cli.plugins.VALID_HOOKS` documents 27
  lifecycle events total.

**Important caveat, not hypothetical**: GitHub issue #2817 on the Hermes repo reports
these exact hooks (`pre_llm_call`, `post_llm_call`, `on_session_start`,
`on_session_end`) were *documented but never actually invoked* in some past version, with
PR #3542 (`feat: activate plugin lifecycle hooks`) fixing it. **Before designing anything
real on top of hooks, verify on the live Pi instance that they actually fire** — a
throwaway plugin that logs `"hook fired"` on `on_session_start` is a five-minute test
that avoids building on a broken foundation.

## Two concrete uses worth naming

**1. `on_session_start` → auto-inject a live UB snapshot.** Instead of Hermes needing the
model to remember to call `daily_review_snapshot` before it can say anything useful,
a hook calls it automatically and injects the result as context the moment a session
opens. Every conversation starts already knowing today's overdue items, what's on My
Day, and the inbox count — this is the single highest-leverage move for "feels native":
UB state becomes ambient truth Hermes already has, not something it goes and fetches.
**Low engineering cost** — one hook, one existing tool call, no new logic.

**2. `pre_tool_call` → enforce the plan-lock mechanically, not just conversationally.**
PURPOSE.md's core mechanism — off-checkpoint replan attempts get named and logged, not
quietly honored — currently lives entirely in *instructions* (what Hermes is told to do).
A `pre_tool_call` hook could intercept calls like `update_task`/`bulk_update_tasks` that
modify already-committed My Day items outside a checkpoint window, and either block with
a redirect or auto-write to the `Hermes Log` note type (per mcp-roadmap.md's decided
approach) — turning "the model is supposed to catch this" into "the system physically
catches this." This is real enforcement, not better wording in a prompt.

## The honest tradeoff on #2

This is real engineering, not config: the hook needs to know *what counts as
already-committed* (today's locked My Day set) and *when a checkpoint window is
currently open* — state that doesn't exist as a single queryable fact today, it has to be
derived from UB data (or tracked separately) each time the hook fires. That's a
meaningfully bigger lift than #1, and per
[../../hermes/mcp-roadmap.md](../../hermes/mcp-roadmap.md)'s own guiding principle — add
infrastructure for concrete proven need, not hypothetical robustness — **this should wait
until the conversational version (Hermes just following the PURPOSE.md instructions) has
actually been tried and shown to leak.** Building the mechanical enforcement before
knowing whether the instruction-following version is even insufficient is the same
over-preparation pattern this whole project exists to avoid, just moved one layer deeper
into infrastructure.

## Does UB need a dedicated custom plugin at all?

Yes, if either hook idea above is pursued — **hooks only exist inside plugins**; the MCP
server registration (already done) gives Hermes UB's *tools*, but grants no hook access
on its own. A small `ub-context` plugin (just `on_session_start`, calling the existing
`daily_review_snapshot` tool) is the minimum viable version of #1 and is small enough to
be worth building now. A plugin doing #2's tool-call interception is a separate, larger
piece of work and should stay deferred per the tradeoff above.

## Recommendation

Build the `on_session_start` context-injection plugin now — cheap, directly serves "UB
state should be ambient, not fetched," and doubles as the verification test for whether
hooks even fire correctly on this Hermes version. Leave `pre_tool_call` lock-enforcement
as a named, deferred option — revisit only once the conversational lock mechanism has
been run for real and shown where instruction-following actually breaks down.

## Sources

- [Event Hooks — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/hooks)
- [Build a Hermes Plugin — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/developer-guide/plugins)
- [GitHub Issue #2817 — hooks documented but never invoked](https://github.com/NousResearch/hermes-agent/issues/2817)
- [PR #3542 — activate plugin lifecycle hooks](https://github.com/NousResearch/hermes-agent/pull/3542)
- [Plugins feature doc — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/plugins)

## Status

Researched and recommended, not built. Next: verify hooks actually fire on the Pi's
installed version before writing the `on_session_start` plugin.
