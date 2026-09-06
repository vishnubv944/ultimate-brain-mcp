# Ultimate Brain Android — UI Audit Report

**Date:** 2026-09-06
**Scope:** `D:\Projects\ultimate-brain-mcp\app\ultimate-brain-app\app`
**Author:** Automated audit (4 parallel agents — theme ×2, structure/i18n, spec-comparison) + on-device capture against Moto `5XVCMNUSFAQ4KN4T`

This report consolidates everything wrong with the current Ultimate Brain Android build, what the user sees on device, and the recommended fix order.

---

## 1. Executive summary

The current Android app (`com.aistudio.ultimatebrain.ptvz`, launcher `com.example.MainActivity`) is closer to the Stitch spec than the user's intuition suggests — entity colors, primary indigo, and surface tonal elevation all resolve correctly in the theme. The user's "UI is wrong" complaint collapses into roughly **three layered root causes**:

1. **A broken top-bar layout on the My Day landing**: the `RitualJumpBar` (Plan/Execute/Wrap Up pills) renders *above* the screen title, pushing the title down into the body area at default text size. This single visible defect is the dominant "title is missing/oversized" smell.
2. **A non-functional theme bridge**: the `Entity*` / `Success*` / `Error*` / `Warning*` color palette in `Color.kt` is referenced as raw `Color` imports across 16 screens, bypassing `MaterialTheme.colorScheme`. The `*Dark` variants exist but are unused; dark-mode collisions are inevitable.
3. **Hardcoded demo data and English-only copy**: state-derived strings (KPI counts, "Live: 02:37", "Q3 launch", the active-focus task name) are baked into the screens, so the app reads as a static prototype. `strings.xml` has one entry, so i18n is non-existent.

Fixing items 1–3 of the recommended order at the bottom of this report should close the visible complaint; items 4–10 are the deeper structural cleanup that prevents regressions.

---

## 2. On-device evidence

Captured via `adb shell screencap -p` against a Moto `5XVCMNUSFAQ4KN4T`.

### 2.1 Current My Day landing (`screenshots/09_current.png`)

The built app, top to bottom:

- **Top**: a sticky `RitualJumpBar` with `1. Plan`, `2. Execute`, `3. Wrap Up` pills (Plan and Wrap Up are unselected; Execute is selected = indigo pill).
- **Empty top app bar**: the `LargeTopAppBar` renders but its `title` slot is occupied by the `RitualJumpBar` via `actions = { RitualJumpBar(...) }`, leaving the title slot to render bare (no app-bar background visible above the title).
- **"My Day"** at default body-text size in the body area, NOT inside the top app bar.
- **`Plan` section** with chips that show `Inbox (0)` and `Overdue (0)` — see Finding 3.
- **`Executing focus banner`**: `FOCUS RUNNING · Q3 launch` followed by `Cut release branch and tag` — but neither label updates when the active focus task changes (Finding 10).
- **Bottom nav** correctly shows Today selected.

### 2.2 Tasks tab (`screenshots/12_tasks_real.png`)

- Top bar: `Tasks` title in `titleLarge + Bold`, search/sort/filter icons on the right. The bar background is `surface` (= same as body), so the bar boundary is invisible — the title looks like a body line that happens to be bolder.
- Filter chips: `All Tasks`, `Status: Doing (3)`, `Priority: High`, `Project: Q3 launch`, `Due: Today`, `+ Add filter`. Selected (`All Tasks`) is rendered with a check icon — but `Status: Doing (3)` has indigo text suggesting it's also selected, so the selection state is ambiguous.
- Overdue row `Send Q2 retrospective summary` correctly uses `ErrorContainerLight` background and red `Due Sep 4` badge.
- Bottom nav: `Tasks` selected.

### 2.3 Earlier diagnostic (`screenshots/06_no_dialog.png`)

A USB-selection system dialog was overlay-blocking the actual app. Once dismissed, subsequent captures produced real content. The first several captures all showed the same dark frame (because the dialog was actually occupying the screen) — that's why the file is small. Nothing was wrong with the app — the dialog was.

### 2.4 Install-state confusion

During the audit, two different UB packages were observed installed on the device at different times:

| Package | Launcher | Source |
|---|---|---|
| `com.aistudio.ultimatebrain.ptvz` | `com.example.MainActivity` | Current APK in `app/build/outputs/apk/debug/app-debug.apk` |
| `com.ultimatebrain.debug` | `com.ultimatebrain.ui.MainActivity` | A prior install (probably an earlier build before the theme swap) |

Both can be on the device simultaneously. The current APK is the one with the correct indigo theme; the prior install was a peachy-cream / rust-orange variant that doesn't match the spec. The "themed wrong" complaints may have been against the older install.

---

## 3. Findings, ranked by impact

### Finding 1 — Top-bar layout is broken on My Day

**File:** `app/src/main/java/com/example/ui/screens/MyDayScreen.kt`
**Symptom:** the `RitualJumpBar` was inserted into the `LargeTopAppBar`'s `actions` slot, but pill-shaped text buttons aren't valid children of an `actions` slot — only icons are. The bar renders above the title, the title renders empty in its slot, and `My Day` falls into the body area at default text size.

**Fix:** either
- Move the `RitualJumpBar` to a sticky tab strip directly below the `LargeTopAppBar` (preferred — keeps the M3 top-bar title visible), or
- Render the jump bar AS the title slot content.

### Finding 2 — Zero localization

`app/src/main/res/values/strings.xml` has exactly one entry: `<string name="app_name">Ultimate Brain</string>`.

- `LocalContext.current.getString(`: **0 hits**
- `stringResource(`: **0 hits**
- `R.string.`: **0 hits**

Every user-facing string (`"My Day"`, `"Tasks"`, `"New Project"`, `"Filter"`, `"Sort"`, `"P1"`, `"Cut release branch and tag"`, …) is a Kotlin literal. Filter chips compare `uiState.selectedPlanFilter == "Today"` against English strings (`MyDayViewModel.kt:54-56, 79`). Changing locale would silently break every filter without code changes — no test would catch it.

**Fix (priority high):** populate `strings.xml` with the canonical English copy (one-time ~150 entries) and route every `Text("...")` through `stringResource(...)`. With only English today this is still a structural fix — it would break in any non-English market and currently allows wording drift across screens.

### Finding 3 — Plan section shows `Inbox (0)` / `Overdue (0)` permanently

`app/src/main/java/com/example/ui/components/PlanSection.kt:76-77`:

```kotlin
fun PlanSection(...
  inboxCount: Int = 0,
  overdueCount: Int = 0,
  ...
)
```

`app/src/main/java/com/example/ui/screens/MyDayScreen.kt:227-233` never overrides these defaults, so both chips always read `(0)` even when there are inbox or overdue tasks.

**Fix:** pipe `viewModel.uiState.inboxTasks.size` and `viewModel.uiState.overdueTasks.size` (or equivalent) into the call site.

### Finding 4 — Inconsistent top-bar title styling across the 6 main screens

| Screen | Title style | Source |
|---|---|---|
| MyDay | default `Text("My Day")` | `MyDayScreen.kt:97` |
| Tasks | `titleLarge.copy(fontWeight = Bold)` | `TasksScreen.kt:108` |
| Projects | `titleLarge.copy(fontWeight = Bold)` | `ProjectsScreen.kt:90` |
| Notes | `titleLarge.copy(fontWeight = Bold)` | `NotesScreen.kt:79` |
| Goals | `titleLarge.copy(fontWeight = Bold)` | `GoalsScreen.kt:83` |
| **MoreHub** | **`headlineSmall.copy(fontWeight = Bold)`** | `MoreHubScreen.kt:72` |

Five of six are consistent; `MoreHub` is oversized. M3 spec says `titleLarge` is the screen-title tier — pick one and apply uniformly.

### Finding 5 — Six different filter-chip implementations

| Screen | Implementation |
|---|---|
| `TasksScreen` | raw `Surface` + `Row` (`TasksScreen.kt:199-341`) |
| `ProjectsScreen` | custom `ProjectFilterChip` (`ProjectsScreen.kt:435-472`) |
| `GoalsScreen` | M3 `FilterChip` (`GoalsScreen.kt:144-153`) |
| `NotesScreen` | M3 `FilterChip` (`NotesScreen.kt:140-153`) |
| `ExecuteSection` | custom `ExecuteChip` (`ExecuteSection.kt:473-513`) |
| `PlanSection` | M3 `FilterChip` (`PlanSection.kt:290-329`) |

Same conceptual primitive, five private reimplementations. **Fix:** standardize on M3 `FilterChip` everywhere.

### Finding 6 — Theme parallel palette leaks (visible in dark mode)

`app/src/main/java/com/example/ui/theme/Color.kt:46-72` defines an entity/functional palette separate from the M3 color scheme:

```
EntityProjects / EntityTasks / EntityGoals / EntityNotes /
EntityTagArea / EntityTagResource / EntityTagEntity /
SuccessGreen / SuccessContainerLight / SuccessContainerDark /
ErrorRed / ErrorContainerLight / ErrorContainerDark /
WarningAmber / WarningContainerLight / WarningContainerDark
```

Each has a `*Dark` counterpart. Imports show the **light** variants only are referenced across 16 screens (`EditProjectScreen.kt:56,220`, `ExecuteSection.kt:58-63,123,128,171,384,399,443,573-574,622-685`, `GoalDetailScreen.kt:56-59,125-126,166,201,209,245,253,304,315`, …). The `*Dark` variants are referenced nowhere outside `Color.kt` itself.

Result in dark mode: bright `EntityProjects` (`#8B5CF6`) on `SurfaceDark` (`#0F172A`); bright `SuccessGreen` chips on slate; `ErrorContainerLight` (`#FFDAD6`, salmon) on dark backgrounds. **Two parallel color systems in the same codebase.**

**Fix paths:**
- **A (small):** add an extension property `val ColorScheme.entityProjects get() = if (isDark) EntityProjectsDark else EntityProjects` and route every call site through `MaterialTheme.colorScheme.entityProjects`.
- **B (larger):** redefine status colors as M3 token extensions and replace `Color.White` content-color tokens with `onPrimary`/`onError`/`onSuccess`. Also replaces the `Color.White` reads at `GoalDetailScreen.kt:126,171`, `GoalsScreen.kt:255`, `MilestonesScreen.kt:262`, `TasksScreen.kt:480,487`, `ProjectsScreen.kt:416`.

### Finding 7 — App-package state confusion

The current build that's spec-correct is at `com.aistudio.ultimatebrain.ptvz` (launcher `com.example.MainActivity`). The Moto device at various points also had `com.ultimatebrain.debug` (launcher `com.ultimatebrain.ui.MainActivity`) installed — a prior build with the peachy-cream / rust-orange variant. If the user only looked at the older install and reported "themes are wrong", we now know why. The two should be reconciled: delete the older install from the device, and lock the applicationId in `app/build.gradle.kts` so future builds can't drift.

### Finding 8 — FAB inconsistencies

| Screen | Component | Container | On | Elevation |
|---|---|---|---|---|
| MyDay | `FloatingActionButton` | `primary` | `onPrimary` | `6.dp` |
| Tasks | `FloatingActionButton` | `primaryContainer` | `onPrimaryContainer` | `4.dp` |
| Projects | `ExtendedFloatingActionButton` | `primaryContainer` | `onPrimaryContainer` | default |
| Notes | `ExtendedFloatingActionButton` | `primaryContainer` | `onPrimaryContainer` | default |
| Goals | `ExtendedFloatingActionButton` | `primaryContainer` | `onPrimaryContainer` | default |

MyDay is the odd one (filled + raised 6.dp); the other five are container-toned + flatter. **Fix:** one rule — `primaryContainer` fill, `onPrimaryContainer` content, `4.dp` elevation. Apply uniformly.

### Finding 9 — Hardcoded demo data baked into screens

| File:line | Literal | What it should be |
|---|---|---|
| `GoalsScreen.kt:177-178` | `"51%"`, `"3 Active · 1 Achieved"` | derived from `goal.completedMilestonesCount/goal.totalMilestonesCount` |
| `MoreHubScreen.kt:165` | `"Live: 02:37"` | derived from active focus session elapsed timer |
| `MoreHubScreen.kt:155` | `"9 Tags"` | derived from `uiState.tags.size` |
| `MoreHubScreen.kt:144` | `"6 milestones across Q3 deliverable blocks"` | derived |
| `TasksScreen.kt:295` | `"Project: Q3 launch"` filter chip text | derived from selected project name |
| `TasksScreen.kt:532` | `"Target: Complete by 6 PM"` | derived |
| `ExecuteSection.kt:190-196` | `· Q3 launch` + `Cut release branch and tag` next to live timer | derived from `uiState.activeFocus` |
| `MyDayViewModel.kt:667` | `"Goal marked achieved! 🎉"` | toast text |
| `PlanSection.kt` defaults | `inboxCount = 0`, `overdueCount = 0` | real counts |

The user sees these and the app looks frozen/broken. Wiring them up to state fixes a large share of the "UI is wrong" perception.

### Finding 10 — Active focus banner lies

`app/src/main/java/com/example/ui/components/ExecuteSection.kt:190-196` hardcodes:

```kotlin
"FOCUS RUNNING · Q3 launch"
"Cut release branch and tag"
```

next to a `formattedTimer` parameter that IS live. If the user changes the active task, the banner keeps the old labels. **Fix:** pass `uiState.activeFocus.taskName` and `uiState.activeFocus.projectName` (or equivalent) into `ExecuteSection` and read those.

### Finding 11 — `MyDayViewModel.kt` filter keys are English state

`MyDayViewModel.kt:54-56`:

```kotlin
selectedPlanFilter: String = "Today",
selectedExecuteFilter: String = "My Day",
selectedTasksFilter: String = "All Tasks",
```

These are state-machine keys compared in screens (`TasksScreen.kt:201,231,255,281,307`; `MyDayScreen.kt:217-281`). State keys should be enums or resource IDs, not display strings — moving them to enums makes i18n trivial and prevents brittle string matching.

### Finding 12 — Dead code and duplications

- `HeaderSection.kt` is dead since `MyDayScreen.kt` now uses `LargeTopAppBar`. It still contains a hardcoded Google avatar URL (`HeaderSection.kt:67`) duplicated with `MyDayScreen.kt:116`. Delete the file (or re-wire it into MyDay as a header below the top bar).
- `EntityTasks` is imported in `TasksScreen.kt:74` but never referenced in the file.
- `EntityProjectsDark`, `EntityTasksDark`, `EntityTagAreaDark`, `EntityTagResourceDark`, `EntityTagEntityDark`, `EntityNotesDark`, `SuccessGreenDark`, `SuccessContainerDark`, `ErrorContainerDark`, `WarningContainerDark` — defined, never referenced outside `Color.kt`/`Theme.kt`.

### Finding 13 — Pluralization bug

`WrapUpSection.kt:235`:

```kotlin
"Remaining in Queue (${overdueTasks.size} item)"
```

Will read `"3 item"` instead of `"3 items"`. `MyDayViewModel.kt:456` already does this correctly (`task${if (myDayCount == 1) "" else "s"}`). Apply the same pattern.

### Finding 14 — Spec mismatch on entity colors

`app-spec/04-design-tokens.md` defines:

| Entity | Spec | Current (`Color.kt`) |
|---|---|---|
| Tasks | `#3B82F6` (blue 500) | `EntityTasks = #0284C7` (blue 600) |
| Projects | `#8B5CF6` | `EntityProjects = #8B5CF6` ✓ |
| Notes | `#F59E0B` | `EntityNotes = #F59E0B` ✓ |
| Goals | `#10B981` | `EntityGoals = #10B981` ✓ |
| Tags (Area) | `#14B8A6` (teal 500) | `EntityTagArea = #0D9488` (teal 600) |
| Tags (Resource) | `#06B6D4` (cyan 500) | `EntityTagResource = #3B82F6` (collides with Tasks spec value!) |
| Tags (Entity) | `#EC4899` | `EntityTagEntity = #EC4899` ✓ |

**Two fixes here:** align `EntityTasks` to `#3B82F6` and fix `EntityTagResource` to `#06B6D4`. The Resources color literally collides with the spec's Tasks color otherwise — bugs guaranteed.

### Finding 15 — Nav-bridge LaunchedEffect re-fires on every state mutation

`app/src/main/java/com/example/MainActivity.kt:54-62`:

```kotlin
LaunchedEffect(uiState.currentScreen) { navController.navigate(route) }
```

`uiState.currentScreen` mutates for many reasons — toggling a task, posting a notification — not all of which mean "the user moved to a different screen". Combined with `launchSingleTop = true`, navigation may silent-no-op repeatedly. **Fix:** key the `LaunchedEffect` on a navigation-intent event stream, not on the entire `currentScreen` field.

### Finding 16 — Settings dynamic-color toggle is dead

`SettingsScreen.kt:69, 181`:

```kotlin
var dynamicColor by remember { mutableStateOf(true) }
```

`dynamicColor` is never read. The toggle is a static switch. **Fix:** wire it through `MyApplicationTheme(dynamicColor = ...)` at the root (`MainActivity.kt:64`).

### Finding 17 — Tasks filter-chip selection state is ambiguous

`TasksScreen.kt:201,231,255,281,307` show filter chips rendered with the new M3 `FilterChip` style from one branch and raw `Surface` from another (per Finding 5). In screenshot 12 it's unclear whether `All Tasks` is selected (it has a check icon) vs `Status: Doing (3)` which has indigo text. Several chips have a count, several don't: `Priority: High`, `Project: Q3 launch`, `Due: Today` show no count while `Status: Doing (3)` does.

### Finding 18 — GoalsScreen has both ArrowBack AND bottom nav

`GoalsScreen.kt:84-87` renders an `ArrowBack` button in its top bar while `GoalsScreen.kt:104-115` ALSO renders the global bottom nav. Double affordance — the back button is redundant since the bottom nav handles tab switching. Remove the `ArrowBack`, or remove the bottom nav for this single screen (probably keep the bottom nav; remove the back arrow).

### Finding 19 — Inconsistent "today-tap" behavior in bottom nav

- Tapping `Today` while on MyDay: scrolls to top (`MyDayScreen.kt:182-184`)
- Tapping `Today` from any other tab: navigates (`TasksScreen.kt:152`, `ProjectsScreen.kt:125`, etc.)

Two behaviors for the same affordance. Pick one (always navigate, never scroll, is the standard pattern).

### Finding 20 — Tab bottom-padding inconsistency

| File | Padding |
|---|---|
| `ProjectsScreen.kt:215` | `Spacer(80.dp)` |
| `NotesScreen.kt:172` | `Spacer(80.dp)` |
| `GoalsScreen.kt:209` | `Spacer(80.dp)` |
| `TasksScreen.kt:624` | `Spacer(72.dp)` |

One value — `80.dp` — everywhere.

---

## 4. Theme audit detail

### Theme system what's there

- M3 `lightColorScheme` / `darkColorScheme` are defined in `Theme.kt:10-70`. Tokens used: `primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, onTertiaryContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceContainer, surfaceContainerLow, surfaceContainerLowest, surfaceContainerHigh, surfaceContainerHighest, outline, outlineVariant, error, errorContainer`. 664 `MaterialTheme.colorScheme.*` references across 26 files.
- Typography is a complete M3 scale with line-heights/letter-spacing matching the documented defaults.
- Surface tonal elevation is correctly applied: `surface` for card bodies, `surfaceContainerLow` for inset cards, `surfaceContainerHigh` for hero stats, `surfaceContainer` for top-app-bars and bottom nav.
- Outline tokens are used with `.copy(alpha = ...)` to soften — consistent pattern across 30+ places.

### Theme system what's bypassed

- The "Entity/Functional" palette (`Color.kt:46-72`) is imported directly in 16 screens (`EditProjectScreen.kt`, `ExecuteSection.kt`, `GoalDetailScreen.kt`, `GoalsScreen.kt`, `GlobalSearchScreen.kt`, `HeaderSection.kt`, `MyDayScreen.kt`, `MilestonesScreen.kt`, `NoteDetailScreen.kt`, `NoteEditorScreen.kt`, `NotesScreen.kt`, `ProjectDetailScreen.kt`, `ProjectsScreen.kt`, `QuickAddBottomSheet.kt`, `SearchDialog.kt`, `SettingsScreen.kt`, `TagDetailScreen.kt`, `TagsScreen.kt`, `TaskDetailScreen.kt`, `TasksScreen.kt`, `WrapUpSection.kt`). `Color.White` as text-on-color is hardcoded in 7 places (`ProjectsScreen.kt:416`, `GoalDetailScreen.kt:126,171`, `GoalsScreen.kt:255`, `TasksScreen.kt:480,487`, `MilestonesScreen.kt:262`).
- `NotesScreen.kt:294-298` `NoteTypeBadge` is fully hardcoded (5 `Color(0x...)` pairs) and duplicates `EntityNotes`/`tertiaryContainer` literals.
- `TaskDetailScreen.kt:967, 975` hardcodes `Color(0xFF283044)` and `Color(0xFFEEF0FF)` for a code-snippet block, calling it "inverse surface" — but the theme has no `inverseSurface` token wired.

### Specific visual problems

1. **EntityProject purple + dark surfaces**: `#8B5CF6` on `#0F172A` is too close in luminance. Dark variant `EntityProjectsDark = #C4B5FD` exists, unused.
2. **Goals = Tertiary collision**: `tertiary = EntityGoals` in `Theme.kt:51,20` ties the M3 tertiary token to a domain entity color. Any future palette change has to update both the theme and every direct `EntityGoals` import.
3. **Note type badge colors are light-tinted but sit on dark surfaces in dark mode** (`#E0E7FF` etc. on `#0F172A`). Reads as pastels pasted onto dark cards.
4. **Status-pill `Color.White`** content on saturated colored chips is unreadable in dark mode (white on light periwinkle `PrimaryIndigoDark`).
5. **`EntityTagResource` color collision** with the spec's `Tasks` color (both `#3B82F6`).

### What works

- Full M3 type scale with correct line heights.
- Surface tonal elevation hierarchy is correctly used.
- 19 distinct `colorScheme` tokens are read — token usage is broader than is typical for an early-stage Android app.
- Outline tokens with alpha-blended borders are a defensible pattern.

---

## 5. Spec-vs-implementation comparison

### What matches the spec

- Material 3 color scheme tokens exist and are used.
- Typography scale matches M3.
- Iconography uses Material Symbols / Icons.Filled — the same source set.
- Surface tonal elevation is correct (`surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`).
- Bottom-nav with `Today / Tasks / Projects / Notes / More` matches the bottom-nav inventory.
- Top-app-bar pattern (`LargeTopAppBar` on landing, `MediumTopAppBar` on lists) matches M3 conventions.

### What diverges from the spec

- **Tasks entity color** (`EntityTasks = #0284C7` vs spec `#3B82F6`) — see Finding 14.
- **Tags-Area entity color** (`EntityTagArea = #0D9488` vs spec `#14B8A6`) — same finding, off by one shade.
- **Tag-Resource entity color** (`EntityTagResource = #3B82F6` vs spec `#06B6D4`) — collides with Tasks entity color.
- **Spec icon for Goals tab**: `flag`. Code uses `TrackChanges`. Minor.
- **Spec spacing/radius tokens**: spec defines `space-1` … `space-7` and `radius-sm` … `radius-xl`. Code uses raw `.dp` values throughout.
- **Spec "Motion tokens"**: `motion-fast 150ms`, `motion-medium 250ms`, `motion-slow 400ms`. No timing constants in code.
- **Spec "Tabular numbers" with `font-feature-settings: 'tnum'`**: not applied anywhere.

### What's built that's not in the spec

- `HeaderSection.kt` ("Priya's Avatar" hero with AsyncImage) — removed from My Day but file remains.
- `MyDayScreen.kt:111` "Custom hero block" with SVG avatar — appears in the original Play prototype but not in the spec screen inventory.

### What's in the spec but not built

- A `values-es/` or any other locale folder. Zero non-English strings.xml.
- A `values/colors.xml` matching the spec's named tokens (`@color/primary`, `@color/on_surface_disabled`, etc.). The Kotlin `Color.kt` is the only spot where colors live.
- Animated `mark complete` motion (task row collapse + content fade). Default behavior only.
- Recurring task "Reset for next Friday" toast.

---

## 6. Recommended fix order

Each item lists expected UX impact / risk. Pick and ship incrementally.

| # | Fix | Files | Impact | Risk |
|---|---|---|---|---|
| **1** | Move `RitualJumpBar` out of the `LargeTopAppBar.actions` slot into a sticky tab strip below the top app bar (or render it AS the title slot). | `MyDayScreen.kt` | High — fixes the dominant visible defect. | Low. |
| **2** | Wire `inboxCount` / `overdueCount` into the `PlanSection` call site. | `MyDayScreen.kt` | High — chips start reading real data. | Trivial. |
| **3** | Pipe `uiState.activeFocus.taskName` / `.projectName` into `ExecuteSection` so the focus banner stops lying. | `ExecuteSection.kt`, `MyDayViewModel.kt` (add fields) | High — biggest demo-data smell. | Low. |
| **4** | Replace KPI literals in `GoalsScreen.kt:177-178`, `MoreHubScreen.kt:165,155,144`, `TasksScreen.kt:295,532` with state-derived values. | 3 files | High — kills "looks frozen" perception. | Low. |
| **5** | Populate `strings.xml` with the canonical English copy for the top ~50 visible strings; route `Text(...)` through `stringResource(...)`. | `res/values/strings.xml` + every screen + every component | High — enables future i18n. | Medium — large diff. |
| **6** | Move `MyDayViewModel.kt:54-56,79` filter strings from English literals to enums (`enum class PlanFilter { TODAY, ACTIVE_PROJECTS, INBOX, WEEK, OVERDUE, RECURRING }`). | `MyDayViewModel.kt` + matching screens | High — future-proofs filters. | Medium — requires updating every comparison site. |
| **7** | Standardize FAB and chip styles across all six tabs. FAB: `primaryContainer/onPrimaryContainer/4.dp`. Chip: M3 `FilterChip` everywhere. | `MyDayScreen.kt`, `TasksScreen.kt`, `ProjectsScreen.kt`, `NotesScreen.kt`, `GoalsScreen.kt`, `MoreHubScreen.kt` | Medium — visual consistency. | Low. |
| **8** | Route `Entity*` / `SuccessGreen` / `ErrorRed` / `WarningAmber` through `MaterialTheme.colorScheme` extension properties so dark mode flips. | `Color.kt` (add extension file), every screen | Medium-High — fixes dark mode. | Medium. |
| **9** | Add `inverseSurface`/`inverseOnSurface` tokens and replace `Color(0xFF283044)` / `Color(0xFFEEF0FF)` in `TaskDetailScreen.kt:967,975`. | `Theme.kt`, `TaskDetailScreen.kt` | Low. | Low. |
| **10** | Move `NoteTypeBadge` color literals in `NotesScreen.kt:294-298` into the theme. | `NotesScreen.kt`, `Color.kt` | Low. | Low. |
| **11** | Spec-color fixes: `EntityTasks = #3B82F6`, `EntityTagArea = #14B8A6`, `EntityTagResource = #06B6D4`. | `Color.kt` | Low — corrects spec drift. | Low. |
| **12** | Wire the `Settings` `dynamicColor` toggle into `MyApplicationTheme`. | `SettingsScreen.kt`, `MainActivity.kt`, `Theme.kt` | Low. | Low. |
| **13** | Standardize top-bar title styling: `titleLarge + SemiBold` from M3 spec, applied uniformly. One off (MoreHub → `headlineSmall`) corrected. | All 6 screens | Medium. | Low. |
| **14** | Wrap `TasksScreen.kt` filter chips in M3 `FilterChip`. | `TasksScreen.kt` | Low — but completes the chip unification. | Low. |
| **15** | Remove the dead `HeaderSection.kt` file. | (delete) | Low. | Trivial. |
| **16** | Fix `WrapUpSection.kt:235` pluralization. | `WrapUpSection.kt` | Low. | Trivial. |
| **17** | Standardize bottom-nav "Today" tap behavior (always navigate, never scroll). | All 6 screens | Low. | Low. |
| **18** | Standardize tab trailing `Spacer` padding (`80.dp` everywhere). | `TasksScreen.kt:624` | Trivial. | Trivial. |
| **19** | Replace `LaunchedEffect(uiState.currentScreen)` in `MainActivity.kt:54-62` with a navigation-intent event stream. | `MainActivity.kt`, `MyDayViewModel.kt` | Low — but fixes subtle silent-no-op nav. | Medium. |
| **20** | Drop `EntityTasks` dead import in `TasksScreen.kt:74`. | `TasksScreen.kt` | Trivial. | Trivial. |

### Suggested ship sequence

**Round 1 (closes the user complaint):** items 1–3.
**Round 2 (kills the "looks frozen" perception):** items 4–7.
**Round 3 (structural cleanup):** items 8–20.

---

## 7. Files inspected

```
app/src/main/java/com/example/MainActivity.kt
app/src/main/java/com/example/ui/theme/Color.kt
app/src/main/java/com/example/ui/theme/Theme.kt
app/src/main/java/com/example/ui/theme/Type.kt
app/src/main/java/com/example/ui/components/BottomNavBar.kt
app/src/main/java/com/example/ui/components/ExecuteSection.kt
app/src/main/java/com/example/ui/components/HeaderSection.kt
app/src/main/java/com/example/ui/components/PlanSection.kt
app/src/main/java/com/example/ui/components/QuickAddBottomSheet.kt
app/src/main/java/com/example/ui/components/RitualJumpBar.kt
app/src/main/java/com/example/ui/components/SearchDialog.kt
app/src/main/java/com/example/ui/components/WrapUpSection.kt
app/src/main/java/com/example/ui/screens/MyDayScreen.kt
app/src/main/java/com/example/ui/screens/TasksScreen.kt
app/src/main/java/com/example/ui/screens/ProjectsScreen.kt
app/src/main/java/com/example/ui/screens/GoalsScreen.kt
app/src/main/java/com/example/ui/screens/NotesScreen.kt
app/src/main/java/com/example/ui/screens/MoreHubScreen.kt
app/src/main/java/com/example/ui/screens/MilestonesScreen.kt
app/src/main/java/com/example/ui/screens/EditProjectScreen.kt
app/src/main/java/com/example/viewmodel/MyDayViewModel.kt
app/src/main/res/values/strings.xml
```

## 8. References

- Spec files: `app-spec/01-project-context.md`, `app-spec/02-tools-inventory.md`, `app-spec/03-screen-inventory.md`, `app-spec/04-design-tokens.md`, `app-spec/05-component-catalog.md`, `app-spec/06-interaction-patterns.md`, `app-spec/07-state-matrix.md`, `app-spec/tokens/material3-theme.json`
- On-device captures: `screenshots/09_current.png`, `screenshots/12_tasks_real.png`
- Notion-backed Hermes context: `hermes/ultimate-brain-notion/`
- Project conventions: `CLAUDE.md`
