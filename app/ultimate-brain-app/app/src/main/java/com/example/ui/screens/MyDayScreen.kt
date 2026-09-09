package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EmptyLine
import com.example.ui.components.FocusCard
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.SearchDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShowMoreRow
import com.example.ui.components.TaskRow
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.components.bottomNavHandler
import com.example.ui.components.page
import com.example.ui.components.rememberVisibleCount
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.PlanFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PLAN_FILTERS = listOf(
  PlanFilter.TODAY to "Today",
  PlanFilter.WEEK to "This week",
  PlanFilter.OVERDUE to "Overdue",
  PlanFilter.ACTIVE_PROJECTS to "Active projects",
  PlanFilter.INBOX to "Inbox",
  PlanFilter.RECURRING to "Recurring",
)

/**
 * "Today" — one scrollable surface for the whole day: what you've committed to,
 * what to pull in, and (at the bottom) the evening wrap-up. Replaces the old
 * Plan / Execute / Wrap-up tab ceremony.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDayScreen(
  viewModel: MyDayViewModel,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val focusSession by viewModel.focusSession.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  val shortlist = uiState.upNextTasks
  val suggestions = uiState.planSuggestions
  val doneToday = uiState.doneTodayTasks
  val openCount = uiState.openTodayTasks.size
  val browse = uiState.planBrowseTasks
  val browseVisible = rememberVisibleCount(uiState.selectedPlanFilter)

  var addOpen by remember { mutableStateOf(false) }
  var doneOpen by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Column {
        TopAppBar(
          title = {
            Column {
              Text("Today", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
              Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          navigationIcon = { ProfileAvatar() },
          actions = {
            IconButton(onClick = { viewModel.setSearchOpen(true) }) {
              Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onToggleTheme) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle theme",
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      }
    },
    bottomBar = {
      BottomNavBar(activeDestination = BottomNavDestination.TODAY, onDestinationSelected = bottomNavHandler(viewModel))
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.setQuickAddOpen(true) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      ) { Icon(Icons.Default.Add, contentDescription = "New task") }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(4.dp))

      if (focusSession != null) {
        Spacer(Modifier.height(12.dp))
        FocusCard(
          session = focusSession!!,
          onPause = { viewModel.pauseFocus(context) },
          onResume = { viewModel.resumeFocus(context) },
          onStop = { viewModel.stopFocus(context) },
          onOpenTask = { viewModel.openTaskDetail(focusSession!!.taskId) },
        )
      }

      SectionHeader("On today", shortlist.size)
      if (shortlist.isEmpty()) {
        EmptyLine(
          "Nothing planned for today yet.",
          actionLabel = "Add a task",
          onAction = { viewModel.setQuickAddOpen(true) },
        )
      } else {
        shortlist.forEachIndexed { i, task ->
          val active = focusSession?.taskId == task.id
          TaskRow(
            task = task,
            onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) },
            trailing = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (focusSession == null) {
                  IconButton(onClick = { viewModel.startFocus(context, task) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start focus", tint = MaterialTheme.colorScheme.primary)
                  }
                } else if (active) {
                  Text(
                    "focusing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(onClick = { viewModel.toggleMyDay(task.id) }) {
                  Icon(Icons.Default.Close, contentDescription = "Remove from today", modifier = Modifier.size(18.dp))
                }
              }
            },
          )
          if (i < shortlist.lastIndex) ThinDivider()
        }
      }

      if (suggestions.isNotEmpty()) {
        SectionHeader("Suggested", suggestions.size)
        suggestions.forEachIndexed { i, task ->
          TaskRow(
            task = task,
            onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) },
            trailing = {
              IconButton(onClick = { viewModel.toggleMyDay(task.id) }) {
                Icon(Icons.Default.WbSunny, contentDescription = "Add to today", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
              }
            },
          )
          if (i < suggestions.lastIndex) ThinDivider()
        }
      }

      // ---- Add from your other lists (collapsed by default) ----
      com.example.ui.components.ExpanderHeader("Add from your lists", addOpen, { addOpen = !addOpen })
      AnimatedVisibility(visible = addOpen) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PLAN_FILTERS.forEach { (filter, label) ->
              val n = uiState.planFilterCount(filter)
              FilterChip(
                selected = uiState.selectedPlanFilter == filter,
                onClick = { viewModel.selectPlanFilter(filter) },
                label = { Text(if (n > 0) "$label  $n" else label) },
              )
            }
          }
          val onTodayIds = shortlist.map { it.id }.toSet()
          if (browse.isEmpty()) {
            EmptyLine("Nothing in this list right now.")
          } else {
            val shown = browse.page(browseVisible.intValue)
            shown.forEachIndexed { i, task ->
              val already = task.id in onTodayIds
              TaskRow(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
                onClick = { viewModel.openTaskDetail(task.id) },
                trailing = {
                  IconButton(onClick = { viewModel.toggleMyDay(task.id) }) {
                    Icon(
                      imageVector = if (already) Icons.Default.Check else Icons.Default.WbSunny,
                      contentDescription = if (already) "On today" else "Add to today",
                      tint = if (already) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                },
              )
              if (i < shown.lastIndex) ThinDivider()
            }
            ShowMoreRow(browse.size - browseVisible.intValue, browseVisible)
          }
        }
      }

      // ---- Evening wrap-up ----
      Spacer(Modifier.height(28.dp))
      WrapUpCard(
        doneCount = doneToday.size,
        focusedSeconds = uiState.focusedSecondsToday,
        openCount = openCount,
        onMoveAllTomorrow = { viewModel.moveAllOpenToTomorrow() },
        onWriteJournal = { viewModel.openTodayJournal() },
      )

      if (doneToday.isNotEmpty()) {
        com.example.ui.components.ExpanderHeader("Done today", doneOpen, { doneOpen = !doneOpen }, count = doneToday.size)
        AnimatedVisibility(visible = doneOpen) {
          Column {
            doneToday.forEachIndexed { i, task ->
              TaskRow(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
                onClick = { viewModel.openTaskDetail(task.id) },
              )
              if (i < doneToday.lastIndex) ThinDivider()
            }
          }
        }
      }

      Spacer(Modifier.height(120.dp))
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

@Composable
private fun WrapUpCard(
  doneCount: Int,
  focusedSeconds: Long,
  openCount: Int,
  onMoveAllTomorrow: () -> Unit,
  onWriteJournal: () -> Unit,
) {
  val h = focusedSeconds / 3600
  val m = (focusedSeconds % 3600) / 60
  val focusedLabel = if (h > 0) "${h}h ${m}m" else "${m}m"

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(18.dp)) {
      Text(
        "Wrap up",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        WrapStat(doneCount.toString(), "done")
        WrapStat(focusedLabel, "focused")
        WrapStat(openCount.toString(), "still open")
      }
      Spacer(Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (openCount > 0) {
          TextButton(onClick = onMoveAllTomorrow) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  Move to tomorrow")
          }
        }
        TextButton(onClick = onWriteJournal) {
          Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
          Text("  Journal")
        }
      }
    }
  }
}

@Composable
private fun WrapStat(value: String, label: String) {
  Column {
    Text(
      value,
      style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun ProfileAvatar() {
  Box(
    modifier = Modifier.size(44.dp).padding(4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      AsyncImage(
        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDDmg7JhqUVlvrwxyBT8yr__AF65dkZzDJo5-_p7FkmFE8cqIKQaHiauNXj0wqSMSB7JUn1qvPIQzKqj_hM-wfkUKJ6QkUbgi_eBDPe4ugxxpDFLKvaMmRXgQvEHgP-HJpTibQY1G3ej-XswA4yrQr2BYoA2Cm2iN3cm-gB2BdTOSBigZlnt3a5vQAlgQPYs2JUo_hVn7Qh_h_4pJv5o661GPfLvNu96EdJZdmhCLmuxJfWCxeAg9CY",
        contentDescription = "Profile",
        modifier = Modifier.size(36.dp).clip(CircleShape),
      )
    }
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.success)
        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        .align(Alignment.BottomEnd),
    )
  }
}
