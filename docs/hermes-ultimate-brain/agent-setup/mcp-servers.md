# MCP Servers — Making Ultimate Brain the Obvious Choice

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
[../pi-setup-audit.md](../pi-setup-audit.md). The audit already confirmed the mechanics
work: `ultimate-brain` is registered, running, and its tools surface under their real
names (no forced `mcp__` prefix — Hermes's own test suite confirms this,
`test_preserves_native_mcp_server_tool_name`). This doc is narrower: is there a
config-level way to make UB the *default*/preferred choice over any other MCP server or
built-in tool when there's ambiguity, and is the delegation setting correct.

## No built-in "priority" mechanism exists

Checked Hermes's MCP config reference and source directly. The available levers per
server are: `enabled`/`disabled`, connection details (`command`/`url`/`env`), and an
`include` list to filter which of a server's tools get exposed at all. There is no
priority, weighting, or "prefer this server on ambiguity" setting anywhere in the MCP
config surface. Hermes also ships a curated catalog of community MCP servers that are
disabled by default — unrelated to priority, just an install-time default.

**Practical consequence: ambiguity between servers/tools isn't resolved by config — it's
resolved by instructions.** This lands back on the same conclusion as the feature-surface
doc's closing section: making UB the obvious choice is a SOUL.md/AGENTS.md job (tell the
model plainly that Ultimate Brain is the system of record and named alternatives are not
to be used), not something a config flag can do for you.

## The one server that matters here: `vault-mcp`

Per the audit, `vault-mcp` exists in `mcp_servers` but is `enabled: false`. Since it's
already off, it isn't currently a source of ambiguity — but if it's ever turned on for
some other purpose, it's worth checking then whether it overlaps with anything UB does
(unknown from config alone; would need to inspect what `vault-mcp` actually provides
before re-enabling it). Not an action item today, just a trap to avoid later: don't
enable a second server that quietly competes with UB the same way `todo` did as a native
toolset.

## `delegation.inherit_mcp_toolsets: true` is correct, and here's why

Confirmed via Hermes docs: all servers under the top-level `mcp_servers` key are loaded
globally and available to both the main agent and any child agents spawned via
`delegate_task`. `inherit_mcp_toolsets: true` (already the setting on the Pi, per the
audit) means subagents get the same MCP toolset the parent has — i.e., any subagent
Hermes spawns (for a deep weekly review, a research task, anything) still has full UB
access rather than losing it the moment work gets delegated. Flipping this to `false`
would silently cut UB out of any delegated work, which is the opposite of "lives and
breathes Ultimate Brain everywhere." Leave it as-is.

## What would actually move the needle

Since there's no config-level priority knob, the two things that make UB feel like the
default rather than one option among several are:
1. **Toolset cleanup** — see [toolsets.md](toolsets.md): fewer competing tools means less
   ambiguity to resolve in the first place, which matters more than any priority setting
   would.
2. **Identity content** — SOUL.md/AGENTS.md explicitly naming Ultimate Brain as the
   system of record, per the feature-surface doc's conclusion. This is the actual lever;
   everything checked in this doc confirms there isn't a config shortcut around it.

## Status

Config confirmed correct as-is (`ultimate-brain: enabled: true`, `inherit_mcp_toolsets:
true`). No further MCP-server-level config change identified — the remaining work is
identity content (SOUL.md/AGENTS.md) and toolset cleanup (todo), not anything in this
file's scope.

Sources:
- [MCP Config Reference — Hermes Agent](https://hermes-agent.nousresearch.com/docs/reference/mcp-config-reference/)
- [MCP (Model Context Protocol) — Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-guide/features/mcp)
- [Use MCP with Hermes — guide](https://github.com/NousResearch/hermes-agent/blob/main/website/docs/guides/use-mcp-with-hermes.md)
- [Issue #15528 — delegation.mcp_servers feature request (child-only MCP loading, not yet available)](https://github.com/NousResearch/hermes-agent/issues/15528)
