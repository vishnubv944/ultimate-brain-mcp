# Gaps — Ultimate Brain MCP vs. what Hermes needs

Status: **for review**. Nothing here is prioritized or committed to yet —
this is the full list surfaced so far, to be triaged together before any
implementation work starts.

Two separate angles were used to find these:
- **Gap A** — comparing the server against the raw Notion API surface
  (what Notion's API can do that this server never calls).
- **Gap B** — comparing the server against how Ultimate Brain itself is
  documented to work (`hermes/ultimate-brain-notion/`), i.e. gaps that
  exist even within the Notion API calls already in use.

---

## Gap A — Notion API capabilities not used

### A1. No database/schema creation or mutation
No `create_database`, `create_data_source`, or property-schema editing
(add/rename/remove a property, change a select's options, rename a
database). `notion_client.py` only has read-only `get_database`/
`get_data_source`. **This is the main blocker for "spin up a new Finances
database on the fly."**

### A2. Relation properties aren't paginated
Notion caps inline relations at 25 items per read. The API's paginated
property-item endpoint (`GET /pages/{id}/properties/{property_id}`) that
would fetch the rest is never called — large relations (e.g. a project
with 40 tasks) get silently truncated with a `_truncated_relations`
warning flag instead of being fully resolved.

### A3. No single-block retrieve/update
Only children-listing (`get_blocks`), append (`append_blocks`), and
delete (`delete_block`) are implemented. There's no `GET /blocks/{id}` or
`PATCH /blocks/{id}` — in-place editing of one block only works through
the markdown replace/patch endpoints, not directly.

### A4. Archive ≠ Notion's native trash
`archive_item` flips a custom `Archived` checkbox property. It doesn't
use `archived`/`in_trash` on `PATCH /pages/{id}`, Notion's actual trash
mechanism. Only works on databases that happen to have an `Archived`
property.

### A5. Whole categories unused
- **Comments** — no read/post.
- **File uploads** — can't attach files/images to pages or blocks.
- **Users** — no workspace member directory lookup.
- **Webhooks** — no real-time push; server is pure request/response.
- **Search** — `notion_client.search()` exists but is only used
  internally by `setup_dev.py`, not exposed as an agent tool.
- **OAuth** — single static integration secret, single workspace by
  design.

---

## Gap B — Ultimate-Brain-domain gaps (within already-used API surface)

These don't require A1 (schema mutation) to fix — the databases and
properties already exist in a real UB workspace; the server just doesn't
read/write/expose them yet.

### B1. 8 of 13 databases have no typed tools
Milestones, People, Books, Reading Log, Genres, Recipes, Meal Planner,
and Work Sessions are wired in via env vars (`config.py:
SECONDARY_DB_ENV_MAP`) but only reachable through the generic
`query_database`/`get_page`/`update_page` fallback — no search/create/
update tools, and `format_generic_page` is schema-blind (no "active
milestones," "prospect pipeline," etc. semantics).

Work Sessions is notable on its own: `features/time-tracking.md`
describes it as a first-class UB feature, but there's no start/stop/query
tool and no "is a session currently active" surfaced anywhere.

### B2. Documented properties on covered DBs aren't surfaced or writable

**Tasks:**
- `Smart List`, `Snooze`, `Wait Date` — the GTD routing fields that drive
  the entire Process/GTD workflow (`features/gtd-process.md`) — no params
  on `create_task`/`update_task`/`bulk_update_tasks`, not read by
  `format_task`.
- `Days` + `Enforce Schedule` — recurring weekday scheduling, absent
  entirely.
- `Description`, `Assignee`/`People` — absent.
- `Energy` — the My-Day batching tag documented alongside the
  already-supported `Location`, but with no equivalent support.

**Projects:** `People`, `Review Notes`, `Pulled Notes`/`Pulled Tags` (the
Research Project "Pulls" feature) unsupported.

**Goals:** `Goal Set` date absent.

**Notes:** `People`, `Review Date`, `Image` absent.

**Formula/computed properties generally unexposed:** Projects'
`Progress`/`Meta`/`Time Tracked (Mins)`; Tasks' `Time Tracked`, `Time
Tracking Status`, `Current Session`, `Next Due`; Goals' `Progress`. An
agent asked "what's my progress on X" has to re-derive this client-side
today instead of reading Notion's own computed value.

### B3. Recurrence handling has a real correctness bug
`format_task`'s `recurrence` string only understands "every N unit."
`complete_task`'s `_advance_date` can't parse the advanced recur units
documented in `features/recurring-tasks.md` (Nth weekday of month, last
day/weekday of month, `Days`-based weekday recurrence) — and **silently
defaults to "+1 week"** when it can't parse, rather than erroring or
trusting Notion's own `Next Due` formula. This is a correctness gap, not
just a missing feature — it can silently reschedule a task wrong.

### B4. Workflow features with no tool wrapper
- **Project templates** — `create_project` never spins up the template's
  pre-defined child tasks (`features/project-templates.md`).
- **"Clear My Day"** — a bulk uncheck of `My Day`, one `bulk_update_tasks`
  call away but not wrapped as a named action.

### B5. "Custom database" in UB means more than creating a database
Per `setup/custom-databases.md`'s worked example (adding a "Companies"
DB), a full manual custom-database setup involves:
1. Creating a full-page database.
2. Adding **two-way relations** (with a *named reverse property*, not
   just a one-directional link) to existing DBs like Projects/People.
3. Customizing the page layout to surface the new relation.
4. Creating a **database template** with self-referential filtered views.

Beyond A1 (create/mutate schema), this means a real "create Finances the
way UB creates things" tool would also need:
- **Two-way relation creation** — not in `notion_client.py`.
- **Database template + filtered view creation** — not in
  `notion_client.py`, and **needs verification**: it's not yet confirmed
  whether the Notion API exposes templates/views at all, or whether
  they're UI-only (in which case this part of B5 isn't closeable via API
  and Hermes would need to either skip it or guide the user to do it
  manually once).

---

## Gap C — surfaced by research into how people actually run this (see `RESEARCH.md`)

Not from comparing against the Notion API or UB's docs, but from how
Hermes Agent's own users work and known AI-agent-on-Notion failure modes.
Full writeup and sourcing in `hermes/docs/RESEARCH.md`.

### C1. No duplicate-detection on create tools
`create_task`/`create_note` (and the other create tools) just create —
nothing checks "does something like this already exist" first. Named
directly as a common AI-agent-on-Notion failure mode in the research;
becomes a real risk once Hermes is creating items autonomously and
unattended rather than one at a time under direct supervision.

### C2. No cheap, narrowly-scoped "everything relevant to project X" call
`get_project_detail` exists but is the heavier, full-detail call. Hermes
users report running one sub-agent per project, each wanting a fast,
focused "what's live on this project" query — worth checking whether
`get_project_detail` already serves that well enough or whether a
lighter variant is warranted.

### C3. No canonical *weekly* rollup that includes Goals/Milestones
`daily_review_snapshot` exists and is daily-task-focused by name and
scope. Review-cadence research says goals/milestones need to appear in a
*regular* review too (weekly, not quarterly-only) or they silently go
stale — the same failure mode documented for GTD's skipped weekly
review and for abandoned personal OKRs. Nothing today rolls Goals/
Milestones progress into a recurring check the way tasks already get
via `daily_review_snapshot`.

---

## Notes on sequencing (not decided — for discussion)

- Gap B items mostly don't depend on Gap A and could be built first —
  they'd directly improve "manage my tasks / manage my day" quality
  without needing schema-mutation support.
- Gap A1 (create_database) is the actual blocker for the original ask
  ("spin up a Finances tracker on the fly"). B5 shows it's necessary but
  not sufficient — relations and templates/views are separate follow-on
  work even after A1 exists.
- B3 (recurrence correctness bug) arguably deserves early attention
  regardless of the Finances/Hermes roadmap, since it's a silent-wrong-
  answer bug in existing behavior, not a missing capability.
- Gap C items are cheap relative to A/B and address failure modes that
  specifically bite an *unattended, autonomous* agent (vs. a human
  driving the same tools) — worth weighing higher than their size
  suggests once Hermes is actually running unsupervised.
