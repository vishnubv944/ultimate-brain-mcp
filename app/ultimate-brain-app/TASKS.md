# App backlog

Running list of things to build / fix in the Android app. Work through them
one at a time; move finished items to **Done** with the commit hash.

---

## Implementation plan (for review)

The 13 backlog items collapse into **5 build phases** + **3 shared pieces of
infra**. Sequenced so the shared infra lands first, then work goes tab-by-tab
and page-by-page (each independently shippable + testable on device).

Phase 5 (Hermes chat, item 13) is fully independent of the rest — new tab, new
data layer, new VM — so it can be slotted in whenever; it does not block and is
not blocked by phases 1–4.

### Shared infra (build once, reused everywhere)

- **INFRA-A · "View" model for filter bars.** Today `BuiltinFilters` keys carry
  only a filter predicate. Extend to a `BuiltinView { key, label, icon,
  predicate, sort?, groupBy? }` so a chip can also change ordering or switch the
  list into grouped mode. `MyDayUiState` gains `viewFor(scope, key)` and
  `groupedItems(scope, key)`. Custom-filter system is untouched (still filter +
  sort only).
- **INFRA-B · `GroupedList` accordion.** Extract the expand/collapse
  group list already prototyped in MyDayScreen (Active Projects) into a reusable
  composable: `GroupedList(groups: List<Group>, expandedState, row)`. Used by
  grouped filter views (Projects/Goals/Tags) and by the hub-page sections.
- **INFRA-C · `EntityHub` scaffold.** One detail-page skeleton:
  `EntityHub(icon, title, onRename, viewDetails { … }, propertyStrip { … },
  relations { … }, nav?, sections: List<HubSection>)`. Each `HubSection` =
  title + optional view chips + inline "New" + body. Task/Note/Project/Goal/Tag
  detail all instantiate this.
  - Sub-piece **INFRA-C1**: `MarkdownBody` gains real collapsible toggle
    rendering (`<details>/<summary>` → expandable), needed for Goal Overview.
  - Sub-piece **INFRA-C2**: `DetailTabs` (Content / Sub-Tasks / …) segmented
    control + per-tab action slot.

### Phase 1 — Filters (items 2, 4, 5, 6, 7)  ·  low risk, high daily value

Depends on INFRA-A + INFRA-B.

1. **P1.1 Tasks tab** — finish the item-2 set: add `OVERDUE`, `ALL_PROJECTS`,
   `DO_NEXT`; order chips as Notion lists them.
2. **P1.2 Today tab** — point the "Add to today" browse chips at the same task
   view set (retire the bespoke `PlanFilter` list, or map it through).
3. **P1.3 Projects tab** — status views: 3 group chips + 5 status chips +
   Archived; grouped layout via `GroupedList`.
4. **P1.4 Notes tab** — Notes / Inbox / Fav. / Clips / Voice / Journal /
   Meetings / All.
5. **P1.5 Goals tab** — By Activity / By Deadline / By Tag (grouped) / Achieved.
6. **P1.6 Tags tab** — Fav. / A-Z / Types (grouped).

Ships as ~6 small commits.

### Phase 2 — Long-press quick edit (item 1)  ·  small, self-contained

1. **P2.1** `onLongClick` on `TaskRow` + `EntityRow` (`combinedClickable`).
2. **P2.2** VM: `quickEditTaskId` / `quickEditNoteId` state + open/close.
3. **P2.3** `QuickEditSheet` (ModalBottomSheet, reuses DetailKit chips); render
   once in `MainActivity`.
4. **P2.4** Wire the 4 list call-sites (Tasks, Today ×2 rows, Project detail,
   Notes).

### Phase 3 — Detail pages → EntityHub (items 8, 9, 11, 12, 10)

Depends on INFRA-C. One page per commit, in this order (simplest first):

1. **P3.1 Task detail** — header + "View details" + property strip
   (Status/Project/Due/My Day) + tabs Content / Sub-Tasks / History / Time.
2. **P3.2 Note detail** — property strip (Tag/URL/Favorite/Type) + tabs
   Content / Tasks + Relations row.
3. **P3.3 Project detail** — property strip + Goal chip w/ Replace + sections
   Tasks (List/Board/Calendar) + Notes.
4. **P3.4 Goal detail** — property strip + Goal Overview (collapsible body) +
   sections Milestones / Journal / Goal Projects.
5. **P3.5 Tag detail** — property strip + sections Sub Tags / Projects & Tasks /
   Notes / Web Clips / Goals / People.

### Phase 4 — Day-view time-blocking (item 3)  ·  most novel, do last

1. **P4.1** `DayTimeline` composable: vertical hour rail, now-line, 15-min snap.
2. **P4.2** Task blocks positioned from start/end date-time; drag-to-move +
   resize handles.
3. **P4.3** Unscheduled My-Day tray; drag onto timeline to schedule.
4. **P4.4** Commit → `setTaskDueDate(id, startIso, endIso)` with time-of-day.
   **Pre-req check:** confirm `UbRepository.setTaskDue` / `notion_client`
   round-trips time-of-day, not just date (item 3 assumes yes via ff281d8 —
   verify first).
5. **P4.5** Entry point link near the "On today" header → timeline mode.

### Phase 5 — Hermes chat (item 13)  ·  independent, needs the Pi URL + key

Breakdown P5.0–P5.8 lives under item 13 below. Blocked only on **connectivity
info from you**: the reachable base URL for the Pi's Hermes API server (port
8642) and the `API_SERVER_KEY`. Everything else can proceed once P5.0 (the
schema spike against the live server) is done.

### Open questions (proceeding with the noted default unless you say otherwise)

| # | Question | Default I'll use |
|---|----------|------------------|
| Q1 | "Do Next (GTD)" exact filter | not done, not snoozed/waiting, (due ≤ today OR no due), no incomplete parent |
| Q2 | Notes "Notes" vs "All" | "Notes" = not archived; "All" = everything incl. archived |
| Q3 | Notes "Inbox" | no Project relation set |
| Q4 | Projects filters: group chips, status chips, or both | both (3 groups + 5 statuses + Archived) |
| Q5 | ~~Task detail "History" tab data source~~ | **Resolved:** History = recurring task's past occurrences, backed by the Tasks DB `Occurrences` self-relation. Tab hidden for non-recurring / no occurrences. |
| Q6 | Project Tasks "Board" / "Calendar" sub-views — v1 scope | List + Calendar for v1 (Calendar reuses `TaskCalendar`); Board deferred |
| Q7 | "View details" — inline expander vs. push a sub-screen | inline expander (keeps context) |
| Q8 | Hermes: how does the app reach the Pi (Tailscale / Cloudflare Tunnel / LAN)? What's the base URL + `API_SERVER_KEY`? | **Need from you.** Plan assumes a single reachable HTTP(S) base URL, configurable in Settings, seeded from `.env`. |
| Q9 | Hermes chat: one session per "conversation" (ChatGPT sidebar) via Sessions API — correct? Or use `/v1/responses` named conversations? | Sessions API (`/api/sessions/*`) — richest control (list/rename/fork/delete/history). |

### Suggested order to actually execute

INFRA-A → **Phase 1** (all tabs) → INFRA-B folded in where needed →
**Phase 2** (quick edit) → INFRA-C → **Phase 3** (detail pages) →
INFRA-C1 (toggles, just before P3.4) → **Phase 4** (timeline).

**Phase 5 (Hermes chat)** runs on its own track — start it as soon as the Pi
URL + key are available (Q8), in parallel with phases 1–4.

Rationale: Phase 1 & 2 are the highest value-per-effort and lowest risk, and
they don't touch the detail-page rewrite. Phase 3 is the big one and benefits
from the scaffold being proven on the simplest page first. Phase 4 is isolated
and genuinely new, so it goes last where it can't block anything. Phase 5
touches almost no existing code (one nav swap), so it's schedule-independent.

---

## To do

### 1. Long-press quick-edit for tasks & notes
Long-pressing a task row or a note row (in any list) should open a quick
property editor — the way Todoist / TickTick do it — without navigating into
the full detail screen.

- Trigger: long-press on the row (`combinedClickable`), keep the normal tap =
  open detail.
- Surface: a modal bottom sheet with the item title at the top and a row of
  property chips (reuse `DetailKit` `SelectChip` / `DateChip` / `PropertyChip`).
- Tasks: Status, Due date, Project, Priority, My Day. Optionally quick actions
  (Complete, Archive, Open full details).
- Notes: Type, Date, Project, Favorite.
- Writes go through the existing optimistic VM setters
  (`setTaskProjectRelation`, `setTaskDueDate`, `updateTaskPriority`,
  `updateTaskStatus`, `toggleMyDay`, `setNoteType`, `setNoteDate`,
  `setNoteProjectRelation`, `toggleNoteFavorite`).
- Wire it into: Tasks list, Today (browse + shortlist rows), Project detail
  task list, Notes list. Prefer centralising the sheet (VM state +
  render once in `MainActivity`) so new list screens get it for free.

### 2. Today tab — full Notion view filter set on the task list
Bring the complete Notion "Search for a view" filter set to the **Today tab**'s
task filters (the "Add to today" / browse chips), and align the **Tasks tab**
to the same full set.

Complete set, in Notion's order:

1. Today
2. Active Projects
3. Inbox
4. Week
5. Month
6. Overdue
7. Scheduled
8. Recurring
9. No Due
10. All Projects
11. Do Next (GTD)
12. All
13. Done

New vs. what the Tasks tab has now (item 1 of the earlier filter change):
- **Overdue** — due date before today, not done
- **All Projects** — task has any project (vs. Active Projects = project is
  non-archived / not Done)
- **Do Next (GTD)** — needs definition; in GTD this is next-actionable items.
  Likely: not done, not waiting/snoozed, has no blocking parent, due today or
  no due date. Confirm the exact Notion filter before building.

Match Notion's icons where practical (wrench = Active Projects, rocket = All
Projects, arrow = Do Next).

### 3. Today tab — day-view time-blocking (drag to schedule)
A way to time-block the day's tasks, like Google Calendar's day view.

- Entry point: a link/toggle near the **"On today"** header (screenshot) that
  switches the Today tab into a **day-view calendar** for the current day.
- The day view lists the tasks that are on My Day / on today, shown as blocks
  on a vertical hour timeline.
- Blocks are **draggable** (move to a new time) and **resizable** (change
  duration) — GCal-style.
- Committing a drag/resize updates that task's **start and end date+time**
  (Due = start, Due end = end). Reuse `setTaskDueDate(taskId, iso, endIso)`
  which already supports `due_end` / time-of-day.
- Unscheduled My-Day tasks sit in a tray (top or side); dragging one onto the
  timeline schedules it.
- Respect the workspace timezone (`UB_TIMEZONE`).
- Notion side already supports time blocking (due_end param + due_end on
  reads, commit ff281d8) — no MCP schema change needed.

### 4. Projects tab — status-based filters
Replace the current Projects tab filter chips (All / Active / Doing / Done /
Archived) with filters that mirror the Notion **Status** property and its
groups:

Groups → statuses (from the Notion status config):
- **To-do:** Planned, On Hold
- **In progress:** Doing, Ongoing
- **Complete:** Done

Proposed chip set: the 3 group filters (To-do, In progress, Complete) plus the
5 individual statuses (Planned, On Hold, Doing, Ongoing, Done). Keep an
**Archived** chip too (archived projects are otherwise hidden). Confirm whether
the user wants group chips, per-status chips, or both.

Pull the live status options from the schema (`optionsFor("project.Status")`)
rather than hardcoding, and colour the dots to match Notion (Planned = blue,
On Hold = red, Doing = green, Ongoing = amber, Done = purple).

### 5. Notes tab — Notion view filters
Replace the current Notes tab filter chips with the Notion "Search for a view"
set, in order:

1. Notes  (list icon — the default / all notes view)
2. Inbox  (inbox icon — likely notes with no project / unfiled)
3. Fav.   (star — favourites)
4. Clips  (globe — type = Web Clip)
5. Voice  (mic — type = Voice Note)
6. Journal (brush — type = Journal / Daily)
7. Meetings (people — type = Meeting)
8. All    (asterisk)

Confirm the exact filter behind "Notes" vs "All" and behind "Inbox" from the
Notion view definitions. Map type-based chips to the live `note.Type` options
(`optionsFor`). Match icons where practical.

### 6. Goals tab — Notion view set
Replace the current Goals tab filter chips (Active / Achieved / Dropped) with
the Notion view set:

1. **By Activity** (up arrow) — active goals, ordered by recent activity
2. **By Deadline** (calendar) — active goals, ordered by deadline
3. **By Tag** (tag) — grouped by Tag / Area
4. **Achieved** (party) — status = Achieved

Note: these are mostly **sort/group** views over the same (mostly active) set,
not exclusive filters. So the Goals tab likely needs a sort/group control, not
just filter chips:
- By Activity / By Deadline → change the ordering
- By Tag → switch to a grouped (accordion) layout keyed on Area
- Achieved → the one that actually filters by status

Decide: model as chips that also flip sort/group, or a separate
"Sort / Group" affordance. Keep a way to see Dropped/Archived goals.

### 7. Tags tab — Notion view set
Replace the current Tags tab filter chips (All / Areas / Resources / Entities)
with the Notion view set:

1. **Fav.** (star) — favourited tags
2. **A-Z** (list) — all tags, alphabetical
3. **Types** (grouped icon) — grouped by tag Type (Area / Resource / Entity)

Same shape as Goals (item 6): A-Z and Types are sort/group views, Fav. is the
filter. "Types" → grouped accordion by tag Type.

### 8. Task detail page — Notion-style layout (top properties + tabbed body)
Rework the Task detail screen to match the Notion task page:

- **Header:** task icon, large editable title, a quiet **"View details"** link
  under it that expands the full property list (energy, location, smart list,
  repeats, snooze, wait, focus type, labels, assignee, people, etc. — the
  current "More details" set).
- **Primary property strip:** a small labelled grid — Status, Project, Due,
  My Day — each with its icon and value directly editable (chip / picker).
  This is the always-visible top set; everything else lives behind "View
  details".
- **Tabbed body** below the strip:
  - **Content** — the page body / markdown (current notes/description area)
  - **Sub-Tasks** — the subtask list + add / link existing
  - **History** — **recurring tasks only**: the task's past occurrences.
    Backed by the Tasks DB **`Occurrences`** property (a single-property
    self-relation → Task pages in the same DB). Each occurrence row renders
    from its related Task page: Name + `Completed` date + `Status`. Tab is
    hidden when `Occurrences` is empty / the task isn't recurring. Needs a
    new load (`loadOccurrencesForTask`) + `Task.occurrenceIds`.
  - **Time** — work sessions for this task + the focus-time graph (the
    existing per-task Work Sessions screen, folded in as a tab)
- Per-tab actions on the right (filter / sort / search / New) where relevant,
  like the Sub-Tasks tab's "New task" / "Link existing".
- Keep Android-native styling — this is a layout/IA change, not a re-skin.

Supersedes the earlier "icon chips, no empty boxes" pass — that was a stopgap.

### 9. Note detail page — same Notion-style pattern as item 8
Rework the Note detail screen to match:

- **Header:** editable title + quiet **"View details"** expander for the full
  property list.
- **Primary property strip:** Tag, URL, Favorite, Type — icon + value, each
  directly editable; empty ones show "Empty".
- **Tabbed body:**
  - **Content** — the note body / markdown
  - **Tasks** — tasks linked to this note (list + add / link)
- Under the tabs, a **Relations** row of quick-add links: Add Project, Add
  People, Add Tasks, Add Books (whatever relations the Notes DB has).
- Same Android-native styling; layout/IA change only.

Apply the same pattern to **Project** and **Goal** detail pages too where it
fits (properties strip + View details + relevant tabs).

### 10. Tag detail page — full "hub" layout
Rebuild the Tag detail screen as a rich hub page (Notion "Career" tag example),
not just a name + a couple of lists:

- **Header:** folder/tag icon, editable title, **"View details"** expander.
- **Property strip:** Type (Area/Resource/Entity), Favorite, Note Count (and
  any other rollups).
- **Relations** quick-add row: Add Sub-Tags, Projects, Goals, People.
- **In-page nav** (jump links) across the sections below.
- **Sections**, each with its own filter/sort chips + "New":
  - **Sub Tags** — child tags
  - **Projects & Tasks** — projects with this tag, grouped by status
    (Ongoing / Doing / …), each showing its active/overdue task counts;
    sub-views: Active / Planned / Board / Active Tasks
  - **Notes** — notes with this tag; sub-views: Recent / Fav / A-Z / By Tag /
    Voice
  - **Web Clips** — clip-type notes with this tag; Recent / A-Z / By Tag /
    By Site
  - **Goals** — goals with this tag, grouped by status (Active / Dream /
    Achieved); By Activity / By Deadline / Achieved
  - **People** — people linked to this tag; Contacts / Birthdays / Pipeline
- This is the same pattern the **Project** and **Goal** detail pages should
  follow (item 9) — a header + property strip + a stack of related-item
  sections with per-section view controls. Consider a shared "EntityHub"
  scaffold for all of Tag / Project / Goal detail.

### 11. Project detail page — hub layout (Notion "Intutions" example)
Same EntityHub pattern as items 9 / 10:

- **Header:** rocket/project icon, editable title, **"View details"** expander.
- **Property strip:** Status, Target Deadline, Progress (rollup % with a bar),
  Tag — icon + value, editable; empty ones show "Empty".
- **Goal** row: the linked goal shown as a chip with its status + area, plus a
  **Replace** action.
- **Relations** quick-add row: Add People (and any other Project relations).
- **In-page nav** jump links.
- **Sections:**
  - **Tasks** — with **List / Board / Calendar** view switch, grouped by
    status (To Do / Done), per-group counts, `New task` inline, `Load more`,
    filter/sort controls, due date on the right of each row.
  - **Notes** — notes linked to this project, with view chips + New.
- Keep Android-native styling.

### 12. Goal detail page — hub layout (Notion "Upskilling in AI" example)
Same EntityHub pattern:

- **Header:** trophy/goal icon, editable title, **"View details"** expander.
- **Property strip:** Status, Tag, Progress (rollup) — icon + value.
- **In-page nav** jump links (Goals / Dashboard).
- **Goal Overview** — a collapsible-section block rendered from the goal's
  page body (Why? / How? / Overview and Routine / Worst Case Scenario …).
  Needs proper rendering of nested Notion toggle blocks + numbered lists in
  `MarkdownBody` (check current fidelity).
- **Sections:**
  - **Milestones** — table view (Name / Target Deadline / Date Completed),
    New page, filter/sort.
  - **Journal** — collapsible; linked journal notes.
  - **Goal Projects** — projects toward this goal, grouped by status
    (Doing / Ongoing / …), each row showing active/overdue task counts + a
    progress bar; Active / Board sub-views.
- Keep Android-native styling.

Cross-cutting for items 8–12: build a shared **EntityHub** scaffold —
header + "View details" expander + property strip + Relations row + optional
in-page nav + a stack of related-item sections each with view/filter/sort
chips and an inline "New". Task/Note/Project/Goal/Tag detail all instantiate
it with their own property list and section set.

### 13. Hermes chat — in-app ChatGPT-style assistant
A full chat experience for talking to the user's **Hermes agent** (running on
their Raspberry Pi), built into the app.

**Nav change:** replace the **Notes** bottom-tab with **Chat**; move Notes into
the **More** hub (Productivity section). 5 tabs stay: Today, Tasks, Projects,
Chat, More.

**Backend — Hermes API server** (docs: nousresearch.com Hermes user guide →
API server). OpenAI-compatible + custom REST. Key facts:
- Bearer-token auth (`Authorization: Bearer <API_SERVER_KEY>`). Default port
  **8642**, base path `/v1` for OpenAI-compat, `/api/...` for REST control.
- **Sessions API** is the right fit for a ChatGPT UI:
  | Method | Path | Use |
  |---|---|---|
  | GET | `/api/sessions` | chat list (sidebar), paginated |
  | POST | `/api/sessions` | new chat |
  | GET | `/api/sessions/{id}` | metadata |
  | PATCH | `/api/sessions/{id}` | rename (title) |
  | DELETE | `/api/sessions/{id}` | delete chat |
  | GET | `/api/sessions/{id}/messages` | history on open |
  | POST | `/api/sessions/{id}/fork` | branch a chat |
  | POST | `/api/sessions/{id}/chat/stream` | **send msg, SSE turn** |
- SSE events on `/chat/stream`: `assistant.delta` (token stream),
  `tool.started`, `tool.completed`, `run.completed`. (Runs API
  `/v1/runs/{id}/events` adds `subagent.start/complete` + `hermes.tool.progress`
  — use later if we add long-run tasks.)
- Approvals: `POST /v1/runs/{run_id}/approval` (Hermes has terminal access;
  some tools are approval-gated). Body/stream-signal shapes are **not in the
  docs** — probe the live server.
- Images: messages accept a `content` array with `text` + `image_url`
  (`data:` base64 ok). No file upload.
- `X-Hermes-Session-Key` header for stable long-term memory across `/new`.
- `GET /health` for a connection check; `GET /v1/models` for the model id.

**Connectivity (open question — needs the user):** the API binds
`127.0.0.1:8642` on the Pi. The app needs a reachable URL + the
`API_SERVER_KEY`. Options: Tailscale MagicDNS (`http://<pi>.<tailnet>.ts.net:8642`),
Cloudflare Tunnel (gives HTTPS), or LAN IP (home only). Default plan:
**base URL + key configurable in Settings**, seeded from `.env`
(`HERMES_BASE_URL`, `HERMES_API_KEY`) via the existing Secrets-Gradle →
`BuildConfig` pattern (same as the Notion token). Also set
`API_SERVER_CORS_ORIGINS` isn't needed (native client, not a browser).

**App architecture:**
- `data/hermes/HermesClient.kt` — OkHttp + Retrofit for the REST calls; a
  hand-rolled SSE reader on a streaming `ResponseBody` (or add
  `com.squareup.okhttp3:okhttp-sse`) for `/chat/stream`.
- `data/hermes/HermesRepository.kt` — sessions list, message history, send
  (returns a `Flow<ChatEvent>`), rename, delete, health.
- Room cache for offline chat history (optional v2).
- `ChatViewModel` + `ChatUiState` (separate from `MyDayViewModel`).

**UI (ChatGPT-style):**
- **Chat screen:** message thread (user right / assistant left or full-width
  like ChatGPT), streaming assistant bubble, markdown via existing
  `MarkdownBody`, code blocks, copy button, auto-scroll, "stop" while
  streaming.
- **Tool activity:** inline collapsible chips/cards for `tool.started` /
  `tool.completed` ("🔧 Running terminal…", "✓ done") so the user sees what
  Hermes is doing.
- **Approvals:** when a turn pauses for approval, show an inline
  Approve / Deny prompt; wire to the approval endpoint.
- **Chat list drawer:** past sessions (title + last-message time), new chat,
  rename, delete, search.
- **Composer:** multiline input, send, image attach (camera/gallery →
  base64), voice-to-text later.
- **Empty state:** suggestion prompts ("Plan my day", "What's overdue?",
  "Add a task to …").
- Deep-link affordance later: "Ask Hermes about this" from a task/project.

**Phase 5 breakdown:**
1. **P5.0 Spike** — with the real base URL + key, probe `/health`,
   `/api/sessions`, `/api/sessions/{id}/messages`, and one `/chat/stream`
   turn; capture the exact JSON shapes (messages, SSE frames, approval
   signal). Write findings back here.
2. **P5.1** Secrets wiring + Settings fields (base URL, key, "Test
   connection").
3. **P5.2** `HermesClient` + `HermesRepository` (REST: list / get messages /
   rename / delete / health).
4. **P5.3** SSE send turn → `Flow<ChatEvent>`; `ChatViewModel`.
5. **P5.4** Chat screen — thread + streaming + markdown + stop.
6. **P5.5** Chat list drawer + new/rename/delete.
7. **P5.6** Tool-activity chips + approval prompt.
8. **P5.7** Nav swap (Chat in, Notes → More) + route wiring.
9. **P5.8** Image attach; empty-state prompts; polish.

## Done

- **Phase 1 — filters** (items 2, 4, 5, 6, 7)
  - INFRA-A (view keys w/ sort + group) + INFRA-B (`GroupedList`) — `0da7472`
  - Tasks / Projects / Notes / Goals / Tags Notion view sets — `0da7472`
  - P1.2 Today tab "Add to today" chip set — `5459976`
  - Remaining polish: status-coloured project dots (deferred).
- **Phase 2 — long-press quick-edit** (item 1) — `0325217`
  - `combinedClickable` onLongClick on TaskRow / EntityRow; `QuickEditSheet`
    (task: Status/Due/Project/Priority/My Day; note: Type/Date/Project/Fav);
    wired into Tasks, Today, Project detail, Notes.
- **Phase 3 — detail pages → EntityHub** (items 8, 9, 10, 11, 12)
  - INFRA-C: `components/EntityHub.kt` (EntityHubHeader / DetailTabs /
    HubSection) — `38599ce`
  - P3.1 Task detail: tabs Content / Sub-Tasks / History (recurring
    `Occurrences`) / Time — `38599ce`
  - P3.2-3.5 Note / Project / Goal / Tag detail — `2d78e48`
  - Deferred: true collapsible toggle rendering in `MarkdownBody`
    (INFRA-C1); Project Tasks List/Board/Calendar switch; the Notion tag/
    project hub's extra sections (Web Clips, People, Journal accordion);
    real tag rollup counts (formatter-stubbed at 18); Relations quick-add
    rows on Note detail (NoteModel lacks the relation id lists).
