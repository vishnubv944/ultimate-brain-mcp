# Cron — Reconciling the Existing Jobs Against the Designed Mechanisms

Context: [../pi-setup-audit.md](../pi-setup-audit.md), [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
`docs/hermes/daily-plan-phase.md`, `eod-review.md`, `planning-mode.md`, `mcp-roadmap.md`.
Four cron jobs already exist on the Pi (`~/.hermes/cron/jobs.json`). This is a
job-by-job diff against the more precise design, not a generic description of the cron
feature (see feature-surface.md for that).

## How Hermes cron actually behaves (from official docs, not yet reflected in the audit)

- **Fresh session every run, no memory of any current chat.** Job prompts must be fully
  self-contained — nothing from a prior conversation carries in. SOUL.md/AGENTS.md
  identity and instructions still load normally (they're session-level context, not chat
  memory), so whatever gets written there applies uniformly to every cron-triggered
  session too — this is the same lever across all four jobs, not something to repeat
  per-job.
- **The scheduler pre-validates the run** before invoking the agent: provider API key
  resolves, attached skills are ready, delivery platform has credentials. This is exactly
  why all three broken jobs show `"last_status": "error"` — the ollama-cloud 403 fails
  this pre-check every time.
- **`cronjob`, `messaging`, and `clarify` toolsets are disabled inside a scheduled run** —
  prevents a job from recursively scheduling more jobs, sending arbitrary messages, or
  opening interactive prompts. **Open question, not yet resolved**: two of the four
  existing jobs (`nightly-close-my-day`, `morning-plan-my-day`) explicitly say "wait for
  his reply" and continue as a multi-turn conversation over Telegram — that only makes
  sense if the scheduler's `deliver: telegram` mechanism hands off to an ordinary gateway
  conversation thread once the user replies (which would run with full toolsets, being a
  normal platform session rather than the cron session itself). Worth confirming this
  handoff behavior directly rather than assuming the existing jobs are relying on
  something fragile.

## `todoist-inbox-sync` — recommend retiring, not reconciling

Every 30 minutes, pulls Todoist Inbox tasks into UB, closes them in Todoist. This isn't a
gap against any designed mechanism — it's the competing legacy capture path already
flagged in pi-setup-audit.md. [inbox-processing.md](../../hermes/inbox-processing.md)
assumes UB's own inbox is the single capture destination; Todoist sitting in front of it
as a relay doesn't fit that model. Decision, not a redesign: keep it only if there's a
real reason quick capture still needs to go through Todoist first (e.g. a phone widget
habit not yet replaced); otherwise retire once native UB capture is confirmed to work
end to end.

## `morning-plan-my-day` (4 AM) vs. `daily-plan-phase.md`

**What now exists in the model (post-revision of `daily-plan-phase.md`)**: planning
runs *once*, at 9 PM in `nightly-close-my-day`, against tomorrow — not fresh each
morning. The morning job's intended role has narrowed to "overnight check: did
anything change since 9 PM that needs adjusting before the first block starts?" —
a tiny correction pass, not a from-scratch plan.

**What already matches in the morning job**: doesn't pack every minute, prefers
focus-worthy tasks, asks before scheduling, never blocks the past.

**Gaps that remain**:
- **The morning prompt still assumes a from-scratch planning conversation** rather than
  the narrow overnight-correction role the new model gives it. It needs rewriting to
  roughly: pull `daily_review_snapshot`, ask "anything change since 9 PM?", and if
  the answer is no (the common case) say so and exit. The full per-block discussion
  is gone.
- **The old job still says "propose a plan"** with no awareness that the plan was
  already committed last night. Until it's rewritten, the morning job and the
  `daily-plan-phase.md` revision are talking past each other.
- **No use of Work Session history for duration estimates** — same as before.
  Estimates stay rough.

## `nightly-close-my-day` (9 PM) vs. `eod-review.md` + `inbox-processing.md` + `daily-plan-phase.md`

## `nightly-close-my-day` (9 PM) vs. `eod-review.md` + `inbox-processing.md`

**What already matches, and exceeds the design**: does per-item inbox + unfinished-My-Day
triage over Telegram (close to `eod-review.md`'s Diff/ForEach step), and adds a
**3-question nightly journaling ritual** we hadn't designed at all — a real reflection
layer worth keeping and referencing back into `docs/hermes/MODES.md`'s Review section.

**Gaps**:
- **No Hermes Log surfacing.** `mcp-roadmap.md` decided off-checkpoint replan attempts
  get logged as a `Hermes Log` Note; nothing in this job's prompt reads or mentions that
  log. Right now, even if the mechanism existed, nothing would ever resurface it.
- **Inbox processing isn't run through the specific GTD decision tree.** The prompt says
  "ask what to do with each item, for example route it to a project, set a due date,
  snooze it, mark it done, or archive it" — close in spirit to
  `inbox-processing.md`'s flowchart but not the same explicit sequence (actionable? →
  next action clear? → under 2 min? → delegate? → hard deadline?). Worth aligning the
  wording so the same decision logic runs whether it's this cron job or a live session.
- **Carry-forward doesn't distinguish IN/OUT from WHEN.** With `daily-plan-phase.md`'s
  new per-block sub-task model, this gap shrinks: carrying an unfinished sub-task forward
  is now just `update_task(due=<tomorrow's same block due>, parent_task_id=<tomorrow's
  same block id>)`. The job prompt should describe this exactly. Today's prompt is
  ambiguous about whether reschedule means "lift the due date to tomorrow" or "actually
  move into a specific block" — these are now different operations.
- **The 9 PM job now owns the daily plan pass** as its final step
  (`daily-plan-phase.md`'s revised design), targeting tomorrow. This is the new load on
  the nightly conversation — narrow it down to the per-block discussion protocol in
  the daily-plan doc, not a free-form "build me a day" prompt.
- **A real discrepancy worth resolving, not silently picking a side on**: `eod-review.md`
  says "Clear My Day" is UI-only and Hermes must remind the user to press it themselves —
  based on the older UB documentation, which predates the MCP's own `clear_my_day` tool.
  This cron job's Step 5 calls `clear_my_day` directly, bypassing that entirely. Either
  the UB docs' "UI-only" framing is stale now that the MCP tool exists, or there's a
  deliberate reason (the physical-button-as-closure-ritual argument from earlier design
  discussion) to keep it human-pressed regardless of what's technically possible. This
  needs an actual decision, not an assumption either way.

## `weekly-plan-my-week` (Sunday 6 PM) vs. `planning-mode.md`

**What already matches**: pulls `weekly_review_snapshot`, summarizes the week
(done/overdue/project & goal progress, calls out stalled items), turns the reply into
concrete projects/tasks, stays a conversation until the plan is captured.

**Gaps**:
- **No Area-backlog entry point.** `planning-mode.md` treats picking an item out of an
  Area's Ongoing-project backlog as one of Planning's three legitimate entry points —
  this job only offers "new priorities, projects to push forward, goals to revisit," with
  no mention of surfacing backlog items as candidates.
- **Classify branches are incomplete.** The prompt only mentions `create_project`,
  `create_task`, `update_task` — missing `create_goal` and `create_note` (Idea), and the
  "stays as-is, no promotion" outcome. Without SOUL.md/AGENTS.md carrying the full
  classify logic, the job currently can't produce a Goal or an Idea note even when that's
  the right outcome.
- **No explicit lock statement.** `planning-mode.md` requires stating the checkpoint
  cadence out loud once something is committed ("this is locked until Sunday"). Nothing
  in the prompt does this — the week's plan gets written, but nothing marks it as closed
  against off-checkpoint replanning.

## Status

Job-by-job diff complete. None of the four jobs need to be rebuilt from zero — they need
targeted prompt edits once the underlying mechanisms they're missing (Hermes Log,
recurring-task fields, the classify branches) actually exist. The cron-vs-chat toolset
handoff behavior for mid-conversation replies is unconfirmed and worth verifying before
relying on it further.

## Sources

- [Scheduled Tasks (Cron) — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/cron)
- [Automate Anything with Cron — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/guides/automate-with-cron)
- [Cron Internals — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/developer-guide/cron-internals)
- [Cron Troubleshooting — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/guides/cron-troubleshooting)
