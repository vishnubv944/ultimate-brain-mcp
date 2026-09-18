# Ultimate Brain MCP — Tool Reference

Complete reference for every tool exposed by the **Ultimate Brain MCP** server.
The server wraps Thomas Frank's [Ultimate Brain](https://thomasjfrank.com/productivity/ultimate-brain/) Notion template and exposes **39 tools** grouped by entity type and workflow.

> The README says "30 tools" and CLAUDE.md says "31 tools" — both are stale. The live count is 39 once you include `patch_page_content`, `clear_my_day`, `weekly_review_snapshot`, the three milestone tools, and the two work-session tools that were added without updating the docs.

---

## Table of Contents

- [Overview](#overview)
- [Configuration & Setup](#configuration--setup)
- [Common Patterns](#common-patterns)
- [Tasks (6 tools)](#tasks-6-tools)
- [Projects (5 tools)](#projects-5-tools)
- [Notes (4 tools)](#notes-4-tools)
- [Tags (3 tools)](#tags-3-tools)
- [Goals (4 tools)](#goals-4-tools)
- [Milestones (3 tools)](#milestones-3-tools)
- [Work Sessions (2 tools)](#work-sessions-2-tools)
- [Cross-Cutting (4 tools)](#cross-cutting-4-tools)
- [Workflow Consolidators (4 tools)](#workflow-consolidators-4-tools)
- [Generic Page Tools (4 tools)](#generic-page-tools-4-tools)
- [Common Response Fields](#common-response-fields)
- [Limitations & Edge Cases](#limitations--edge-cases)

---

## Overview

The MCP server is built on Anthropic's **FastMCP** SDK with async **httpx** for Notion API calls (API version `2025-09-03` for most calls, `2026-03-11` for the page-markdown endpoints). Every tool:

- Returns a list of dicts or a single dict
- Errors come back as `{"error": "<message>"}` (or with a `partial_write` block if a write partially succeeded)
- Includes Notion API hints (`hint`) for 400/404/403/401 errors
- Annotates each tool with `readOnlyHint`, `destructiveHint`, `idempotentHint` so MCP clients know what's safe to call

It uses the **PARA** methodology (Projects / Areas / Resources / Archive) plus a Goals layer on top.

---

## Configuration & Setup

Six env vars are **required**:

| Variable | Purpose |
|---|---|
| `NOTION_INTEGRATION_SECRET` | Notion integration token |
| `UB_TASKS_DS_ID` | Tasks data source ID |
| `UB_PROJECTS_DS_ID` | Projects data source ID |
| `UB_NOTES_DS_ID` | Notes data source ID |
| `UB_TAGS_DS_ID` | Tags data source ID |
| `UB_GOALS_DS_ID` | Goals data source ID |

**Optional**:

- `UB_TIMEZONE` — IANA name (e.g. `Europe/London`). Used by `daily_review_snapshot` to resolve `now` / `today` / `tomorrow`. Falls back to `TZ`, then `UTC`.
- Secondary DB IDs — make optional databases available via `query_database`:
  - `UB_WORK_SESSIONS_DS_ID`, `UB_MILESTONES_DS_ID`, `UB_PEOPLE_DS_ID`, `UB_BOOKS_DS_ID`, `UB_READING_LOG_DS_ID`, `UB_GENRES_DS_ID`, `UB_RECIPES_DS_ID`, `UB_MEAL_PLANNER_DS_ID`

Run `uv run python setup_dev.py` to auto-discover the IDs and write `.env`.

---

## Common Patterns

### Discovering IDs

Most writes need a page ID. Workflow for finding them:

```text
1. search_*          → find candidates by name/filter
2. get_*_detail      → confirm + see linked items
3. update_*/create_* → perform the action
```

### Filtering dates

All `due_*` / `deadline_*` / `completed_*` / `achieved_*` / `date_after` params take `YYYY-MM-DD`. Combine `due_before` + `due_after` for ranges, or use `due_on` for a single day.

### Page body content

The four `create_*` tools (`create_task`, `create_note`, `create_project`, `create_goal`) accept an optional `content` parameter that accepts Markdown:

```markdown
# Headings, - bullets, 1. numbered lists, - [ ] to-dos,
\`\`\`code blocks\`\`\`, > quotes, --- dividers, plain paragraphs.
```

`set_page_content` and `patch_page_content` (on any page) provide richer body editing.

### Time-blocking

Set a time range on a task via ISO 8601 datetimes on `due` + `due_end`:

```json
{ "due": "2026-05-09T09:00:00+05:30", "due_end": "2026-05-09T10:30:00+05:30" }
```

### Search defaults

- `search_tasks` — defaults to **non-Done** tasks
- `search_projects` — defaults to **active** (Doing + Ongoing)
- `search_goals` — defaults to **Active**
- `search_notes` / `search_tags` — no default status filter (returns all by default, but archived items may still appear depending on the workspace)

### Tool annotations

| Annotation | Meaning |
|---|---|
| `readOnlyHint: true` | Safe — no Notion writes |
| `destructiveHint: true` | Permanently alters or removes data (`archive_item`, `set_page_content` replace, `patch_page_content`) |
| `idempotentHint: true` | Repeated calls produce the same result |

---

## Tasks (6 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_tasks` | R | Filter by name, status, project, priority, due date, My Day, labels, parent, completion |
| `get_my_day` | R | My Day tasks sorted by priority |
| `get_inbox_tasks` | R | Unprocessed tasks (To Do, no project, no due) — needs triage |
| `create_task` | W | Create with name + optional fields incl. time-blocking, location, parent, tags |
| `update_task` | W | Patch any properties on an existing task |
| `complete_task` | W | Mark Done + set Completion date; handles recurring tasks |

### `search_tasks`

Filter parameters (all optional):

- `status` — `To Do` | `Doing` | `Done` (omit for non-Done)
- `project_id` — relation filter
- `priority` — `Low` | `Medium` | `High`
- `due_before`, `due_after` — date range (mutually exclusive with `due_on`)
- `due_on` — single day
- `my_day` — `true` to filter to My Day only
- `parent_task_id` — for sub-tasks
- `label` — match by label name (multi-select)
- `query` — title substring
- `completed_before`, `completed_after` — best with `status='Done'`
- `limit` — default 50, max 100

Sorted by `Due` ascending.

### `create_task`

| Param | Required | Notes |
|---|---|---|
| `name` | ✅ | Title |
| `status` | | Default `To Do` |
| `due` | | `YYYY-MM-DD` or ISO datetime for time-blocking |
| `due_end` | | ISO datetime; pair with `due` for time-blocks |
| `priority` | | `Low` / `Medium` / `High` |
| `project_id` | | Link to project |
| `labels` | | `list[str]` (multi-select) |
| `my_day` | | `bool`, default false |
| `parent_task_id` | | Sub-task of an existing task |
| `tag_ids` | | Link to Tag relation (PARA Area/Resource/Entity) — distinct from `labels` |
| `location` | | Auto-detected (select/status/multi_select); no-op if Tasks has no Location property |
| `enforce_schedule` | | Recurring tasks: keep due date on fixed cadence |
| `content` | | Page body as Markdown |

**Returns**: formatted task. On success may include `possible_duplicate` (warning if a same-named task exists) and `_warning` (if `location` was ignored).

### `update_task`

Same fields as `create_task` minus `name` and `content`. Returns `{"error": "..."}` if no fields supplied.

### `complete_task`

- Single param: `task_id`
- If recurring (`recurrence` set on the task): resets status to `To Do`, clears My Day, and advances `Due` — prefers Notion's own `Next Due` formula (handles all recur units), falls back to a simple day/week/month parser
- If non-recurring: sets `Status = Done`, `Completed = today`, `My Day = false`

**Returns** the updated task. May include `_note` (next due date for recurring) or `_warning` if next due couldn't be determined.

### Response fields (task)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "status": "To Do | Doing | Done",
  "priority": "Low | Medium | High | null",
  "due": "YYYY-MM-DD",
  "due_end": "ISO datetime (only when time-blocked)",
  "my_day": true | false,
  "project_ids": ["..."],
  "project_name": "...",        // resolved when snapshot lookups present
  "parent_task_ids": ["..."],
  "tag_ids": ["..."],
  "area_tag_names": ["..."],     // resolved names
  "labels": ["..."],
  "location": "...",             // only when Tasks DB has Location
  "recurrence": "every 1 week",  // formatted from Recur Unit + Recur Interval
  "next_due": "...",             // Notion formula
  "enforce_schedule": true,
  "time_tracked": "HH:MM:SS",    // Work Sessions rollup
  "time_tracking_status": "Active Now | Not Tracking | Done",
  "smart_list": "Calendar | Do Next | Delegated | Snoozed | Someday | Inbox",
  "completion_date": "YYYY-MM-DD"
}
```

---

## Projects (5 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_projects` | R | Filter by status, tag, goal, deadline, archived |
| `get_project_detail` | R | Properties + task breakdown by status + recent notes |
| `list_project_templates` | R | List Notion templates defined on the Projects DB |
| `create_project` | W | Create from blank, content, default template, or specific template |
| `update_project` | W | Patch project properties; auto-sets Completed date on `status='Done'` |

### `search_projects`

Filters: `status` (omit → active Doing+Ongoing), `tag_id`, `goal_id`, `query` (title), `deadline_before`, `deadline_after`, `completed_before`, `completed_after`, `archived`, `limit`.

### `get_project_detail`

Returns the project plus:

- `tasks.total` — total task count
- `tasks.by_status` — `{status → count}`
- `tasks.items` — formatted task list
- `recent_notes` — up to 10 most-recent notes linked to this project

`resolve_relations=True` follows up on Notion's 25-item inline cap by paginating to fetch the full Tag list (attaches as `_resolved_relations`, removes from `_truncated_relations`).

### `list_project_templates`

Returns `{templates: [{id, name, is_default}]}` for the Projects DB.

### `create_project`

- `name` — required
- `status` — `Not Started` (default) | `Doing` | `Ongoing` | `Done`
- `deadline` — `YYYY-MM-DD`
- `tag_id`, `goal_id` — relations
- `content` — Markdown body (mutually exclusive with `use_default_template` / `template_id`)
- `use_default_template` — `True` to apply the Projects DB's default template
- `template_id` — specific template by ID

### `update_project`

Same fields as create minus `content` / `template_*`. Setting `status='Done'` auto-fills `Completed` to today.

### Response fields (project)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "status": "Not Started | Doing | Ongoing | Done",
  "deadline": "YYYY-MM-DD",
  "tag_ids": ["..."],
  "goal_ids": ["..."],
  "completed_date": "YYYY-MM-DD",
  "archived": true | false,
  "progress": "...",               // Notion formula
  "meta": "...",                   // Notion formula
  "time_tracked_mins": 0,          // Notion formula (number)
  "_truncated_relations": [...]    // when a relation hit Notion's 25-item cap
}
```

---

## Notes (4 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_notes` | R | Filter by type, project, tag, favorite, date |
| `get_note_content` | R | Properties + page body as enhanced Markdown |
| `create_note` | W | Create with type, project, tags, URL, content |
| `update_note` | W | Patch note properties incl. `favorite` |

### `search_notes`

Filters: `note_type` (validated against live discovered options), `project_id`, `tag_id`, `favorite`, `date_after`, `query` (title), `limit`.

Live `note_type` validation — pass exactly one of the workspace's select options. Fallback static list (when discovery fails): `Journal`, `Meeting`, `Web Clip`, `Lecture`, `Reference`, `Book`, `Idea`, `Plan`, `Recipe`, `Voice Note`, `Daily`, `Note`, `Brainstorm`.

### `get_note_content`

Returns note properties + `content` (Markdown body via Notion's server-side markdown endpoint — falls back to block-to-text converter if the API version isn't available).

### `create_note`

| Param | Required | Notes |
|---|---|---|
| `name` | ✅ | Title |
| `note_type` | | Validated against live options |
| `project_id` | | Link to project |
| `tag_ids` | | List of tag page IDs |
| `source_url` | | URL property (Web Clip use case) |
| `content` | | Markdown body |

Note Date auto-set to today. May return `possible_duplicate` if a same-named note exists.

### `update_note`

Patchable: `name`, `note_type` (validated), `project_id`, `tag_ids`, `favorite`, `source_url`.

### Response fields (note)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "type": "Journal | Meeting | ... | Brainstorm",
  "note_date": "YYYY-MM-DD",
  "project_ids": ["..."],
  "tag_ids": ["..."],
  "favorite": true | false,
  "source_url": "..."
}
```

---

## Tags (3 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_tags` | R | Filter by PARA type, name, parent, favorite |
| `create_tag` | W | Create with PARA type and parent |
| `update_tag` | W | Patch name, type, parent, favorite |

PARA types: `Area` (ongoing responsibility), `Resource` (topic of interest), `Entity` (person/place). Tags can have a `Parent Tag` for hierarchy.

### Response fields (tag)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "type": "Area | Resource | Entity",
  "parent_tag_ids": ["..."],
  "favorite": true | false
}
```

---

## Goals (4 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_goals` | R | Filter by status, tag, project, deadline, achieved date |
| `get_goal_detail` | R | Properties + linked projects (with status + progress) |
| `create_goal` | W | Create with status, deadline, tag, linked projects, content |
| `update_goal` | W | Patch goal; auto-sets Achieved date on `status='Achieved'` |

### `search_goals`

Filters: `status` (omit → Active), `query`, `tag_id`, `project_id`, `deadline_before`, `deadline_after`, `achieved_before`, `achieved_after`, `limit`. Sorted by Target Deadline ascending.

Statuses: `Active` | `Achieved` | `Dropped`.

### `get_goal_detail`

Returns goal properties + `projects` (live query of all projects linked via the Goal relation, with full formatted project dicts).

`resolve_relations=True` paginates to fetch the full project/tag list beyond Notion's 25-item inline cap.

### `create_goal` / `update_goal`

| Param | Required | Notes |
|---|---|---|
| `name` | ✅ (create) | Title |
| `status` | | Default `Active` |
| `deadline` | | `YYYY-MM-DD` |
| `tag_id` | | Single tag relation |
| `project_ids` | | List of linked projects |
| `content` | | Markdown body (create only) |

Setting `status='Achieved'` on update auto-fills `Achieved` to today.

### Response fields (goal)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "status": "Active | Achieved | Dropped",
  "deadline": "YYYY-MM-DD",
  "achieved": "YYYY-MM-DD",
  "tag_ids": ["..."],
  "project_ids": ["..."]
}
```

---

## Milestones (3 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_milestones` | R | Filter by name + (optional) Goal relation |
| `create_milestone` | W | Create with name + Goal / Date Completed / Target Deadline (workspace-dependent) |
| `update_milestone` | W | Patch name / Goal / Date Completed / Target Deadline |

**Schema is workspace-dependent.** Live discovery at startup checks for `Goal` (or `Goals`) relation, `Date Completed`, and `Target Deadline`. Any field that's not present is silently skipped with a `_warning` on the response.

### `search_milestones`

Filters: `query` (title substring), `goal_id` (only works if a Goal relation exists), `limit`.

Returns an error if `Milestones` isn't configured (`UB_MILESTONES_DS_ID` missing) or if `goal_id` is passed but the workspace has no Goal relation.

### `create_milestone` / `update_milestone`

| Param | Required | Notes |
|---|---|---|
| `name` | ✅ (create) | Title |
| `goal_id` | | Only applied if Goal relation exists |
| `date_completed` | | Only applied if `Date Completed` exists |
| `target_deadline` | | Only applied if `Target Deadline` exists |

If any field is silently skipped, the response carries `_warning` with a human-readable reason.

### Response fields (milestone)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "...",
  "goal_ids": ["..."],          // only if Goal relation present
  "date_completed": "YYYY-MM-DD",
  "target_deadline": "YYYY-MM-DD"
}
```

---

## Work Sessions (2 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `search_work_sessions` | R | Find sessions by task or currently-running (`active_only`) |
| `log_work_session` | W | Start a session (no End) or log a completed one |

Both require `UB_WORK_SESSIONS_DS_ID` configured.

### `search_work_sessions`

Filters: `task_id`, `active_only` (only sessions with no `End` set), `limit`. Sorted by Start descending.

### `log_work_session`

| Param | Required | Notes |
|---|---|---|
| `start` | ✅ | ISO 8601 timestamp |
| `end` | | ISO 8601; omit to start an in-progress session |
| `task_id` | | Link this session to a task |
| `name` | | Defaults to `'Work Session'` |

Duration is computed by Notion from Start/End.

### Response fields (work session)

```jsonc
{
  "id": "...",
  "url": "...",
  "name": "Work Session",
  "start": "ISO datetime",
  "end": "ISO datetime | null",
  "active": true | false,     // true when End is not set
  "duration": "HH:MM:SS",     // Notion formula
  "duration_mins": 0,         // Notion formula (number)
  "task_ids": ["..."],
  "team_member": [...]        // People relation (names or IDs)
}
```

---

## Cross-Cutting (4 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `daily_summary` | R | Count-only overview: My Day, overdue, inbox, active projects/goals |
| `archive_item` | W (destructive) | Archive any UB item (sets Archived checkbox); reversible |
| `set_page_content` | W (destructive) | Replace or append Markdown body to any page |
| `patch_page_content` | W (destructive) | Targeted find-and-replace edits on any page |

### `daily_summary`

Five parallel queries (single round-trip):

```jsonc
{
  "date": "YYYY-MM-DD",
  "my_day":     { "count": N, "tasks": [...] },
  "overdue":    { "count": N, "tasks": [...] },
  "inbox_count": N,
  "active_projects_count": N,
  "active_goals_count": N
}
```

Lighter than `daily_review_snapshot` (no lookups, no per-bucket task details for inbox, smaller response).

### `archive_item`

- Single param: `page_id` (any UB item — task / project / note / tag / goal)
- Sets `Archived` checkbox to `True`
- Reversible — uncheck Archived to restore
- Where the workspace's Tasks DB has no Archived property, use Notion's trash (a trashed page has `in_trash: true` and is excluded from `search_*` results)

### `set_page_content`

| Param | Required | Notes |
|---|---|---|
| `page_id` | ✅ | Any Notion page |
| `content` | ✅ | Markdown body (see syntax below) |
| `mode` | | `'replace'` (default, overwrites) or `'append'` |

Markdown supports: `#`–`###` headings, `-` bullets, `1.` numbered lists, `- [ ]` to-dos, ` ```code blocks``` `, `>` quotes, `---` dividers, tables, plain paragraphs.

**Replace mode** uses Notion's server-side Markdown endpoint (handles tables, toggles, nesting, block-splitting). If the endpoint isn't available (older workspaces), it falls back to delete-then-append blocks (no tables/toggles). The choice is remembered per-session (`AppContext.markdown_supported`) so a real 400 error isn't silently swallowed by a degraded engine.

**Append mode** always uses the local block builder (subset of block types).

Empty `content` + `mode='replace'` clears the page body.

### `patch_page_content`

Targeted find-and-replace edits — much cheaper than rewriting the whole page for small changes (check off a to-do, fix a line, update a value).

| Param | Required | Notes |
|---|---|---|
| `page_id` | ✅ | Any Notion page |
| `edits` | ✅ | Up to 100, applied in order |

Each edit is `{old_str, new_str, replace_all_matches?}`:

- `old_str` must match the page's current Markdown **exactly** — use `get_page_content` to see the current text
- `new_str` must be non-empty (use `set_page_content` for deletion)
- `replace_all_matches` defaults to false

Returns:

```jsonc
{
  "ok": true,
  "page_id": "...",
  "edits_applied": N,
  "unmatched": [{ "index": i, "old_str": "..." }]   // edits that found no match
}
```

Edits compound — each is applied against the document as mutated by the previous edit. Requires Notion API version `2026-03-11`.

---

## Workflow Consolidators (4 tools)

| Tool | R/W | Purpose |
|---|---|---|
| `daily_review_snapshot` | R | Everything a daily review needs in one call |
| `weekly_review_snapshot` | R | Weekly-cadence: completed tasks, overdue, active projects/goals, milestones |
| `bulk_update_tasks` | W | Apply many task patches concurrently with per-row results |
| `clear_my_day` | W | Unset My Day on every currently-flagged task (end-of-day reset) |

### `daily_review_snapshot`

Single-call replacement for ~7 read calls. Returns:

```jsonc
{
  "now": "ISO datetime with offset",
  "timezone": "IANA name",

  "buckets": {
    "completed_today":      [...],   // tasks marked Done today
    "overdue_or_due_today": [...],   // non-Done, due ≤ today
    "due_tomorrow":         [...],   // non-Done, due tomorrow exactly
    "on_my_day":            [...],   // non-Done, My Day flag set (any due date)
    "inbox":                [...]    // non-Done, To Do, no project, no due
  },

  "outstanding": [...],              // dedup union of overdue_or_due_today ∪ on_my_day

  "lookups": {
    "projects":  { "<project_id>": { "name": "...", "status": "..." } },
    "area_tags": { "<tag_id>":     { "name": "..." } }
  },

  "task_schema": {
    "has_location_property":  true | false,
    "location_property_name": "Location" | null,
    "location_property_type": "select | multi_select | status | null",
    "location_options":       ["Home", "Office", ...],
    "labels_options":         ["Deep work", "Email", ...]
  },

  "truncated": {                     // per-bucket overflow flag
    "completed_today":      false,
    "overdue_or_due_today": false,
    "due_tomorrow":         false,
    "on_my_day":            false,
    "inbox":                false
  }
}
```

Each bucket is capped at `_SNAPSHOT_BUCKET_CAP` (100). `inbox` cap is configurable via `inbox_limit`. Bucket overflow is surfaced via `truncated.<bucket>: true` so the caller knows to drill in with a narrower search.

Tasks in `overdue_or_due_today` / `on_my_day` carry resolved `project_name` and `area_tag_names` from the lookups.

### `bulk_update_tasks`

Apply many task patches concurrently with per-row results. **Never raises on a single failure** — failed rows come back through `results` with `ok: false`.

| Param | Required | Notes |
|---|---|---|
| `updates` | ✅ | List of `BulkTaskUpdate` dicts (max 100 recommended) |

Each `BulkTaskUpdate` mirrors `update_task` plus `tag_ids` and `location`:

```jsonc
{
  "task_id":         "...",
  "name":            "...",
  "status":          "To Do | Doing | Done",
  "due":             "YYYY-MM-DD | ISO datetime",
  "due_end":         "ISO datetime",
  "priority":        "Low | Medium | High",
  "project_id":      "...",
  "labels":          ["..."],
  "my_day":          true | false,
  "parent_task_id":  "...",
  "tag_ids":         ["..."],
  "location":        "...",
  "enforce_schedule": true | false
}
```

Returns:

```jsonc
{
  "results": [
    { "task_id": "...", "ok": true,  "task": { ...formatted task... }, "_warnings": [...] },
    { "task_id": "...", "ok": false, "error": "human-readable reason" }
  ],
  "summary": { "ok": N, "failed": M, "total": N+M }
}
```

Concurrency capped at 10 in flight, all serialised through the Notion rate limiter (~3 req/s).

### `clear_my_day`

Unset My Day on every task currently flagged for it. Thin wrapper over the same bulk-update machinery — returns the same `{results, summary}` shape as `bulk_update_tasks`.

| Param | Required | Notes |
|---|---|---|
| _(none)_ | | No parameters |

Returns:

```jsonc
{
  "results": [
    { "task_id": "...", "ok": true, "task": { ...formatted task... } }
  ],
  "summary": { "ok": N, "failed": M, "total": N+M }
}
```

If no tasks have My Day set, returns `{results: [], summary: {ok: 0, failed: 0, total: 0}}`. Useful for end-of-day / start-of-day reset workflows.

### `weekly_review_snapshot`

Weekly-cadence review: completed tasks over the period, overdue tasks, active projects, active goals, and milestones (if configured). Distinct from `daily_review_snapshot` (which is task-bucket focused) — this exists so Goals/Milestones get checked regularly. Goals that go unreviewed between quarterly check-ins are a known failure mode this closes.

| Param | Default | Notes |
|---|---|---|
| `days_back` | 7 | Range 1–90 |

Returns:

```jsonc
{
  "period": { "from": "...", "to": "...", "days_back": N },
  "completed_this_period": [...],
  "overdue": [...],
  "active_projects": [...],
  "active_goals": [...],
  "milestones": [...] | null,        // null when Milestones not configured
  "truncated": {
    "completed_this_period": false,
    "overdue": false,
    "active_projects": false,
    "active_goals": false,
    "milestones": false
  }
}
```

All list caps are `_SNAPSHOT_BUCKET_CAP` (100); overflow surfaces via the `truncated` map.

---

## Generic Page Tools (4 tools)

For working with secondary databases (Books, People, Recipes, Meal Planner, Genres, Reading Log, etc.) and one-off pages not covered by the dedicated tools.

| Tool | R/W | Purpose |
|---|---|---|
| `query_database` | R | Query any configured secondary database by name with raw Notion filter/sorts |
| `get_page` | R | Fetch any page by ID with all properties |
| `get_page_content` | R | Properties + body as Markdown |
| `update_page` | W | Patch any page properties with auto type coercion |

### `query_database`

- `database` — name (e.g. `'Books'`, `'People'`); omit to list available databases
- `filter` — raw Notion filter object
- `sorts` — raw Notion sorts array
- `limit` — default 50, max 100

Without args, returns `{available_databases: [...]}`. Returns an error if the named database isn't configured.

### `get_page`

Single param: `page_id`. Returns `{id, url, name, ...all properties...}` via the generic page formatter.

### `get_page_content`

Returns the same as `get_page` plus `content` (Markdown body). Same fallback behaviour as `get_note_content`.

### `update_page`

Patch any page properties with auto type coercion. For each property the caller supplies, the tool fetches the page first to learn its type, then converts the simple value into the right Notion format.

| Property type | Coercion |
|---|---|
| `title` | `str(value)` |
| `rich_text` | `str(value)` |
| `select` | `str(value)` |
| `multi_select` | `list[str]` or single value |
| `status` | `str(value)` |
| `checkbox` | `bool(value)` |
| `number` | `float(value)` |
| `date` | `{"start": "..."[, "end": "..."]}` or `str` |
| `url` | `str(value)` |
| `relation` | `list[str]` of page IDs |

Returns an error if a supplied property name doesn't exist on the page (lists all available names) or if the value can't be coerced.

---

## Common Response Fields

### Standard fields (most entity types)

- `id` — Notion page ID
- `url` — public Notion URL
- `name` — title property

### Truncation warnings

For any formatted page with a relation exceeding Notion's 25-item inline cap, the response carries `_truncated_relations: ["Tag", ...]`. Pass `resolve_relations=True` to `get_project_detail` / `get_goal_detail` to fetch the full list via a paginated call (populates `_resolved_relations`).

### Field-level warnings

Writer tools may add:

- `_warning` — e.g. location ignored (Tasks has no Location), milestone fields silently skipped
- `_note` — e.g. recurring task reset to To Do with next due date
- `possible_duplicate` — same-name item found; warning only, never blocks

---

## Common Workflows

These patterns come from the project's `ub` Claude skill and the Notion-side feature docs.

### Daily review (start of session)

```text
1. daily_summary          → counts only (cheap pre-flight)
3. daily_review_snapshot   → all 5 buckets + lookups + Tasks schema in one call
3. (optional) bulk_update_tasks to triage the inbox
```

Use `search_notes(query=...)` only for **title** matches — body text isn't searchable. For body keyword search, narrow by `note_type` and scan results.

### Note posture (from `.claude/skills/ub/SKILL.md`)

The skill enforces strict authorship rules per note type:

- **Idea** — Claude may capture proactively when an idea is clearly developed. Always run a novelty gate first: search notes by `"Idea"` + keywords, then projects, then goals. Extend existing idea notes via `set_page_content(mode="append")`.
- **Journal** — Read access only. Append feedback only when the user explicitly asks, under heading `## Claude's Feedback - YYYY-MM-DD`. Narrow exception: a guided-journaling flow creates a Journal entry from user-provided answers, formatted as Q&A.
- **Meeting** — Read access only. Never author or edit.

Default: confirm with the user before any write that isn't in the documented exception set.

### My Day lifecycle

`My Day` is the user's commitment flag for today's plan. To commit a task, set `my_day=true` on `create_task` / `update_task` / `bulk_update_tasks`. `complete_task` automatically unsets My Day on completion. `clear_my_day` is the bulk reset for end-of-day / start-of-day.

### GTD routing

Tasks have a `Smart List` select with values `Do Next`, `Delegated`, `Someday`, plus a computed `Smart List (Formula)` that also derives `Calendar` (has Due), `Snoozed` (has Snooze), and `Inbox`. The process page distinguishes two inboxes:

- **Task Inbox** — exits when assigned to a Project
- **Task Intake** — exits when routed via Smart List

All routing is reachable via the API.

### Batch threshold

Use `bulk_update_tasks` for **3 or more** task updates in a single workflow (daily review, inbox triage). Per-row results mean a partial failure doesn't sink the batch.

### Recurring tasks

- `complete_task` advances the date via Notion's own `Next Due` formula first, then falls back to a simple day/week/month parser (`_advance_date`). Complex recur units (Nth weekday of month, last weekday of month) are handled correctly only by the formula path.
- `Enforce Schedule` checkbox keeps the due date on a fixed cadence even when overdue. Read it from `format_task.enforce_schedule`; set it on create/update.

### Time tracking

`log_work_session(start)` with no `end` begins an in-progress session. Add `end` later (or via a second write) to close it — Notion computes `Duration` from the pair. `search_work_sessions(active_only=true)` finds what's currently running.

---

## Project Status Vocabulary

`config.py:PROJECT_STATUSES` constrains the MCP server to: `Not Started`, `Doing`, `Ongoing`, `Done`.

The older `managing-tasks.md` feature doc uses `Planned`, `On Hold`, `Ongoing`, `Doing`, `Done` — that's the Ultimate Brain 3.0 vocabulary. The MCP server rejects values outside the constrained set, so trust this doc (and `PROJECT_STATUSES`) over the older feature docs when in doubt.

---

## Limitations & Edge Cases

- **Notion rate limit**: ~3 req/sec. The server has a token-spaced rate limiter + retry-with-backoff on 429/5xx. `bulk_update_tasks` caps concurrency at 10 in flight; `set_page_content` block-delete caps at 5.
- **Search defaults**: most search tools default to non-Done / Active items. To see everything (including archived/done), set the status explicitly.
- **Schema discovery is best-effort**: Tasks `Location`, Notes `Type`, and Milestones `Goal` / `Date Completed` / `Target Deadline` are introspected at startup. If discovery fails the tools degrade gracefully with warnings — they never crash the server.
- **Page-markdown API version**: `get_page_content`, `get_note_content`, `set_page_content` (replace mode), and `patch_page_content` require Notion API version `2026-03-11`. On older workspaces they fall back to the block path (no tables/toggles in append mode, no block fallback for `patch_page_content`).
- **`update_page` only patches properties**, not body content — use `set_page_content` / `patch_page_content` for body edits. `update_page` is also not destructive (it can't delete a page).
- **Recurring tasks**: `complete_task` advances the date via Notion's own `Next Due` formula first, then falls back to a simple day/week/month parser. Complex recur units (Nth weekday, last day/weekday) are handled correctly by the formula path.
- **`bulk_update_tasks` never raises** — check `summary.failed` and individual `results[*].ok` for partial failures.
- **`archive_item` is reversible** — it sets `Archived = true`, doesn't trash. Uncheck Archived in Notion to restore.
- **Secondary databases** are opt-in via env vars. Without them, `query_database` returns an empty `available_databases` list and the dedicated tools (`search_milestones`, `log_work_session`, etc.) return a configuration error.
- **Field-name case sensitivity**: property names like `Name`, `Status`, `Due`, `Tag`, `My Day` are matched as written — case-sensitive on the Notion side.
- **Select option validation**: `note_type` is validated against the live discovered list (case-sensitive exact match). Other select fields (`status`, `priority`, `location`, milestone statuses) are sent verbatim — Notion will 400 on typos.
- **Note search is title-only** — `search_notes(query=...)` doesn't search the body. For body content, use `get_note_content` (or `get_page_content` for non-notes).
- **Relation truncation**: any relation exceeding Notion's 25-item inline cap is flagged via `_truncated_relations` on the formatted response. `get_project_detail` and `get_goal_detail` accept `resolve_relations=True` to pay an extra paginated call for the full list.
- **`possible_duplicate` is warning-only**: `create_task` and `create_note` search for same-title items and surface a `possible_duplicate` field if found, but never block the create.

---

## Tool Count Summary

| Group | Count |
|---|---|
| Tasks | 6 |
| Projects | 5 |
| Notes | 4 |
| Tags | 3 |
| Goals | 4 |
| Milestones | 3 |
| Work Sessions | 2 |
| Cross-Cutting | 4 |
| Workflow Consolidators | 4 |
| Generic | 4 |
| **Total** | **39** |

### Annotation breakdown

- **Destructive (3):** `archive_item`, `set_page_content`, `patch_page_content`
- **Not idempotent (9):** `create_task`, `create_project`, `create_note`, `create_tag`, `create_goal`, `create_milestone`, `log_work_session`, `set_page_content`, `patch_page_content`
- **Read-only (15):** every search, get, list, and snapshot tool
- **Idempotent + non-destructive writes (22):** `update_task`, `update_project`, `update_note`, `update_tag`, `update_goal`, `update_milestone`, `complete_task`, `archive_item` (reversible), `bulk_update_tasks`, `clear_my_day`, `update_page` — plus all read-only tools

---

## See Also

- [README.md](README.md) — quick-start, env vars, Docker
- [CLAUDE.md](CLAUDE.md) — codebase architecture, testing patterns, release flow
- [CHANGELOG.md](CHANGELOG.md) — release history
- [AGENTS.md](AGENTS.md) — repo guidelines for AI agents
- [`.claude/skills/ub/SKILL.md`](.claude/skills/ub/SKILL.md) — Claude skill that wraps these tools
- [hermes/ultimate-brain-notion/](hermes/ultimate-brain-notion/) — Notion-side database and feature docs