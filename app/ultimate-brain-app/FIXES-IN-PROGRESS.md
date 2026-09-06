# UI Audit Fix Progress Log

**Started:** 2026-09-06
**Branch:** hermes-agent
**Goal:** Walk through all 20 audit findings from `UI-AUDIT-REPORT.md`
**Commit policy:** NO commits — working tree only, this file is the audit trail.

---

## Round 1 — Visible defect fixes
**Status:** ✅ Code complete, compiles clean.

| Finding | File | Change | Verified |
|---|---|---|---|
| **#1** Broken top-bar layout on My Day | `MyDayScreen.kt` | Removed `RitualJumpBar` from `LargeTopAppBar.actions` slot. Re-rendered below the top bar as a sticky segmented strip. Wrapped the LazyColumn in a new outer `Column { RitualJumpBar; LazyColumn }`. Added `import androidx.compose.foundation.layout.Column`. | Compile clean. Visual: title now renders in the top bar, ritual strip sits directly below it. |
| **#3** Plan section `Inbox (0)` / `Overdue (0)` | `MyDayScreen.kt` (call site) + `PlanSection.kt` (already had params) | Passed `inboxCount = uiState.tasks.count { !it.isDone && !it.isMyDay && it.projectName == null }` and `overdueCount = uiState.overdueTasks.size` into `PlanSection`. | Compile clean. |
| **#10** Active focus banner hardcoded | `ExecuteSection.kt` + `MyDayScreen.kt` | Added `activeFocusTask: Task?` and `activeFocusProjectName: String?` params to `ExecuteSection`. Replaced hardcoded `"FOCUS RUNNING · Q3 launch"` + `"Cut release branch and tag"` with state-driven values; falls back to `"No active focus"` when null. `MyDayScreen` computes `activeFocusTask = uiState.tasks.firstOrNull { it.isActiveSession }` (remember-stable by state). | Compile clean. |
| **#13** WrapUp pluralization | `WrapUpSection.kt:235` | `"${overdueTasks.size} item"` → ternary `(1 item)` vs `(${size} items)`. | Compile clean. |

Compile check: `cd app/ultimate-brain-app && ./gradlew :app:compileDebugKotlin --no-daemon -q` — passed twice.

---

## Round 2 — Kill "looks frozen" perception
**Status:** ✅ Code complete, compiles clean.

| Finding | File | Change | Verified |
|---|---|---|---|
| **#4** Title style uniformity | `MoreHubScreen.kt:72` | `headlineSmall.copy(fontWeight = Bold)` → `titleLarge.copy(fontWeight = Bold)`. Goals/Tasks/Notes already use `titleLarge`. | Compile clean. |
| **#5** Filter-chip unification | `TasksScreen.kt:188-341` | Replaced 6 hand-rolled `Surface + Row + BorderStroke` chips with M3 `FilterChip` blocks (`primaryContainer` + `onPrimaryContainer` when selected, outlined when not). Pattern now matches the Goals / Notes / Projects chips. Added imports for `FilterChip` and `FilterChipDefaults`. | Compile clean. |
| **#6** FAB standardization | `GoalsScreen.kt:117-128`, `NotesScreen.kt:111-122`, `ProjectsScreen.kt:134-146` | Added explicit `elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)` and `padding(bottom = 12.dp)` to the three `ExtendedFloatingActionButton`s. MyDay's `FloatingActionButton` deliberately keeps `primary` + `onPrimary` + 6.dp — that's the home screen primary action, not a bug. | Compile clean. |
| **#7** Goals KPI | `GoalsScreen.kt:74-101, 189-201` | Derived `overallProgressPercent` from `goals.sumOf { completedMilestonesCount } / totalMilestonesCount`, plus `activeGoals`/`achievedGoals` from `count { it.status == ... }`. "51%" and "3 Active · 1 Achieved" now respond to state changes. | Compile clean. |
| **#7** MoreHub KPI | `MoreHubScreen.kt:65-79, 144-188` | Derived `activeGoalCount`, `achievedGoalCount`, `activeTagCount`, `activeMilestoneCount`, `milestonesTodayCount`, and `timerLabel` from `uiState`. Fixed `daysUntil` → `isToday` (latter is the real field on `MilestoneModel`). Falsy badges now suppress (`if (... > 0)`). | Compile clean. |
| **#7** Tasks "Project: Q3 launch" / "Target" | `TasksScreen.kt:90-115, 549` | Replaced hardcoded "Project: Q3 launch" with `projectChipLabel` derived from `topProject` (project with the most active tasks). Replaced "Target: Complete by 6 PM" with `todayTargetLabel` driven by `todayTimedTaskCount`. | Compile clean. |
| **#17** Tasks filter chip selection state | covered by **#5** | `selectedTasksFilter == "All Tasks"` etc. now selects via `FilterChip`'s `selected` param, eliminating the `BorderStroke`/container-color branching inconsistency. | Compile clean. |
| **#19** Bottom-nav "Today" tap | `MyDayScreen.kt:171-181` | Removed the `coroutineScope.launch { animateScrollToItem(0) }` branch — now always calls `viewModel.navigateTo(AppScreen.TODAY)` like the other four tabs. | Compile clean. |

Compile check: `./gradlew :app:compileDebugKotlin --no-daemon -q` from the
`D:/Projects/ultimate-brain-mcp/app/ultimate-brain-app` dir — clean exit, no output.

---

## Round 3 — Full i18n pass
**Status:** ✅ Code complete, compiles clean.

Findings in scope:
- **#2** Zero localization. ~150 strings to extract to `res/values/strings.xml` + route every `Text(...)` through `stringResource()`.
- **#6/ViewModel** Filter keys as English strings — convert `MyDayUiState.selectedPlanFilter`, `.selectedExecuteFilter`, `.selectedTasksFilter`, `.selectedProjectFilter`, `.selectedNoteFilter`, `.selectedGoalFilter`, `.selectedMilestoneFilter`, `.selectedTagFilter` to enums. Also wire to enum-keyed M3 `FilterChip`.

### Stage 2 — canonical strings.xml
- Added ~150 keyed strings in `res/values/strings.xml`, organized by screen. Includes shared keys (`status_doing`, `status_todo`, `status_done`, `priority_high/medium/low`, `value_none`, `action_search/sort/filter/back/more/favorite`) and per-screen keys (`goals_*`, `notes_*`, `projects_*`, `tasks_*`, `tags_*`, `milestones_*`, `more_hub_*`, `plan_*`, `execute_*`, `wrap_*`, `task_detail_*`).

### Stage 3a — ViewModel: filter enums + snackbar sealed class (`MyDayViewModel.kt`)
- Added 9 sealed enum classes with `@StringRes labelRes` constructor params:
  - `PlanFilter` (TODAY, ACTIVE_PROJECTS, INBOX, WEEK, OVERDUE, RECURRING)
  - `ExecuteFilter` (MY_DAY, TIME, ENERGY, LOCATION)
  - `TasksFilter` (ALL, TODAY, OVERDUE, MY_DAY, HIGH_PRIORITY, RECURRING)
  - `ProjectFilter` (ALL, STATUS_DOING, TAG_WORK, GOAL_Q3)
  - `NoteFilter` (ALL, MEETING, REFERENCE, IDEA, JOURNAL, BOOK)
  - `GoalFilter` (ACTIVE, ACHIEVED, DROPPED)
  - `MilestoneFilter` (ALL, IN_PROGRESS, COMPLETED, PENDING)
  - `TagFilter` (ALL, AREAS, RESOURCES, ENTITIES)
- Replaced `selectedXFilter: String` with `selectedXFilter: EnumType` everywhere on `MyDayUiState`.
- Replaced 9 `selectXFilter(filter: String)` setters with enum-typed signatures.
- Replaced all 16 `snackbarMessage = "literal"` assignments with `SnackbarMessage.X` sealed-class instances (carrying format-arg payloads where needed: `TaskStatusChanged(newStatus)`, `PriorityUpdated(newPriority)`, etc.).
- Added two resolvers above `clearSnackbar()`:
  - `resolveSnackbarMessageRes(msg: SnackbarMessage): Int` — resource-id lookup for tests.
  - `formatSnackbarMessage(context, msg): String` — context-resolving display resolver so VM stays unit-testable.

### Stage 3b — route screens + components through `stringResource()`
- **`PlanSection.kt`** — `selectedFilter: PlanFilter`, `onSelectFilter: (PlanFilter) -> Unit`. All 6 chip `onClick` use enum constants. All chips, titles, buttons through `stringResource`.
- **`ExecuteSection.kt`** — `selectedFilter: ExecuteFilter`, `onSelectFilter: (ExecuteFilter) -> Unit`. All 4 chip `onClick` use enum constants. "Execute", "Focus Active", "FOCUS RUNNING", "No active focus", "Doing · X", "To Do · X", "Done Today", "Hide"/"Show", "Habits" labels → `stringResource`. Pause/Resume/Complete task contentDescriptions → `stringResource`.
- **`MyDayScreen.kt`** — Added `val context = LocalContext.current`. `LaunchedEffect` snackbar now calls `snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))`. Title, avatar contentDescription, Search/Toggle theme cds, "Add new task" → `stringResource`.
- **`MoreHubScreen.kt`** — `timerLabel` uses `more_hub_timer_live_format` / `more_hub_timer_idle_format`. All item titles, subtitles, badges via `stringResource`.
- **`GoalsScreen.kt`** — Created `val filterOptions = listOf(GoalFilter.ACTIVE to R.string.goals_chip_active, GoalFilter.ACHIEVED to R.string.goals_chip_achieved, GoalFilter.DROPPED to R.string.goals_chip_dropped)`. Chip iteration: `filterOptions.forEach { (filter, labelRes) -> val isSelected = uiState.selectedGoalFilter == filter; onClick = { viewModel.selectGoalFilter(filter) } }`. All `Text`/`contentDescription` via `stringResource`.
- **`NotesScreen.kt`** — Same pattern. `NoteFilter.ALL` special-case for `notes_chip_all_format` with count.
- **`MilestonesScreen.kt`** — Same pattern. `MilestoneFilter.ALL` special-case with `milestones_chip_all_format` + count. Health label uses `milestones_health_value_format`.
- **`ProjectsScreen.kt`** — 4 chips use `ProjectFilter.ALL`, `STATUS_DOING`, `TAG_WORK`, `GOAL_Q3`. All titles, subtitles, badges, "Completed"/"Progress", "Goal: X", tasks count, "Archived" → `stringResource`.
- **`TagsScreen.kt`** — `val tabs = listOf(TagFilter.ALL to R.string.tags_tab_all, TagFilter.AREAS to ..., RESOURCES to ..., ENTITIES to ...)`. Chip iteration uses enum comparison. `TagItemCard` `items_count_format`, `projects_notes_format`, `children_items_format`, favorite cd → `stringResource`.
- **`TasksScreen.kt`** — All 6 FilterChips migrated to enum (`ALL`, `MY_DAY`, `HIGH_PRIORITY`, `OVERDUE`, `TODAY`). All `Text`/`contentDescription` via `stringResource`. Snackbar `LaunchedEffect` now calls `viewModel.formatSnackbarMessage(context, msg)`.
- **`TaskDetailScreen.kt`** — Snackbar `LaunchedEffect` now calls `viewModel.formatSnackbarMessage(context, msg)`. All top bar / status pill / priority pill / dropdown / sub-tasks / notes / dialog literals → `stringResource`. Priority label resolved via `when (task.priority) { Priority.HIGH -> stringResource(R.string.priority_high); ... }` instead of name sniffing.

Compile check: `./gradlew :app:compileDebugKotlin --no-daemon -q` from `D:/Projects/ultimate-brain-mcp/app/ultimate-brain-app` — clean exit, no output.

---

## Round 4 — Theme bridge + theme polish
**Status:** ✅ Code complete, compiles clean.

| Finding | File | Change | Verified |
|---|---|---|---|
| **#6** Establish theme bridge | `theme/ColorSchemeExtensions.kt` (new) + `theme/Color.kt` | Added 13 `ColorScheme` extension properties: `entityProjects`, `entityTasks`, `entityGoals`, `entityNotes`, `entityTagArea`, `entityTagResource`, `entityTagEntity`, `success`, `successContainer`, `onSuccessContainer`, `warning`, `warningContainer`, `onWarningContainer`, `errorAccent`. Each resolves to its light or dark variant via a luminance check on the active `surface` (`isDark = surface.luminance() < 0.5f`). | Compile clean. |
| **#6** Migrate screen call sites | All `ui/screens/*.kt` + `ui/components/*.kt` | Replaced direct references to `EntityProjects`/`SuccessGreen`/`ErrorContainerLight`/`WarningAmber`/`Color.White` and the equivalent dark variants with `MaterialTheme.colorScheme.X` (using lowercase extension imports). Removed unused imports of `androidx.compose.ui.graphics.Color` once they were orphaned. Notable swaps: `Color.White` on success → `onSuccessContainer`; `Color.White` on primary → `onPrimary`; `Color.White` on ErrorRed pill → `onErrorContainer`. Covered: `MyDayScreen`, `HeaderSection`, `WorkSessionsScreen`, `SearchDialog`, `PlanSection`, `WrapUpSection`, `QuickAddBottomSheet`, `ExecuteSection`, `TasksScreen`, `TaskDetailScreen`, `GoalsScreen`, `GoalDetailScreen`, `NotesScreen`, `NoteDetailScreen`, `NoteEditorScreen`, `NotesScreen` (`NoteCard`), `ProjectsScreen`, `ProjectDetailScreen`, `TagDetailScreen`, `TagsScreen`, `GlobalSearchScreen`, `MilestonesScreen`, `EditProjectScreen`, `MoreHubScreen`, `SettingsScreen`. | Compile clean. |
| **#11** NoteTypeBadge hardcoded literals | `NotesScreen.kt:313-335` + new `theme/ColorSchemeExtensions.kt` helper | Added 5 note-type container/onContainer pairs to `Color.kt` (Meeting/Reference/Idea/Journal/Book), each with explicit light + dark variants (dark containers are tinted, not diluted). Added `noteTypeColors(type: String, scheme: ColorScheme): NoteTypeColors` helper that returns the right pair for the active scheme. Rewrote `NoteTypeBadge` body to call the helper. | Compile clean. |
| **#12** inverseSurface tokens | `theme/Theme.kt:36-39, 73-75` | Added `inverseSurface = Color(0xFF283044)` and `inverseOnSurface = Color(0xFFEEF0FF)` to BOTH light and dark `ColorScheme` blocks (intentional — code tiles stay on a navy chip regardless of theme so monospace text always contrasts). Migrated `TaskDetailScreen.kt` code-block Surface from the hardcoded literals to `MaterialTheme.colorScheme.inverseSurface` / `inverseOnSurface`. | Compile clean. |
| **#14** Spec color drift fix | `theme/Color.kt:52-57` | `EntityTasks = 0xFF3B82F6` (was 0xFF0284C7 — blue 600, off-spec). `EntityTagArea = 0xFF14B8A6` (was 0xFF0D9488 — teal 600, off-spec). `EntityTagResource = 0xFF06B6D4` (was 0xFF3B82F6 — blue 500, collided with the wrong Tasks color). Added inline comments naming the spec source. | Compile clean. |
| **#16** Wire dynamicColor toggle | `MyDayViewModel.kt` + `MainActivity.kt` + `SettingsScreen.kt` + `theme/Theme.kt` | Added `dynamicColorEnabled: Boolean = false` to `MyDayUiState` plus `setDynamicColorEnabled(enabled: Boolean)` setter. `MainActivity` now passes `dynamicColor = uiState.dynamicColorEnabled` to `MyApplicationTheme`. `SettingsScreen`'s toggle was a static `remember { mutableStateOf(false) }` — replaced with `val uiState by viewModel.uiState.collectAsState()` and `Switch(checked = uiState.dynamicColorEnabled, onCheckedChange = { viewModel.setDynamicColorEnabled(it) })`. Removed `FloatingActionButton` parameter doc comment drift caught in passing. | Compile clean. |

Compile check: `./gradlew :app:compileDebugKotlin --no-daemon -q` from `D:/Projects/ultimate-brain-mcp/app/ultimate-brain-app` — clean exit, no output.

### Notes on extension naming
- Did NOT create an `errorContainer` / `onErrorContainer` extension — those names shadow Material 3's built-in `ColorScheme.errorContainer` / `onErrorContainer` tokens. Call sites use `MaterialTheme.colorScheme.errorContainer` (M3 built-in, already wired in `Theme.kt`).
- Did create `errorAccent` extension (using `ErrorRed` / `ErrorRedDark`) — different intent ("severity dot / stripe" vs "destructive action button"), safer than shadowing M3.
- `success`, `warning`, and family DO NOT shadow M3 tokens (Material 3 has no `success`/`warning` built-ins), so the extensions are named with the family prefix for clarity (`success`, `successContainer`, `onSuccessContainer`, `warning`, `warningContainer`, `onWarningContainer`).

---

## Round 5 — Structural cleanup
**Status:** ✅ Code complete, compiles clean.

| Finding | File | Change | Verified |
|---|---|---|---|
| **#12 (dead code)** HeaderSection.kt | `app/src/main/java/com/example/ui/components/HeaderSection.kt` (deleted) | File had no remaining call sites — `MyDayScreen` already inlined the avatar box into `LargeTopAppBar.navigationIcon` in Round 1. The only remaining reference in source was a comment in `MyDayScreen.kt:114` ("Profile avatar (replaces HeaderSection avatar block)"). Deleted via `rm`. | Compile clean. |
| **#12 (dead code)** EntityTasks import | `TasksScreen.kt:79` | `import com.example.ui.theme.entityTasks` was orphaned (Round 4 kept it "in case" but the file never used it). Dropped. | Compile clean. |
| **#15** Navigation-intent event stream | `MyDayViewModel.kt` + `MainActivity.kt` | Added `sealed class NavIntent { data class Navigate(val screen: AppScreen) : NavIntent() }`. Added `private val _navigationEvents = Channel<NavIntent>(Channel.BUFFERED)` and exposed `val navigationEvents: Flow<NavIntent> = _navigationEvents.receiveAsFlow()`. Updated `navigateTo()` to emit `NavIntent.Navigate(screen)` via `trySend` (in addition to keeping the `currentScreen` UiState field for read-only consumers). `MainActivity` now uses `LaunchedEffect(Unit) { viewModel.navigationEvents.collect { intent -> ... } }` instead of `LaunchedEffect(uiState.currentScreen)`. The `LaunchedEffect(Unit)` key guarantees the collector runs exactly once for the lifetime of the composition; the event stream decouples navigation from incidental UiState mutations. | Compile clean. |
| **#18** GoalsScreen ArrowBack removal | `GoalsScreen.kt` | Removed the entire `navigationIcon = { IconButton(...) { Icon(Icons.AutoMirrored.Filled.ArrowBack, ...) } }` block from `MediumTopAppBar`. The bottom nav still handles tab switching; the back arrow was redundant. Also dropped the now-unused `androidx.compose.material.icons.automirrored.filled.ArrowBack` import. The `action_back` string resource remains in `strings.xml` for any other screen that still uses it. | Compile clean. |
| **#20** Trailing Spacer standardization | `TasksScreen.kt:619` + `TaskDetailScreen.kt:1054` | `Spacer(modifier = Modifier.height(72.dp))` → `Spacer(modifier = Modifier.height(80.dp))`. Fixed both spots — `TaskDetailScreen` had the same off-by-one inconsistency as `TasksScreen` (audit specified only `TasksScreen:624` but the trailing spacer pattern is system-wide). Grep confirmed no other `Spacer(...height(7X.dp))` remains. All list-bottom trailing spacers now use 80.dp. | Compile clean. |

Compile check: `./gradlew :app:compileDebugKotlin --no-daemon -q` from `D:/Projects/ultimate-brain-mcp/app/ultimate-brain-app` — clean exit, no output.

### Notes
- `#15` keeps `currentScreen` on `MyDayUiState` (and `navigateTo` still updates it) because other places in the codebase may still read it as a breadcrumb / state-introspection field. The new flow is the navigation *trigger*; the UiState field remains the navigation *state*. If we wanted to drop the field entirely we could do that in a follow-up audit round, but it would ripple to every `state.copy(currentScreen = X)` site.
- `#15` uses `Channel.BUFFERED` rather than `UNLIMITED` so a misbehaving collector (e.g. one that stops collecting) eventually gets backpressure rather than unbounded memory growth. `trySend` never suspends.
- `#18` removed `IconButton(onClick = { viewModel.navigateTo(AppScreen.MORE_HUB) })` because that handler pointed at the wrong destination — Goals is a primary tab, not a sub-screen of More Hub. The bottom-nav `MORE` tab handles the navigation properly.
- `#20` extended beyond the audit's explicit `TasksScreen.kt:624` call-out to fix `TaskDetailScreen.kt:1054` (same off-by-one inconsistency). Per the user's instruction: "Grep for `Spacer(modifier = Modifier.height(72.dp))` — fix any other 72.dp → 80.dp inconsistencies."

---

## Out-of-scope reminders (not in the audit but noted)
- Two-package install-state confusion (`com.aistudio.ultimatebrain.ptvz` vs `com.ultimatebrain.debug`) — needs `applicationId` lock in `app/build.gradle.kts`. Not in audit but referenced. Flagged.
- Build sheet for the dynamic dark/light M3 spec token mapping if needed in Round 4.

---

## Round 6 — Headless screen crawler (tooling, not a code change)
**Status:** ✅ Tooling live, smoke test pass. App-side code untouched.

### Motivation
Rounds 1–5 covered static-defect findings (layout bugs, hardcoded text, theme-token drift, dead imports). What they can't catch is **runtime visual ugliness** — clipping, overlap, off-screen elements, color contrast under real fonts/animations. The user asked for a way to walk every reachable screen and dump screenshots so they can flip through and flag the ugly ones.

### Tool
Single-file Python 3 script at `app/ultimate-brain-app/crawl/crawl.py`. Stdlib-only (no `requirements.txt`). Lives outside `app/src/`, so Gradle ignores it and it doesn't pollute the build.

- BFS over clickable nodes parsed from `uiautomator dump`.
- `adb exec-out screencap -p` for capture (Windows-safe; avoids the `\n → \r\n` PNG corruption that bites `adb shell screencap`).
- Hash dedup on PNG content so the same screen in different states collapses to one file.
- Tap-safety: skips destructive labels (`delete`/`archive`/`cancel`/etc.), EditText nodes, FABs in the bottom-right unless labelled `add`/`new`/`search`/`create`/`compose`/`filter`.
- Status-bar-region filter (top 96 px) — earlier smoke runs were tapping the system bar and bouncing to the launcher; this was the root cause.
- Forward-only BFS — earlier draft issued KEYCODE_BACK after every tap, which popped out of My Day to the launcher because routine taps (ritual-jump-bar) don't change the visible state. Replaced with: capture after tap, dedupe by hash, only reset on focus loss.
- Slug derivation: combines the first 4 visible text nodes with the last tap's label so each captured state has a meaningfully distinct filename (`009_complete_task_cut_release.png`).
- Manifest JSON with per-capture `{index, file, hash, first_text_nodes, tap_path}` provenance so any ugly screen can be retraced.

### Smoke-test result (cap 10)
10 distinct captures in ~70s. Files in `crawl/out/screenshots/`. Manifest at `crawl/out/manifest.json`. No focus-loss warnings. Hash dedup confirmed working (`state hash 50c89dab already visited; skipping capture` in logs).

### Visual issues spotted from first 10 captures (audit input for future rounds)
- `Recurring` filter chip clipped to `Recurr…` on the Plan section's chip row — horizontal overflow, likely needs `Row` scroll or wrap.
- The 3-step evening-review cards are very faint — possibly needs more surface contrast.

### Usage
```bash
cd app/ultimate-brain-app/crawl
python crawl.py --max-screens 10 --verbose   # smoke
python crawl.py --max-screens 80             # full
```
Output lands in `crawl/out/`. Already gitignored.

### Out of scope
- App-side code changes — none made in this round.
- Tapping into text fields and typing — out of scope (not needed for screen-by-screen capture).
- Swipe gestures / long-press — out of scope (all 18 screens reachable via tap).
- Visual diff against baseline — separate audit, not asked for here.

---

## Round 7 — My Day native pass + navigation regression fix
**Status:** ✅ Code complete, `assembleDebug` clean, verified on device (Moto `5XVCMNUSFAQ4KN4T`).

### 7a — My Day / QuickAdd visual pass (from `crawl/out` screenshots)
| Area | File | Change |
|---|---|---|
| RitualJumpBar was a hand-rolled Box+clickable pill strip with a double 16dp horizontal inset (caller + composable both padded). | `ui/components/RitualJumpBar.kt` | Rewrote on M3 `SingleChoiceSegmentedButtonRow` + `SegmentedButton`. Caller owns outer padding; composable adds none. Genuinely native segmented control. |
| Ritual tabs didn't switch content — they `animateScrollToItem(max*0.5f)` through one mega-list, landing mid-item and leaving a clipped card sliver (`⋮ ⋮`) above every "Execute"/"Wrap Up" view. | `ui/screens/MyDayScreen.kt` | Body is now `when (selectedPhase) { PLAN/EXECUTE/WRAP_UP -> section }` inside a single `LazyColumn` item. One section on screen at a time, no scroll-jump, no slivers. |
| `LargeTopAppBar` left a big dead band between the icon row and the "My Day" title. | `ui/screens/MyDayScreen.kt` | Switched to small `TopAppBar` (`titleLarge` bold) + `pinnedScrollBehavior`. Tighter, matches the other 5 screens (audit Finding 4). |
| QuickAdd attribute row: a 160dp `OutlinedTextField` "Project" sat among short hand-rolled `Surface` pills of a different height; "Set Time" clipped at the edge. | `ui/components/QuickAddBottomSheet.kt` | All six controls are now native M3 chips (`FilterChip` for Due/Project/Priority/My Day, `AssistChip` for Tag/Set Time). Project chip anchors a `DropdownMenu`. One consistent control set. |
| FAB overlapped the last list card. | `ui/screens/TasksScreen.kt`, `MyDayScreen.kt` | Trailing spacer 80/96 → 96/120dp. |

### 7b — List → detail navigation regression (user-reported)
**Symptom:** tapping any item in a listing (Tasks, Projects, Notes, Goals, Tags) did nothing — the detail screen never opened.

**Root cause:** Round 5 (Finding 15) replaced `LaunchedEffect(uiState.currentScreen)` in `MainActivity` with `LaunchedEffect(Unit) { navigationEvents.collect { … } }`. Only `navigateTo()` emits a `NavIntent`; the detail-open functions (`openTaskDetail`, `openProjectDetail`, `openNoteDetail`, `openNoteEditor`, `openGoalDetail`, `openTagDetail`, `openEditProject`) plus post-save redirects (`saveProject`, `archiveProject`, `saveNote`, `createNewNote`, `createNewGoal`, `dropGoal`, `achieveGoal`) only mutated `currentScreen` on `MyDayUiState` and never emitted an intent — so the NavHost back stack never moved.

**Fix:** `MyDayViewModel.kt` — added `private fun emitNav(screen)` and call it after every `_uiState.update {}` that sets `currentScreen`. 14 call sites. Verified on device: Tasks→task detail and Projects→project detail both open.

Compile check: `./gradlew :app:assembleDebug --no-daemon -q` — clean, APK installs and runs.

---

## Methodology notes

- Inline edits, no sub-agent dispatch needed for Round 1 (small scope, full ground truth in context).
- Compile gate between rounds via `./gradlew :app:compileDebugKotlin --no-daemon -q`.
- Atomic-commit policy: skipped per user instruction.
- Files NOT under git in this branch (`app/ultimate-brain-app/...` is untracked working tree) — git status is not a progress signal here, only this file + compile are.
