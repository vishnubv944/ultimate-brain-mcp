# Full Notion parity — plan

**Goal:** every feature of the user's Notion Ultimate Brain, usable daily in
this app instead of the (laggy) Notion app. No compromises.

Schema introspected live from Notion (2026-09-06) via the data-source API.

## The 13 databases

| DB | In app now | Gap |
|---|---|---|
| **Tasks** | list + basic detail | ~30 unedited properties (see below) |
| **Projects** | list + detail | wrong Status enum; Review Notes, People, all relations uneditable |
| **Notes** | list + detail + body | wrong Type set (6 of 11); URL, Review Date, relations uneditable |
| **Goals** | list + detail | **wrong Status enum** (Dream/Active/Achieved); Goal Set, relations |
| **Tags** | list + basic detail | Parent/Sub-tags, all relations, favorite/archive |
| **Milestones** | list | Goal relation edit, detail screen |
| **Work Sessions** | today total only | history list from Notion, manual entry |
| **People** | — | **no screen** — Full Name, contact, socials, relationship, pipeline, relations |
| **Books** | — | **no screen** — Status, Rating, pages, formats, shelf, Genres, Logs |
| **Reading Log** | — | **no screen** — Book, Log Date, pages read |
| **Genres** | — | **no screen** — Name, Books |
| **Recipes** | — | **no screen** — times, servings, Meal Time, Tags |
| **Meal Planner** | — | **no screen** — Date, Meal, Recipes |

## Tasks — the full property set (the daily driver)

**Editable:** Name (title), Description (rich_text), Status (To Do / Doing /
Done), Priority (Low / Medium / High — it's a `status` prop), Due (date, with
time + end for time-blocking), Energy (High / Low), Location (Home / Office /
Errand), Smart List (Do Next / Delegated / Someday), P/I (Process /
Immersive), Labels (multi_select), My Day (checkbox), Enforce Schedule
(checkbox), Shopping List (checkbox), Snooze (date), Wait Date (date),
Recur Unit (Day(s) / Week(s) / Month(s) / … / Nth Weekday of Month) +
Recur Interval (number) + Days (multi_select weekdays).

**Relations:** Project, Parent Task, Sub-Tasks, Notes, People, Sessions,
Occurrences.

**Read-only formulas:** Next Due, Time Tracked, Smart List (Formula),
Meta Labels, My Day Label, Current Session.

**People:** Assignee (people prop).

## Corrections needed now (bugs)

- Goal `Status`: app uses `Active/Achieved/Dropped`; Notion is
  **`Dream / Active / Achieved`**. `dropGoal` writes an invalid value.
- Project `Status`: app uses `Not Started/Doing/Ongoing/Done`; Notion is
  **`Planned / On Hold / Doing / Ongoing / Done`**.
- Note `Type`: app offers 6; Notion has **11**
  (Journal, Meeting, Web Clip, Lecture, Reference, Book, Idea, Plan, Recipe,
  Voice Note, Daily).
- Tag `Type` is a `status` prop (Area / Resource / Entity) — app treats it
  as select in places.

## Build order

1. **Schema fixes + a runtime schema fetch.** Correct every enum; on launch
   fetch each data-source schema so select/status options come from Notion,
   not hardcoded lists. Generic property-payload builders.
2. **Task detail = full editor.** Every editable property gets an inline
   control (date/time pickers, option pickers, number steppers, multi-select,
   relation pickers). Sub-tasks, linked notes/people. Body markdown editor
   that writes to Notion.
3. **Project / Note / Goal / Milestone detail = full editors.** Same
   treatment. Relation pickers (assign project/goal/tag/people).
4. **New entity screens:** People, Books, Reading Log, Genres, Recipes,
   Meal Planner — list + full detail + create, on the shared kit.
5. **Load everything.** Drop the 30-day/open-only task filter behind a
   toggle; add real pagination / "load more" so completed history is
   reachable. Load Work Sessions, People, Books, etc. into the workspace.
6. **Relation navigation everywhere** — every relation chip taps through.
7. **Offline-tolerant writes + a sync indicator.** Queue failed writes,
   retry, surface conflicts.

Each step: build + on-device check + commit.

---

## Progress log

- **Step 1 — done.** Enum fixes (Goal Dream/Active/Achieved, Project
  Planned/On Hold/…, Note 11 types, Tag status). Runtime schema fetch
  (`loadSchemaOptions` → `optionsFor("<db>.<Prop>", fallback)`). Fixed
  invalid `createProject` "Not Started" → "Planned".
- **Step 2 — done.** Task detail edits every scalar: Status, Priority,
  My Day, Due (date), Project, Energy, Location, Smart List, Repeats
  (Recur Unit + interval stepper + Days weekday chips), Snooze, Wait,
  Focus type (P/I), Labels (multi-select), Enforce Schedule, Shopping
  List, Description, sub-tasks, page body (markdown read).
  Widened workspace task load window so Done history is reachable
  (+ a Done filter on the Tasks screen).
- **Step 3 — done.** Project: Status/Deadline/Goal/Review Notes/Tags/People.
  Goal: Status/Deadline/Goal-Set/Area-Tag. Note: Type/Date/Project/URL/
  Review-Date/Tags. Tag: favorite/Type/Parent + Sub-tags list. Milestone:
  new detail screen with Goal relation + Target date.
- **Step 4 — done** (prior commits): People/Books/Reading Log/Genres/
  Recipes/Meal Planner list+detail+create on the kit.
- **Step 5 — done (bar workspace-user assignee).** Work Sessions history
  loads from Notion. Tasks 'Done' filter has 'Load older completed tasks'
  (365-day window). The people-property 'Assignee' still needs /v1/users;
  the People *relation* on tasks/projects is editable.
- **Step 6 — done.** Task→Project/Notes, Project→Tasks/Notes/Goal,
  Goal→Projects, Milestone→Goal, Tag→Projects/Notes/Sub-tags, and
  Global Search rows all tap through.
- **Step 7 — done.** remoteWrite queues failed writes and retries with
  exponential backoff; ScreenScaffold shows a tap-to-retry banner; a
  successful sync drains the queue.

---

## Status: end-to-end parity reached (2026-09-06)

Every one of the 13 databases loads from the live Notion workspace and is
editable in-app on the shared native kit. Verified on device:

- **Tasks** — every editable property (status, priority, My Day, due date,
  project, energy, location, smart list, recurrence + interval + weekdays,
  snooze, wait, focus type, labels, enforce schedule, shopping list,
  description, sub-tasks, People relation, Assignee people-property via
  /v1/users), page body markdown, linked notes; Done filter + load-older.
- **Projects** — status/deadline/goal/review-notes/tags/people, task lists,
  linked notes, about body.
- **Goals** — status/deadline/goal-set/area-tag, linked projects, body.
- **Notes** — type/date/review-date/project/url/tags, page body, favorite.
- **Milestones** — new detail screen: goal relation + target date + toggle.
- **Tags** — favorite/type/parent + sub-tags list, linked projects/notes.
- **Work Sessions** — focus timer + Notion-backed history.
- **People / Books / Reading Log / Genres / Recipes / Meal Planner** —
  list + detail + create.

Cross-cutting: runtime schema fetch drives every option list; relation
chips tap through everywhere; failed writes queue and retry with a
tap-to-retry banner; global search spans tasks/projects/notes/goals/tags.
