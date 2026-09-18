---
title: GTD in Ultimate Brain — Process Page
purpose: How the GTD methodology works and how to practice it using the Process page in Ultimate Brain
agent_relevant: true
key_topics: [gtd, getting-things-done, process-page, inbox, next-actions, smart-list, someday, snooze, delegated, task-intake, note-processing]
related_docs: [features/managing-tasks.md, features/daily-planning.md, databases/tasks.md]
---

# GTD in Ultimate Brain — Process Page

Ultimate Brain has the best GTD implementation for Notion. That said, **GTD is entirely optional** — it doesn't interfere with normal task management. You can use it, ignore it, or switch between the two styles. The creator himself uses the My Day daily planning approach instead.

## What Is GTD?

Getting Things Done (David Allen) is a decision-making framework for processing everything that enters your life. The goal: ensure nothing important slips through the cracks by routing every incoming item to a specific, trusted list.

### The Core Lists

| List | Purpose |
|---|---|
| **Inbox** | Default landing zone for everything. Process it regularly. |
| **Do Next** (Next Actions) | Stuff you should do next; clear, obvious next action |
| **Calendar** | Tasks with a specific hard deadline or event time |
| **Delegated** | Tasks you've passed to someone else |
| **Snoozed** (Tickler) | Deferred items to revisit at a future date |
| **Someday** | Possibly worth doing, but no commitment yet |
| **Reference Materials** | Non-actionable information worth keeping |
| **Trash/Archive** | Delete or archive — out of your life |

### The GTD Decision Flowchart

When processing each inbox item, ask in order:

1. **Is this actionable?**
   - No → Reference (add a Tag), Archive, Someday, or Snooze it
   - Yes → continue

2. **Is the next action clear and obvious?**
   - No → Break it into a Project with smaller tasks until one task is clear
   - Yes → continue

3. **Can you do it in under 2 minutes?**
   - Yes → Do it now
   - No → continue

4. **Can you delegate it?**
   - Yes → Delegate it (set Smart List = Delegated, link to a Person)
   - No → continue

5. **Does it have a hard deadline?**
   - Yes → Calendar (set a Due date)
   - No → Do Next (set Smart List = Do Next)

> GTD differs from traditional task management by discouraging arbitrary due dates. Only add a Due date if it actually matters. Everything else goes on the Do Next list, worked through as capacity allows.

## How Ultimate Brain Implements GTD

### The Task Smart List Property

The `Smart List` select property on Tasks has three values:
- `Do Next` — next actions list
- `Delegated` — tasks you've handed off
- `Someday` — maybe/someday list

The `Smart List (Formula)` property derives automatically from these combinations:

| Condition | Derived Value |
|---|---|
| Has a Due date | `Calendar` |
| Smart List = "Do Next" | `Do Next` |
| Smart List = "Delegated" | `Delegated` |
| Has a Snooze date | `Snoozed` |
| Smart List = "Someday" | `Someday` |
| None of the above | `Inbox` |

### Task Intake vs. Task Inbox

**Critical distinction:** The GTD-specific view is called **Task Intake**, not the regular Task Inbox.

| | Task Inbox | Task Intake (GTD) |
|---|---|---|
| Leaves when you add a Project? | Yes | **No** |
| Leaves when you add a Smart List value? | No | Yes |
| Use for | Traditional task management | GTD processing |

In GTD, adding a project to a task doesn't get it "processed" — it only leaves the Intake when routed to a specific GTD list (Calendar, Do Next, Delegated, Snoozed, Someday).

### The Snooze Property

Setting a **Snooze** date on a task moves it to the Snoozed list. It won't show in normal views until the snooze date arrives. The Deferred page has two views:
- **This Week** — snoozed items with a snooze date within the next 7 days
- **All Snoozed** — everything snoozed regardless of date

Use Snooze for items you want to consciously defer and review later, rather than having them clog your inbox now.

## The Process Page

The **Process (GTD)** page is the single page from which you can practice your entire GTD workflow. It has two main sections:

### Note Processing

Two views:
- **Note Intake** — notes where URL is empty or Type is Voice Note (not web clips)
- **Web Clip Intake** — notes that are web clips

Daily habit: scan both, then for each item decide:
- Add a Tag (routes to reference material)
- Add a Project (contextualizes the note)
- Archive it (removes from all views)

### Task Processing

The main GTD work area. Views across the top:

| View | Shows |
|---|---|
| Intake | All unprocessed tasks (no Smart List value, no Due date) |
| Do Next | Tasks with Smart List = "Do Next", grouped by Context |
| Calendar | Tasks with a Due date, shown in calendar layout |
| Delegated | Tasks with Smart List = "Delegated"; has Description field for notes |
| Snoozed | Tasks with a Snooze date |
| Someday | Tasks with Smart List = "Someday" |
| Done | Completed tasks |

### Processing Actions (API-Addressable)

To route a task in GTD:
- **Do Next:** set `Smart List = "Do Next"`
- **Calendar:** set `Due` date
- **Delegated:** set `Smart List = "Delegated"`, optionally link to `People`
- **Snoozed:** set `Snooze` date (leave Smart List empty)
- **Someday:** set `Smart List = "Someday"`
- **Reference (note):** add a `Tag` relation

## Note vs. Agent Scope

An agent can perform all routing actions via the API (set Smart List, set Due, set Snooze, add Tag/Project). The agent cannot:
- Trigger the inbox automations
- See what the user has physically processed vs. not processed without querying

For GTD inbox review sessions, the agent can query `Smart List (Formula) = "Inbox"` to surface unprocessed tasks and help the user route them.
