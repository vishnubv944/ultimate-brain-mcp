# Hermes Pi Setup Audit

Explored via SSH (`ssh hermes`, Tailscale host `100.123.210.71`) on 2026-09-18. Purpose:
figure out what needs to change so the Ultimate Brain MCP tools feel like a native part
of Hermes rather than a bolted-on external system, and what shape the "soul"/identity
work should take. Context: [../hermes/PURPOSE.md](../hermes/PURPOSE.md) and the rest of
`docs/hermes/`.

## What's already wired and working (technically)

- **The MCP is already registered and running.** `ultimate-brain` is a live entry under
  `mcp_servers` in `~/.hermes/config.yaml` (`command: /home/vishnubv944/.local/bin/ub-mcp.sh`,
  `enabled: true`), and the process list on the Pi confirms it's actually running as a
  subprocess of the `hermes-gateway.service`.
- **Tools already surface unprefixed.** Hermes's own test suite includes
  `test_preserves_native_mcp_server_tool_name` and
  `test_mcp_tools_resolve_through_server_aliases` — MCP tools show up to the model by
  their real name (`search_tasks`, not `mcp__ultimate-brain__search_tasks`). The "feel
  native" problem is very likely **not** a technical/naming problem — mechanically, the
  tools already look native to the model.

## The bigger discovery: three of the four daily/weekly rituals already exist as cron jobs

Found in `~/.hermes/cron/jobs.json` (Hermes's own cron system, delivering over Telegram):

- **`morning-plan-my-day`** — cron `0 4 * * *`. Pulls `daily_review_snapshot`, messages
  Telegram, waits for a reply, sets `my_day` / time-blocks via `bulk_update_tasks`. This
  is [../hermes/daily-plan-phase.md](../hermes/daily-plan-phase.md), already built — and
  its prompt already encodes sensible guardrails: never block past the current time,
  30-minute minimum blocks, 20-30% buffer between blocks, don't pack every open minute,
  propose the schedule and confirm before writing.
- **`nightly-close-my-day`** — cron `0 21 * * *`. Inbox + unfinished-My-Day triage over
  Telegram, then a **3-question nightly journaling ritual** (what went well / what are
  you grateful for / what would make tomorrow better) written into a UB Journal note, plus
  a 4th open-ended question only stored if something is actually added. This is
  [../hermes/eod-review.md](../hermes/eod-review.md), already built, plus a reflection
  layer beyond what we'd designed.
- **`weekly-plan-my-week`** — cron `0 18 * * 0` (Sunday 6pm). `weekly_review_snapshot`,
  summarizes the week (done / overdue / project & goal progress, calls out anything
  stalled), turns the reply into projects/tasks. Close to the weekly checkpoint in
  [../hermes/planning-mode.md](../hermes/planning-mode.md).

None of these three need to be invented. They need to be **reconciled against the more
precise mechanisms already designed** in `docs/hermes/` — the Hermes Log for
off-checkpoint replan attempts, the routines-as-recurring-tasks time-window logic, the
"surface, don't push" convergence rule from planning-mode.md, People check-ins — not
built from scratch.

## What's actually broken right now, unrelated to any design work

**Every cron job is currently failing.** The sole configured model provider,
`ollama-cloud` (`deepseek-v4-flash`), is returning `403: your subscription payment is
past due`. This is why Hermes has felt absent regardless of anything discussed in this
conversation — it's an operational/billing issue, not a design gap.

There's an unused `fallback_model` block already scaffolded (commented out) in
`config.yaml`. `.env` already has working `OPENROUTER_API_KEY`, `GOOGLE_API_KEY`, and
`GROQ_API_KEY` that aren't being used as a fallback. Wiring one of these in is a fast,
separate fix from everything else in this audit.

## What's competing with Ultimate Brain instead of native to it

- **`todoist-inbox-sync`** cron job (every 30 minutes) — syncs a *second* task inbox
  (Todoist) into UB via `search_tasks` de-dup + `create_task`. A legacy capture path
  running in parallel with UB's own quick capture. Worth deciding whether to retire it now
  that UB is meant to be the single system of record.
- **The built-in `todo` toolset** is enabled for `cli`/`telegram`/`discord` in
  `platform_toolsets` — Hermes has its own native task list separate from Ultimate
  Brain's Tasks database. With both available, the model can arbitrarily reach for
  either, undermining "one source of truth." Likely belongs in `disabled_toolsets`.
- **`kanban.db`** exists on disk in `~/.hermes/` — not yet confirmed whether it's actively
  used or a leftover from an earlier setup. Needs checking before deciding what to do
  with it.
- The **`memory`** toolset (Hermes's own built-in conversational memory, distinct from
  the `memories/` directory) is enabled and likely fine to coexist — its job
  (conversational continuity, remembered preferences) is different from UB's structured
  PARA system — but the boundary between "what Hermes remembers natively" and "what goes
  in UB" hasn't been made explicit anywhere and probably should be.

## What's genuinely missing: the soul

`display.personality` is empty (`''`), and the only `personalities:` defined in
`config.yaml` are generic template presets — `helpful`, `concise`, `technical`,
`creative`, `teacher`, `kawaii`, `catgirl`, `pirate`, `shakespeare`, `surfer`, `noir`,
`uwu`, `philosopher`. Boilerplate from whatever base product Hermes ships as, never
replaced. `~/.hermes/profiles/` is empty. There is no custom identity anywhere reflecting
anything from [../hermes/PURPOSE.md](../hermes/PURPOSE.md),
[../hermes/MODES.md](../hermes/MODES.md), or the behavior contract built out across
`docs/hermes/`. This is the one piece that's a genuine blank slate, not a reconciliation
job.

## Open items to resolve next

1. Fix the provider outage (fallback_model → openrouter/google/groq) — fast, unblocks
   everything else from actually running.
2. Decide the fate of `todoist-inbox-sync` and the built-in `todo` toolset.
3. Check what `kanban.db` actually holds before deciding to keep or retire it.
4. Reconcile the three existing cron-job prompts against `docs/hermes/`'s more precise
   mechanisms (Hermes Log, routines, convergence rule, People check-ins) rather than
   rewriting them from zero.
5. Write the actual soul/identity content — currently nothing exists to reconcile against.

## Status

Point-in-time audit, 2026-09-18. Config file locations and line numbers may drift —
re-verify against the live `~/.hermes/config.yaml` before acting on specifics here.
