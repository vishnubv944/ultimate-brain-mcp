package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DailyRitualPhase
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.ExecuteSection
import com.example.ui.components.PlanSection
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.RitualJumpBar
import com.example.ui.components.SearchDialog
import com.example.ui.components.WrapUpSection
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.PlanFilter
import com.example.viewmodel.ExecuteFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDayScreen(
  viewModel: MyDayViewModel,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val scrollState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  var activeNavDestination by remember { mutableStateOf(BottomNavDestination.TODAY) }

  // Handle snackbar messages from ViewModel — resolve the sealed
  // [com.example.viewmodel.SnackbarMessage] to a localized string via the
  // VM's formatSnackbarMessage(Context, SnackbarMessage) helper.
  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  // Active focus task — derived from state, not hardcoded. Used to give the
  // ExecuteSection banner real labels (audit Finding 10).
  val activeFocusTask = remember(uiState.tasks, uiState.isTimerRunning) {
    uiState.tasks.firstOrNull { it.isActiveSession }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(
            text = stringResource(R.string.myday_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
          )
        },
        navigationIcon = {
          // Profile avatar (replaces HeaderSection avatar block)
          Box(
            modifier = Modifier
              .size(44.dp)
              .padding(4.dp)
              .testTag("profile_avatar_box"),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDDmg7JhqUVlvrwxyBT8yr__AF65dkZzDJo5-_p7FkmFE8cqIKQaHiauNXj0wqSMSB7JUn1qvPIQzKqj_hM-wfkUKJ6QkUbgi_eBDPe4ugxxpDFLKvaMmRXgQvEHgP-HJpTibQY1G3ej-XswA4yrQr2BYoA2Cm2iN3cm-gB2BdTOSBigZlnt3a5vQAlgQPYs2JUo_hVn7Qh_h_4pJv5o661GPfLvNu96EdJZdmhCLmuxJfWCxeAg9CY",
                contentDescription = stringResource(R.string.myday_avatar_cd),
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape),
                contentScale = ContentScale.Crop
              )
            }
            // Green status badge
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.success)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .align(Alignment.BottomEnd)
            )
          }
        },
        actions = {
          // RitualJumpBar removed from actions slot — was breaking the M3
          // top app bar (Finding 1). Now lives below the bar in the body
          // where the segmented-tab pattern belongs.
          IconButton(
            onClick = { viewModel.setSearchOpen(true) },
            modifier = Modifier.testTag("search_button")
          ) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search_tasks))
          }
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.testTag("theme_toggle_button")
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = stringResource(R.string.action_toggle_theme)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      BottomNavBar(
        activeDestination = BottomNavDestination.TODAY,
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
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .size(56.dp)
          .testTag("add_task_fab")
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
      // Native M3 segmented control directly below the top app bar. Selecting
      // a phase swaps the section shown below — no scroll-jumping through a
      // single mega-list (which left clipped card slivers at the top).
      RitualJumpBar(
        selectedPhase = uiState.selectedPhase,
        onPhaseSelected = { phase ->
          viewModel.selectPhase(phase)
          coroutineScope.launch { scrollState.animateScrollToItem(0) }
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )

      LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize()
      ) {
        item {
          Spacer(modifier = Modifier.height(4.dp))

          when (uiState.selectedPhase) {
            DailyRitualPhase.PLAN -> PlanSection(
              tasks = uiState.tasks,
              selectedFilter = uiState.selectedPlanFilter,
              onSelectFilter = { viewModel.selectPlanFilter(it) },
              onToggleMyDay = { viewModel.toggleMyDay(it) },
              onAddTaskClick = { viewModel.setQuickAddOpen(true) },
              // Audit Finding 3: wire real counts so chips stop reading (0).
              inboxCount = uiState.tasks.count { !it.isDone && !it.isMyDay && it.projectName == null },
              overdueCount = uiState.overdueTasks.size
            )

            DailyRitualPhase.EXECUTE -> ExecuteSection(
              doingTasks = uiState.doingTasks,
              todoTasks = uiState.todoTasks,
              doneTasks = uiState.doneTodayTasks,
              formattedTimer = uiState.formattedTimer,
              isTimerRunning = uiState.isTimerRunning,
              selectedFilter = uiState.selectedExecuteFilter,
              isDoneExpanded = uiState.isDoneExpanded,
              // Finding 10: pipe real task + project name instead of hardcoded
              // "Q3 launch / Cut release branch and tag".
              activeFocusTask = activeFocusTask,
              activeFocusProjectName = activeFocusTask?.projectName,
              onToggleTimer = { viewModel.toggleTimer() },
              onSelectFilter = { viewModel.selectExecuteFilter(it) },
              onToggleDoneAccordion = { viewModel.toggleDoneAccordion() },
              onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
              onTaskClick = { viewModel.openTaskDetail(it) }
            )

            DailyRitualPhase.WRAP_UP -> WrapUpSection(
              overdueTasks = uiState.overdueTasks,
              currentStep = uiState.eveningReviewStep,
              onStepSelected = { viewModel.setEveningStep(it) },
              onClearMyDay = { viewModel.clearMyDay() },
              onPostponeTask = { viewModel.postponeOverdueTask(it) },
              onRescheduleAll = { viewModel.rescheduleAllOverdue() }
            )
          }

          // Clearance for the FAB + bottom navigation bar.
          Spacer(modifier = Modifier.height(120.dp))
        }
      }
    }
  }

  // Quick Add Bottom Sheet
  if (uiState.isQuickAddOpen) {
    QuickAddBottomSheet(
      onDismiss = { viewModel.setQuickAddOpen(false) },
      onSaveTask = { name, projectId, priority, isMyDay ->
        viewModel.addNewTask(name, projectId, priority, isMyDay)
      },
      projects = uiState.projects
    )
  }

  // Search Dialog
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
