# Hermes cron-job prompts

This folder holds the **prompt bodies** that get embedded into each entry in
`~/.hermos/cron/jobs.json`. Keeping them here (in the repo) makes them reviewable
and version-controlled alongside the docs they implement. The job JSON itself stays
on the Pi — only the prompt text is versioned here.

Each file is a single prompt, ready to paste as the `prompt` field of a cron job,
including any pre-loaded `included_context_files` the cron run needs.

## Current jobs

| Cron | Cadence | Prompt |
|---|---|---|
| `nightly-close-my-day` | 21:00 daily | [`nightly-close-my-day.md`](nightly-close-my-day.md) |
| `morning-plan-my-day` | 04:00 daily | [`morning-check.md`](morning-check.md) *(revision pending — pre-rewrite version still in `jobs.json`)* |
| `weekly-plan-my-week` | Sunday 18:00 | *(not yet authored here)* |
| `todoist-inbox-sync` | every 30 min | *(deprecated — see `cron.md`)* |

The prompts here are written against the post-Tier-1/Tier-2/Tier-3 MCP revision of
the planning model — they assume `daily-plan-phase.md`'s per-block sub-task model
and the new `routine_blocks` bucket in `daily_review_snapshot`.
