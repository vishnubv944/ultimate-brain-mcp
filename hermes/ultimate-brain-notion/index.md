---
title: Ultimate Brain — Docs Navigation Index
purpose: Navigation map for all agent-relevant documentation — tells the agent which file to read for which question
agent_relevant: true
key_topics: [index, navigation, all-docs]
related_docs: []
---

# Ultimate Brain — Docs Navigation Index

Use this index to find the right file before reading. Files are grouped by category.
Read the minimum required — start with the specific file that matches the question.

---

## Databases — Full Property Schemas

Read these when you need exact property names, valid status values, relation targets, or formula behavior.

| File | What's in it |
|---|---|
| `databases/tasks.md` | Full Tasks DB schema — all properties, status options (To Do/Doing/Done), Smart List, Priority, Recur, Days, automations |
| `databases/projects.md` | Full Projects DB schema — status options (Planned/On Hold/Ongoing/Doing/Done), relations to Tags/Goals/Tasks/Notes |
| `databases/notes.md` | Full Notes DB schema — Type options (Journal/Meeting/Web Clip/Voice Note/Idea/Plan/etc.), relations |
| `databases/tags.md` | Full Tags DB schema — Type options (Area/Resource/Entity), PARA implementation, automations |
| `databases/goals.md` | Full Goals DB schema — status options (Dream/Active/Achieved), Milestones relation |
| `databases/milestones.md` | Full Milestones DB schema — Date Completed, Target Deadline, Goal relation |
| `databases/people.md` | Full People DB schema — Relationship types, Pipeline Status, all contact properties |
| `databases/work-sessions.md` | Full Work Sessions DB schema — Start, End, Duration formulas, Task relation |
| `databases/books.md` | Full Books DB schema — Status, Rating, Genres, Owned Formats, Shelf |
| `databases/reading-log.md` | Full Reading Log DB schema — Book relation, Log Date, Start/End Page |
| `databases/genres.md` | Full Genres DB schema |
| `databases/recipes.md` | Full Recipes DB schema — Prep/Cook Time, Recipe Tags, Is Quick formula, Inbox flag |
| `databases/recipe-tags.md` | Full Recipe Tags DB schema — Style/Course/Equipment types |
| `databases/meal-planner.md` | Full Meal Planner DB schema — Date, Meal status (Breakfast/Lunch/Dinner/Snack), Recipes relation |

---

## Features — How Things Work

Read these when the user asks how a feature works or you need to know the right workflow.

| File | What's in it |
|---|---|
| `features/overview.md` | Full system tour — all pages, sections, navigation, PARA architecture, design principles |
| `features/managing-tasks.md` | Task and project management — inbox, processing, projects, recurring tasks, subtasks, My Day |
| `features/daily-planning.md` | My Day page — Plan/Execute/Wrap-Up, PI tagging, energy/location, time tracking, Clear My Day |
| `features/gtd-process.md` | GTD methodology + Process page — decision flowchart, all GTD lists, Smart List routing, Task Intake vs Inbox |
| `features/recurring-tasks.md` | Recurring tasks deep dive — all recur units, Nth weekday, task history, automation details, edge cases |
| `features/time-tracking.md` | Time tracking setup — Work Sessions DB, Start/End buttons, session formulas, My Day integration |
| `features/project-templates.md` | Project templates with pre-defined tasks — automation method vs manual method |

---

## Setup & Customization

Read these when the user asks how to configure, customize, or troubleshoot the system.

| File | What's in it |
|---|---|
| `setup/getting-started.md` | Beginner guide — first steps, quick capture, daily use |
| `setup/unlocking-databases.md` | How to unlock/lock databases for structural changes |
| `setup/database-templates.md` | How database page templates work — applying, editing, creating |
| `setup/adding-properties.md` | How to add new properties to databases |
| `setup/customizing.md` | Customization overview — what can be changed and how |
| `setup/database-customizations.md` | Relation limits and advanced database customization |
| `setup/relate-tasks-to-tags.md` | How to set up a direct Task → Tag relation (optional, advanced) |
| `setup/custom-databases.md` | How to add entirely new databases and integrate them |
| `setup/project-templates.md` | Pre-defined task templates for projects (paid: automation; free: manual) |
| `setup/faqs.md` | Frequently asked questions |
| `setup/notion-bugs.md` | Known Notion bugs that affect Ultimate Brain and workarounds |
| `setup/whats-new-v3.md` | What changed in Ultimate Brain 3.0 |
| `setup/changelog.md` | Recent updates and fixes |
| `setup/creators-companion.md` | Creator's Companion overview — content planning integration |

---

## Quick Topic Lookup

| Question / Topic | Read |
|---|---|
| What status values does Tasks have? | `databases/tasks.md` |
| What status values does Projects have? | `databases/projects.md` |
| How do I create a recurring task? | `features/recurring-tasks.md` |
| How does My Day work? | `features/daily-planning.md` |
| How does GTD work in Ultimate Brain? | `features/gtd-process.md` |
| How does the Smart List property work? | `databases/tasks.md` or `features/gtd-process.md` |
| What Note Types exist? | `databases/notes.md` |
| How does time tracking work? | `features/time-tracking.md` and `databases/work-sessions.md` |
| What are the Meal status options? | `databases/meal-planner.md` |
| How do Tags / Areas / Resources work? | `databases/tags.md` and `features/overview.md` |
| How do Goals and Milestones relate? | `databases/goals.md` and `databases/milestones.md` |
| What is the People pipeline? | `databases/people.md` |
| How to unlock a database? | `setup/unlocking-databases.md` |
| What's new in v3? | `setup/whats-new-v3.md` |
| Known Notion bugs? | `setup/notion-bugs.md` |
