package com.example.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.DateUtils
import com.example.data.DummyData
import com.example.model.AcceptanceCriterion
import com.example.model.DailyRitualPhase
import com.example.model.GoalModel
import com.example.model.MilestoneModel
import com.example.model.NoteActionItem
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
  TAGS,
  TAG_DETAIL,
  WORK_SESSIONS,
  SETTINGS,
  GLOBAL_SEARCH,
  MORE_HUB
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
  RECURRING(R.string.tasks_chip_recurring)
}

enum class ProjectFilter(@StringRes val labelRes: Int) {
  ALL(R.string.projects_chip_all_format),
  STATUS_DOING(R.string.projects_chip_status_doing_format),
  TAG_WORK(R.string.projects_chip_tag_format),
  GOAL_Q3(R.string.projects_chip_goal_format)
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
  val isSearchOpen: Boolean = false,
  val searchQuery: String = "",
  val eveningReviewStep: Int = 1, // 1: Clear Day, 2: Calendar, 3: Tomorrow
  val snackbarMessage: SnackbarMessage? = null,

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

  // Tags State
  val tags: List<TagModel> = DummyData.tagsList,
  val selectedTagId: String? = null,
  val selectedTagFilter: TagFilter = TagFilter.ALL,

  // Work Sessions State
  val workSessions: List<WorkSessionModel> = DummyData.workSessionsList,
  val activeSessionSeconds: Long = 0L,
  val isWorkSessionActive: Boolean = false,

  // Global Search State
  val globalSearchQuery: String = "",
  val globalSearchScope: String = "All", // "All", "Tasks", "Projects", "Notes" — kept as String since this filter is for a search engine, not a localized label

  // Theme preferences (audit Finding 16). The SettingsScreen toggle mutates
  // this; MainActivity observes it via collectAsState() and feeds it into
  // MyApplicationTheme(dynamicColor = ...).
  val dynamicColorEnabled: Boolean = false,
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

  val filteredProjects: List<ProjectModel>
    get() = when (selectedProjectFilter) {
      ProjectFilter.STATUS_DOING -> projects.filter { it.status == "Doing" }
      ProjectFilter.TAG_WORK -> projects.filter { it.tags.contains("#Work") }
      ProjectFilter.GOAL_Q3 -> projects.filter { it.goalName?.contains("v2.0") == true }
      ProjectFilter.ALL -> projects
    }

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
      GoalFilter.ACTIVE -> goals.filter { it.status == "Active" }
      GoalFilter.ACHIEVED -> goals.filter { it.status == "Achieved" }
      GoalFilter.DROPPED -> goals.filter { it.status == "Dropped" }
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

  val doneTodayTasks: List<Task>
    get() = tasks.filter { it.isDone }

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

  init {
    startTimerLoop()
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
  }

  fun toggleSubTask(taskId: String, subTaskId: String) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          val updatedSubTasks = task.subTasks.map { st ->
            if (st.id == subTaskId) st.copy(isCompleted = !st.isCompleted) else st
          }
          task.copy(subTasks = updatedSubTasks)
        } else {
          task
        }
      }
      state.copy(tasks = updated)
    }
  }

  fun addSubTask(taskId: String, name: String) {
    if (name.isBlank()) return
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          val newSub = SubTask(
            id = "st-${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            isCompleted = false
          )
          task.copy(
            subTasks = task.subTasks + newSub,
            subTasksCount = task.subTasks.size + 1
          )
        } else {
          task
        }
      }
      state.copy(tasks = updated, snackbarMessage = SnackbarMessage.SubTaskAdded)
    }
  }

  fun toggleAcceptanceCriterion(taskId: String, criterionId: String) {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          val updatedCriteria = task.acceptanceCriteria.map { ac ->
            if (ac.id == criterionId) ac.copy(isChecked = !ac.isChecked) else ac
          }
          task.copy(acceptanceCriteria = updatedCriteria)
        } else {
          task
        }
      }
      state.copy(tasks = updated)
    }
  }

  fun toggleDoneAccordion() {
    _uiState.update { it.copy(isDoneExpanded = !it.isDoneExpanded) }
  }

  fun setQuickAddOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isQuickAddOpen = isOpen) }
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
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.id == taskId) {
          task.copy(
            dueDisplay = "Tomorrow",
            isOverdue = false,
            isMyDay = false
          )
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        snackbarMessage = SnackbarMessage.TaskPostponed
      )
    }
  }

  fun rescheduleAllOverdue() {
    _uiState.update { state ->
      val updated = state.tasks.map { task ->
        if (task.isOverdue) {
          task.copy(
            dueDisplay = "Tomorrow",
            isOverdue = false
          )
        } else {
          task
        }
      }
      state.copy(
        tasks = updated,
        snackbarMessage = SnackbarMessage.AllOverdueMoved
      )
    }
  }

  fun addNewTask(name: String, projectId: String?, priority: Priority?, isMyDay: Boolean) {
    if (name.isBlank()) return
    val projectName = projectId?.let { id -> _uiState.value.projects.find { it.id == id }?.name }
    val newTask = Task(
      id = "tk-custom-${UUID.randomUUID().toString().take(6)}",
      name = name.trim(),
      status = TaskStatus.TODO,
      priority = priority,
      dueDisplay = "Today",
      isMyDay = isMyDay,
      projectName = projectName,
      labels = if (projectName != null) listOf("Planned") else listOf("Quick")
    )
    _uiState.update { state ->
      state.copy(
        tasks = listOf(newTask) + state.tasks,
        isQuickAddOpen = false,
        snackbarMessage = SnackbarMessage.TaskAdded(name.trim())
      )
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
    // Return to the project detail we came from (edit_project -> project_detail).
    navigateBack()
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
    // project_detail -> projects list.
    navigateBack()
  }

  // --- Notes Actions ---
  fun openNoteDetail(noteId: String) {
    _uiState.update { it.copy(selectedNoteId = noteId, currentScreen = AppScreen.NOTE_DETAIL) }
    emitNav(AppScreen.NOTE_DETAIL)
  }

  fun openNoteEditor(noteId: String) {
    _uiState.update { it.copy(selectedNoteId = noteId, currentScreen = AppScreen.NOTE_EDITOR) }
    emitNav(AppScreen.NOTE_EDITOR)
  }

  fun createNewNote(projectId: String? = null) {
    val newId = "n-${System.currentTimeMillis()}"
    val projName = projectId?.let { pId -> _uiState.value.projects.find { it.id == pId }?.name } ?: _uiState.value.projects.firstOrNull()?.name
    val newNote = NoteModel(
      id = newId,
      title = "Untitled Note",
      type = "Reference",
      date = "Today",
      excerpt = "",
      rawMarkdown = "# Untitled Note\n\nStart writing here...",
      projectName = projName,
      isFavorite = false
    )
    _uiState.update { state ->
      state.copy(
        notes = listOf(newNote) + state.notes,
        selectedNoteId = newId,
        currentScreen = AppScreen.NOTE_EDITOR
      )
    }
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
  }

  fun toggleNoteActionItem(noteId: String, actionId: String) {
    _uiState.update { state ->
      val updated = state.notes.map { note ->
        if (note.id == noteId) {
          val updatedItems = note.actionItems.map { if (it.id == actionId) it.copy(isDone = !it.isDone) else it }
          note.copy(actionItems = updatedItems)
        } else {
          note
        }
      }
      state.copy(notes = updated)
    }
  }

  fun saveNote(updatedNote: NoteModel) {
    _uiState.update { state ->
      val updated = state.notes.map { if (it.id == updatedNote.id) updatedNote else it }
      state.copy(
        notes = updated,
        currentScreen = AppScreen.NOTE_DETAIL,
        snackbarMessage = SnackbarMessage.NoteSaved
      )
    }
    // note_editor -> note_detail.
    navigateBack()
  }

  // --- Goals & Milestones Actions ---
  fun createNewGoal() {
    val newId = "g-${System.currentTimeMillis()}"
    val newGoal = GoalModel(
      id = newId,
      name = "New Goal",
      status = "Active",
      deadline = "Dec 31, 2026",
      aggregatedProgress = 0f,
      aggregatedProgressText = "0%"
    )
    _uiState.update { state ->
      state.copy(
        goals = listOf(newGoal) + state.goals,
        selectedGoalId = newId,
        currentScreen = AppScreen.GOAL_DETAIL
      )
    }
    emitNav(AppScreen.GOAL_DETAIL)
  }

  fun openGoalDetail(goalId: String) {
    _uiState.update { it.copy(selectedGoalId = goalId, currentScreen = AppScreen.GOAL_DETAIL) }
    emitNav(AppScreen.GOAL_DETAIL)
  }

  fun selectGoalFilter(filter: GoalFilter) {
    _uiState.update { it.copy(selectedGoalFilter = filter) }
  }

  fun dropGoal(goalId: String) {
    _uiState.update { state ->
      val updated = state.goals.map { if (it.id == goalId) it.copy(status = "Dropped") else it }
      state.copy(goals = updated, currentScreen = AppScreen.GOALS, snackbarMessage = SnackbarMessage.GoalDropped)
    }
    navigateBack()
  }

  fun achieveGoal(goalId: String) {
    _uiState.update { state ->
      val updated = state.goals.map { if (it.id == goalId) it.copy(status = "Achieved", aggregatedProgress = 1.0f, aggregatedProgressText = "100%") else it }
      state.copy(goals = updated, currentScreen = AppScreen.GOALS, snackbarMessage = SnackbarMessage.GoalAchieved)
    }
    navigateBack()
  }

  fun selectMilestoneFilter(filter: MilestoneFilter) {
    _uiState.update { it.copy(selectedMilestoneFilter = filter) }
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

