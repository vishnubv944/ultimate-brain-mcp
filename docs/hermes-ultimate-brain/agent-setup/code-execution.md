# Code Execution — Relevance to Ultimate Brain Fusion

## What it is

`execute_code` (the "Code Execution Sandbox" / Programmatic Tool Calling) lets the LLM
write a Python script that calls Hermes tools programmatically, running in a child
process on the agent host and communicating over a Unix domain socket RPC (loopback TCP
on Windows, where `AF_UNIX` is unreliable). The benefit is real: intermediate tool
results never enter the model's context window — only the script's final `print()`
output comes back — which both cuts token usage and collapses a multi-step chain into a
single inference turn instead of many round-trips. Security is scoped: the script
reaches tools only through the RPC channel, and environment variables with `KEY`,
`TOKEN`, `SECRET`, `PASSWORD`, `CREDENTIAL`, `PASSWD`, or `AUTH` in their names are
excluded from what it can read directly.

## Unresolved: does this reach MCP-registered tools, or only a fixed built-in set?

The research here is genuinely conflicting, and this matters for whether the feature is
usable at all for UB:

- The official **Code Execution** feature page lists a specific, fixed set of tools
  available inside the sandbox: `web_search`, `web_extract`, `read_file`, `write_file`,
  `search_files`, `patch`, `terminal` (foreground only). Notably, this list does **not**
  include MCP-server tools like `search_tasks` or `bulk_update_tasks`.
- A separate DeepWiki page titled "Code Execution and MCP Tools" exists specifically for
  this intersection, and other summaries describe the sandbox as reaching "registered
  Hermes tools" more broadly — which would imply MCP tools *are* reachable.

These two signals don't agree, and neither was confirmed against the actual version
running on the Pi. **Don't assume either way — verify directly** (e.g. ask Hermes to run
`execute_code` calling `search_tasks` and see whether it resolves or errors) before
building anything that depends on it.

## Would it be worth pursuing, if it does work?

Genuinely, only as an optimization — and per `docs/hermes/mcp-roadmap.md`'s own stated
principle ("add a tool when there's a real operation to perform... unused code is where
bugs hide longest"), this is premature right now. Nothing about the Plan/Execute/Review
mechanism has been proven out yet with the simple, direct tool-call version (Hermes
calling `daily_review_snapshot`, then `bulk_update_tasks`, as two separate turns). The
"collapse pull-state → compute-gaps → draft-schedule into one script" idea from
`daily-plan-phase.md` is a plausible future use — fewer round-trips for the morning Plan
session — but optimizing round-trip count before the mechanism itself is validated is
exactly the "build infrastructure instead of running the system" pattern this whole
project is designed to avoid, just at the code layer instead of the Notion layer.

## Recommendation

Do not pursue now. Worth a five-minute empirical check of whether MCP tools are
reachable from `execute_code` (cheap to know), but no design or implementation work
justified until the core daily loop has actually run for a while and round-trip
overhead is a real, felt cost rather than a theoretical one.

## Sources

- [Code Execution (Programmatic Tool Calling) — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/code-execution)
- [Code Execution and MCP Tools — DeepWiki](https://deepwiki.com/NousResearch/hermes-agent/5.6-code-execution-and-mcp-tools)
- [Tools & Toolsets — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/tools)

## Status

Assessed, deliberately not pursued yet. MCP-tool reachability from the sandbox is an
open empirical question, not a design decision.
