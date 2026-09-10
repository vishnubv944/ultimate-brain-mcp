package com.example.ui.screens

import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.layout.width
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// "Add to today" browse chips — the same Notion view set as the Tasks tab
// (minus Done), but with their own independent selection.
private val BROWSE_KEYS =
  com.example.viewmodel.BuiltinFilters.keys(com.example.model.FilterScope.TASKS).filter { it != "DONE" }

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
  val doneToday = uiState.doneTodayTasks
  val openCount = uiState.openTodayTasks.size
  var browseKey by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("TODAY") }
  val browse = uiState.tasksMatching(browseKey)
  val browseVisible = rememberVisibleCount(browseKey)
  val groupByProject = browseKey == "ACTIVE_PROJECTS" || browseKey == "ALL_PROJECTS"
  val onTodayIds = shortlist.map { it.id }.toSet()

  var doneOpen by remember { mutableStateOf(false) }
  val projExpanded = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
  val listState = androidx.compose.foundation.lazy.rememberLazyListState()

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
    androidx.compose.foundation.lazy.LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = TodayPad),
    ) {
      if (focusSession != null) {
        item("focus") {
          Spacer(Modifier.height(12.dp))
          FocusCard(
            session = focusSession!!,
            onPause = { viewModel.pauseFocus(context) },
            onResume = { viewModel.resumeFocus(context) },
            onStop = { viewModel.stopFocus(context) },
            onOpenTask = { viewModel.openTaskDetail(focusSession!!.taskId) },
          )
        }
      }

      item("ontoday-hdr") { SectionHeader("On today", shortlist.size) }
      if (shortlist.isEmpty()) {
        item("ontoday-empty") {
          EmptyLine(
            "Nothing planned yet — add from your lists below.",
            actionLabel = "Add a task",
            onAction = { viewModel.setQuickAddOpen(true) },
          )
        }
      } else {
        itemsIndexed(shortlist, key = { _, t -> "st:${t.id}" }) { i, task ->
          val active = focusSession?.taskId == task.id
          TaskRow(
            task = task,
            onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) },
            onLongClick = { viewModel.openTaskQuickEdit(task.id) },
            trailing = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (focusSession == null) {
                  IconButton(onClick = { viewModel.startFocus(context, task) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start focus", tint = MaterialTheme.colorScheme.primary)
                  }
                } else if (active) {
                  Text("focusing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

      // ---- Add to today: always visible, the browse lists ----
      item("add-hdr") {
        Spacer(Modifier.height(4.dp))
        SectionHeader("Add to today")
      }
      item("add-chips") {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BROWSE_KEYS.forEach { key ->
            val label = com.example.viewmodel.BuiltinFilters.label(key)
            val n = uiState.tasksMatching(key).size
            FilterChip(
              selected = browseKey == key,
              onClick = { browseKey = key },
              label = { Text(if (n > 0) "$label  $n" else label) },
            )
          }
        }
      }

      if (browse.isEmpty()) {
        item("add-empty") { EmptyLine("Nothing in this list right now.") }
      } else if (groupByProject) {
        // Accordion: one collapsible header per project.
        val grouped = browse.groupBy { it.projectName?.takeIf { n -> n.isNotBlank() } ?: "No project" }
          .toList().sortedByDescending { it.second.size }
        grouped.forEach { (proj, projTasks) ->
          val expanded = projExpanded[proj] == true
          item(key = "grp:$proj") {
            com.example.ui.components.ExpanderHeader(
              proj, expanded, { projExpanded[proj] = !expanded }, count = projTasks.size,
            )
          }
          if (expanded) {
            itemsIndexed(projTasks, key = { _, t -> "gt:$proj:${t.id}" }) { i, task ->
              BrowseTaskRow(task, task.id in onTodayIds, viewModel)
              if (i < projTasks.lastIndex) ThinDivider(Modifier.padding(start = 44.dp))
            }
          }
        }
      } else {
        val shown = browse.page(browseVisible.intValue)
        itemsIndexed(shown, key = { _, t -> "bt:${t.id}" }) { i, task ->
          BrowseTaskRow(task, task.id in onTodayIds, viewModel)
          if (i < shown.lastIndex) ThinDivider()
        }
        item("showmore") { ShowMoreRow(browse.size - browseVisible.intValue, browseVisible) }
      }

      // ---- Wrap up ----
      item("wrapup") {
        Spacer(Modifier.height(24.dp))
        ThinDivider()
        WrapUp(
          doneCount = doneToday.size,
          focusedSeconds = uiState.focusedSecondsToday,
          openCount = openCount,
          onMoveAllTomorrow = { viewModel.moveAllOpenToTomorrow() },
          onWriteJournal = { viewModel.openTodayJournal() },
        )
      }

      if (doneToday.isNotEmpty()) {
        item("done-hdr") {
          com.example.ui.components.ExpanderHeader("Done today", doneOpen, { doneOpen = !doneOpen }, count = doneToday.size)
        }
        if (doneOpen) {
          itemsIndexed(doneToday, key = { _, t -> "dt:${t.id}" }) { i, task ->
            TaskRow(
              task = task,
              onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
              onClick = { viewModel.openTaskDetail(task.id) },
            )
            if (i < doneToday.lastIndex) ThinDivider()
          }
        }
      }

      item("bottom") { Spacer(Modifier.height(120.dp)) }
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

/** A task row in the "Add to today" list — trailing sun toggles My Day in place. */
@Composable
private fun BrowseTaskRow(task: com.example.model.Task, onToday: Boolean, viewModel: MyDayViewModel) {
  TaskRow(
    task = task,
    onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
    onClick = { viewModel.openTaskDetail(task.id) },
    onLongClick = { viewModel.openTaskQuickEdit(task.id) },
    trailing = {
      IconButton(onClick = { viewModel.toggleMyDay(task.id) }) {
        Icon(
          imageVector = if (onToday) Icons.Default.Check else Icons.Default.WbSunny,
          contentDescription = if (onToday) "On today" else "Add to today",
          tint = if (onToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    },
  )
}

@Composable
private fun WrapUp(
  doneCount: Int,
  focusedSeconds: Long,
  openCount: Int,
  onMoveAllTomorrow: () -> Unit,
  onWriteJournal: () -> Unit,
) {
  val h = focusedSeconds / 3600
  val m = (focusedSeconds % 3600) / 60
  val focusedLabel = if (h > 0) "${h}h ${m}m" else "${m}m"

  Column(Modifier.padding(top = 16.dp)) {
    Text(
      "Wrap up",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      buildList {
        add("$doneCount done")
        if (focusedSeconds > 0) add("$focusedLabel focused")
        add("$openCount still open")
      }.joinToString("  ·  "),
      style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(top = 4.dp),
    )
    Row(
      modifier = Modifier.padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (openCount > 0) {
        TextButton(
          onClick = onMoveAllTomorrow,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Move to tomorrow")
        }
      }
      TextButton(
        onClick = onWriteJournal,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Write journal")
      }
    }
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
