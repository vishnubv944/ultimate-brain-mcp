# Screens

Every screen in the app (`AppScreen` enum in `MyDayViewModel.kt`), one line each.

## Bottom-nav tabs

- **Today** — the daily home surface: today's shortlist, "add to today" browse chips (Inbox/Today/Active projects/Week/Month/Overdue/…), and the evening wrap-up.
- **Tasks** — full task list with the same filter-chip row as Today, grouped and sortable.
- **Projects** — all projects grouped by status (In progress / To-do / Complete) with status filter chips.
- **Chat** — the Hermes AI assistant: streaming replies, tool-call chips, a "/" command palette (built-ins + live skills), image/document attach.
- **More** — overflow hub linking to every screen below that isn't a bottom-nav tab.

## Tasks

- **Task detail** — one task's properties (status, project, due, priority, My Day), description, sub-tasks, recurrence history, and time-tracking tabs.
- **Time-block** — drag-and-drop hourly schedule for today's My Day tasks (or another day's due tasks).
- **Task work sessions** — logged focus-session history for one specific task.

## Projects

- **Project detail** — one project's properties (status, deadline, progress, goal), linked open/done tasks, and notes.
- **Edit project** — form for a project's core fields (status, deadline, tags, review notes).

## Notes

- **Notes** — note list with filter chips (Inbox, Favorites, Clips, Voice, Journal, Meetings).
- **Note detail** — one note's properties (type, date, project, favorite), rendered markdown body, linked tasks.
- **Note editor** — full-screen markdown editor for writing/editing a note's body.

## Goals

- **Goals** — goal list with sort/filter chips (by activity, deadline, tag) plus Achieved/Dropped.
- **Goal detail** — one goal's properties (status, area, progress, deadline), linked projects and milestone stats.
- **Milestones** — every milestone across all goals, filterable by status (in progress / completed / pending).
- **Milestone detail** — one milestone's status, target date, and linked tasks.

## Tags

- **Tags** — tags/areas grouped by type (Area / Resource / Entity), with favorites and A–Z sort.
- **Tag detail** — one tag's type/favorite properties, its sub-tags, and everything tagged with it (projects, notes, goals).

## Work sessions

- **Work sessions** — history of every logged focus session across all tasks.

## Library (books, people, food)

- **People** — list of people/contacts in the workspace.
- **Person detail** — one person's page.
- **Books** — reading-library book list.
- **Book detail** — one book's page.
- **Reading log** — dated log entries of reading progress (pages read, per book).
- **Genres** — book genre list with per-genre book counts.
- **Recipes** — recipe list.
- **Recipe detail** — one recipe's page.
- **Meal planner** — dated meal plan linking recipes to breakfast/lunch/dinner slots.

## System

- **Routines** — scheduled Hermes prompts (cron jobs): list, create, pause/resume, run now.
- **Global search** — one search box across tasks, projects, notes, tags, and goals.
- **Settings** — Notion/Hermes connection status, theme toggle, sync controls, debug info.
