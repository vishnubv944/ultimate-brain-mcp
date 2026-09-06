# Today tab — analysis & redesign proposal

**Status:** IMPLEMENTED (2026-09-06). See "DELIVERED" at the bottom. Kept for
rationale + before/after.
**Scope:** the three phases of the Today tab — Plan, Execute, Wrap Up —
plus the shell that switches between them.

---

## 1. What's actually broken right now

### 1.1 The phase switcher

- It's a segmented button (`RitualJumpBar`), not tabs. You can't swipe
  between phases, there's no underline/indicator, and it doesn't feel like
  the standard Android "tabbed screen".
- **You asked for real Material tabs + swipe.** Agreed.

### 1.2 Plan phase

| Problem | Detail |
|---|---|
| **The filter chips do nothing** | `Today / Active Projects / Inbox / Week / Overdue / Recurring` are rendered and highlight on tap, but the task list below is hard-coded to `tasks.filter { !done && !overdue }.take(3)`. `selectedPlanFilter` is stored in state and never read. So every filter shows the same 3 tasks. |
| **Only 3 tasks, no real list** | There's no way to see your actual backlog and pick what to work on. `.take(3)` is a prototype stub. |
| **"Journal & Meeting Notes" card is dead** | `clickable { /* Opens notes drawer */ }` — no handler. |
| **No "commit to today" affordance** | Tapping a row toggles `My Day`, but it's a tiny checkbox with no feedback and the row also navigates nowhere. The core Plan job — "pick today's shortlist from everything" — isn't supported. |
| **"Morning Ritual" eyebrow label** | Decorative ALL-CAPS chrome, adds nothing. |

### 1.3 Execute phase

| Problem | Detail |
|---|---|
| **"Focus Active" pill + "FOCUS RUNNING" banner are fake** | `isActiveSession` is a local boolean that nothing sets. The banner shows whatever task happens to have the flag (usually none → "No active focus"). It's a nonsense widget. **You asked to remove it.** Agreed. |
| **No real timer** | `toggleTimer()` just flips `isTimerRunning`; `formattedTimer` counts a single global `elapsedSeconds`. It's not tied to a task, doesn't survive backgrounding, has no notification, no persistence, and isn't written to Notion Work Sessions. **You asked for a proper native per-task timer.** Agreed. |
| **The `My Day / Time / Energy / Location` chips do nothing** | Same as Plan — `selectedExecuteFilter` is stored and never read. Time/Energy/Location aren't even real task fields in the model. |
| **Doing / To Do / Done are just raw buckets** | No ordering, no "what should I do next", no drag. |

### 1.4 Wrap Up phase

| Problem | Detail |
|---|---|
| **"Journal Entry" button is dead** | `clickable { /* Open journal draft */ }` — no handler. |
| **"Clear My Day" works locally but not really** | It unsets `My Day` on every task in memory. It is **not** written to Notion (so it silently reverts on next sync), and there's no confirmation for a bulk action. |
| **"3-Step Evening Review" (Clear Day / Calendar / Tomorrow)** | Another mini tab strip with numbered markers. Step 2 "Calendar" shows nothing. Step 3 "Tomorrow" shows nothing. Only step 1 has content (the overdue queue). It's 90% empty chrome. |
| **"Reschedule all" / "+1 Day"** | Local-only, not synced, `dueDisplay = "Tomorrow"` is a cosmetic string change — the actual `due` date isn't moved. |
| **"Evening Ritual" eyebrow** | Same decorative chrome. |

### 1.5 Cross-cutting visual problems (why it reads as "AI-generated")

Matches the common generated-UI tells almost exactly:

- **Every section is an identical rounded card** — same 12–14dp radius, same
  1dp hairline `outlineVariant` border, same soft look, regardless of
  importance. No hierarchy.
- **ALL-CAPS eyebrow labels** above every block ("MORNING RITUAL",
  "3-STEP EVENING REVIEW", "FOCUS RUNNING").
- **Middle-dot meta strings** ("FOCUS RUNNING · Q3 launch — v2.0").
- **Everything is a chip or a pill.** Filters, statuses, counts, dates,
  the "My Day" tag — all the same chip primitive.
- **Numbered markers (1 / 2 / 3)** on content that isn't a strict sequence.
- **Icon soup** — most rows have 2–4 tiny 12–18dp icons competing.
- No single focal point. The eye has nowhere to land.

---

## 2. Structural change: real tabs + pager

Replace `RitualJumpBar` with:

- `PrimaryTabRow` (M3) pinned directly under the top app bar, 3 tabs:
  **Plan · Execute · Wrap up**.
- `HorizontalPager` for the content, sharing one `pagerState` with the tab
  row. Tap a tab or swipe left/right to move between phases.
- Tab label can carry a count where it helps (e.g. `Execute` shows the
  number of tasks in progress).
- The app opens on **Execute** during the day and could auto-select
  **Plan** in the morning / **Wrap up** in the evening (optional, later).

```
┌─────────────────────────────────────┐
│  ☰            My Day           🔍 ⚙ │   top app bar (small)
├─────────────────────────────────────┤
│   Plan        Execute        Wrap up│   PrimaryTabRow
│               ━━━━━━━               │   ← animated underline
├─────────────────────────────────────┤
│                                     │
│         « swipeable pager »         │
│                                     │
└─────────────────────────────────────┘
```

Reference: M3 guidance puts primary tabs directly under the top app bar;
`TabRow` is superseded by `PrimaryTabRow`; tab row + pager share
`pagerState`.

---

## 3. Focus timer (native, per-task)

### Behaviour
- Start a timer **from a specific task** (button on the task row / task
  detail). One timer at a time. Starting a new one stops the previous.
- Runs in a **foreground service** with an ongoing notification showing the
  task name + elapsed time and Pause / Stop actions. Survives app being
  backgrounded or killed.
- The Execute tab shows a compact **running-timer bar** *only when a timer
  is actually running* (this replaces the fake "FOCUS RUNNING" banner).
- Stop → writes a **Work Session** row to Notion (`Start`, `End`, `Tasks`
  relation) so time actually accrues on the task, matching the Python
  server's `format_work_session`. Also bumps the task to `Doing` if it
  wasn't.
- Optional Pomodoro mode later (25/5) — not v1.

### UX
- Big, quiet timer readout. Tabular figures. One primary action
  (Pause/Resume), Stop as secondary.
- Tap the running bar → jumps to that task.

Reference: mainstream pattern (TickTick, Focus To-Do) is "start a session
from any task, session logs against that task"; Android timers need a
foreground service + ongoing notification to be reliable.

---

## 4. Visual / UX redesign direction

Goal: looks like a considered product, not a component gallery. Grounded in
the daily-planner category (Sunsama's guided ritual, Things 3's calm
restraint, TickTick's today+timer).

### Principles
1. **One focal point per phase.** Plan → the shortlist you're building.
   Execute → the current task + timer. Wrap up → today's scorecard.
2. **Kill the card-for-everything pattern.** Use plain list rows on the
   page background. Reserve a card/elevation for the *one* thing that's
   special on that screen (the active timer; the day summary).
3. **Hierarchy through type and space, not borders.** 4/8-pt spacing
   system. Section headers are just medium-weight text with generous space
   above — no ALL-CAPS, no eyebrow, no pill.
4. **Fewer, bigger touch targets.** Task row = checkbox + title + one line
   of muted metadata (project · due). Drop the icon soup; show project as
   a small colored dot + name, due as plain text, priority only when high.
5. **Real empty states.** "Nothing planned yet — add your first task for
   today" with an action, not a blank card.
6. **Copy in the app's voice.** Buttons say what they do: "Add to today",
   "Start focus", "Move to tomorrow". Totodgermlinololo—no. Plain verbs.

### Palette / type
- Keep the existing indigo primary + entity colors (they're fine and
  already spec'd). Tighten usage to the 60/30/10 rule — most of the screen
  is background, ~30% surface for the few grouped elements, indigo only for
  the true primary action and the active-timer accent.
- Type scale: one family (the current M3 sans is fine). Establish a real
  scale — screen title / section header / row title / metadata — with
  distinct sizes and weights instead of everything at bodyMedium+SemiBold.

### Target layouts (rough)

**Plan** — "build today's shortlist"
```
Plan
Pick what you'll focus on today.

  Suggestions                            (from overdue + due today + flagged)
  ○ Review OAuth library PR        Migrate auth · Today
  ○ Write release notes            Q3 launch · Today
  ○ Send Q2 retrospective          overdue
  + Add a task

  On today  (3)                           ← the shortlist you've committed
  ● Cut release branch and tag     Doing
  ● Bottle clean                   habit
  ● Draft onboarding email

  [ Start focusing → ]                     ← jumps to Execute
```
No filter chips. If filtering is wanted later, it's a single overflow /
segmented control with *working* options (Today / This week / Inbox).

**Execute** — "do the thing"
```
        ┌───────────────────────────────┐
        │  Cut release branch and tag   │   ← active task card (the 1 card)
        │  Q3 launch                    │
        │        01:12:47               │   ← big tabular timer
        │     ⏸ Pause      ⏹ Stop        │
        └───────────────────────────────┘

  Up next
  ○ Write release notes            Q3 launch
  ○ Review OAuth library PR        Migrate auth
  ○ Bottle clean                   habit

  Done today  4                            ▸  (collapsed)
```
No Time/Energy/Location chips. "Up next" is My-Day tasks not yet done,
ordered (Doing first, then by due, then priority). Tap a row → start focus
on it (or open it).

**Wrap up** — "close the day"
```
Wrap up
Fri, Sep 6

  ✓ 4 done      ⏱ 2h 40m focused      3 still open

  Still open on today  (3)
  ○ Write release notes        → tomorrow   done
  ○ Draft onboarding email     → tomorrow   done
  ...
  [ Move all to tomorrow ]

  [ Write journal entry → ]                 ← opens the journal note editor
```
Drop the 3-step wizard. The "review" is just: see your numbers, deal with
what's left, optionally journal. "Clear my day" becomes "Move all to
tomorrow" (which actually reschedules + syncs) or is removed.

---

## DECISIONS (locked 2026-09-06)

- **Tabs:** `PrimaryTabRow` + `HorizontalPager`, "Plan / Execute / Wrap up",
  swipe enabled, open on Execute.
- **Plan filters:** implement them for real. Mapping:
  Today = due today; This week = due ≤7d; Overdue = past due; Recurring =
  `isRecurring`; Inbox = no project AND no due date; Active projects = task's
  project is not Done/archived. All not-done. Count badges reflect reality.
  Plus an "On today" shortlist (My Day picks) above the browse list.
- **Focus timer:** full — foreground service + ongoing notification
  (Pause/Stop), per-task, survives backgrounding; Stop writes a Notion Work
  Session (Start/End/Tasks) and sets the task to Doing.
- **Wrap up:** flat — scorecard + open items + journal. "Clear my day" →
  "Move all to tomorrow" (synced reschedule). "Journal entry" opens the Note
  editor as today's `Journal` note.
- **Sequencing:** one combined pass (structure + visual together).

## 5. Open decisions (superseded by DECISIONS above)

1. **Tabs** — confirm: `PrimaryTabRow` + `HorizontalPager`, labels
   "Plan / Execute / Wrap up", swipe enabled. Start on Execute?

2. **Plan filters** — remove entirely for v1 (Suggestions + On today), or
   keep a small *working* Today/Week/Inbox control?

3. **Focus timer scope for v1** —
   a) in-app timer + foreground-service notification + Work Session write, or
   b) start simpler: in-app + notification, defer the Notion Work Session write?

4. **Wrap up** — drop the 3-step wizard entirely and go to the flat
   "scorecard + open items + journal" layout? And "Clear my day" → replace
   with "Move all to tomorrow" (synced) or keep both?

5. **Journal Entry** — wire it to open the existing Note editor pre-filled
   as a `Journal`-type note for today? (Server already supports Journal
   notes.)

6. **Redesign depth** — do the full visual pass (de-card, retype, respace,
   new row component) as part of this, or land the structural/functional
   fixes first and do the visual pass as a second commit?

7. **Scope of "rest of the UI"** — after Today, which tab next: Tasks,
   Projects, or Notes?

---

## DELIVERED (2026-09-06)

**Shell** — `MyDayScreen` rebuilt on `PrimaryTabRow` + `HorizontalPager`
(Plan / Execute / Wrap up), swipeable, opens on Execute. `RitualJumpBar`,
`PlanSection`, `ExecuteSection`, `WrapUpSection` deleted.

**Plan** (`PlanPane`) — "On today" shortlist (remove-from-today per row) +
"Browse" with the six filters *actually implemented* (`MyDayUiState.planFilterMatches`
/ `planBrowseTasks` / `planFilterCount`), live count badges, sun/check toggle
per row to add/remove from today. "Start focusing" jumps to Execute.

**Execute** (`ExecutePane`) — the fake "Focus Running" banner is gone.
`FocusCard` (the one card) shows only while a session runs: task, project,
live tabular timer, Pause/Resume + Stop. "Up next" = ordered My-Day tasks
(Doing first) each with a play button. "Done today" collapsible.

**Focus timer** — `focus/FocusController` (process-wide session state),
`focus/FocusTimerService` (foreground service, type `specialUse`, ongoing
notification with Pause/Stop, ticks every 1s). `startFocus` sets the task to
Doing (+ Notion PATCH). `stopFocus` writes a Work Session row to Notion
(Start / End / Tasks) for sessions ≥ 60s, via `UbRepository.createWorkSession`.
`POST_NOTIFICATIONS` requested in `MainActivity`.

**Wrap up** (`WrapUpPane`) — flat: date, scorecard card (done / focused /
still open), "Still open" list with per-row and bulk "Move all to tomorrow"
(`moveAllOpenToTomorrow` — reschedules + un-My-Days + syncs `setTaskDue`),
"Write journal entry" → `openTodayJournal()` opens the Note editor as a
`Journal` note for today. The 3-step wizard is gone.

**Visual pass** — shared `TaskRow` (plain row on page bg, no per-item card,
one muted metadata line, project dot, HIGH-priority dot). `SectionHeader`
(plain text + count, no eyebrow/caps/pill). `TodayPaneParts` for
`EmptyLine` / `ThinDivider`. 20dp screen margin. Cards reserved for the
timer and the scorecard only.

**Verified on device:** all three tabs, swipe, filters + counts, add/remove
today, start focus → FGS + notification confirmed via dumpsys, Stop → Work
Session `POST /pages` 200, Wrap-up scorecard reflects focused minutes. Test
data (one task, one Work Session) reverted afterward.

### Not done / follow-ups
- Note persistence to Notion (journal note is local-only for now — same gap
  as the rest of Notes).
- `postponeOverdueTask` (per-row move-to-tomorrow) is still local-only; only
  the bulk "Move all" syncs.
- "Active projects" count shows 400 (the task-fetch cap) — cosmetic.
- Legacy VM members (`toggleTimer`, `elapsedSeconds`, `selectedPhase`,
  `eveningReviewStep`, `clearMyDay`, `rescheduleAllOverdue`, …) left in place;
  unused, safe to prune later.
