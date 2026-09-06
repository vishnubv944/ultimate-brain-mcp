package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DateUtils
import com.example.model.Priority
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.SearchDialog
import com.example.ui.components.ShowMoreRow
import com.example.ui.components.page
import com.example.ui.components.rememberVisibleCount
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent
import com.example.ui.theme.successContainer
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.TasksFilter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  // Audit Finding 7/9 — derive KPI/project labels from real state, not
  // hardcoded "Q3 launch" / "Complete by 6 PM" literals.
  val totalActiveTasks = uiState.tasks.count { !it.isDone }
  val todayTasksAll = uiState.tasks.filter {
    DateUtils.bucket(it.due) == DateUtils.DueBucket.TODAY && !it.isDone
  }
  // Distinguish "today by time" (has a clock reading in dueDisplay) from
  // "today by date" — the section header labels each appropriately.
  val todayTimedTaskCount = todayTasksAll.count { task ->
    task.dueDisplay.matches(Regex("""\d{1,2}:\d{2}\s*[AP]M""", RegexOption.IGNORE_CASE))
  }
  // If tasks are time-blocked today, surface the latest due as the soft
  // target; otherwise fall back to "All-day" target.
  val todayTargetLabel = when {
    todayTimedTaskCount == 0 -> stringResource(R.string.tasks_target_all_day)
    todayTimedTaskCount == 1 -> stringResource(R.string.tasks_target_one_block)
    else -> stringResource(R.string.tasks_target_n_blocks, todayTimedTaskCount)
  }
  // Picking the project with the most active tasks makes the chip feel
  // alive ("Project: Q3 launch / 12 tasks") instead of a single hardcoded
  // literal. Falls back to a generic label when no tasks carry a project.
  val topProject = uiState.tasks
    .filter { !it.isDone && it.projectName != null }
    .groupingBy { it.projectName!! }
    .eachCount()
    .maxByOrNull { it.value }
  val projectChipLabel = topProject?.let { (name, count) ->
    stringResource(R.string.tasks_project_chip_format, name, count)
  } ?: stringResource(R.string.tasks_project_chip_none)

  // Localized snackbar routing — SnackbarMessage is a sealed type from the VM;
  // VM.formatSnackbarMessage(Context, SnackbarMessage) resolves the resource.
  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.tasks_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        actions = {
          IconButton(
            onClick = { viewModel.setSearchOpen(true) },
            modifier = Modifier.testTag("tasks_search_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = stringResource(R.string.action_search),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { /* Sort options */ },
            modifier = Modifier.testTag("tasks_sort_btn")
          ) {
            Icon(
              imageVector = Icons.Default.SwapVert,
              contentDescription = stringResource(R.string.action_sort),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { /* Filter options */ },
            modifier = Modifier.testTag("tasks_filter_btn")
          ) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = stringResource(R.string.action_filter),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      BottomNavBar(
        activeDestination = BottomNavDestination.TASKS,
        onDestinationSelected = { dest ->
          when (dest) {
            BottomNavDestination.TODAY -> viewModel.navigateTo(AppScreen.TODAY)
            BottomNavDestination.TASKS -> viewModel.navigateTo(AppScreen.TASKS)
            BottomNavDestination.PROJECTS -> viewModel.navigateTo(AppScreen.PROJECTS)
            BottomNavDestination.NOTES -> viewModel.navigateTo(AppScreen.NOTES)
            BottomNavDestination.MORE -> viewModel.navigateTo(AppScreen.MORE_HUB)
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.setQuickAddOpen(true) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .size(56.dp)
          .testTag("tasks_add_fab")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = stringResource(R.string.action_add_new_task),
          modifier = Modifier.size(24.dp)
        )
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Horizontal Filter Chips Bar (lives below the MediumTopAppBar in the body
      // so it scrolls with content; topBar slot stays reserved for the title).
      //
      // Audit Finding 5: hand-rolled Surface chips → M3 FilterChip, matching
      // the rest of the app (Projects / Goals / Notes screens already use this
      // pattern). Audit Finding 7/9: project label is state-derived from the
      // top project by active-task count.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = uiState.selectedTasksFilter == TasksFilter.ALL,
          onClick = { viewModel.selectTasksFilter(TasksFilter.ALL) },
          label = {
            Text(
              text = stringResource(R.string.tasks_chip_all, totalActiveTasks),
              fontWeight = if (uiState.selectedTasksFilter == TasksFilter.ALL) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )

        FilterChip(
          selected = uiState.selectedTasksFilter == TasksFilter.MY_DAY,
          onClick = { viewModel.selectTasksFilter(TasksFilter.MY_DAY) },
          label = {
            Text(
              text = stringResource(R.string.tasks_chip_my_day) + " (${uiState.doingTasks.size})",
              fontWeight = if (uiState.selectedTasksFilter == TasksFilter.MY_DAY) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )

        FilterChip(
          selected = uiState.selectedTasksFilter == TasksFilter.HIGH_PRIORITY,
          onClick = { viewModel.selectTasksFilter(TasksFilter.HIGH_PRIORITY) },
          label = {
            Text(
              text = stringResource(R.string.tasks_chip_high_priority),
              fontWeight = if (uiState.selectedTasksFilter == TasksFilter.HIGH_PRIORITY) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )

        FilterChip(
          selected = uiState.selectedTasksFilter == TasksFilter.OVERDUE,
          onClick = { viewModel.selectTasksFilter(TasksFilter.OVERDUE) },
          label = {
            Text(
              text = stringResource(R.string.tasks_chip_overdue) + " · $projectChipLabel",
              fontWeight = if (uiState.selectedTasksFilter == TasksFilter.OVERDUE) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )

        FilterChip(
          selected = uiState.selectedTasksFilter == TasksFilter.TODAY,
          onClick = { viewModel.selectTasksFilter(TasksFilter.TODAY) },
          label = {
            Text(
              text = stringResource(R.string.tasks_chip_today),
              fontWeight = if (uiState.selectedTasksFilter == TasksFilter.TODAY) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )

        // "+ Add filter" stays as an outlined non-selectable chip — leaving the
        // existing "+ filter" intent intact, but using Material's outline color
        // for the disabled feel.
        FilterChip(
          selected = false,
          onClick = { /* Add filter dialog */ },
          label = {
            Text(stringResource(R.string.tasks_add_filter), fontWeight = FontWeight.Normal)
          },
          colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.outline
          )
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {

        // SECTION 1: Overdue Group (Alert Styled)
        val overdueList = uiState.tasks.filter { it.isOverdue && !it.isDone }
      if (overdueList.isNotEmpty()) {
        item {
          val overdueVisible = rememberVisibleCount(uiState.selectedTasksFilter)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = stringResource(R.string.tasks_section_overdue),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.errorAccent
                )
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(
                    text = "${overdueList.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.errorAccent,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = stringResource(R.string.tasks_needs_triage),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.errorAccent
              )
            }

            overdueList.page(overdueVisible.intValue).forEach { task ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.errorAccent.copy(alpha = 0.35f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { viewModel.openTaskDetail(task.id) }
                  .testTag("overdue_task_${task.id}")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorAccent)
                    )
                    IconButton(
                      onClick = { viewModel.toggleTaskCompletion(task.id) },
                      modifier = Modifier.size(48.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = stringResource(R.string.tasks_complete_task_cd),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = task.name,
                      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      if (task.projectName != null) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = MaterialTheme.colorScheme.surface,
                          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            Box(
                              modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.entityProjects)
                            )
                            Text(
                              text = task.projectName,
                              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                        }
                      }

                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorAccent
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(13.dp)
                          )
                          Text(
                            text = task.dueDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.onErrorContainer
                            )
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
            ShowMoreRow(overdueList.size - overdueVisible.intValue, overdueVisible)
          }
        }
      }

      // SECTION 2: Today
      val todayTasks = uiState.tasks.filter { DateUtils.bucket(it.due) == DateUtils.DueBucket.TODAY && !it.isDone }
      item {
        val todayVisible = rememberVisibleCount(uiState.selectedTasksFilter)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              val todayDateLabel = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
              Text(
                text = stringResource(R.string.tasks_section_today_format, todayDateLabel),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = "${todayTasks.size}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = todayTargetLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Today tasks grouped in single bordered card container
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column {
              val shown = todayTasks.page(todayVisible.intValue)
              shown.forEachIndexed { index, task ->
                TaskRowItem(
                  task = task,
                  formattedTimer = uiState.formattedTimer,
                  onTaskClick = { viewModel.openTaskDetail(task.id) },
                  onCompleteToggle = { viewModel.toggleTaskCompletion(task.id) }
                )
                if (index < shown.lastIndex) {
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 1.dp
                  )
                }
              }
              ShowMoreRow(todayTasks.size - todayVisible.intValue, todayVisible)
            }
          }
        }
      }

      // SECTION 3: Upcoming Group
      val upcomingTasks = uiState.tasks.filter {
        val b = DateUtils.bucket(it.due)
        (b == DateUtils.DueBucket.TOMORROW || b == DateUtils.DueBucket.UPCOMING) && !it.isDone
      }
      if (upcomingTasks.isNotEmpty()) {
        item {
          val upcomingVisible = rememberVisibleCount(uiState.selectedTasksFilter)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(horizontal = 2.dp)
            ) {
              Text(
                text = stringResource(R.string.tasks_section_upcoming),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
              ) {
                Text(
                  text = "${upcomingTasks.size}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                val shownUp = upcomingTasks.page(upcomingVisible.intValue)
                shownUp.forEachIndexed { index, task ->
                  TaskRowItem(
                    task = task,
                    formattedTimer = null,
                    onTaskClick = { viewModel.openTaskDetail(task.id) },
                    onCompleteToggle = { viewModel.toggleTaskCompletion(task.id) }
                  )
                  if (index < shownUp.lastIndex) {
                    HorizontalDivider(
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                      thickness = 1.dp
                    )
                  }
                }
                ShowMoreRow(upcomingTasks.size - upcomingVisible.intValue, upcomingVisible)
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(96.dp))
      }
      }
    }
  }

  if (uiState.isQuickAddOpen) {
    QuickAddBottomSheet(
      onDismiss = { viewModel.setQuickAddOpen(false) },
      onSaveTask = { name, projectId, priority, isMyDay ->
        viewModel.addNewTask(name, projectId, priority, isMyDay)
      },
      projects = uiState.projects
    )
  }

  if (uiState.isSearchOpen) {
    SearchDialog(
      searchQuery = uiState.searchQuery,
      onQueryChange = { viewModel.updateSearchQuery(it) },
      searchResults = uiState.filteredTasksForSearch,
      onTaskClick = { task -> viewModel.openTaskDetail(task.id) },
      onDismiss = { viewModel.setSearchOpen(false) }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRowItem(
  task: Task,
  formattedTimer: String?,
  onTaskClick: () -> Unit,
  onCompleteToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onTaskClick() }
      .padding(14.dp)
      .testTag("task_row_${task.id}"),
    verticalAlignment = Alignment.Top
  ) {
    // Priority Dot + Checkbox
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(top = 2.dp)
    ) {
      val dotColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.errorAccent
        Priority.MEDIUM -> MaterialTheme.colorScheme.warning
        Priority.LOW -> MaterialTheme.colorScheme.outline
        null -> MaterialTheme.colorScheme.outline
      }
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(dotColor)
      )
      Checkbox(
        checked = task.isDone,
        onCheckedChange = { onCompleteToggle() },
        colors = CheckboxDefaults.colors(
          checkedColor = MaterialTheme.colorScheme.primary,
          uncheckedColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.testTag("task_checkbox_${task.id}")
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Text(
          text = task.name,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )

        // Time / Due text — derive bucket from ISO due, not by sniffing display string
        val dueBucket = DateUtils.bucket(task.due)
        val isTimeOnly = dueBucket == DateUtils.DueBucket.TODAY &&
          task.dueDisplay.matches(Regex("""\d{1,2}:\d{2}\s*[AP]M""", RegexOption.IGNORE_CASE))
        if (isTimeOnly) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(start = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = task.dueDisplay,
              style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Text(
            text = DateUtils.displayLabel(task.due),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (dueBucket == DateUtils.DueBucket.TODAY) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (dueBucket == DateUtils.DueBucket.TODAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
          )
        }
      }

      // Active Time Tracking Badge
      if (task.isActiveSession && formattedTimer != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.successContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.success,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = formattedTimer,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
              ),
              color = MaterialTheme.colorScheme.onSuccessContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Metadata Chips & Status Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.weight(1f)
        ) {
          if (task.projectName != null) {
            // The original logic always resolved to entityProjects on both branches
            // (the found-project branch and the elvis fallback). TaskRowItem only
            // receives a `Task`, so we hardcode the project color here.
            val dotColor = MaterialTheme.colorScheme.entityProjects
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                )
                Text(
                  text = task.projectName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          task.labels.forEach { label ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceContainer
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          if (task.subTasksCount > 0 && task.labels.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AccountTree,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = stringResource(R.string.tasks_subtasks_format, task.subTasksCount),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Status Pill (Doing / To Do)
        when (task.status) {
          TaskStatus.DOING -> {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = stringResource(R.string.status_doing),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          TaskStatus.TODO -> {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color.Transparent,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
              Text(
                text = stringResource(R.string.status_todo),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          TaskStatus.DONE -> {}
        }
      }
    }
  }
}
