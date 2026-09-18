---
job_id: morning-check
schedule: 0 4 * * *
description: >
  Narrowed post-Tier-2 morning anchor — the daily plan was already committed at
  9 PM. This job asks "anything changed overnight that needs a small course
  correction before the first block?" and exits. The full per-block discussion
  protocol belongs to nightly, not here.
loads_context:
  - ~/.claude/AGENTS.md
  - ~/.claude/SOUL.md
  - docs/hermes/daily-plan-phase.md
---

# Morning check — 4 AM

The day's plan was already committed last night at 9 PM. My job this morning is
much narrower: confirm nothing material changed between 9 PM and now, and make
small corrections if it did. If everything is still as planned, say so and exit.

## MCP setup

Same as `nightly-close-my-day`: `mcp__ultimate-brain-dev__*`. Pull state with
`daily_review_snapshot` only — no separate reads for what one call already gives
you.

## What "material change" means

- A routine block has been **moved or deleted** by me or a calendar sync.
- An already-planned sub-task became **impossible** (the project was cut, the
  dependency won't land in time).
- An **emergency** came in that should bump existing sub-tasks out of the
  earliest block.

If none of these, exit after a one-line check-in.

## What this job is NOT

- It is NOT a from-scratch planning conversation. That work belongs to
  `nightly-close-my-day`. If anything asks to build a new plan from scratch,
  redirect: "the plan was set last night — want me to pull up tomorrow's
  blocks or is something specific off?"
- It is NOT an inbox-processing pass. Anything new in the inbox waits for
  tonight.
- It is NOT a journaling ritual. That's still tonight.

## Allowed actions

- `daily_review_snapshot` — one call. State the date, list tomorrow's
  `routine_blocks` with already-planned sub-tasks.
- `update_task` / `bulk_update_tasks` — for the narrow cases above (move a
  sub-task out of a block, drop something).
- No writes to Notion's calendar view directly.

## What NOT to do

- Don't propose redoing the night's plan unless I ask.
- Don't surface every routine block one-by-one — that's a per-block
  confirmation loop, and tonight already did it. Just confirm the structure
  is intact.
- Don't write a Hermes Log entry unless I explicitly asked for one.

## Output shape

```
Plan intact for tomorrow, <N> routine blocks, <M> sub-tasks committed,
<K> open slots. <single-line summary of anything notable, or "nothing
changed.">
```

If something needs a correction, propose the smallest possible change and ask
before writing.
