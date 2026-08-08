# Hermes — Personal Assistant Agent (on top of Ultimate Brain MCP)

## What this is

This fork exists to turn `ultimate-brain-mcp` from a Notion/Ultimate-Brain-specific
tool server into the backend data layer for **Hermes** — a personal assistant
agent that manages the user's day-to-day life: tasks, schedule, projects,
goals, and eventually domains well beyond what Thomas Frank's Ultimate Brain
template covers out of the box (finances, health, etc.).

Hermes itself lives outside this repo. This repo is the MCP server Hermes
connects to in order to read and act on the user's real data in Notion.

## The core idea

> "I want a Hermes agent for myself which acts as a personal assistant on
> the grounds of this. Manage my tasks, manage my day, and all. I will be
> connecting this MCP to Hermes and it will be taking care of everything in
> my life that is done via this. For example, I might want to have a
> separate database for my finances and have a new view just like how
> Ultimate Brain is there — and this MCP should be able to do it."

Two distinct things need to be true for that to work:

1. **Assistant behavior** — the existing Tasks/Projects/Notes/Goals tools
   (search, create, update, daily review, bulk update, etc.) need to be
   good enough to run "manage my day" style workflows end-to-end.
2. **Extensibility beyond Ultimate Brain's fixed schema** — the user should
   be able to say "set up a Finances tracker" and have the system create a
   new Notion database (with its own properties/views) and immediately get
   typed tools to operate on it, the same way Tasks/Projects/Notes/Goals
   work today.

(1) is mostly already covered by the upstream project. (2) is the actual
gap this fork needs to close.

## What the upstream server can and cannot do today

Established via direct comparison against the Notion API surface
(`developers.notion.com/reference`, API version `2025-09-03`, plus the
`2026-03-11` page-markdown endpoints already in use):

**Implemented (upstream):**
- Read/write rows (properties) in a fixed set of pre-existing databases:
  Tasks, Projects, Notes, Tags, Goals, plus optional "secondary" databases
  wired in via env vars (Work Sessions, Milestones, People, Books, Reading
  Log, Genres, Recipes, Meal Planner).
- Page body content: read as markdown, full replace, targeted
  find-and-replace patch, plus a legacy block-based fallback.
- Workflow helpers: `daily_summary`, `daily_review_snapshot`,
  `bulk_update_tasks`, `archive_item` (soft-delete via a custom `Archived`
  checkbox property, not Notion's native trash).
- Generic fallback tools (`query_database`, `get_page`, `update_page`) for
  any database already configured via env vars.
- Read-only schema introspection (`get_database`, `get_data_source`) used
  internally at startup — not exposed as agent tools.

**Not implemented (the gap):**
- **Database/schema creation and mutation** — no `create_database`,
  `create_data_source`, or property-schema editing (add/rename/remove a
  column, change a select's options, etc.). This is the main blocker for
  "spin up a new Finances database on the fly."
- **Paginated relation properties** — Notion caps inline relations at 25
  items per read; the API's paginated property-item endpoint
  (`GET /pages/{id}/properties/{property_id}`) that would fetch the rest is
  not called anywhere, so large relations get truncated with a warning
  flag instead of fully resolved.
- **Single-block retrieve/update** — only children-listing, append, and
  delete are implemented; there's no in-place block edit outside the
  markdown replace/patch endpoints.
- **Native trash** — `archived`/`in_trash` on `PATCH /pages/{id}` isn't
  used; "archive" means flipping a custom checkbox property, which only
  works on databases that have one.
- **Comments, file uploads, users/workspace directory, webhooks, search
  (exposed as a tool), OAuth** — none of these are wired up. The server is
  strictly single-workspace, single static integration secret, pure
  request/response (no push notifications from Notion).

Full detail on this comparison lives in the conversation history that
produced this doc; re-derive with a fresh Notion API pass if it goes stale.

## Gap #2: Ultimate-Brain-domain gaps (not just raw Notion API gaps)

The above is the *Notion API* gap. Separately, the user supplied a full
end-to-end doc set on how Ultimate Brain itself actually works
(`hermes/ultimate-brain-notion/`, 30 files — see its `index.md`). Cross-
referencing that against `server.py`/`formatters.py`/`config.py` surfaces
gaps that exist even *within* the Notion-API capabilities the server
already uses — i.e. things it could support today without needing
schema-mutation tools, but doesn't yet.

**1. No typed-tool coverage for 8 of the 13 databases.** Milestones,
People, Books, Reading Log, Genres, Recipes, Meal Planner, and Work
Sessions are wired in as env vars (`config.py:SECONDARY_DB_ENV_MAP`) but
only reachable through the generic `query_database`/`get_page`/
`update_page` trio — no search/create/update tools, and
`format_generic_page` is schema-blind (no domain semantics like "active
milestones" or "prospect pipeline" filtering). Work Sessions in particular
has no start/stop/query tool despite being a first-class UB feature
(`features/time-tracking.md`).

**2. Documented properties on already-covered DBs that aren't surfaced or
writable:**
- **Tasks** — GTD routing fields (`Smart List`, `Snooze`, `Wait Date`) that
  drive the entire Process/GTD workflow (`features/gtd-process.md`) have no
  params on `create_task`/`update_task`/`bulk_update_tasks` and aren't read
  by `format_task`. Also missing: `Days` + `Enforce Schedule` (recurring
  weekday scheduling), `Description`, `Assignee`/`People`, and `Energy`
  (My-Day batching tag, sibling to the already-supported `Location`).
- **Recurrence is a simplification, not the real model** — `format_task`'s
  `recurrence` string only understands "every N unit"; the advanced recur
  units (Nth weekday of month, last day/weekday, `Days`-based weekday
  recurrence) documented in `features/recurring-tasks.md` aren't parsed,
  and `complete_task`'s `_advance_date` silently defaults to "+1 week" for
  anything it can't handle — a real correctness gap, not just a missing
  feature.
- **Projects** — `People`, `Review Notes`, `Pulled Notes`/`Pulled Tags`
  (the Research Project "Pulls" feature) unsupported.
- **Goals** — `Goal Set` date absent. **Notes** — `People`, `Review Date`,
  `Image` absent.
- **Formula/computed properties generally unexposed** — Projects'
  `Progress`/`Meta`/`Time Tracked`, Tasks' `Time Tracked`/`Time Tracking
  Status`/`Current Session`/`Next Due`, Goals' `Progress`. An agent doing
  "what's my progress on X" has to re-derive this client-side today.

**3. Workflow features with no tool wrapper**, even though the underlying
data is reachable: **project templates** (`create_project` never spins up
the template's pre-defined child tasks, per `features/project-templates.md`),
**"Clear My Day"** (a bulk uncheck — one `bulk_update_tasks` call away but
not wrapped as a named action), **recurring task advancement diverging
from Notion's own `Next Due` formula** instead of trusting it.

**4. What "add a custom database" actually means in UB**, per
`setup/custom-databases.md`'s worked example (adding a "Companies" DB):
create a full-page DB, add **two-way relations** (with a named reverse
property, not just a one-directional link) to Projects/People, customize
the page layout to surface the new relation, and create a **database
template** with self-referential filtered views. Beyond the already-known
`create_database`/schema-mutation gap, this adds two more concrete
requirements for a "create Finances the way UB creates things" tool:
**two-way relation creation** and **database template creation with
filtered views** — neither exists in `notion_client.py` today, and it's
worth checking whether the Notion API even exposes database templates and
views (they may be UI-only, unlike most of the schema).

This second gap matters for sequencing: some of it (typed tools for
existing secondary DBs, missing properties on Tasks/Projects/Notes/Goals,
Smart-List/recurrence correctness) can be built *now*, without waiting on
schema-mutation support, and would make Hermes noticeably better at
"manage my tasks / manage my day" in the near term.

## Direction for this fork

Working title for the branch: `hermes-agent`.

The rough shape of the work (not yet sequenced/committed — to be broken
into discussed, individually-scoped changes):

1. **Schema-mutation tools** — `create_database`, `create_data_source`,
   `update_data_source` (add/edit/remove properties) so new life-domains
   (starting with Finances) can be provisioned through the agent instead
   of by hand in Notion, then immediately read/written via the existing
   generic tools (`query_database`, `update_page`, etc.) or new typed
   tools built the same way Tasks/Projects/Notes/Goals were.
2. **Finances as the first new domain**, used as the template/proof for
   (1) — a real database with its own properties and views, plus whatever
   typed tools make sense on top (e.g. `search_transactions`,
   `create_transaction`) mirroring the existing per-database tool pattern
   in `server.py`.
3. **Close remaining API gaps that block assistant quality**, prioritized
   by what actually bites Hermes in practice — relation pagination and
   native trash are the most likely early pain points.
4. **Broader assistant-quality tools** as needed — e.g. richer day/week
   planning, cross-database search — once the extensibility foundation is
   in place.

Nothing above is final. Each numbered item should get its own discussion
and plan before implementation, following the existing repo conventions
(`CLAUDE.md`, `CONTRIBUTING.md`, the tool-adding checklist in `CLAUDE.md`
under "Adding New Tools").

Given Gap #2 above, there's a plausible re-sequencing: tackle some of the
"available now" domain gaps (Work Sessions/Milestones typed tools, Tasks
GTD fields, recurrence correctness) before or alongside the schema-mutation
work, since they don't block on it and directly improve "manage my day"
quality. To be decided when we sequence real work — not decided yet.

## Non-goals (for now)

- Rebuilding Hermes itself (the agent) — out of scope for this repo.
- Multi-workspace / OAuth support — the user has one Notion workspace;
  no need to generalize beyond that yet.
- Webhooks / real-time push — Hermes will poll/call on demand for now.
