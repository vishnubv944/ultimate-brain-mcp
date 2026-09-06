# App-wide cleanup — findings & plan

**Goal:** carry the new Today-tab visual language and quality bar to every
screen. The same three classes of problem the Today tab had (card-soup UI,
dead controls, local-only / fake data) recur across the app; this is the
inventory before we start fixing.

**Status:** exploration complete. Executing.

### Decisions (2026-09-06)
- **Fictional fields:** DELETE. `Task.acceptanceCriteria`, `Task.codeSnippet`,
  `Note.attendees/agenda/noteBullets/decisions/actionItems` come out of the
  model and UI. Detail screens show real properties + the real Notion page
  body (fetched via the markdown endpoint).
- **Secondary DBs:** IN SCOPE. People / Books / Reading Log / Recipes / Meal
  Planner get basic list + detail screens on the shared kit — added as
  **Batch 6**.
- **Cadence:** work through all batches; build + on-device screenshot +
  commit after each.

---

## A. The shared visual system (build once, apply everywhere)

The Today rebuild produced reusable pieces. These become the app-wide kit:

| Piece | File | Use everywhere for |
|---|---|---|
| `TaskRow` | `ui/components/TaskRow.kt` | every task list row (Tasks, project detail, goal detail, search) |
| `SectionHeader` | `ui/components/TodayPaneParts.kt` | every "Section (N)" header — plain text + count, no eyebrow/caps/pill |
| `EmptyLine` / `ThinDivider` | same | empty states, row separators |
| `TodayPad` (20dp) | same | screen horizontal margin |
| paging kit | `ui/components/Paging.kt` | any list that can grow long |

**New pieces we still need:**
- `EntityRow` — the generalised row for non-task items (project / note / goal
  / tag / milestone / work session): leading icon-or-dot, title, one muted
  metadata line, optional trailing. Same shape as `TaskRow`.
- `ScreenScaffold` — one wrapper: small `TopAppBar` (title `titleLarge` bold),
  standard back handling, bottom nav where relevant, FAB slot, snackbar host.
  Kills the current per-screen top-bar drift.
- `DetailScaffold` — for detail screens: back arrow, title, overflow menu,
  a scroll body with `SectionHeader`-delimited sections (no card per section).
- `StatCard` — the one-card summary block (already exists inline in
  Wrap-up / project detail; extract it).
- `SegmentedFilter` — a single scrollable `FilterChip` row helper with count
  badges (Tasks/Projects/Notes/Goals/Tags all hand-roll this today).

### The rules (from the Today pass)
1. No card per list item. Plain rows on the page background, `ThinDivider`
   between them. A card is reserved for *one* summary/hero element per screen.
2. Section headers are text + count. No ALL-CAPS eyebrows, no pills, no
   "MORNING RITUAL" chrome.
3. One metadata line per row, muted, `·`-joined. Project = colored dot +
   name. Priority = a dot only when High. No 3–4 icon clusters.
4. Hierarchy from type scale + spacing (8pt), not borders.
5. Real empty states — a sentence + the action, never a blank card.
6. Copy: plain verbs, sentence case. Buttons say what they do.
7. Every visible control does something, or it isn't there.

---

## B. Per-screen findings

### Tasks (`TasksScreen.kt`)
- **Visual:** `TaskRowItem` is a mini-card with pills; 14 Surfaces. Overdue
  section wraps each row in its own error-tinted `Surface`. Filter row is M3
  `FilterChip` (good) but overflows and `+ Add filter` is dead.
- **Dead:** Sort icon (`/* Sort options */`), Filter icon (`/* Filter
  options */`), `+ Add filter` chip (`/* Add filter dialog */`).
- **Data:** `Target: N time-block` label and the "Project: X" chip are
  derived now (earlier fix) — OK. Filter chips (`Status: Doing (3)` etc.)
  are partly hardcoded label text.
- **Fix:** re-skin on `TaskRow` + `SectionHeader`; make the 3 real filters
  (All / My Day / Overdue / Today / High / Recurring) the only chips, drop
  the fake ones; wire or remove Sort.

### Projects (`ProjectsScreen.kt`)
- **Visual:** `ProjectCard` = `Surface(16dp radius, border)` per item with a
  nested goal `Surface`, status pill, progress bar, tag chips, folder icon.
  Full card-soup.
- **Dead:** Sort icon, More icon, filter-bar `clickable { /* open filter
  dialog */ }`.
- **Data:** task counts + progress derived from real data now (Notion
  integration) — OK. `ProjectFilter` chips (`STATUS_DOING`, `TAG_WORK`,
  `GOAL_Q3`) are **hardcoded to demo values** — `GOAL_Q3` filters on
  `goalName.contains("v2.0")`, `TAG_WORK` on `tags.contains("#Work")`. Broken
  for this workspace.
- **Fix:** `EntityRow` (folder dot + name + "N tasks · goal · deadline" +
  progress as a thin inline bar or % text). Replace the 3 fake filters with
  real ones (All / Active / status facets from live data).

### Notes (`NotesScreen.kt`)
- **Visual:** `NoteCard` per item + `NoteTypeBadge` (colored pill). 5
  Surfaces. Grid/List toggle present.
- **Dead:** Grid/List toggle (`/* Grid/List toggle */`), More icon.
- **Data:** `filteredNotes` IS wired to `selectedNoteFilter` — filters work.
  Note **content is never loaded** (excerpt/rawMarkdown empty from Notion).
- **Writes:** `createNewNote` and `saveNote` are **local-only** — no
  `repo.createNote` / `repo.saveNote`. New notes vanish on next sync.
- **Fix:** `EntityRow` (type dot + title + "type · project · date"). Wire
  note create/save to Notion. Fetch note body (markdown endpoint).

### Goals (`GoalsScreen.kt`)
- **Visual:** `GoalCard` per item, progress ring/bar, "N Active · M
  Achieved" summary. 7 Surfaces.
- **Dead:** More icon.
- **Data:** `filteredGoals` wired — filters work. Aggregated progress is
  derived from linked projects now.
- **Writes:** `createNewGoal` **local-only** (no `repo.createGoal`).
  `dropGoal`/`achieveGoal` sync (status). Goal name/deadline edits: no path.
- **Fix:** `EntityRow` + one `StatCard` summary. Wire goal create.

### Milestones (`MilestonesScreen.kt`)
- **Visual:** card per milestone, health bar, goal badge.
- **Dead:** More icon, `+ create milestone` FAB (`/* create milestone */`).
- **Data:** **Milestones are never loaded from Notion** — 100% DummyData.
  `toggleMilestoneStatus` is local-only.
- **Fix:** wire the Milestones data source into `UbRepository.loadWorkspace`
  (+ mapper), re-skin, wire create + status toggle.

### Tags (`TagsScreen.kt`)
- **Visual:** card per tag, PARA type tabs, hierarchy children.
- **Dead:** More icon, `+ add tag` FAB (`/* add tag */`).
- **Data:** tags load from Notion (read only). `filteredTags` wired.
  Children/counts are DummyData-shaped (not populated from Notion).
- **Writes:** none — create/edit/favorite tag all absent.
- **Fix:** `EntityRow` (type dot + name + "N items"). Wire favorite at
  least; create tag optional.

### Tag detail (`TagDetailScreen.kt`)
- **Dead:** favorite toggle (`/* favorite toggle */`), More.
- **Data:** shows DummyData relations.
- **Fix:** real linked tasks/projects/notes (filter live data by tag id),
  wire favorite.

### Work Sessions (`WorkSessionsScreen.kt`)
- **Visual:** `"FOCUS SESSION LIVE"` / `"PAUSED"` ALL-CAPS, card per session,
  hardcoded `"Q3 launch — v2.0"`.
- **Dead:** More icon, `+ log manual session` FAB.
- **Data:** **never loaded from Notion** — DummyData. `toggleWorkSession` is
  the old fake timer (superseded by the real focus timer).
- **Fix:** load Work Sessions from Notion (we already *write* them via the
  focus timer — now read them back); show real session history + today's
  total; drop the fake live-session UI (the focus timer owns that now).

### Task detail (`TaskDetailScreen.kt`) — 1093 lines, the worst offender
- **Visual:** every field is its own bordered `Surface` block — Schedule,
  Project, Labels, Taxonomy Area, Repeat, Sub-tasks, Notes, Acceptance
  Criteria. Card-soup at maximum.
- **Dead:** Share, More, "Edit time" (`/* Edit time */`), "Navigate to
  project" (`/* Navigate to project */` — should open project detail!),
  "Add label" (`/* Add label */`), "Repeat sheet" (`/* Repeat sheet */`),
  "Subtask menu", "Edit markdown" (`/* Edit markdown */`).
- **Fake:** `implementationNotes` / `codeSnippet` / `acceptanceCriteria` are
  **DummyData-only fields** — the mapper never sets them, so every real task
  shows the same fake "Ensure production smoke builds..." note and a fake
  checklist. The real Notion page body is never fetched.
- **Writes that sync:** status, priority(?), My Day. **Local-only:**
  sub-tasks, acceptance criteria, labels, schedule, repeat.
- **Fix:** rebuild as a `DetailScaffold` — title, status + priority + My Day
  as a compact control row, then plain `SectionHeader` sections for
  Schedule / Project (tappable → project detail) / Labels / Repeat /
  Sub-tasks / Notes. Fetch the real page body (markdown). Delete the fake
  acceptance-criteria concept. Wire label + schedule + repeat edits to
  Notion, or make them read-only until wired (no dead affordances).

### Project detail (`ProjectDetailScreen.kt`)
- **Dead:** More icon.
- **Visual:** card sections (progress card, tasks card, notes card).
- **Data:** tasks/notes filtered from live data — OK. Progress card derived.
- **Fix:** `DetailScaffold` + `TaskRow` for the task list; `EntityRow` for
  notes; one `StatCard` for progress.

### Note detail / Note editor (`NoteDetailScreen.kt`, `NoteEditorScreen.kt`)
- **Data:** `attendees / agenda / noteBullets / decisions / actionItems` are
  **DummyData-only** — real notes have none of these, so detail shows just
  `rawMarkdown` (which is also empty from Notion). Editor edits are
  local-only.
- **Fix:** fetch note body markdown; render it; editor writes back to Notion.
  Drop the fake structured-meeting-note fields.

### Goal detail (`GoalDetailScreen.kt`)
- **Dead:** "edit goal", More.
- **Data:** linked projects from live data — OK.
- **Fix:** `DetailScaffold`, wire edit or remove the affordance.

### Global Search (`GlobalSearchScreen.kt`)
- **Dead:** mic icon.
- **Visual:** result cards. Works functionally (searches live data).
- **Fix:** `TaskRow` / `EntityRow` for results, `SectionHeader` per type.

### More hub (`MoreHubScreen.kt`)
- **Visual:** 3 grouped `Surface` cards, big rows with icon tiles + badges.
- **Data:** badges (`3 Active`, `9 Tags`, timer label) derived now — OK.
- **Fix:** lighter — plain rows with `SectionHeader` groups, keep the icon
  tiles small. Mostly fine, just de-card.

### Settings (`SettingsScreen.kt`)
- **Fake:** `"Priya Sharma"`, `"priya@company.internal"`, `"PRO"` badge,
  `"Workspace: Priya — Work (PARA Hub v2.4)"`, `"Last synced 2 minutes
  ago"`, `"Up to date"` — all hardcoded.
- **Dead:** sync button (`/* sync */`), clear-cache button (`/* clear cache
  */`).
- **Working:** dynamic-color toggle (wired in an earlier round).
- **Fix:** show the real Notion connection state (`NotionConfig.isConfigured`,
  last `refreshFromNotion` time, task/project counts). Wire the sync button
  to `refreshFromNotion()`. Remove the fake account block or show real
  build/version info. Keep it a plain settings list.

### Quick Add sheet (`QuickAddBottomSheet.kt`)
- **Dead:** Voice input, Tag selector, Set Time, keyboard toggle,
  attachment, subtasks — all `/* ... */`.
- **Fix:** keep Due / Project / Priority / My Day (working). Remove the dead
  icons, or wire Tag + Time (both are real Notion fields).

---

## C. Systemic issues

### C1. Write-sync gaps (mutations that never reach Notion)
`updateTaskPriority`, `toggleSubTask`, `addSubTask`, `clearMyDay`,
`postponeOverdueTask` (single), `rescheduleAllOverdue`, `saveProject`,
`createNewNote`, `saveNote`, `toggleNoteActionItem`, `createNewGoal`,
`toggleMilestoneStatus`, `toggleWorkSession`.
→ Add the matching `UbRepository` methods (`setTaskPriority`, `updateProject`,
`createNote`, `updateNoteBody`, `createGoal`, `setMilestoneStatus`, …) and
call them from the VM, same optimistic-then-`remoteWrite` pattern already in
place.

### C2. Entities not loaded from Notion
- **Milestones** — screen is 100% DummyData.
- **Work Sessions** — screen is DummyData (but we now *write* sessions).
- **People / Books / Reading Log / Recipes / Meal Planner** — data sources
  are in `.env` / `SECONDARY_DB_ENV_MAP` but no screens or repo wiring.
  (Out of scope unless you want them.)

### C3. Page body content is never fetched
Task notes, note bodies, project descriptions — all show DummyData or empty.
Notion's markdown endpoint (`GET /pages/{id}/markdown`, version 2026-03-11)
is what the Python server uses. Add `NotionClient.getPageMarkdown` +
`replacePageMarkdown` and wire the detail/editor screens.

### C4. Fictional data model fields
`Task.acceptanceCriteria`, `Task.codeSnippet`, `Task.implementationNotes`
(as distinct from body), `Note.attendees/agenda/noteBullets/decisions/
actionItems`, `Task.timeBlock` string — none map to Notion. Either derive
them from the page body or delete them. They're the reason detail screens
look "designed for a demo".

### C5. Top-bar / scaffold drift
Every screen hand-rolls its `TopAppBar` with slightly different title styles,
colors, and back-button wiring (audit Finding 4, still true on the
not-yet-touched screens). One `ScreenScaffold` / `DetailScaffold` fixes it.

### C6. `formatSnackbarMessage` / dead VM members
`toggleTimer`, `elapsedSeconds`, `isTimerRunning`, `formattedTimer`,
`selectedPhase`, `selectExecuteFilter`, `eveningReviewStep`, `setEveningStep`,
`endLiveSession`, `clearMyDay`, `rescheduleAllOverdue`, `toggleWorkSession`,
`DailyRitualPhase` — dead after the Today rebuild. Prune.

---

## D. Proposed order

**Batch 1 — the kit + the two list-heavy tabs**
`ScreenScaffold` / `DetailScaffold` / `EntityRow` / `SegmentedFilter` /
`StatCard`, then re-skin **Tasks** and **Projects** on them. Wire/remove
their dead controls. Fix the fake Project filters.

**Batch 2 — Notes + Goals + their detail screens**
Re-skin Notes, Goals, Note detail, Goal detail, Project detail. Add
`repo.createNote` / `saveNote` / `createGoal` and the page-markdown fetch so
note/goal content is real.

**Batch 3 — Task detail**
The big one. `DetailScaffold` rebuild, real page body, delete acceptance
criteria, wire label/schedule/repeat or make them read-only.

**Batch 4 — Milestones + Work Sessions + Tags**
Wire Milestones and Work Sessions into `loadWorkspace` (+ mappers), re-skin.
Tags read + favorite. Work Sessions shows real focus history.

**Batch 5 — More hub, Settings, Global Search, Quick Add**
De-card, real connection state in Settings, wire the sync button, strip the
dead Quick-Add icons. Prune dead VM members.

Each batch: build + on-device screenshot + commit.
