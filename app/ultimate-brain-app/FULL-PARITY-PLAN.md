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
