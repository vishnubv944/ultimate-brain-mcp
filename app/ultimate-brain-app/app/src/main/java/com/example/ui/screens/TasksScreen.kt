package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.model.Task
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EmptyLine
import com.example.ui.components.FilterOption
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SearchDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.TaskRow
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.TasksFilter

private val TASK_FILTERS = listOf(
  TasksFilter.ALL to "All",
  TasksFilter.MY_DAY to "My Day",
  TasksFilter.TODAY to "Today",
  TasksFilter.OVERDUE to "Overdue",
  TasksFilter.HIGH_PRIORITY to "High priority",
  TasksFilter.RECURRING to "Recurring",
  TasksFilter.DONE to "Done",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  val filter = uiState.selectedTasksFilter
  val options = remember(uiState.tasks, filter) {
    TASK_FILTERS.map { (f, label) -> FilterOption(f, label, uiState.tasksFilterCount(f)) }
  }

  ScreenScaffold(
    title = "Tasks",
    viewModel = viewModel,
    active = BottomNavDestination.TASKS,
    modifier = modifier,
    snackbarHost = snackbarHostState,
    actions = {
      IconButton(onClick = { viewModel.setSearchOpen(true) }) {
        Icon(Icons.Default.Search, contentDescription = "Search")
      }
    },
    fab = {
      FloatingActionButton(
        onClick = { viewModel.setQuickAddOpen(true) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      ) { Icon(Icons.Default.Add, contentDescription = "New task") }
    },
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        SegmentedFilter(
          options = options,
          selected = filter,
          onSelect = viewModel::selectTasksFilter,
        )
        Spacer(Modifier.height(4.dp))
      }

      if (filter == TasksFilter.ALL) {
        val overdue = uiState.tasks.filter { !it.isDone && DateUtils.bucket(it.due) == DateUtils.DueBucket.OVERDUE }
        val dueToday = uiState.tasks.filter { !it.isDone && DateUtils.bucket(it.due) == DateUtils.DueBucket.TODAY }
        val upcoming = uiState.tasks.filter {
          val b = DateUtils.bucket(it.due)
          !it.isDone && (b == DateUtils.DueBucket.TOMORROW || b == DateUtils.DueBucket.UPCOMING)
        }
        val noDate = uiState.tasks.filter { !it.isDone && DateUtils.bucket(it.due) == DateUtils.DueBucket.NONE }
        taskSection("Overdue", overdue, viewModel)
        taskSection("Today", dueToday, viewModel)
        taskSection("Upcoming", upcoming, viewModel)
        taskSection("No date", noDate, viewModel)
      } else {
        val list = uiState.tasksForFilter(filter)
        if (list.isEmpty()) {
          item { EmptyLine("No tasks match this filter.", Modifier.padding(horizontal = TodayPad)) }
        } else {
          taskRows(list, viewModel)
        }
      }

      if (filter == com.example.viewmodel.TasksFilter.DONE && !uiState.olderCompletedLoaded) {
        item {
          androidx.compose.material3.TextButton(
            onClick = { viewModel.loadOlderCompleted() },
            enabled = !uiState.loadingOlder,
            modifier = Modifier.padding(horizontal = TodayPad),
          ) {
            androidx.compose.material3.Text(if (uiState.loadingOlder) "Loading…" else "Load older completed tasks")
          }
        }
      }

      item { Spacer(Modifier.height(96.dp)) }
    }
  }

  if (uiState.isQuickAddOpen) {
    QuickAddBottomSheet(
      onDismiss = { viewModel.setQuickAddOpen(false) },
      onSaveTask = { name, projectId, priority, isMyDay, dueIso -> viewModel.addNewTask(name, projectId, priority, isMyDay, dueIso) },
      projects = uiState.projects,
    )
  }
  if (uiState.isSearchOpen) {
    SearchDialog(
      searchQuery = uiState.searchQuery,
      onQueryChange = { viewModel.updateSearchQuery(it) },
      searchResults = uiState.filteredTasksForSearch,
      onTaskClick = { task -> viewModel.openTaskDetail(task.id) },
      onDismiss = { viewModel.setSearchOpen(false) },
    )
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
  title: String,
  tasks: List<Task>,
  viewModel: MyDayViewModel,
) {
  if (tasks.isEmpty()) return
  item(key = "hdr_$title") {
    SectionHeader(title, tasks.size, Modifier.padding(horizontal = TodayPad))
  }
  taskRows(tasks, viewModel, keyPrefix = title)
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskRows(
  tasks: List<Task>,
  viewModel: MyDayViewModel,
  keyPrefix: String = "",
) {
  itemsIndexed(tasks, key = { _, t -> "$keyPrefix:${t.id}" }) { index, task ->
    TaskRow(
      task = task,
      onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
      onClick = { viewModel.openTaskDetail(task.id) },
      modifier = Modifier.padding(horizontal = TodayPad),
    )
    if (index < tasks.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
  }
}
