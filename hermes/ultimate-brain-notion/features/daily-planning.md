---
title: Planning Your Day — My Day Page
purpose: How to use the My Day page for daily planning using the Plan / Execute / Wrap-Up workflow
agent_relevant: true
key_topics: [my-day, daily-planning, execute, plan, wrap-up, clear-my-day, time-tracking, task-ordering, process-vs-immersive, energy-tagging]
related_docs: [features/managing-tasks.md, features/gtd-process.md, databases/tasks.md]
---

# Planning Your Day — My Day Page

The **My Day** page is the most operationally important page in Ultimate Brain. Its purpose is to help you create a deliberate, realistic plan for the day and then execute it — rather than reacting to an ever-growing pile of due-date tasks.

> "Plans are worthless, but planning is everything." — Eisenhower

The core problem it solves: most people overestimate how much they can get done in a day, especially when looking at a large task list. By manually committing tasks to My Day, you construct a focused plan and build confidence in your ability to execute it.

## The Three-Step Process

My Day is divided into three sections you move through across the day:

### 1. Plan

At the start of the day (or the night before), go through the task views in the Plan section — identical to the task manager on the homepage, but with one key difference: **Today** and **Overdue** are separate tabs. This forces a clearer distinction between what's actually due today vs. what you've been ignoring.

For each task you decide to do today, check the **My Day** checkbox. The task immediately appears in the Execute section.

Key principle: don't add everything due today blindly. You are constructing a commitment. If you pile in six tasks and can only realistically do three, you'll fail your own plan. Keep it lean.

### 2. Execute

Execute shows only the tasks you committed to in the Plan step. This is your day's work — nothing else.

**Ordering tasks for clarity:**

No default sort is applied so you can drag tasks into any order. Recommended strategy:

- **Do the hardest/most important thing first** (or second, after a quick warm-up). If you defer the hardest task until after completing "easier" ones, those easier tasks will expand to fill the entire day.
- **Do process tasks before immersive tasks** — see PI tagging below.

**PI Tagging (Process vs. Immersive):**

This is one of the most powerful features in My Day.

| Type | Definition | Examples |
|---|---|---|
| **Process** | You put in a small amount of effort to kick something off; someone/something else continues it | Send a Slack message to unblock a colleague, start the laundry, delegate a task |
| **Immersive** | Requires your full, sustained attention the entire time | Writing, deep development work, filming, analysis |

If you batch process tasks together at the start of the day, you unblock other people and parallel workflows while you then go heads-down on an immersive task. Doing it in reverse (immersive first) means you block others for hours.

**Energy and Location tagging:**

Tasks can be tagged with:
- **Energy:** High / Low — represents how much mental effort it takes to *start*, not necessarily how hard it is. Cardio might be "Low Energy" if you find it easy to start.
- **Location:** Home / Errand / Office / Gym — helps you batch tasks that require the same physical location.

These tags let you order tasks intelligently and batch location-based tasks together.

**Time Tracking (optional):**

The Execute section has a **Time** view that groups tasks by tracking status: `Active Now` / `Not Tracking` / `Done`. Use the Start/End buttons on any task to track work sessions. This gives you real data on how long tasks actually take vs. how long you thought they'd take (the "fudge ratio"). Even a two-week trial of time tracking substantially improves future daily planning accuracy.

### 3. Wrap-Up

At the end of the day:

1. **Review Calendar** — shows tasks with My Day checked that aren't Done. Reschedule any that need new due dates.
2. **Clear My Day** — click this button. It unchecks the My Day checkbox on all tasks. This is critical — it resets the page to a blank slate so tomorrow starts fresh.
3. **Plan Tomorrow** — sorted by last-edited date (so unfinished tasks from today float to the top), this lets you re-add specific tasks to My Day for tomorrow deliberately.

> **Never leave My Day cluttered from the previous day.** The entire value of this page is that everything in Execute was deliberately chosen *today*. If you skip Clear My Day, it becomes just another dumping ground.

## Key Behaviors

- **My Day checkbox** — checking it on any task adds that task to the Execute view
- **My Day Label property** — the text label "My Day" that appears next to the checkbox in list views. Can be hidden via properties if you prefer
- **Clear My Day button** — UI-only, cannot be triggered by an agent; the user must click it
- **The page resets daily** — there is no automatic reset; the user does it manually via Clear My Day

## Agent Usage Notes

- Agent can set `My Day = true` on tasks if the user explicitly asks to add something to today's plan
- Agent cannot click the Start/End time tracking buttons (UI-only)
- Agent cannot click the Clear My Day button (UI-only)
- Agent can query tasks where `My Day = true` to show the user what's currently planned for today
