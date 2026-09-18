---
title: Managing Tasks and Projects
purpose: Complete guide to task and project management in Ultimate Brain — inbox, processing, recurring tasks, subtasks, My Day, research projects
agent_relevant: true
key_topics: [tasks, projects, inbox, quick-capture, recurring-tasks, subtasks, my-day, active-projects, research-project, polls, processing]
related_docs: [features/daily-planning.md, features/gtd-process.md, features/recurring-tasks.md, databases/tasks.md, databases/projects.md]
---

# Managing Tasks and Projects

Task management is the most developed part of Ultimate Brain. This guide covers the full workflow from capturing tasks to completing them.

## Where Tasks Live on the Homepage

The homepage has a **Tasks** section with tabbed views covering ~95% of daily task management needs:

| Tab | What It Shows |
|---|---|
| Today | Tasks due today or overdue |
| Inbox | Tasks without a project or due date |
| Week | Calendar view of this week |
| Month | Calendar view of this month |
| Scheduled | All tasks with due dates, grouped by relative date |
| No Due | All tasks without a due date |
| Recurring | Tasks with a Recur Interval set |
| Active Projects | Tasks grouped by currently active projects (Doing/Ongoing status) |
| Done | Completed tasks |

Hovering over any view tab shows its description. Hovering over any property in a task page shows its description.

## The Inbox — Default Capture Destination

The Inbox shows tasks that have no Project, no Smart List value, and are not Done. This is the GTD-style "brain dump" destination.

**For quick capture (especially mobile):** Use the **Quick Capture** page. It has a Task Inbox view plus a **Today** tab that only shows tasks created today — it auto-clears the previous day's tasks, so it stays clean for mobile use. Consider adding it as a widget on your phone's home screen.

**Processing inbox tasks** — daily habit:
1. Open the inbox
2. For each task: either check it off (if trivially done) or assign a Project
3. Once a project is assigned, the task leaves the inbox

The Tasks database has a more capable table view in the dedicated Tasks page (not just the homepage) — useful for bulk-editing properties across many tasks.

## Projects

Projects group tasks and notes around a defined outcome. Every project should have a **clear end goal** (unlike ongoing areas of life, which use Ongoing status).

### Project Statuses

| Status | Meaning |
|---|---|
| `Planned` | Created but not started |
| `On Hold` | Started but currently paused |
| `Ongoing` | Maintenance project for an area (never "done") |
| `Doing` | Actively working toward a defined end |
| `Done` | Complete |

**Ongoing projects** are the recommended way to associate tasks with life Areas (since Tasks has no direct relation to Tags). Create a project called "Work Ongoing" with status Ongoing, and put all maintenance/area tasks there.

### Creating a Project

1. From the inbox, open any task → click the Project field → type a new name → New page in Projects
2. The Project template applies automatically, creating task and note views on the project page
3. Set Target Deadline, Tag (Area), and Goal if applicable

### The Research Project Template

An alternate project template that adds a **Pulls** feature. Use it when a project needs to reference materials from other parts of your second brain:

- **Pulled Notes** — manually pull specific notes from anywhere in the system into the project's reference view (without formally linking them to the project)
- **Pulled Tags** — pull ALL notes from a Tag into the project's reference view

This makes the project page a one-stop shop for everything related to the project, including outside reference materials.

## Recurring Tasks

Any task with both a **Due date** AND a **Recur Interval** is a recurring task. Ultimate Brain ships with built-in automations to process them.

### Setting Up

In a task's properties:
- `Recur Interval` (Number): how many units to recur by
- `Recur Unit` (Select): Day(s), Week(s), Month(s), Year(s), or special options (Last Weekday, Nth Weekday of Month)
- `Days` (Multi-Select): specific weekdays if using Day(s) + Interval 1

Examples:
| Goal | Interval | Unit | Days |
|---|---|---|---|
| Daily | 1 | Day(s) | — |
| Mon/Wed/Fri | 1 | Day(s) | Monday, Wednesday, Friday |
| Every 3rd Thursday | 3 | Nth Weekday of Month | Thursday |
| Monthly | 1 | Month(s) | — |

### What Happens When You Check It Off

The **Recurring Tasks (Simple)** automation (enabled by default) fires:
1. Sets `Due` = value of `Next Due` (the computed next occurrence)
2. Sets `Status` back to `To Do`

This happens automatically — no agent action needed.

### Recurring Tasks (Advanced) — Task History

An optional automation (disabled by default) that additionally creates a **historical record** page every time the task is completed. This enables lightweight habit tracking.

To enable: unlock Tasks database → click ⚡️ → disable Simple → enable Advanced.

Once enabled, completed recurring tasks gain a **Task History** view on the page (after applying the "Recurring Task w/ History" template) showing every past completion.

### Agent Note on Recurring Tasks

An agent can **set** the Recur Interval and Recur Unit properties to configure a recurring task. The automation that **processes** the task when it's completed is UI-triggered only — the agent cannot trigger it. If needed, an agent can manually update `Due` to the value of `Next Due` and reset `Status` to `To Do`.

## Sub-Tasks

Ultimate Brain uses a custom subtasks system (not Notion's native sub-items). To create subtasks:

1. Open a task as a full page
2. Apply the **Task with Sub-Tasks** template from the template picker
3. A task view appears on the page — create subtasks directly in it

Subtasks appear in list views with an arrow `↓` indicator under their parent. The parent task shows a `↳` indicator when it has active subtasks.

**Limitation:** Only one level of subtasks is supported (subtasks of subtasks cause issues due to Notion limitations).

## My Day — Daily Planning

The **My Day** page (separate from the task manager) is the recommended primary interface for daily work. See [features/daily-planning.md](features/daily-planning.md) for the full guide.

Key concept: checking the **My Day checkbox** on any task adds it to the My Day Execute view. This creates a deliberate daily plan instead of relying on due dates.

## Dedicated Task Pages

Beyond the homepage, there are specialized pages for each view type:

| Page | Purpose |
|---|---|
| Task Inbox | List + Table + Board + Calendar views of inbox tasks |
| Today | Tasks due today in all layout types |
| Next 7 Days | Tasks due within one week |
| Next Month | Tasks due within one month |
| All Tasks | Every task regardless of due date or project |
| Tasks by Project | All tasks grouped by their project |
| Someday | Tasks with Smart List = "Someday" |
| Priority View | Tasks grouped by priority (High/Medium/Low) |
| Recurring Tasks | All recurring tasks with automation access |

Table views in these pages are especially useful for bulk-editing properties (shift+scroll for horizontal scrolling).

## Project Sub-Pages

The Projects page has:
- **Active** — Doing + Ongoing projects
- **Planned** — not yet started
- **Board** — all statuses in kanban view
- **Deadlines Calendar** — projects by Target Deadline (can open in Notion Calendar)
- **Completed** — Done projects
- **By Tag** — swim lane view by Area
- **By People** — swim lane view by associated contacts
