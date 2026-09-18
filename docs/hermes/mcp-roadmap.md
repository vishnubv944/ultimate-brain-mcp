# MCP Roadmap

Context: [PURPOSE.md](PURPOSE.md), [MODES.md](MODES.md), [daily-plan-phase.md](daily-plan-phase.md),
[inbox-processing.md](inbox-processing.md), [eod-review.md](eod-review.md),
[planning-mode.md](planning-mode.md). What needs to change in `ultimate-brain-mcp` itself
so Hermes can actually run everything designed in this folder, plus what's deliberately
out of scope and why.

## Guiding principle

Add tools for concrete, near-term need — not a blanket wrap of every Notion API surface
"in case it's useful later." That's the same failure pattern this whole project exists to
fix (over-preparing instead of executing), just relocated from Notion planning into code.
A bloated tool list also has a real cost: more surface area for the agent to pick the
wrong tool from, more to test and keep correct as the Notion API changes underneath it,
and unused code is where bugs hide longest. Add a tool when there's a real operation to
perform; a cheap addition later beats an unused one carried indefinitely.

## Tier 1 — People, first-class

Chosen priority for expanding beyond Tasks/Projects/Goals. The People database already
has `Check-In` and `Last Check-In` properties — the same accountability shape as the
study-goal mechanism, applied to relationships instead of tasks.

- `search_people` — filter by `relationship`, `pipeline_status`, `check_in_before`/`after`
  (due for a check-in), `last_check_in_before` (relationships going stale), query
- `get_person_detail` — properties + related Notes/Projects/Tasks (same pattern as
  `get_project_detail`)
- `create_person` — full name, relationship(s), email/phone/company/title, birthday,
  check-in date, location, socials, tag_ids, content (gift ideas/quick notes)
- `update_person` — patch any field
- `log_checkin` — convenience tool mirroring `complete_task`: sets `Last Check-In = today`
  in one call

## Tier 2 — Daily-loop plumbing

Blocks flows already designed in this folder, not hypothetical future need.

- Expose `recur_interval` / `recur_unit` / `days` on `create_task`/`update_task` — missing
  today; blocks the routines-as-recurring-tasks mechanism in
  [daily-plan-phase.md](daily-plan-phase.md) directly
- Expose `smart_list` / `snooze` on `update_task` — missing today; blocks clean GTD
  routing in [inbox-processing.md](inbox-processing.md)
- Verify and, if needed, fix `complete_task` preserving the `due_end` offset when
  advancing a recurring task's date — routines need their time-block length to persist
  every cycle, not just the date
- `bulk_create_tasks` — mirrors `bulk_update_tasks`; useful when Planning breaks a project
  into several tasks at once

## Database CRUD — a deliberately narrow generic surface

Not a blanket API wrap. Just enough to create and evolve a new database on demand
(Finance, Sleep, whatever comes up later) without new dedicated code each time.

- `create_database(parent_page_id, title, properties)` — creates a new database (+ its
  initial data source) under a specified page. Returns the new IDs.
- `get_database_schema(database_id)` — reads the full property schema (names, types,
  select/status options)
- `update_database_schema(database_id, properties)` — add a property, add a select/status
  option (this is how the `Hermes Log` note type gets added), or remove a property.
  Destructive/non-idempotent, same treatment as `set_page_content` — this can restructure
  a real database
- `query_data_source(data_source_id, filter, sorts, limit)` — generic query by ID, so a
  brand-new database works immediately without an env var + redeploy. `query_database`
  (existing) stays for pre-configured, named databases.

**Deliberately excluded: `delete_database`.** Matches the existing pattern in this
codebase — `archive_item` is reversible by design, no hard deletes exist anywhere else in
the MCP, and a database-level delete has a much bigger blast radius than a page-level one.
Retiring a database stays a manual, deliberate Notion-side action.

Row-level CRUD on whatever lives *inside* a new database needs no new tools — the generic
`create_page`/`get_page`/`update_page`/`get_page_content` tools already work by page ID.

## Decided, not revisited

- **Off-checkpoint replan / deviation logging** → a new `Hermes Log` Note type. Config-only
  change in the live Notion workspace (the `note_type` validation is already
  live-discovered), zero MCP code.

## Deferred — real capability, no current pull on it

- **Webhooks** (Notion API version 2026-03-01) — would let Hermes react to page
  create/update/delete events instead of only noticing things when invoked in a session.
  This is the single biggest gap relative to the Mode × Trigger map's "Event-triggered"
  column being genuinely reactive rather than poll-on-request. Explicitly deferred until
  the core Plan/Execute/Review loop is proven out with the simpler, session-based version.
- **Books/Reading Log and Recipes/Meal Planner as first-class** — real domains identified
  earlier, not chosen this round.
- **New domains beyond what's already planned** (Finance, Sleep tracking, etc.) —
  technically unblocked by Database CRUD above, but no concrete need yet.
- **Comments API, file uploads, Views API, Workers/Database Sync, External Agents** —
  all real Notion API capabilities (confirmed via research), none of them have a concrete
  use case pulling on them from anything designed in this folder so far.

## Status

Consolidated plan, not yet built. Implementation not started.
