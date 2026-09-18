# Checkpoints — Relevance to Ultimate Brain Fusion

## What it is

Hermes's Checkpoint Manager keeps a single shared shadow git repository under
`~/.hermes/checkpoints/store/` and snapshots a project's working directory before
destructive terminal operations (`rm`, `mv`, `sed -i`, output redirects, `git
reset/clean/checkout`, etc.), so `/rollback` can restore it. The real project `.git` is
never touched. **Opt-in as of v2** — off by default, enabled per-session with
`--checkpoints`, because most users never use `/rollback` and shadow-store growth is
non-trivial over time.

## Relevance to Ultimate Brain

Low, close to none. Checkpoints protect **local filesystem state in a working
directory**. Nothing in the Ultimate Brain workflows designed in `docs/hermes/` touches
the local filesystem — every UB operation is a remote Notion API call through the MCP
(`create_task`, `bulk_update_tasks`, `search_projects`, etc.). There is no working
directory for Checkpoints to snapshot in a Plan/Execute/Review session, and no
destructive terminal command in the loop for it to guard against.

## Where it would actually matter

The one place this becomes relevant is unrelated to UB-as-a-system and specific to
this repo's own maintenance: `CLAUDE.md`'s `deploy-hermes` skill lets Hermes make local
edits to the `ultimate-brain-mcp` source itself and redeploy. If Hermes is ever
routinely editing its own MCP server's code on the Pi, checkpoints become a sensible
safety net for *that* working directory (`~/Projects/ultimate-brain-mcp`) — but that's a
code-maintenance safeguard, not part of making UB feel native to Hermes's identity or
daily operation.

## Recommendation

Not part of the UB-fusion work. Revisit only if/when Hermes is given routine
responsibility for editing this repo's own source — at that point, enable
`--checkpoints` scoped to that specific working directory, nothing more.

## Sources

- [Checkpoints and /rollback — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/checkpoints-and-rollback)
- [PR #824 — filesystem checkpoints and /rollback command](https://github.com/NousResearch/hermes-agent/pull/824)

## Status

Assessed, not pursued. No action needed for Ultimate Brain fusion.
