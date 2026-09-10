package com.example.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.DateUtils
import com.example.data.DummyData
import com.example.data.FilterStore
import com.example.data.UbRepository
import com.example.domain.FilterEngine
import com.example.domain.filterRow
import com.example.focus.FocusController
import com.example.focus.FocusSession
import com.example.focus.FocusTimerService
import com.example.model.ChipRowConfig
import com.example.model.CustomFilter
import com.example.model.DailyRitualPhase
import com.example.model.FilterScope
import com.example.model.customId
import com.example.model.customKey
import com.example.model.isCustomKey
import com.example.model.GoalModel
import com.example.model.MilestoneModel
import com.example.model.NoteModel
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.model.SubTask
import com.example.model.TagModel
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.WorkSessionModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
  TODAY,
  TASKS,
  TASK_DETAIL,
  PROJECTS,
  PROJECT_DETAIL,
  EDIT_PROJECT,
  NOTES,
  NOTE_DETAIL,
  NOTE_EDITOR,
  GOALS,
  GOAL_DETAIL,
  MILESTONES,
  MILESTONE_DETAIL,
  TAGS,
  TAG_DETAIL,
  WORK_SESSIONS,
  TASK_WORK_SESSIONS,
  SETTINGS,
  GLOBAL_SEARCH,
  MORE_HUB,
  PEOPLE, PERSON_DETAIL,
  BOOKS, BOOK_DETAIL,
  READING_LOG,
  GENRES,
  RECIPES, RECIPE_DETAIL,
  MEAL_PLANNER,
}

/**
 * One-shot navigation-intent events emitted by [MyDayViewModel]. MainActivity
 * collects this stream via LaunchedEffect(Unit) and only navigates when an
 * intent actually arrives — not when UiState mutates for unrelated reasons.
 *
 * Audit Finding 15 (Round 5): replaces LaunchedEffect(uiState.currentScreen)
 * which re-fired on every state change.
 */
sealed class NavIntent {
  data class Navigate(val screen: AppScreen) : NavIntent()
  /** Pop the current destination off the back stack (in-app back arrows). */
  data object Back : NavIntent()
}

/**
 * The five top-level bottom-navigation destinations. Navigating to one of
 * these resets to a single back-stack entry (standard M3 bottom-nav
 * behaviour) rather than stacking peers on top of each other.
 */
val AppScreen.isTopLevelTab: Boolean
  get() = this == AppScreen.TODAY || this == AppScreen.TASKS ||
    this == AppScreen.PROJECTS || this == AppScreen.NOTES ||
    this == AppScreen.MORE_HUB

/**
 * Type-safe filter values for the various list screens. Each enum constant
 * carries an [StringRes] so callers can map to a localized label without
 * resorting to stringly-typed comparisons.
 *
 * Round 3 Finding #2/#6: replaces the previous String-typed filter state.
 */
enum class PlanFilter(@StringRes val labelRes: Int) {
  TODAY(R.string.plan_chip_today),
  ACTIVE_PROJECTS(R.string.plan_chip_active_projects),
  INBOX(R.string.plan_chip_inbox),
  WEEK(R.string.plan_chip_week),
  OVERDUE(R.string.plan_chip_overdue),
  RECURRING(R.string.plan_chip_recurring)
}

enum class ExecuteFilter(@StringRes val labelRes: Int) {
  MY_DAY(R.string.execute_chip_my_day),
  TIME(R.string.execute_chip_time),
  ENERGY(R.string.execute_chip_energy),
  LOCATION(R.string.execute_chip_location)
}

enum class TasksFilter(@StringRes val labelRes: Int) {
  ALL(R.string.tasks_chip_all),
  TODAY(R.string.tasks_chip_today),
  OVERDUE(R.string.tasks_chip_overdue),
  MY_DAY(R.string.tasks_chip_my_day),
  HIGH_PRIORITY(R.string.tasks_chip_high_priority),
  RECURRING(R.string.tasks_chip_recurring),
  DONE(R.string.tasks_chip_recurring),
}

enum class ProjectFilter(@StringRes val labelRes: Int) {
  ALL(R.string.projects_chip_all_format),
  ACTIVE(R.string.projects_chip_status_doing_format),
  DOING(R.string.projects_chip_status_doing_format),
  DONE(R.string.projects_chip_goal_format),
  ARCHIVED(R.string.projects_chip_tag_format),
}

enum class NoteFilter(@StringRes val labelRes: Int) {
  ALL(R.string.notes_chip_all_format),
  MEETING(R.string.notes_chip_meeting),
  REFERENCE(R.string.notes_chip_reference),
  IDEA(R.string.notes_chip_idea),
  JOURNAL(R.string.notes_chip_journal),
  BOOK(R.string.notes_chip_book)
}

enum class GoalFilter(@StringRes val labelRes: Int) {
  ACTIVE(R.string.goals_chip_active),
  ACHIEVED(R.string.goals_chip_achieved),
  DROPPED(R.string.goals_chip_dropped)
}

enum class MilestoneFilter(@StringRes val labelRes: Int) {
  ALL(R.string.milestones_chip_all_format),
  IN_PROGRESS(R.string.milestones_chip_in_progress),
  COMPLETED(R.string.milestones_chip_completed),
  PENDING(R.string.milestones_chip_pending)
}

enum class TagFilter(@StringRes val labelRes: Int) {
  ALL(R.string.tags_tab_all),
  AREAS(R.string.tags_tab_areas),
  RESOURCES(R.string.tags_tab_resources),
  ENTITIES(R.string.tags_tab_entities)
}

/**
 * Sealed class of snackbar events emitted by the ViewModel. Carries the
 * string resource id plus optional format arguments so callers can render
 * the message via [androidx.compose.ui.res.stringResource].
 *
 * Round 3 Finding #2: replaces the previous snackbarMessage: String? field
 * so that the message is localizable without putting English in the VM.
 */
sealed class SnackbarMessage {
  data object FocusSessionEnded : SnackbarMessage()
  data object TaskStatusUpdated : SnackbarMessage()
  data class TaskStatusChanged(val newStatus: TaskStatus) : SnackbarMessage()
  data class PriorityUpdated(val newPriority: Priority?) : SnackbarMessage()
  data object SubTaskAdded : SnackbarMessage()
  data object MyDayAlreadyEmpty : SnackbarMessage()
  data class MyDayCleared(val count: Int) : SnackbarMessage()
  data object TaskPostponed : SnackbarMessage()
  data object AllOverdueMoved : SnackbarMessage()
  data class TaskAdded(val name: String) : SnackbarMessage()
  data class ProjectSaved(val name: String) : SnackbarMessage()
  data object ProjectArchived : SnackbarMessage()
  data object NoteSaved : SnackbarMessage()
  data object GoalDropped : SnackbarMessage()
  data object GoalAchieved : SnackbarMessage()
}

data class MyDayUiState(
  val currentScreen: AppScreen = AppScreen.TODAY,
  val selectedTaskId: String? = null,
  val tasks: List<Task> = DummyData.initialTasks,
  val selectedPhase: DailyRitualPhase = DailyRitualPhase.EXECUTE,
  val selectedPlanFilter: PlanFilter = PlanFilter.TODAY,
  val selectedExecuteFilter: ExecuteFilter = ExecuteFilter.MY_DAY,
  val selectedTasksFilter: TasksFilter = TasksFilter.ALL,
  val isTimerRunning: Boolean = false,
  val elapsedSeconds: Long = 0L,
  val isDoneExpanded: Boolean = false,
  val isQuickAddOpen: Boolean = false,
  // Long-press quick-edit sheet targets (task / note id, or null when closed).
  val quickEditTaskId: String? = null,
  val quickEditNoteId: String? = null,
  val isSearchOpen: Boolean = false,
  val searchQuery: String = "",
  val eveningReviewStep: Int = 1, // legacy — the wizard was removed
  val snackbarMessage: SnackbarMessage? = null,

  // Total focus time logged today (seconds), for the Wrap-up scorecard.
  val focusedSecondsToday: Long = 0L,

  // Body (markdown) of the currently-open detail page, fetched lazily.
  val detailBody: String? = null,
  val detailBodyLoading: Boolean = false,
  val detailBodyForId: String? = null,

  // Live Notion option lists, keyed "<db>.<Property>". Fetched on sync.
  val schemaOptions: Map<String, List<String>> = emptyMap(),

  // Library / secondary databases.
  val people: List<com.example.model.PersonModel> = emptyList(),
  val books: List<com.example.model.BookModel> = emptyList(),
  val readingLog: List<com.example.model.ReadingLogModel> = emptyList(),
  val genres: List<com.example.model.GenreModel> = emptyList(),
  val recipes: List<com.example.model.RecipeModel> = emptyList(),
  val mealPlan: List<com.example.model.MealPlanModel> = emptyList(),
  val libraryLoaded: Boolean = false,
  val selectedPersonId: String? = null,
  val selectedBookId: String? = null,
  val selectedRecipeId: String? = null,

  // Projects State
  val projects: List<ProjectModel> = DummyData.projectsList,
  val selectedProjectId: String? = null,
  val selectedProjectFilter: ProjectFilter = ProjectFilter.ALL,

  // Notes State
  val notes: List<NoteModel> = DummyData.notesList,
  val selectedNoteId: String? = null,
  val selectedNoteFilter: NoteFilter = NoteFilter.ALL,

  // Goals State
  val goals: List<GoalModel> = DummyData.goalsList,
  val selectedGoalId: String? = null,
  val selectedGoalFilter: GoalFilter = GoalFilter.ACTIVE,

  // Milestones State
  val milestones: List<MilestoneModel> = DummyData.milestonesList,
  val selectedMilestoneFilter: MilestoneFilter = MilestoneFilter.ALL,
  val selectedMilestoneGoalId: String = "All",
  val selectedMilestoneId: String? = null,

  // Tags State
  val tags: List<TagModel> = DummyData.tagsList,
  val selectedTagId: String? = null,
  val selectedTagFilter: TagFilter = TagFilter.ALL,

  // Customizable filter bars — user-defined filters + per-scope chip layout.
  // Keyed by FilterScope.name. See FilterStore, FilterEngine, BuiltinFilters.
  val customFilters: List<com.example.model.CustomFilter> = emptyList(),
  val chipConfigs: Map<String, com.example.model.ChipRowConfig> = emptyMap(),
  val selectedFilterKeys: Map<String, String> = emptyMap(),

  // Work Sessions State
  val workSessions: List<WorkSessionModel> = DummyData.workSessionsList,
  val activeSessionSeconds: Long = 0L,
  val isWorkSessionActive: Boolean = false,
  // Per-task work-session history (the "Time" detail screen).
  val taskWorkSessions: List<WorkSessionModel> = emptyList(),
  val taskWorkSessionsForId: String? = null,
  val taskWorkSessionsLoading: Boolean = false,
  // Per-task recurring occurrences (the "History" tab on the task detail page).
  val taskOccurrences: List<Task> = emptyList(),
  val taskOccurrencesForId: String? = null,
  val taskOccurrencesLoading: Boolean = false,

  // Global Search State
  val globalSearchQuery: String = "",
  val globalSearchScope: String = "All", // "All", "Tasks", "Projects", "Notes" — kept as String since this filter is for a search engine, not a localized label

  // Theme preferences (audit Finding 16). The SettingsScreen toggle mutates
  // this; MainActivity observes it via collectAsState() and feeds it into
  // MyApplicationTheme(dynamicColor = ...).
  val dynamicColorEnabled: Boolean = false,

  // Notion sync (Direct API). `isRemote` false => running on bundled DummyData.
  val isRemote: Boolean = false,
  val isSyncing: Boolean = false,
  val syncError: String? = null,
  val pendingWriteCount: Int = 0,
  val loadingOlder: Boolean = false,
  val olderCompletedLoaded: Boolean = false,
  val workspaceUsers: List<Pair<String, String>> = emptyList(),
) {
  val selectedTask: Task?
    get() = tasks.find { it.id == selectedTaskId } ?: tasks.firstOrNull()

  val selectedProject: ProjectModel?
    get() = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()

  val selectedNote: NoteModel?
    get() = notes.find { it.id == selectedNoteId } ?: notes.firstOrNull()

  val selectedGoal: GoalModel?
    get() = goals.find { it.id == selectedGoalId } ?: goals.firstOrNull()

  val selectedTag: TagModel?
    get() = tags.find { it.id == selectedTagId } ?: tags.firstOrNull()

  val selectedMilestone: MilestoneModel?
    get() = milestones.find { it.id == selectedMilestoneId }

  val formattedTimer: String
    get() {
      val hours = elapsedSeconds / 3600
      val minutes = (elapsedSeconds % 3600) / 60
      val secs = elapsedSeconds % 60
      return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

  val formattedActiveSessionTimer: String
    get() {
      val hours = activeSessionSeconds / 3600
      val minutes = (activeSessionSeconds % 3600) / 60
      val secs = activeSessionSeconds % 60
      return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

  fun projectFilterMatches(p: ProjectModel, f: ProjectFilter): Boolean = when (f) {
    ProjectFilter.ALL -> !p.isArchived
    ProjectFilter.ACTIVE -> !p.isArchived && p.status != "Done"
    ProjectFilter.DOING -> !p.isArchived && p.status == "Doing"
    ProjectFilter.DONE -> !p.isArchived && p.status == "Done"
    ProjectFilter.ARCHIVED -> p.isArchived
  }

  fun projectFilterCount(f: ProjectFilter): Int = projects.count { projectFilterMatches(it, f) }

  val filteredProjects: List<ProjectModel>
    get() = projects.filter { projectFilterMatches(it, selectedProjectFilter) }
      .sortedWith(compareByDescending<ProjectModel> { it.status == "Doing" }.thenBy { it.name })

  val filteredNotes: List<NoteModel>
    get() = when (selectedNoteFilter) {
      NoteFilter.ALL -> notes
      NoteFilter.MEETING -> notes.filter { it.type.equals("Meeting", ignoreCase = true) }
      NoteFilter.REFERENCE -> notes.filter { it.type.equals("Reference", ignoreCase = true) }
      NoteFilter.IDEA -> notes.filter { it.type.equals("Idea", ignoreCase = true) }
      NoteFilter.JOURNAL -> notes.filter { it.type.equals("Journal", ignoreCase = true) }
      NoteFilter.BOOK -> notes.filter { it.type.equals("Book", ignoreCase = true) }
    }

  val filteredGoals: List<GoalModel>
    get() = when (selectedGoalFilter) {
      GoalFilter.ACTIVE -> goals.filter { !it.isArchived && (it.status == "Active" || it.status == "Dream") }
      GoalFilter.ACHIEVED -> goals.filter { !it.isArchived && it.status == "Achieved" }
      GoalFilter.DROPPED -> goals.filter { it.isArchived }
    }

  val filteredMilestones: List<MilestoneModel>
    get() = when (selectedMilestoneFilter) {
      MilestoneFilter.IN_PROGRESS -> milestones.filter { it.status == "In Progress" }
      MilestoneFilter.COMPLETED -> milestones.filter { it.status == "Completed" }
      MilestoneFilter.PENDING -> milestones.filter { it.status == "Pending" }
      MilestoneFilter.ALL -> milestones
    }

  val filteredTags: List<TagModel>
    get() = when (selectedTagFilter) {
      TagFilter.AREAS -> tags.filter { it.type == "Area" }
      TagFilter.RESOURCES -> tags.filter { it.type == "Resource" }
      TagFilter.ENTITIES -> tags.filter { it.type == "Entity" }
      TagFilter.ALL -> tags
    }

  // ---- Unified, customizable filter bar -----------------------------------
  // Built-in chips + user CustomFilters, arranged by a per-scope ChipRowConfig.

  fun scopeCustomFilters(scope: FilterScope): List<CustomFilter> = customFilters.filter { it.scope == scope }

  /** Chip keys to show, in order, after applying the user's layout. */
  fun visibleChipKeys(scope: FilterScope): List<String> {
    val cfg = chipConfigs[scope.name] ?: ChipRowConfig()
    val natural = BuiltinFilters.keys(scope) + scopeCustomFilters(scope).map { customKey(it.id) }
    val ordered =
      if (cfg.order.isEmpty()) natural
      else cfg.order.filter { it in natural } + natural.filterNot { it in cfg.order }
    return ordered.filterNot { it in cfg.hidden }
  }

  fun selectedChipKey(scope: FilterScope): String {
    val visible = visibleChipKeys(scope)
    selectedFilterKeys[scope.name]?.let { if (it in visible) return it }
    chipConfigs[scope.name]?.defaultKey?.let { if (it in visible) return it }
    BuiltinFilters.defaultKey(scope).let { if (it in visible) return it }
    return visible.firstOrNull() ?: BuiltinFilters.keys(scope).first()
  }

  fun chipLabel(scope: FilterScope, key: String): String =
    if (key.isCustomKey()) customFilters.firstOrNull { it.id == key.customId() }?.name ?: "Filter"
    else BuiltinFilters.label(key)

  private fun customFilterFor(scope: FilterScope, key: String): CustomFilter? =
    if (key.isCustomKey()) customFilters.firstOrNull { it.id == key.customId() && it.scope == scope } else null

  fun tasksMatching(key: String): List<Task> {
    val cf = customFilterFor(FilterScope.TASKS, key)
    val base =
      if (cf != null) tasks.filter { FilterEngine.matches(it.filterRow(), cf) }
      else if (key == "ACTIVE_PROJECTS")
        tasks.filter { !it.isDone && it.projectId != null && it.projectId in activeProjectIds }
      else tasks.filter { BuiltinFilters.taskMatches(key, it) }
    return if (cf?.sort != null) FilterEngine.sorted(base, cf.sort) { it.filterRow() }
    else if (key == "DONE") base.sortedByDescending { it.completionDate ?: "" }
    else base.sortedWith(compareBy({ DateUtils.parseIsoDate(it.due) ?: java.time.LocalDate.MAX }, { it.name }))
  }

  fun projectsMatching(key: String): List<ProjectModel> {
    val cf = customFilterFor(FilterScope.PROJECTS, key)
    val base =
      if (cf != null) projects.filter { FilterEngine.matches(it.filterRow(), cf) }
      else projects.filter { BuiltinFilters.projectMatches(key, it) }
    return if (cf?.sort != null) FilterEngine.sorted(base, cf.sort) { it.filterRow() }
    else base.sortedWith(compareByDescending<ProjectModel> { it.status == "Doing" }.thenBy { it.name })
  }

  fun notesMatching(key: String): List<NoteModel> {
    val cf = customFilterFor(FilterScope.NOTES, key)
    val base =
      if (cf != null) notes.filter { FilterEngine.matches(it.filterRow(), cf) }
      else notes.filter { BuiltinFilters.noteMatches(key, it) }
    return if (cf?.sort != null) FilterEngine.sorted(base, cf.sort) { it.filterRow() } else base
  }

  fun goalsMatching(key: String): List<GoalModel> {
    val cf = customFilterFor(FilterScope.GOALS, key)
    val base =
      if (cf != null) goals.filter { FilterEngine.matches(it.filterRow(), cf) }
      else goals.filter { BuiltinFilters.goalMatches(key, it) }
    if (cf?.sort != null) return FilterEngine.sorted(base, cf.sort) { it.filterRow() }
    return BuiltinFilters.goalSort(key)?.let { base.sortedWith(it) } ?: base
  }

  fun tagsMatching(key: String): List<TagModel> {
    val cf = customFilterFor(FilterScope.TAGS, key)
    val base =
      if (cf != null) tags.filter { FilterEngine.matches(it.filterRow(), cf) }
      else tags.filter { BuiltinFilters.tagMatches(key, it) }
    if (cf?.sort != null) return FilterEngine.sorted(base, cf.sort) { it.filterRow() }
    return BuiltinFilters.tagSort(key)?.let { base.sortedWith(it) } ?: base
  }

  fun milestonesMatching(key: String): List<MilestoneModel> {
    val cf = customFilterFor(FilterScope.MILESTONES, key)
    val base =
      if (cf != null) milestones.filter { FilterEngine.matches(it.filterRow(), cf) }
      else milestones.filter { BuiltinFilters.milestoneMatches(key, it) }
    return if (cf?.sort != null) FilterEngine.sorted(base, cf.sort) { it.filterRow() } else base
  }

  fun chipCount(scope: FilterScope, key: String): Int = when (scope) {
    FilterScope.TASKS -> tasksMatching(key).size
    FilterScope.PROJECTS -> projectsMatching(key).size
    FilterScope.NOTES -> notesMatching(key).size
    FilterScope.GOALS -> goalsMatching(key).size
    FilterScope.TAGS -> tagsMatching(key).size
    FilterScope.MILESTONES -> milestonesMatching(key).size
  }

  // Global Search filtered results
  val globalSearchTasks: List<Task>
    get() = if (globalSearchQuery.isBlank()) tasks else tasks.filter {
      it.name.contains(globalSearchQuery, ignoreCase = true) ||
        it.projectName?.contains(globalSearchQuery, ignoreCase = true) == true ||
        it.labels.any { l -> l.contains(globalSearchQuery, ignoreCase = true) }
    }

  val globalSearchProjects: List<ProjectModel>
    get() = if (globalSearchQuery.isBlank()) projects else projects.filter {
      it.name.contains(globalSearchQuery, ignoreCase = true) ||
        it.templateName?.contains(globalSearchQuery, ignoreCase = true) == true
    }

  val globalSearchNotes: List<NoteModel>
    get() = if (globalSearchQuery.isBlank()) notes else notes.filter {
      it.title.contains(globalSearchQuery, ignoreCase = true) ||
        it.excerpt.contains(globalSearchQuery, ignoreCase = true) ||
        it.rawMarkdown.contains(globalSearchQuery, ignoreCase = true)
    }

  val totalSearchMatchesCount: Int
    get() = globalSearchTasks.size + globalSearchProjects.size + globalSearchNotes.size

  val myDayTasks: List<Task>
    get() = tasks.filter { it.isMyDay && !it.isDone }

  val doingTasks: List<Task>
    get() = tasks.filter { it.isMyDay && it.status == TaskStatus.DOING && !it.isDone }

  val todoTasks: List<Task>
    get() = tasks.filter { it.isMyDay && it.status == TaskStatus.TODO && !it.isDone }

  /** Tasks actually completed *today* — not the whole loaded completion history. */
  val doneTodayTasks: List<Task>
    get() {
      val today = java.time.LocalDate.now().toString()
      return tasks.filter { t ->
        t.isDone && (t.completionDate == "Today" || t.completionDate?.substringBefore('T') == today)
      }
    }

  val overdueTasks: List<Task>
    get() = tasks.filter { DateUtils.bucket(it.due) == DateUtils.DueBucket.OVERDUE && !it.isDone }

  val todaySectionTasks: List<Task>
    get() = tasks.filter { DateUtils.bucket(it.due) == DateUtils.DueBucket.TODAY && !it.isDone }

  val upcomingTasks: List<Task>
    get() = tasks.filter {
      val b = DateUtils.bucket(it.due)
      (b == DateUtils.DueBucket.TOMORROW || b == DateUtils.DueBucket.UPCOMING) && !it.isDone
    }

  val filteredTasksForSearch: List<Task>
    get() = if (searchQuery.isBlank()) {
      tasks
    } else {
      tasks.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
          (it.projectName?.contains(searchQuery, ignoreCase = true) == true) ||
          it.labels.any { l -> l.contains(searchQuery, ignoreCase = true) }
      }
    }

  // --- Plan phase -----------------------------------------------------------

  private val activeProjectIds: Set<String>
    get() = projects.filter { !it.isArchived && it.status != "Done" }.map { it.id }.toSet()

  fun planFilterMatches(task: Task, filter: PlanFilter): Boolean {
    if (task.isDone) return false
    val today = java.time.LocalDate.now()
    val date = DateUtils.parseIsoDate(task.due)
    return when (filter) {
      PlanFilter.TODAY -> date == today
      PlanFilter.WEEK -> date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(7))
      PlanFilter.OVERDUE -> date != null && date.isBefore(today)
      PlanFilter.RECURRING -> task.isRecurring
      PlanFilter.INBOX -> task.projectId == null && task.due == null
      PlanFilter.ACTIVE_PROJECTS -> task.projectId != null && task.projectId in activeProjectIds
    }
  }

  /** Tasks shown in Plan's browse list for the selected filter. */
  val planBrowseTasks: List<Task>
    get() = tasks.filter { planFilterMatches(it, selectedPlanFilter) }
      .sortedWith(compareBy({ DateUtils.parseIsoDate(it.due) ?: java.time.LocalDate.MAX }, { it.name }))

  fun planFilterCount(filter: PlanFilter): Int = tasks.count { planFilterMatches(it, filter) }

  /** The shortlist you've committed to today. */
  val onTodayTasks: List<Task>
    get() = tasks.filter { it.isMyDay && !it.isDone }

  /** Auto shortlist candidates: overdue, due today, or flagged — not yet picked. */
  val planSuggestions: List<Task>
    get() {
      val today = java.time.LocalDate.now()
      return tasks.filter { t ->
        if (t.isDone || t.isMyDay) return@filter false
        val d = DateUtils.parseIsoDate(t.due)
        (d != null && !d.isAfter(today)) || t.priority == com.example.model.Priority.HIGH
      }.sortedWith(compareBy({ DateUtils.parseIsoDate(it.due) ?: java.time.LocalDate.MAX }))
        .take(8)
    }

  // --- Execute phase -------------------------------------------------------

  /** My Day tasks still open, ordered for "do this next": Doing, then by due, then priority. */
  val upNextTasks: List<Task>
    get() = tasks.filter { it.isMyDay && !it.isDone }
      .sortedWith(
        compareByDescending<Task> { it.status == TaskStatus.DOING }
          .thenBy { DateUtils.parseIsoDate(it.due) ?: java.time.LocalDate.MAX }
          .thenByDescending { it.priority == com.example.model.Priority.HIGH }
      )

  // --- Wrap up phase -----------------------------------------------------

  val openTodayTasks: List<Task>
    get() = tasks.filter { it.isMyDay && !it.isDone }

  /** Live Notion options for a "<db>.<Property>" key, or [fallback]. */
  fun optionsFor(key: String, fallback: List<String>): List<String> =
    schemaOptions[key]?.takeIf { it.isNotEmpty() } ?: fallback

  // --- Tasks screen -----------------------------------------------------

  fun tasksFilterMatches(task: Task, filter: TasksFilter): Boolean {
    if (filter == TasksFilter.DONE) return task.isDone
    if (task.isDone) return false
    val today = java.time.LocalDate.now()
    val date = DateUtils.parseIsoDate(task.due)
    return when (filter) {
      TasksFilter.ALL -> true
      TasksFilter.DONE -> task.isDone
      TasksFilter.TODAY -> date == today
      TasksFilter.OVERDUE -> date != null && date.isBefore(today)
      TasksFilter.MY_DAY -> task.isMyDay
      TasksFilter.HIGH_PRIORITY -> task.priority == com.example.model.Priority.HIGH
      TasksFilter.RECURRING -> task.isRecurring
    }
  }

  fun tasksFilterCount(filter: TasksFilter): Int = tasks.count { tasksFilterMatches(it, filter) }

  /** Flat, sorted list for a non-ALL filter. */
  fun tasksForFilter(filter: TasksFilter): List<Task> =
    tasks.filter { tasksFilterMatches(it, filter) }
      .let { list ->
        if (filter == TasksFilter.DONE) list.sortedByDescending { it.completionDate ?: "" }
        else list.sortedWith(compareBy({ DateUtils.parseIsoDate(it.due) ?: java.time.LocalDate.MAX }, { it.name }))
      }
}

class MyDayViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(MyDayUiState())
  val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()

  // Audit Finding 15 (Round 5): navigation-intent event stream. Previously,
  // MainActivity keyed a `LaunchedEffect(uiState.currentScreen)` off the full
  // currentScreen field, which mutates for many reasons (toggle task, post a
  // snackbar, etc.). Combined with launchSingleTop=true, repeated silent
  // no-op navigations were inevitable. Now we expose a one-shot Flow of
  // explicit NavIntent events; MainActivity collects with LaunchedEffect(Unit).
  // The Channel is unbounded so emit() never suspends, and we keep
  // `currentScreen` on UiState for read-only consumers (e.g. audit breadcrumbs).
  private val _navigationEvents = Channel<NavIntent>(Channel.BUFFERED)
  val navigationEvents: Flow<NavIntent> = _navigationEvents.receiveAsFlow()

  private val repo = UbRepository()

  init {
    startTimerLoop()
    _uiState.update {
      it.copy(customFilters = FilterStore.loadFilters(), chipConfigs = FilterStore.loadConfigs())
    }
    if (repo.isRemote) {
      refreshFromNotion()
      loadLibrary()
    }
  }

  // ---- Customizable filter bars ------------------------------------------

  fun selectFilterKey(scope: FilterScope, key: String) {
    _uiState.update { it.copy(selectedFilterKeys = it.selectedFilterKeys + (scope.name to key)) }
  }

  fun saveCustomFilter(filter: CustomFilter) {
    _uiState.update { st ->
      val next = st.customFilters.filterNot { it.id == filter.id } + filter
      FilterStore.saveFilters(next)
      st.copy(
        customFilters = next,
        selectedFilterKeys = st.selectedFilterKeys + (filter.scope.name to customKey(filter.id)),
      )
    }
  }

  fun deleteCustomFilter(scope: FilterScope, id: String) {
    _uiState.update { st ->
      val next = st.customFilters.filterNot { it.id == id }
      FilterStore.saveFilters(next)
      val key = customKey(id)
      val cfg = st.chipConfigs[scope.name]
      val nextConfigs =
        if (cfg == null) st.chipConfigs
        else st.chipConfigs + (scope.name to cfg.copy(
          order = cfg.order.filterNot { it == key },
          hidden = cfg.hidden - key,
          defaultKey = cfg.defaultKey?.takeIf { it != key },
        ))
      FilterStore.saveConfigs(nextConfigs)
      st.copy(
        customFilters = next,
        chipConfigs = nextConfigs,
        selectedFilterKeys = st.selectedFilterKeys.filterNot { it.value == key },
      )
    }
  }

  fun saveChipConfig(scope: FilterScope, config: ChipRowConfig) {
    _uiState.update { st ->
      val next = st.chipConfigs + (scope.name to config)
      FilterStore.saveConfigs(next)
      st.copy(chipConfigs = next)
    }
  }

  fun loadLibrary() {
    if (!repo.isRemote) return
    viewModelScope.launch {
      try {
        val lib = repo.loadLibrary()
        _uiState.update {
          it.copy(
            people = lib.people, books = lib.books, readingLog = lib.readingLog,
            genres = lib.genres, recipes = lib.recipes, mealPlan = lib.mealPlan,
            workSessions = lib.workSessions,
            libraryLoaded = true,
          )
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(syncError = e.message ?: "Library load failed") }
      }
    }
  }

  // --- Library nav + edits ---
  fun openPerson(id: String) { _uiState.update { it.copy(selectedPersonId = id) }; emitNav(AppScreen.PERSON_DETAIL) }
  fun openBook(id: String) { _uiState.update { it.copy(selectedBookId = id) }; emitNav(AppScreen.BOOK_DETAIL); loadDetailBody(id) }
  fun openRecipe(id: String) { _uiState.update { it.copy(selectedRecipeId = id) }; emitNav(AppScreen.RECIPE_DETAIL); loadDetailBody(id) }

  fun setBookStatus(id: String, status: String) {
    _uiState.update { s -> s.copy(books = s.books.map { if (it.id == id) it.copy(status = status) else it }) }
    remoteWrite { it.setPageStatus(id, "Status", status) }
  }
  fun toggleRecipeFavorite(id: String) {
    val fav = !(_uiState.value.recipes.firstOrNull { it.id == id }?.favorite ?: false)
    _uiState.update { s -> s.copy(recipes = s.recipes.map { if (it.id == id) it.copy(favorite = fav) else it }) }
    remoteWrite { it.setPageCheckbox(id, "Favorite", fav) }
  }
  fun createPerson(name: String) = createLib(com.example.data.notion.NotionConfig.peopleDsId, "Full Name", name) { loadLibrary() }
  fun createBook(title: String) = createLib(com.example.data.notion.NotionConfig.booksDsId, "Title", title) { loadLibrary() }
  fun createRecipe(name: String) = createLib(com.example.data.notion.NotionConfig.recipesDsId, "Name", name) { loadLibrary() }
  fun createReadingLog(name: String) = createLib(com.example.data.notion.NotionConfig.readingLogDsId, "Name", name) { loadLibrary() }
  fun createGenre(name: String) = createLib(com.example.data.notion.NotionConfig.genresDsId, "Name", name) { loadLibrary() }
  fun createMealPlan(name: String) = createLib(com.example.data.notion.NotionConfig.mealPlannerDsId, "Name", name) { loadLibrary() }

  private fun createLib(dsId: String, titleProp: String, title: String, after: () -> Unit) {
    if (!repo.isRemote || title.isBlank()) return
    viewModelScope.launch {
      try { repo.createInDb(dsId, titleProp, title); after() }
      catch (e: Exception) { _uiState.update { it.copy(syncError = e.message ?: "Create failed") } }
    }
  }

  val selectedPerson get() = uiState.value.people.firstOrNull { it.id == uiState.value.selectedPersonId }
  val selectedBook get() = uiState.value.books.firstOrNull { it.id == uiState.value.selectedBookId }
  val selectedRecipe get() = uiState.value.recipes.firstOrNull { it.id == uiState.value.selectedRecipeId }

  /** Pull the full workspace from Notion and replace the local state. */
  fun loadOlderCompleted() {
    if (!repo.isRemote || _uiState.value.loadingOlder) return
    _uiState.update { it.copy(loadingOlder = true) }
    viewModelScope.launch {
      try {
        val w = repo.loadWorkspace(doneLookbackDays = 365)
        _uiState.update { st ->
          val byId = w.tasks.associateBy { it.id }
          val merged = (st.tasks.associateBy { it.id } + byId).values.toList()
          st.copy(tasks = merged, loadingOlder = false, olderCompletedLoaded = true)
        }
      } catch (_: Exception) {
        _uiState.update { it.copy(loadingOlder = false) }
      }
    }
  }

  fun refreshFromNotion() {
    if (!repo.isRemote) return
    _uiState.update { it.copy(isRemote = true, isSyncing = true, syncError = null) }
    viewModelScope.launch {
      launch {
        val opts = try { repo.loadSchemaOptions() } catch (_: Exception) { emptyMap() }
        if (opts.isNotEmpty()) _uiState.update { it.copy(schemaOptions = opts) }
      }
      launch {
        val users = try { repo.loadUsers() } catch (_: Exception) { emptyList() }
        if (users.isNotEmpty()) _uiState.update { it.copy(workspaceUsers = users) }
      }
      try {
        val w = repo.loadWorkspace()
        android.util.Log.i(
          "UbSync",
          "loaded tasks=${w.tasks.size} projects=${w.projects.size} notes=${w.notes.size} goals=${w.goals.size} tags=${w.tags.size}",
        )
        _uiState.update { state ->
          state.copy(
            tasks = w.tasks,
            projects = w.projects,
            notes = w.notes,
            goals = w.goals,
            tags = if (w.tags.isNotEmpty()) w.tags else state.tags,
            milestones = if (w.milestones.isNotEmpty()) w.milestones else state.milestones,
            isSyncing = false,
            syncError = null,
          )
        }
        drainPendingWrites()
      } catch (e: Exception) {
        android.util.Log.e("UbSync", "workspace load failed", e)
        _uiState.update { it.copy(isSyncing = false, syncError = e.message ?: "Sync failed") }
      }
    }
  }

  // Offline-tolerant write queue. A failed Notion write is retried with
  // backoff instead of being silently dropped; the UI shows the backlog.
  private val pendingWrites = java.util.concurrent.CopyOnWriteArrayList<suspend (UbRepository) -> Unit>()
  private var draining = false

  /** Fire a write to Notion without blocking the optimistic local update. */
  private fun remoteWrite(block: suspend (UbRepository) -> Unit) {
    if (!repo.isRemote) return
    viewModelScope.launch {
      try {
        block(repo)
      } catch (_: Exception) {
        pendingWrites.add(block)
        _uiState.update { it.copy(pendingWriteCount = pendingWrites.size, syncError = "Offline — ${pendingWrites.size} change(s) will retry") }
        drainPendingWrites()
      }
    }
  }

  fun drainPendingWrites() {
    if (draining || pendingWrites.isEmpty() || !repo.isRemote) return
    draining = true
    viewModelScope.launch {
      var backoff = 2000L
      while (pendingWrites.isNotEmpty()) {
        val batch = pendingWrites.toList()
        var anyFailed = false
        for (w in batch) {
          try {
            w(repo)
            pendingWrites.remove(w)
            _uiState.update { it.copy(pendingWriteCount = pendingWrites.size) }
          } catch (_: Exception) {
            anyFailed = true
          }
        }
        if (pendingWrites.isEmpty()) {
          _uiState.update { it.copy(pendingWriteCount = 0, syncError = null) }
          break
        }
        if (anyFailed) {
          delay(backoff)
          backoff = (backoff * 2).coerceAtMost(60_000L)
        }
      }
      draining = false
    }
  }

  private fun startTimerLoop() {
    viewModelScope.launch {
      while (true) {
        delay(1000)
        if (_uiState.value.isTimerRunning) {
          _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        }
      }
    }
  }

  fun navigateTo(screen: AppScreen) {
    // Audit Finding 15: emit an explicit navigation intent instead of relying
    // on MainActivity's LaunchedEffect(uiState.currentScreen) re-firing on
    // every unrelated state mutation. The UiState field is still updated so
    // any read-only consumer (breadcrumbs, tests) sees the latest screen.
    _uiState.update { it.copy(currentScreen = screen) }
    _navigationEvents.trySend(NavIntent.Navigate(screen))
  }

  /**
   * Emit a navigation intent for a screen whose `currentScreen` was already
   * set inside a larger `_uiState.update { ... }` (detail opens, editor opens,
   * post-save redirects). Without this, those transitions updated state but
   * never reached the NavHost back stack after the Round 5 nav-bridge rewrite.
   */
  private fun emitNav(screen: AppScreen) {
    _navigationEvents.trySend(NavIntent.Navigate(screen))
  }

  /**
   * In-app back navigation (top-bar back arrows on detail / sub screens).
   * Pops the NavHost back stack instead of pushing the parent route on top
   * of itself, which previously made the stack grow without bound.
   */
  fun navigateBack() {
    _navigationEvents.trySend(NavIntent.Back)
  }

  /**
   * Toggle for "Dynamic Color (Material You)" in SettingsScreen. Wired
   * through MainActivity → MyApplicationTheme so the theme actually flips
   * when the user toggles the switch. Audit Finding 16.
   */
  fun setDynamicColorEnabled(enabled: Boolean) {
    _uiState.update { it.copy(dynamicColorEnabled = enabled) }
  }

  fun openTaskDetail(taskId: String) {
    _uiState.update {
      it.copy(
        selectedTaskId = taskId,
        currentScreen = AppScreen.TASK_DETAIL
      )
    }
    emitNav(AppScreen.TASK_DETAIL)
    loadDetailBody(taskId)
  }

  /** Open the per-task work-session history / charts screen. */
  fun openTaskWorkSessions(taskId: String) {
    _uiState.update {
      it.copy(
        selectedTaskId = taskId,
        currentScreen = AppScreen.TASK_WORK_SESSIONS,
        // Seed from what's already loaded so the screen never starts empty.
        taskWorkSessions = it.workSessions.filter { s -> s.taskId == taskId },
        taskWorkSessionsForId = taskId,
        taskWorkSessionsLoading = repo.isRemote,
      )
    }
    emitNav(AppScreen.TASK_WORK_SESSIONS)
    if (!repo.isRemote) return
    viewModelScope.launch {
      val sessions = try { repo.loadWorkSessionsForTask(taskId) } catch (_: Exception) { null }
      _uiState.update {
        if (it.taskWorkSessionsForId != taskId) it
        else it.copy(
          taskWorkSessions = sessions ?: it.taskWorkSessions,
          taskWorkSessionsLoading = false,
        )
      }
    }
  }

  /** Load a recurring task's past occurrences for the History tab (lazy, cached per task). */
  fun loadTaskOccurrences(taskId: String) {
    val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
    if (_uiState.value.taskOccurrencesForId == taskId && _uiState.value.taskOccurrences.isNotEmpty()) return
    val seed = task.occurrenceIds.mapNotNull { id -> _uiState.value.tasks.firstOrNull { it.id == id } }
    _uiState.update {
      it.copy(
        taskOccurrences = seed,
        taskOccurrencesForId = taskId,
        taskOccurrencesLoading = repo.isRemote && task.occurrenceIds.isNotEmpty(),
      )
    }
    if (!repo.isRemote || task.occurrenceIds.isEmpty()) return
    viewModelScope.launch {
      val loaded = try { repo.loadOccurrencesForTask(task.occurrenceIds) } catch (_: Exception) { null }
      _uiState.update {
        if (it.taskOccurrencesForId != taskId) it
        else it.copy(
          taskOccurrences = loaded?.takeIf { l -> l.isNotEmpty() } ?: it.taskOccurrences,
          taskOccurrencesLoading = false,
        )
      }
    }
  }

  /** Fetch a page's markdown body for the detail screens. */
  private fun loadDetailBody(pageId: String) {
    if (!repo.isRemote) {
      _uiState.update { it.copy(detailBody = null, detailBodyForId = pageId, detailBodyLoading = false) }
      return
    }
    _uiState.update { it.copy(detailBody = null, detailBodyForId = pageId, detailBodyLoading = true) }
    viewModelScope.launch {
      val body = try { repo.getPageBody(pageId) } catch (_: Exception) { null }
      _uiState.update {
        if (it.detailBodyForId == pageId) it.copy(detailBody = body, detailBodyLoading = false) else it
      }
    }
  }

  fun selectTasksFilter(filter: TasksFilter) {
    _uiState.update { it.copy(selectedTasksFilter = filter) }
  }

  fun selectPhase(phase: DailyRitualPhase) {
    _uiState.update { it.copy(selectedPhase = phase) }
  }

  fun selectPlanFilter(filter: PlanFilter) {
    _uiState.update { it.copy(selectedPlanFilter = filter) }
  }

  fun selectExecuteFilter(filter: ExecuteFilter) {
    _uiState.update { it.copy(selectedExecuteFilter = filter) }
  }

  fun toggleTimer() {
    _uiState.update { it.copy(isTimerRunning = !it.isTimerRunning) }
  }

  // --- Focus timer (foreground service + Notion Work Session) ---------------

  /** The single active focus session, or null. Observed by the Execute UI. */
  val focusSession: StateFlow<FocusSession?> = FocusController.session

  fun startFocus(context: Context, task: Task) {
    FocusController.start(task.id, task.name, task.projectName)
    FocusTimerService.start(context)
    // Move the task into Doing so Execute reflects reality.
    if (task.status != TaskStatus.DOING) {
      _uiState.update { s ->
        s.copy(tasks = s.tasks.map { if (it.id == task.id) it.copy(status = TaskStatus.DOING) else it })
      }
      remoteWrite { it.setTaskStatus(task.id, TaskStatus.DOING) }
    }
  }

  fun pauseFocus(context: Context) {
    FocusController.pause()
    FocusTimerService.send(context, FocusTimerService.ACTION_PAUSE)
  }

  fun resumeFocus(context: Context) {
    FocusController.resume()
    FocusTimerService.send(context, FocusTimerService.ACTION_RESUME)
  }

  fun stopFocus(context: Context) {
    val session = FocusController.stop()
    FocusTimerService.stop(context)
    if (session == null) return
    val elapsedSec = session.elapsedMs() / 1000
    _uiState.update { it.copy(focusedSecondsToday = it.focusedSecondsToday + elapsedSec) }
    // Log anything longer than a stray tap. Notion computes Duration from Start/End.
    if (elapsedSec >= 10) {
      val endIso = java.time.Instant.now().toString()
      // Optimistically show it in the History list right away.
      val optimistic = WorkSessionModel(
        id = "ws-${UUID.randomUUID().toString().take(6)}",
        taskName = session.taskName,
        taskId = session.taskId,
        projectName = session.projectName.orEmpty(),
        timeRange = "",
        duration = "",
        startIso = session.startIso,
        endIso = endIso,
        durationMinutes = (elapsedSec / 60).toInt(),
      )
      _uiState.update { it.copy(workSessions = listOf(optimistic) + it.workSessions) }
      if (repo.isRemote) {
        viewModelScope.launch {
          try {
            repo.createWorkSession(session.taskId, session.taskName, session.startIso, endIso)
            loadLibrary()
          } catch (e: Exception) {
            _uiState.update { it.copy(syncError = e.message ?: "Could not save work session") }
          }
        }
      }
    }
    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.FocusSessionEnded) }
  }

  // --- Wrap up -----------------------------------------------------------

  /** Push every still-open My-Day task to tomorrow and drop it from today. */
  fun moveAllOpenToTomorrow() {
    val tomorrow = java.time.LocalDate.now().plusDays(1)
    val iso = tomorrow.toString()
    val targets = _uiState.value.tasks.filter { it.isMyDay && !it.isDone }
    if (targets.isEmpty()) {
      _uiState.update { it.copy(snackbarMessage = SnackbarMessage.MyDayAlreadyEmpty) }
      return
    }
    _uiState.update { s ->
      s.copy(
        tasks = s.tasks.map {
          if (it.isMyDay && !it.isDone) it.copy(isMyDay = false, due = iso, dueDisplay = "Tomorrow", isOverdue = false) else it
        },
        snackbarMessage = SnackbarMessage.AllOverdueMoved,
      )
    }
    targets.forEach { t -> remoteWrite { repo -> repo.setTaskDue(t.id, iso); repo.setTaskMyDay(t.id, false) } }
  }

  fun endLiveSession(taskId: String) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(isActiveSession = false)
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        isTimerRunning = false,
        snackbarMessage = SnackbarMessage.FocusSessionEnded
      )
    }
  }

  fun toggleTaskCompletion(taskId: String) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          val newDone = !task.isDone
          task.copy(
            isDone = newDone,
            status = if (newDone) TaskStatus.DONE else TaskStatus.TODO,
            completionDate = if (newDone) "Today" else null
          )
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        snackbarMessage = SnackbarMessage.TaskStatusUpdated
      )
    }
    val nowDone = _uiState.value.tasks.firstOrNull { it.id == taskId }?.isDone == true
    remoteWrite { it.setTaskStatus(taskId, if (nowDone) TaskStatus.DONE else TaskStatus.TODO) }
  }

  fun toggleTaskStatus(taskId: String) = toggleTaskCompletion(taskId)

  fun toggleMyDay(taskId: String) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(isMyDay = !task.isMyDay)
        } else {
          task
        }
      }
      state.copy(tasks = updated)
    }
    val myDay = _uiState.value.tasks.firstOrNull { it.id == taskId }?.isMyDay == true
    remoteWrite { it.setTaskMyDay(taskId, myDay) }
  }

  fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(
            status = newStatus,
            isDone = (newStatus == TaskStatus.DONE)
          )
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        snackbarMessage = SnackbarMessage.TaskStatusChanged(newStatus)
      )
    }
    remoteWrite { it.setTaskStatus(taskId, newStatus) }
  }

  fun renameTask(taskId: String, name: String) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    _uiState.update { s -> s.copy(tasks = s.tasks.map { if (it.id == taskId) it.copy(name = trimmed) else it }) }
    remoteWrite { it.setPageTitle(taskId, "Name", trimmed) }
  }

  fun renameProject(projectId: String, name: String) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    _uiState.update { s -> s.copy(projects = s.projects.map { if (it.id == projectId) it.copy(name = trimmed) else it }) }
    remoteWrite { it.setPageTitle(projectId, "Name", trimmed) }
  }

  fun renameGoal(goalId: String, name: String) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    _uiState.update { s -> s.copy(goals = s.goals.map { if (it.id == goalId) it.copy(name = trimmed) else it }) }
    remoteWrite { it.setPageTitle(goalId, "Name", trimmed) }
  }

  fun renameNote(noteId: String, name: String) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    _uiState.update { s -> s.copy(notes = s.notes.map { if (it.id == noteId) it.copy(title = trimmed) else it }) }
    remoteWrite { it.setPageTitle(noteId, "Name", trimmed) }
  }

  fun renameTag(tagId: String, name: String) {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return
    _uiState.update { s -> s.copy(tags = s.tags.map { if (it.id == tagId) it.copy(name = trimmed) else it }) }
    remoteWrite { it.setPageTitle(tagId, "Name", trimmed) }
  }

  fun updateTaskPriority(taskId: String, newPriority: Priority?) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(priority = newPriority)
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        snackbarMessage = SnackbarMessage.PriorityUpdated(newPriority)
      )
    }
    remoteWrite { it.setTaskPriority(taskId, newPriority) }
  }

  private fun patchTask(taskId: String, patch: (Task) -> Task) {
    _uiState.update { s -> s.copy(tasks = s.tasks.map { if (it.id == taskId) patch(it) else it }) }
  }

  fun setTaskDescription(taskId: String, text: String) {
    patchTask(taskId) { it.copy(description = text) }
    remoteWrite { it.setTaskText(taskId, "Description", text) }
  }

  fun setTaskEnergy(taskId: String, value: String?) {
    patchTask(taskId) { it.copy(energy = value) }
    remoteWrite { it.setTaskSelect(taskId, "Energy", value) }
  }

  fun setTaskLocation(taskId: String, value: String?) {
    patchTask(taskId) { it.copy(location = value) }
    remoteWrite { it.setTaskSelect(taskId, "Location", value) }
  }

  fun setTaskSmartList(taskId: String, value: String?) {
    patchTask(taskId) { it.copy(smartList = value) }
    remoteWrite { it.setTaskSelect(taskId, "Smart List", value) }
  }

  fun setTaskDueDate(taskId: String, iso: String?, endIso: String? = null) {
    patchTask(taskId) {
      it.copy(
        due = iso?.substringBefore('T'),
        dueEndIso = endIso,
        dueDisplay = com.example.data.DateUtils.displayLabel(iso).ifBlank { "" },
        isOverdue = iso != null && com.example.data.DateUtils.bucket(iso.substringBefore('T')) == com.example.data.DateUtils.DueBucket.OVERDUE && !it.isDone,
      )
    }
    remoteWrite { it.setTaskDue(taskId, iso, endIso) }
  }

  fun setTaskProjectRelation(taskId: String, projectId: String?) {
    val name = projectId?.let { pid -> _uiState.value.projects.find { it.id == pid }?.name }
    patchTask(taskId) { it.copy(projectId = projectId, projectName = name) }
    remoteWrite { it.setTaskProject(taskId, projectId) }
  }

  fun setTaskLabelSet(taskId: String, labels: List<String>) {
    patchTask(taskId) { it.copy(labels = labels) }
    remoteWrite { it.setTaskLabels(taskId, labels) }
  }

  fun setTaskRecurrence(taskId: String, unit: String?, interval: Int) {
    patchTask(taskId) {
      it.copy(
        recurUnit = unit, recurInterval = interval,
        isRecurring = unit != null,
        recurrenceText = unit?.let { u -> "every $interval $u" },
      )
    }
    remoteWrite { it.setTaskRecurrence(taskId, unit, interval) }
  }

  fun setTaskSnooze(taskId: String, iso: String?) {
    patchTask(taskId) { it.copy(snoozeIso = iso) }
    remoteWrite { it.setTaskDate(taskId, "Snooze", iso) }
  }

  fun setTaskWaitDate(taskId: String, iso: String?) {
    patchTask(taskId) { it.copy(waitIso = iso) }
    remoteWrite { it.setTaskDate(taskId, "Wait Date", iso) }
  }

  fun setTaskProcessImmersive(taskId: String, value: String?) {
    patchTask(taskId) { it.copy(processImmersive = value) }
    remoteWrite { it.setTaskSelect(taskId, "P/I", value) }
  }

  fun setTaskEnforceSchedule(taskId: String, value: Boolean) {
    patchTask(taskId) { it.copy(enforceSchedule = value) }
    remoteWrite { it.setTaskCheckbox(taskId, "Enforce Schedule", value) }
  }

  fun setTaskShoppingList(taskId: String, value: Boolean) {
    patchTask(taskId) { it.copy(shoppingList = value) }
    remoteWrite { it.setTaskCheckbox(taskId, "Shopping List", value) }
  }

  fun toggleTaskAssignee(taskId: String, userId: String, userName: String) {
    val cur = _uiState.value.tasks.find { it.id == taskId }?.assigneeIds ?: emptyList()
    val next = if (userId in cur) cur - userId else cur + userId
    val names = next.mapNotNull { u -> _uiState.value.workspaceUsers.firstOrNull { it.first == u }?.second }
    patchTask(taskId) { it.copy(assigneeIds = next, assigneeNames = names) }
    remoteWrite { it.setTaskPeopleProp(taskId, "Assignee", next) }
  }

  fun toggleTaskPerson(taskId: String, personId: String) {
    val cur = _uiState.value.tasks.find { it.id == taskId }?.personIds ?: emptyList()
    val next = if (personId in cur) cur - personId else cur + personId
    patchTask(taskId) { it.copy(personIds = next) }
    remoteWrite { it.setTaskRelation(taskId, "People", next) }
  }

  fun toggleTaskRecurDay(taskId: String, day: String) {
    val current = _uiState.value.tasks.find { it.id == taskId }?.recurDays ?: emptyList()
    val next = if (day in current) current - day else current + day
    patchTask(taskId) { it.copy(recurDays = next) }
    remoteWrite { it.setTaskMulti(taskId, "Days", next) }
  }

  private fun patchProject(id: String, f: (ProjectModel) -> ProjectModel) {
    _uiState.update { s -> s.copy(projects = s.projects.map { if (it.id == id) f(it) else it }) }
  }
  private fun patchGoal(id: String, f: (GoalModel) -> GoalModel) {
    _uiState.update { s -> s.copy(goals = s.goals.map { if (it.id == id) f(it) else it }) }
  }
  private fun patchNote(id: String, f: (NoteModel) -> NoteModel) {
    _uiState.update { s -> s.copy(notes = s.notes.map { if (it.id == id) f(it) else it }) }
  }

  fun setProjectStatus(id: String, status: String) {
    patchProject(id) { it.copy(status = status) }
    remoteWrite { it.setProjectStatus(id, status) }
  }
  fun setProjectDeadline(id: String, iso: String?) {
    patchProject(id) { it.copy(deadline = DateUtils.displayLabel(iso).ifBlank { "—" }, deadlineIso = iso) }
    remoteWrite { it.setProjectDeadline(id, iso) }
  }
  fun setProjectReviewNotes(id: String, text: String) {
    patchProject(id) { it.copy(reviewNotes = text) }
    remoteWrite { it.setProjectReviewNotes(id, text) }
  }
  fun toggleProjectTag(id: String, tagId: String) {
    val cur = _uiState.value.projects.find { it.id == id }?.tagIds ?: emptyList()
    val next = if (tagId in cur) cur - tagId else cur + tagId
    val names = next.mapNotNull { t -> _uiState.value.tags.find { it.id == t }?.name }.map { "#$it" }
    patchProject(id) { it.copy(tagIds = next, tags = names) }
    remoteWrite { it.setProjectRelation(id, "Tag", next) }
  }
  fun toggleProjectPerson(id: String, personId: String) {
    val cur = _uiState.value.projects.find { it.id == id }?.personIds ?: emptyList()
    val next = if (personId in cur) cur - personId else cur + personId
    patchProject(id) { it.copy(personIds = next) }
    remoteWrite { it.setProjectRelation(id, "People", next) }
  }
  fun setProjectGoalRelation(id: String, goalId: String?) {
    val name = goalId?.let { g -> _uiState.value.goals.find { it.id == g }?.name }
    patchProject(id) { it.copy(goalName = name) }
    remoteWrite { it.setProjectGoal(id, goalId) }
  }
  fun setGoalDeadline(id: String, iso: String?) {
    patchGoal(id) { it.copy(deadline = DateUtils.displayLabel(iso).ifBlank { "—" }, deadlineIso = iso) }
    remoteWrite { it.setGoalDeadline(id, iso) }
  }
  fun setGoalStatusValue(id: String, status: String) {
    patchGoal(id) { it.copy(status = status) }
    remoteWrite { it.setGoalStatus(id, status) }
  }
  fun setGoalSetDate(id: String, iso: String?) {
    patchGoal(id) { it.copy(goalSetIso = iso) }
    remoteWrite { it.setGoalDate(id, "Goal Set", iso) }
  }
  fun setGoalTagRelation(id: String, tagId: String?) {
    val name = tagId?.let { t -> _uiState.value.tags.find { it.id == t }?.name } ?: ""
    patchGoal(id) { it.copy(tagId = tagId, tagArea = name) }
    remoteWrite { it.setGoalTag(id, tagId) }
  }
  fun toggleNoteTag(id: String, tagId: String) {
    val cur = _uiState.value.notes.find { it.id == id }?.tagIds ?: emptyList()
    val next = if (tagId in cur) cur - tagId else cur + tagId
    val names = next.mapNotNull { t -> _uiState.value.tags.find { it.id == t }?.name }.map { "#$it" }
    patchNote(id) { it.copy(tagIds = next, tags = names) }
    remoteWrite { it.setNoteRelation(id, "Tag", next) }
  }
  fun setNoteUrl(id: String, url: String) {
    patchNote(id) { it.copy(url = url) }
    remoteWrite { it.setNoteUrl(id, url) }
  }
  fun setNoteReviewDate(id: String, iso: String?) {
    patchNote(id) { it.copy(reviewDateIso = iso) }
    remoteWrite { it.setNoteReviewDate(id, iso) }
  }
  fun setNoteType(id: String, type: String) {
    patchNote(id) { it.copy(type = type) }
    remoteWrite { it.updateNoteMeta(id, _uiState.value.notes.first { it.id == id }.title, type) }
  }
  fun setNoteDate(id: String, iso: String?) {
    patchNote(id) { it.copy(date = DateUtils.displayLabel(iso).ifBlank { "—" }, dateIso = iso) }
    remoteWrite { it.setNoteDate(id, iso) }
  }
  fun setNoteProjectRelation(id: String, projectId: String?) {
    val name = projectId?.let { p -> _uiState.value.projects.find { it.id == p }?.name }
    patchNote(id) { it.copy(projectName = name) }
    remoteWrite { it.setNoteProject(id, projectId) }
  }

  fun toggleSubTask(taskId: String, subTaskId: String) {
    var nowDone = false
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(subTasks = task.subTasks.map { st ->
            if (st.id == subTaskId) { nowDone = !st.isCompleted; st.copy(isCompleted = !st.isCompleted) } else st
          })
        } else task
      }
      state.copy(tasks = updated)
    }
    // Sub-tasks are real child task pages in Notion — persist the toggle.
    if (!subTaskId.startsWith("st-")) {
      remoteWrite { it.setTaskStatus(subTaskId, if (nowDone) TaskStatus.DONE else TaskStatus.TODO) }
    }
  }

  fun addSubTask(taskId: String, name: String) {
    if (name.isBlank()) return
    val tempId = "st-${UUID.randomUUID().toString().take(6)}"
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(
            subTasks = task.subTasks + SubTask(id = tempId, name = name.trim(), isCompleted = false),
            subTasksCount = task.subTasks.size + 1,
          )
        } else task
      }
      state.copy(tasks = updated, snackbarMessage = SnackbarMessage.SubTaskAdded)
    }
    if (repo.isRemote) {
      viewModelScope.launch {
        try {
          val realId = repo.createTask(name.trim(), null, null, myDay = false, parentTaskId = taskId)
          if (realId != null) _uiState.update { s ->
            s.copy(tasks = s.tasks.map { t ->
              if (t.id == taskId) t.copy(subTasks = t.subTasks.map { if (it.id == tempId) it.copy(id = realId) else it }) else t
            })
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(syncError = e.message ?: "Could not save sub-task") }
        }
      }
    }
  }


  fun toggleDoneAccordion() {
    _uiState.update { it.copy(isDoneExpanded = !it.isDoneExpanded) }
  }

  fun setQuickAddOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isQuickAddOpen = isOpen) }
  }

  fun openTaskQuickEdit(taskId: String) {
    _uiState.update { it.copy(quickEditTaskId = taskId, quickEditNoteId = null) }
  }

  fun openNoteQuickEdit(noteId: String) {
    _uiState.update { it.copy(quickEditNoteId = noteId, quickEditTaskId = null) }
  }

  fun closeQuickEdit() {
    _uiState.update { it.copy(quickEditTaskId = null, quickEditNoteId = null) }
  }

  fun setSearchOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isSearchOpen = isOpen, searchQuery = if (isOpen) it.searchQuery else "") }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun setEveningStep(step: Int) {
    _uiState.update { it.copy(eveningReviewStep = step) }
  }

  fun clearMyDay() {
    _uiState.update { state ->
      val myDayCount = state.tasks.count { it.isMyDay }
      if (myDayCount == 0) {
        // No-op — don't claim we cleared anything if nothing was on My Day.
        state.copy(snackbarMessage = SnackbarMessage.MyDayAlreadyEmpty)
      } else {
        val updated = state.tasks.map {
          if (it.isMyDay) it.copy(isMyDay = false) else it
        }
        state.copy(
          tasks = updated,
          snackbarMessage = SnackbarMessage.MyDayCleared(myDayCount)
        )
      }
    }
  }

  fun postponeOverdueTask(taskId: String) {
    val iso = java.time.LocalDate.now().plusDays(1).toString()
    _uiState.update { state ->
      state.copy(
        tasks = state.tasks.map { task ->
          if (task.id == taskId) task.copy(due = iso, dueDisplay = "Tomorrow", isOverdue = false, isMyDay = false) else task
        },
        snackbarMessage = SnackbarMessage.TaskPostponed,
      )
    }
    remoteWrite { it.setTaskDue(taskId, iso); it.setTaskMyDay(taskId, false) }
  }

  fun rescheduleAllOverdue() {
    val iso = java.time.LocalDate.now().plusDays(1).toString()
    val ids = _uiState.value.tasks.filter { it.isOverdue }.map { it.id }
    _uiState.update { state ->
      state.copy(
        tasks = state.tasks.map { task ->
          if (task.isOverdue) task.copy(due = iso, dueDisplay = "Tomorrow", isOverdue = false) else task
        },
        snackbarMessage = SnackbarMessage.AllOverdueMoved,
      )
    }
    ids.forEach { id -> remoteWrite { it.setTaskDue(id, iso) } }
  }

  fun addNewTask(
    name: String,
    projectId: String?,
    priority: Priority?,
    isMyDay: Boolean,
    dueIso: String? = null,
  ) {
    if (name.isBlank()) return
    val projectName = projectId?.let { id -> _uiState.value.projects.find { it.id == id }?.name }
    val newTask = Task(
      id = "tk-custom-${UUID.randomUUID().toString().take(6)}",
      name = name.trim(),
      status = TaskStatus.TODO,
      priority = priority,
      due = dueIso,
      dueDisplay = com.example.data.DateUtils.displayLabel(dueIso).ifBlank { "Today" },
      isMyDay = isMyDay,
      projectName = projectName,
      labels = emptyList(),
    )
    _uiState.update { state ->
      state.copy(
        tasks = listOf(newTask) + state.tasks,
        isQuickAddOpen = false,
        snackbarMessage = SnackbarMessage.TaskAdded(name.trim())
      )
    }
    if (repo.isRemote) {
      viewModelScope.launch {
        try {
          val realId = repo.createTask(name.trim(), projectId, priority, isMyDay, dueIso)
          if (realId != null) {
            // Swap the optimistic row's temp id for the real Notion page id.
            _uiState.update { state ->
              state.copy(tasks = state.tasks.map { if (it.id == newTask.id) it.copy(id = realId) else it })
            }
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(syncError = e.message ?: "Could not create task") }
        }
      }
    }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  /**
   * Maps a sealed [SnackbarMessage] to its localized string resource id.
   *
   * The ViewModel itself stays [android.content.Context]-free so it can be
   * unit-tested without an Android runtime. The screen does the actual
   * `Context.getString(...)` lookup, which gives us full i18n + RTL for free.
   */
  fun resolveSnackbarMessageRes(msg: SnackbarMessage): Int = when (msg) {
    SnackbarMessage.FocusSessionEnded -> R.string.snack_focus_session_ended
    SnackbarMessage.TaskStatusUpdated -> R.string.snack_task_status_updated
    is SnackbarMessage.TaskStatusChanged -> R.string.snack_task_status_changed_format
    is SnackbarMessage.PriorityUpdated -> R.string.snack_priority_updated_format
    SnackbarMessage.SubTaskAdded -> R.string.snack_subtask_added
    SnackbarMessage.MyDayAlreadyEmpty -> R.string.snack_myday_already_empty
    is SnackbarMessage.MyDayCleared ->
      if (msg.count == 1) R.string.snack_myday_cleared_format
      else R.string.snack_myday_cleared_plural_format
    SnackbarMessage.TaskPostponed -> R.string.snack_task_postponed
    SnackbarMessage.AllOverdueMoved -> R.string.snack_all_overdue_moved
    is SnackbarMessage.TaskAdded -> R.string.snack_task_added_format
    is SnackbarMessage.ProjectSaved -> R.string.snack_project_saved_format
    SnackbarMessage.ProjectArchived -> R.string.snack_project_archived
    SnackbarMessage.NoteSaved -> R.string.snack_note_saved
    SnackbarMessage.GoalDropped -> R.string.snack_goal_dropped
    SnackbarMessage.GoalAchieved -> R.string.snack_goal_achieved
  }

  /**
   * Build the formatted string for a snackbar message. The screen passes the
   * current [android.content.Context] (its `LocalContext.current`) and we
   * return a fully resolved, localized text — preserving the payload values
   * from the sealed cases (status name, priority name, task count, etc.).
   */
  fun formatSnackbarMessage(context: android.content.Context, msg: SnackbarMessage): String {
    return when (msg) {
      SnackbarMessage.FocusSessionEnded -> context.getString(R.string.snack_focus_session_ended)
      SnackbarMessage.TaskStatusUpdated -> context.getString(R.string.snack_task_status_updated)
      is SnackbarMessage.TaskStatusChanged ->
        context.getString(R.string.snack_task_status_changed_format, msg.newStatus.name)
      is SnackbarMessage.PriorityUpdated ->
        context.getString(
          R.string.snack_priority_updated_format,
          msg.newPriority?.name ?: context.getString(R.string.snack_priority_none)
        )
      SnackbarMessage.SubTaskAdded -> context.getString(R.string.snack_subtask_added)
      SnackbarMessage.MyDayAlreadyEmpty -> context.getString(R.string.snack_myday_already_empty)
      is SnackbarMessage.MyDayCleared ->
        if (msg.count == 1)
          context.getString(R.string.snack_myday_cleared_format, msg.count)
        else
          context.getString(R.string.snack_myday_cleared_plural_format, msg.count)
      SnackbarMessage.TaskPostponed -> context.getString(R.string.snack_task_postponed)
      SnackbarMessage.AllOverdueMoved -> context.getString(R.string.snack_all_overdue_moved)
      is SnackbarMessage.TaskAdded -> context.getString(R.string.snack_task_added_format, msg.name)
      is SnackbarMessage.ProjectSaved -> context.getString(R.string.snack_project_saved_format, msg.name)
      SnackbarMessage.ProjectArchived -> context.getString(R.string.snack_project_archived)
      SnackbarMessage.NoteSaved -> context.getString(R.string.snack_note_saved)
      SnackbarMessage.GoalDropped -> context.getString(R.string.snack_goal_dropped)
      SnackbarMessage.GoalAchieved -> context.getString(R.string.snack_goal_achieved)
    }
  }

  // --- Projects Actions ---
  fun openProjectDetail(projectId: String) {
    _uiState.update { it.copy(selectedProjectId = projectId, currentScreen = AppScreen.PROJECT_DETAIL) }
    emitNav(AppScreen.PROJECT_DETAIL)
    loadDetailBody(projectId)
  }

  fun openEditProject(projectId: String) {
    _uiState.update { it.copy(selectedProjectId = projectId, currentScreen = AppScreen.EDIT_PROJECT) }
    emitNav(AppScreen.EDIT_PROJECT)
  }

  fun selectProjectFilter(filter: ProjectFilter) {
    _uiState.update { it.copy(selectedProjectFilter = filter) }
  }

  fun saveProject(updatedProject: ProjectModel) {
    _uiState.update { state ->
      val updated = state.projects.map { if (it.id == updatedProject.id) updatedProject else it }
      state.copy(
        projects = updated,
        currentScreen = AppScreen.PROJECT_DETAIL,
        snackbarMessage = SnackbarMessage.ProjectSaved(updatedProject.name)
      )
    }
    remoteWrite {
      it.updateProject(
        updatedProject.id,
        updatedProject.name,
        updatedProject.status,
        DateUtils.parseIsoDate(updatedProject.deadline)?.toString(),
      )
    }
    // Return to the project detail we came from (edit_project -> project_detail).
    navigateBack()
  }

  /** Create a project and jump straight into its editor. */
  fun createNewProject(name: String = "New project") {
    val projectName = name.ifBlank { "New project" }
    val tempId = "p-new-${System.currentTimeMillis()}"
    val draft = ProjectModel(id = tempId, name = projectName, status = "Planned")
    _uiState.update {
      it.copy(projects = listOf(draft) + it.projects, selectedProjectId = tempId, currentScreen = AppScreen.PROJECT_DETAIL)
    }
    emitNav(AppScreen.PROJECT_DETAIL)
    if (repo.isRemote) {
      viewModelScope.launch {
        try {
          repo.createProject(projectName)?.let { realId ->
            _uiState.update { s ->
              s.copy(
                projects = s.projects.map { if (it.id == tempId) it.copy(id = realId) else it },
                selectedProjectId = if (s.selectedProjectId == tempId) realId else s.selectedProjectId,
              )
            }
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(syncError = e.message ?: "Could not create project") }
        }
      }
    }
  }

  fun archiveProject(projectId: String) {
    _uiState.update { state ->
      val updated = state.projects.map { if (it.id == projectId) it.copy(isArchived = true) else it }
      state.copy(
        projects = updated,
        currentScreen = AppScreen.PROJECTS,
        snackbarMessage = SnackbarMessage.ProjectArchived
      )
    }
    remoteWrite { it.setProjectArchived(projectId, true) }
    // project_detail -> projects list.
    navigateBack()
  }

  // --- Notes Actions ---
  fun openNoteDetail(noteId: String) {
    _uiState.update { it.copy(selectedNoteId = noteId, currentScreen = AppScreen.NOTE_DETAIL) }
    emitNav(AppScreen.NOTE_DETAIL)
    loadDetailBody(noteId)
  }

  fun openNoteEditor(noteId: String) {
    _uiState.update { it.copy(selectedNoteId = noteId, currentScreen = AppScreen.NOTE_EDITOR) }
    emitNav(AppScreen.NOTE_EDITOR)
  }

  fun createNewNote(projectId: String? = null, type: String = "Note") {
    val newId = "n-${System.currentTimeMillis()}"
    val projName = projectId?.let { pId -> _uiState.value.projects.find { it.id == pId }?.name }
    val newNote = NoteModel(
      id = newId,
      title = "Untitled note",
      type = type,
      date = "Today",
      excerpt = "",
      rawMarkdown = "",
      projectName = projName,
      isFavorite = false,
    )
    _uiState.update { state ->
      state.copy(
        notes = listOf(newNote) + state.notes,
        selectedNoteId = newId,
        currentScreen = AppScreen.NOTE_EDITOR,
      )
    }
    emitNav(AppScreen.NOTE_EDITOR)
    if (repo.isRemote) {
      viewModelScope.launch {
        try {
          repo.createNote("Untitled note", type, projectId)?.let { realId ->
            _uiState.update { s ->
              s.copy(
                notes = s.notes.map { if (it.id == newId) it.copy(id = realId) else it },
                selectedNoteId = if (s.selectedNoteId == newId) realId else s.selectedNoteId,
              )
            }
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(syncError = e.message ?: "Could not create note") }
        }
      }
    }
  }

  /**
   * Open today's journal entry from Wrap up. Reuses an existing Journal note
   * dated today if there is one, otherwise starts a new one in the editor.
   * (Note persistence to Notion is not wired yet — tracked separately.)
   */
  fun openTodayJournal() {
    val existing = _uiState.value.notes.firstOrNull {
      it.type.equals("Journal", ignoreCase = true) && (it.date == "Today" || it.date == java.time.LocalDate.now().toString())
    }
    if (existing != null) {
      openNoteEditor(existing.id)
      return
    }
    val newId = "n-journal-${System.currentTimeMillis()}"
    val heading = java.time.LocalDate.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val newNote = NoteModel(
      id = newId,
      title = "Journal — $heading",
      type = "Journal",
      date = "Today",
      excerpt = "",
      rawMarkdown = "# $heading\n\n",
      projectName = null,
      isFavorite = false,
    )
    _uiState.update { it.copy(notes = listOf(newNote) + it.notes, selectedNoteId = newId, currentScreen = AppScreen.NOTE_EDITOR) }
    emitNav(AppScreen.NOTE_EDITOR)
  }

  fun selectNoteFilter(filter: NoteFilter) {
    _uiState.update { it.copy(selectedNoteFilter = filter) }
  }

  fun toggleNoteFavorite(noteId: String) {
    _uiState.update { state ->
      val updated = state.notes.map { if (it.id == noteId) it.copy(isFavorite = !it.isFavorite) else it }
      state.copy(notes = updated)
    }
    val fav = _uiState.value.notes.firstOrNull { it.id == noteId }?.isFavorite == true
    remoteWrite { it.setNoteFavorite(noteId, fav) }
  }

  fun setTagType(tagId: String, type: String) {
    _uiState.update { s -> s.copy(tags = s.tags.map { if (it.id == tagId) it.copy(type = type) else it }) }
    remoteWrite { it.setPageStatus(tagId, "Type", type) }
  }

  fun setTagParent(tagId: String, parentId: String?) {
    val pname = parentId?.let { p -> _uiState.value.tags.find { it.id == p }?.name }
    _uiState.update { s -> s.copy(tags = s.tags.map { if (it.id == tagId) it.copy(parentId = parentId, parentName = pname) else it }) }
    remoteWrite { it.setTagParent(tagId, parentId) }
  }

  fun toggleTagFavorite(tagId: String) {
    _uiState.update { s ->
      s.copy(tags = s.tags.map { if (it.id == tagId) it.copy(isFavorite = !it.isFavorite) else it })
    }
    val fav = _uiState.value.tags.firstOrNull { it.id == tagId }?.isFavorite == true
    remoteWrite { it.setPageCheckbox(tagId, "Favorite", fav) }
  }

  fun saveNote(updatedNote: NoteModel) {
    _uiState.update { state ->
      val updated = state.notes.map { if (it.id == updatedNote.id) updatedNote else it }
      state.copy(
        notes = updated,
        currentScreen = AppScreen.NOTE_DETAIL,
        detailBody = updatedNote.rawMarkdown.ifBlank { null },
        detailBodyForId = updatedNote.id,
        snackbarMessage = SnackbarMessage.NoteSaved,
      )
    }
    remoteWrite {
      it.updateNoteMeta(updatedNote.id, updatedNote.title, updatedNote.type)
      if (updatedNote.rawMarkdown.isNotBlank()) it.setPageBody(updatedNote.id, updatedNote.rawMarkdown)
    }
    // note_editor -> note_detail.
    navigateBack()
  }

  // --- Goals & Milestones Actions ---
  fun createNewGoal(name: String = "New goal") {
    val goalName = name.ifBlank { "New goal" }
    val newId = "g-${System.currentTimeMillis()}"
    val newGoal = GoalModel(id = newId, name = goalName, status = "Active", deadline = "—")
    _uiState.update { state ->
      state.copy(
        goals = listOf(newGoal) + state.goals,
        selectedGoalId = newId,
        currentScreen = AppScreen.GOAL_DETAIL,
      )
    }
    emitNav(AppScreen.GOAL_DETAIL)
    if (repo.isRemote) {
      viewModelScope.launch {
        try {
          repo.createGoal(goalName)?.let { realId ->
            _uiState.update { s ->
              s.copy(
                goals = s.goals.map { if (it.id == newId) it.copy(id = realId) else it },
                selectedGoalId = if (s.selectedGoalId == newId) realId else s.selectedGoalId,
              )
            }
          }
        } catch (e: Exception) {
          _uiState.update { it.copy(syncError = e.message ?: "Could not create goal") }
        }
      }
    }
  }

  fun openGoalDetail(goalId: String) {
    _uiState.update { it.copy(selectedGoalId = goalId, currentScreen = AppScreen.GOAL_DETAIL) }
    emitNav(AppScreen.GOAL_DETAIL)
    loadDetailBody(goalId)
  }

  fun selectGoalFilter(filter: GoalFilter) {
    _uiState.update { it.copy(selectedGoalFilter = filter) }
  }

  fun dropGoal(goalId: String) {
    _uiState.update { state ->
      val updated = state.goals.map { if (it.id == goalId) it.copy(isArchived = true) else it }
      state.copy(goals = updated, currentScreen = AppScreen.GOALS, snackbarMessage = SnackbarMessage.GoalDropped)
    }
    remoteWrite { it.setGoalArchived(goalId, true) }
    navigateBack()
  }

  fun achieveGoal(goalId: String) {
    _uiState.update { state ->
      val updated = state.goals.map { if (it.id == goalId) it.copy(status = "Achieved", aggregatedProgress = 1.0f, aggregatedProgressText = "100%") else it }
      state.copy(goals = updated, currentScreen = AppScreen.GOALS, snackbarMessage = SnackbarMessage.GoalAchieved)
    }
    remoteWrite { it.setGoalStatus(goalId, "Achieved") }
    navigateBack()
  }

  fun selectMilestoneFilter(filter: MilestoneFilter) {
    _uiState.update { it.copy(selectedMilestoneFilter = filter) }
  }

  fun openMilestoneDetail(milestoneId: String) {
    _uiState.update { it.copy(selectedMilestoneId = milestoneId, currentScreen = AppScreen.MILESTONE_DETAIL) }
    emitNav(AppScreen.MILESTONE_DETAIL)
  }

  fun setMilestoneGoal(milestoneId: String, goalId: String?) {
    val name = goalId?.let { g -> _uiState.value.goals.find { it.id == g }?.name } ?: ""
    _uiState.update { s -> s.copy(milestones = s.milestones.map { if (it.id == milestoneId) it.copy(goalId = goalId ?: "", goalName = name) else it }) }
    remoteWrite { it.setMilestoneGoal(milestoneId, goalId) }
  }

  fun setMilestoneDate(milestoneId: String, iso: String?) {
    _uiState.update { s -> s.copy(milestones = s.milestones.map { if (it.id == milestoneId) it.copy(targetDateText = iso?.let { d -> "Target: ${com.example.data.DateUtils.displayLabel(d)}" } ?: "") else it }) }
    remoteWrite { it.setMilestoneDate(milestoneId, iso) }
  }

  fun toggleMilestoneStatus(milestoneId: String) {
    _uiState.update { state ->
      val updated = state.milestones.map { ms ->
        if (ms.id == milestoneId) {
          val newStatus = if (ms.status == "Completed") "In Progress" else "Completed"
          ms.copy(status = newStatus)
        } else {
          ms
        }
      }
      state.copy(milestones = updated)
    }
    val done = _uiState.value.milestones.firstOrNull { it.id == milestoneId }?.status == "Completed"
    remoteWrite { it.setMilestoneCompleted(milestoneId, done) }
  }

  // --- Tags Actions ---
  fun openTagDetail(tagId: String) {
    _uiState.update { it.copy(selectedTagId = tagId, currentScreen = AppScreen.TAG_DETAIL) }
    emitNav(AppScreen.TAG_DETAIL)
  }

  fun selectTagFilter(filter: TagFilter) {
    _uiState.update { it.copy(selectedTagFilter = filter) }
  }

  // --- Work Sessions Actions ---
  fun toggleWorkSession() {
    _uiState.update { it.copy(isWorkSessionActive = !it.isWorkSessionActive) }
  }

  // --- Search Actions ---
  fun setGlobalSearchQuery(q: String) {
    _uiState.update { it.copy(globalSearchQuery = q) }
  }

  fun setGlobalSearchScope(scope: String) {
    _uiState.update { it.copy(globalSearchScope = scope) }
  }
}

