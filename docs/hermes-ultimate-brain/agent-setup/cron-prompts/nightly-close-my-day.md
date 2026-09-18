---
job_id: nightly-close-my-day
schedule: 0 21 * * *
description: >
  Single daily anchor — process today's inbox, review what actually happened,
  and finalize tomorrow's schedule block by block. Per the revised
  docs/hermes/daily-plan-phase.md, the planning pass operates on routine blocks
  (recurring Tasks with due+due_end) and fills each with sub-tasks via the
  existing Parent Task self-relation.
loads_context:
  - ~/.claude/AGENTS.md
  - ~/.claude/SOUL.md
  - docs/hermes/eod-review.md
  - docs/hermes/inbox-processing.md
  - docs/hermes/daily-plan-phase.md
---

# Nightly close — 9 PM

It's 9 PM. Tonight's job is the **single daily anchor**: process inbox, review
the day, schedule tomorrow. Three sequential sub-phases — Process → Reflect →
Schedule. Don't blend them.

## How I want to interact

Telegram. We have a real conversation. I may be brief; that's fine. Don't pretend
to know things you don't have data for — every state claim must be backed by an
MCP read. Don't fabricate task names, projects, or routine times.

You should NOT use the `cronjob`, `messaging`, or `clarify` toolsets inside a
scheduled run. If you need to follow up after I've gone quiet, end the session —
don't try to schedule another job or DM me unprompted.

## MCP setup

The MCP is registered as `mcp__ultimate-brain-dev__*`. Always prefer those tools
over the generic HTTP layer. There are 48 of them; the ones you'll actually need
tonight:

- `daily_review_snapshot` — one call, gets you today's shape (now, timezone,
  completed_today, overdue_or_due_today, due_tomorrow, on_my_day, inbox,
  **routine_blocks**, lookups incl. per-routine planned_subtasks, task_schema).
- `search_people`, `get_person_detail`, `create_person`, `update_person`,
  `log_checkin` — only if a person comes up in inbox or review.
- `complete_task` — for the 2-min-done cases in inbox processing.
- `update_task` — for snoozes, re-priorities, project assignments, GTD
  routing via Smart List (which falls through to `update_page` for that
  specific property).
- `bulk_update_tasks` — for the final commit on multi-task changes.
- `bulk_create_tasks` — for the per-block sub-task commit at the end of
  scheduling.
- `search_tasks`, `search_work_sessions` — as needed for pull and estimation.
- `archive_item`, `update_page` — only when `update_task` is too narrow.

## Sub-phase 1 — Process inbox (~10 min)

For each item in `daily_review_snapshot.buckets.inbox`, run the GTD decision
tree from `docs/hermes/inbox-processing.md`. Decision tree:

```
Actionable? ── no ──> Reference (tag) / Archive / Someday (update_page Smart List) /
                     Snooze (update_task snooze_until=…) /
                     Defer to specific date (snooze)
        │
        └── yes ──> Next action clear? ── no ──> Make it a Project
                                                  (create_project + bulk_create_tasks)
                       │
                       └── yes ──> Under 2 min? ── yes ──> Do it now (complete_task)
                                              │
                                              └── no ──> Delegate? ── yes ──> log_checkin /
                                                                           search_people
                                                                          + update_page
                                                                          Smart List
                                              │
                                              └── no ──> Hard deadline? ── yes ──> update_task
                                                                            due=<date>
                                              │
                                              └── no ──> Smart List = Do Next
                                                        (update_page Smart List)
```

Do this per item. Don't triage the whole inbox as a batch — the user expects to
answer one question per item. If the inbox is large (>10), surface a count first
("12 items in inbox, work through them?") and start at the top.

## Sub-phase 2 — Review the day (~5 min)

```
1. daily_review_snapshot.buckets.completed_today
   ↳ daily_review_snapshot.buckets.due_tomorrow
   ↳ daily_review_snapshot.buckets.on_my_day (these are unfinished)

2. For each unfinished task in on_my_day NOT in due_tomorrow:
   I pick one of:
     - carry forward (update_task due=<tomorrow's block due>,
                      parent_task_id=<tomorrow's same block id>)
     - Area backlog (update_task — clear due, clear parent_task_id)
     - drop (archive_item or complete_task if trivial)

3. Surface Hermes Log (search_notes note_type="Hermes Log" date_after=<today>)
   — say plainly "nothing logged" / "1 replan logged at HH:MM" — no deep dive.
```

## Sub-phase 3 — Schedule tomorrow (~15 min)

Per `docs/hermes/daily-plan-phase.md` (revised): per-block discussion. Don't
draft the whole day in one shot.

```
1. Pull routine_blocks from the snapshot.
   Sort by due (ascending). These ARE tomorrow's skeleton.

2. For each routine block, in time order:
   a. State the block: "<Name> block: <start>–<end>"
   b. State the already-planned sub-tasks:
      lookups.routine_blocks[<parent_id>].planned_subtasks
   c. Ask: "Keep, add, remove, re-time, or move to a different block?"
   d. Estimate fit (sum of Work Session durations for those sub-tasks
      vs. block length). If overflow, say so for THIS block specifically.
   e. Resolve into bulk_create_tasks / bulk_update_tasks for THIS block
      before moving on.

3. After all routine blocks are decided, ask:
   "Anything one-off tomorrow — meetings, calls, blocks I missed?"

4. If yes: create_task with due/due_end (no parent — top-level block).
   If no: open time stays open. Don't fill default-open blocks.
```

### Fit estimation

Rough default: 20 min per sub-task when there's no Work Session history. Pull
real durations via `search_work_sessions(task_id=<sub_task_id>)` — average the
last 5 completed sessions for that sub-task. If you've never done it before,
the 20-min default is fine — note it as an estimate.

### Overflow handling (per block, not per day)

When a block's planned sub-tasks sum to more than `due_end - due`:

- Reorder within block (cheaper items first when energy is fresh).
- Spill one item to the next occurrence of the same routine (just tomorrow's
  block in the common case).
- Area backlog: `update_task(clear due, clear parent_task_id)`.
- Drop: `archive_item`.

Tell me what's overflowing and ask which landing spot. Don't decide for me.

## Sub-phase 4 — Commit and close (no user input)

After the planning conversation:

```
1. clear_my_day — that's the explicit "today is done" lever now that the MCP
   has the tool.
2. One-line confirmation: "Tomorrow's planned. <N> routine blocks, <M>
   sub-tasks total, <K> open slots. See you in the morning."
```

## What NOT to do

- Don't write to Notion until you've asked which option for each decision.
- Don't fabricate routine blocks that don't exist in the snapshot. If a routine
  is missing for a time I expect to be blocked (e.g. "I have a 2 PM call
  tomorrow"), I tell you about it — you don't invent a block.
- Don't auto-promote Someday / Snoozed items back into Do Next. Those are
  holding pens, not backlogs.
- Don't fix any routine block's length by re-stamping the parent — the parent
  recurs automatically. Suggest editing the parent's `due_end` separately if
  we want a permanent length change.
- Don't use Smart List via any tool except `update_page`. `update_task` doesn't
  expose it; using `update_page({"Smart List": …})` is the explicit fallback
  `inbox-processing.md` calls out.

## Failure modes to watch for

- **Work Session history empty for a sub-task.** Default to 20 min, note it.
  Don't pretend to know.
- **Routine block has 5+ planned sub-tasks.** That probably means yesterday's
  carry-over piled up. Flag it; don't silently archive old ones.
- **User asks "what if I just did X tomorrow?"** — that's a hypothetical; you
  can answer based on routine_blocks but don't write anything until they
  confirm.
- **Cron run fires while I'm mid-conversation elsewhere.** State you noticed
  and ask whether to proceed or stand down.
