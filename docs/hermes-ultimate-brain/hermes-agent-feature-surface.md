# Hermes Agent — Full Behavior-Affecting Feature Surface

Research findings, 2026-09-18. "Hermes" is [Hermes Agent by Nous Research](https://github.com/NousResearch/hermes-agent)
("the agent that grows with you") — an open-source, self-hosted, self-improving personal
agent. Official docs: https://hermes-agent.nousresearch.com/docs/. This is every
documented surface that shapes how it behaves, gathered so we know the full space before
deciding what to change. Cross-reference against [pi-setup-audit.md](pi-setup-audit.md)
for what's actually configured on the Pi today.

## Identity layer

**SOUL.md** — the primary identity file, `~/.hermes/SOUL.md` (or `$HERMES_HOME/SOUL.md`).
Occupies **slot #1 in the system prompt**, replacing Hermes's hardcoded default identity
entirely. Defines who the agent is, how it speaks, what it avoids. Injected verbatim
(after security scanning/truncation) if present; falls back to a built-in default
identity if empty or missing. Hermes auto-seeds a starter file if none exists. Not
duplicated elsewhere in context — a true single source for identity, not an additive
layer. **This is empty/default on the Pi today** (per pi-setup-audit.md) — the actual
soul-writing work.

**`/personality`** — a session-level overlay, distinct from SOUL.md. Changes or
supplements the current system prompt for a session without touching the durable
identity. `config.yaml`'s `personalities:` dict (the joke presets we found — kawaii,
pirate, Shakespeare, etc.) are the built-in options this command switches between.
SOUL.md is what should carry the real, durable identity; project/task instructions
belong in AGENTS.md instead, per Hermes's own docs guidance.

## Context files (auto-discovered)

Hermes automatically discovers and loads: `.hermes.md`, `AGENTS.md`, `CLAUDE.md`,
`SOUL.md`, `.cursorrules`. These shape behavior per-project/per-instance. **AGENTS.md is
where project/task instructions belong** — SOUL.md docs explicitly say to keep identity
and instructions separated across the two files, not blended into one.

**Context references** — typing `@` followed by a file, folder, git diff, or URL injects
it directly into a message. Ad hoc, not a standing config surface, but relevant to how
Hermes could be told to reference `docs/hermes/` material mid-conversation.

## Memory

- **`MEMORY.md`** — agent-curated durable facts, written by the agent itself with
  periodic nudges to persist knowledge it decides is worth keeping.
- **`USER.md`** — a deepening model of who you are, built across sessions.
- **Pluggable memory providers** — e.g. Honcho and others, swappable via the plugin
  system (see below).
- This is Hermes's own native memory system, separate from Ultimate Brain's structured
  PARA data. The boundary between "what lives in Hermes's memory" vs. "what lives in UB"
  is not yet defined anywhere — flagged as open in pi-setup-audit.md.

## Skills System

On-demand knowledge documents the agent loads when needed, using **progressive
disclosure** to minimize token usage (only pulled in when relevant, not held in context
constantly). Open standard, compatible with **agentskills.io** — portable, shareable,
community-contributed via a Skills Hub. Hermes can also **write its own skills** from
experience — when it solves a hard problem, it persists a reusable skill document so it
doesn't re-solve the same problem from scratch next time. This is the self-improving
loop the product is named around.

## Plugins

Three types, extending Hermes without touching core code:
- **General plugins** — add custom tools and hooks
- **Memory providers** — swap in alternative cross-session memory backends
- **Context engines** — alternative context-management strategies

**Hooks**: `pre_llm_call`, `post_llm_call`, `on_session_start`, `on_session_end` — fire
in both the agent loop and CLI/gateway. This is the mechanism that would let a plugin
inject something (like a UB-aware reminder) at specific points in every conversation,
rather than relying on the model to remember to check.

## Toolsets

The enabled/disabled capability groups (`web`, `search`, `terminal`, `file`, `browser`,
`vision`, `image_gen`, `skills`, `tts`, `todo`, `memory`, `session_search`, `cronjob`,
`code_execution`, `delegation`, `clarify`, `homeassistant`, `messaging`, `spotify`,
`discord`, `discord_admin`, `debugging`, `safe`, and more) — configured per-platform in
`platform_toolsets` (cli/telegram/discord/etc. can each get a different set).
**This is where the `todo` toolset competing with Ultimate Brain lives** — confirmed in
pi-setup-audit.md as currently enabled alongside UB's own task tools.

## MCP servers

External tool servers (what `ultimate-brain` already is) register under `mcp_servers` in
`config.yaml`. Tools surface to the model under their own native names (no forced
prefix) — confirmed via Hermes's own test suite
(`test_preserves_native_mcp_server_tool_name`). `inherit_mcp_toolsets` controls whether
subagents spawned via delegation automatically get access to the same MCP tools as the
parent.

## Subagent Delegation

`delegate_task` spawns child agent instances with **isolated context, restricted
toolsets**, their own conversation and terminal session. Runs 3 concurrent subagents by
default (configurable). Only the final summary returns to the parent — intermediate tool
calls never enter the parent's context window. Relevant if any Hermes ritual (e.g. a
deep weekly review) should run as an isolated child rather than inline in the main
conversation.

## Scheduled Tasks (Cron)

Natural-language or cron-expression scheduling. Jobs can attach skills, deliver results
to any connected platform (Telegram, Discord, etc.), support pause/resume/edit. **This
is the mechanism already running `morning-plan-my-day`, `nightly-close-my-day`,
`weekly-plan-my-week`, and `todoist-inbox-sync`** per pi-setup-audit.md. By default,
scheduler-launched agents can't use the `cronjob` tool themselves (to prevent runaway
self-scheduling) — opt-in only, via config.

## Autonomous Curator

`hermes curator` periodically reviews **agent-created skills** specifically — consolidates
overlapping ones, archives stale entries, writes per-run reports, protects pinned skills
from being touched. Runs on a schedule (`curator.interval_hours` in config, confirmed
present on the Pi: 168h / weekly, `enabled: true`). Governs skill hygiene, not task or
memory data.

## Checkpoints

Automatic working-directory snapshot before file changes, with `/rollback` to undo.
Relevant if Hermes is ever given file-editing capability (e.g. editing this MCP's own
source) — not directly relevant to Notion-side operations.

## Code Execution

The `execute_code` tool lets the agent write Python that calls Hermes tools
programmatically via sandboxed RPC — collapsing a multi-step tool workflow into a single
LLM turn instead of many round-trips. Could matter for something like the Plan-day
mechanism (pull state, compute gaps, draft schedule) if round-trip tool-call overhead
ever becomes a real cost.

## Profiles

Multiple **isolated Hermes instances** from one installation — each profile gets its own
config, memory, sessions, skills, and gateway service. `~/.hermes/profiles/` is empty on
the Pi today (confirmed in pi-setup-audit.md) — not currently used, but exists as an
option (e.g. a separate profile for something unrelated to personal life-management,
without cross-contaminating SOUL.md/memory).

## What this means for the native-integration question

Given SOUL.md occupies **slot #1** of the system prompt and is the *only* identity layer
(not additive, replaces the default entirely), the "make MCP tools feel native" goal
isn't really a tool-wiring problem — confirmed in pi-setup-audit.md, tools already surface
unprefixed. It's a **SOUL.md-writing problem**: the identity that currently doesn't exist
is exactly the layer that would make Ultimate Brain feel like a native extension of who
Hermes *is*, rather than a system it happens to have access to. AGENTS.md is where the
mode contract (Plan/Execute/Review, the lock mechanism, etc.) belongs as instructions;
SOUL.md is where the persona/tone decisions from earlier in this project (direct,
no-nonsense enforcer) belong as identity.

## Status

Research compiled from official Hermes Agent docs (nousresearch.com) and GitHub source,
2026-09-18. Not yet cross-checked line-by-line against the specific version running on
the Pi — version numbers and exact config keys may have drifted; verify against the live
instance before relying on specifics.
