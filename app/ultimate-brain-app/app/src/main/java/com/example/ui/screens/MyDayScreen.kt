package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.ExecutePane
import com.example.ui.components.PlanPane
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.components.SearchDialog
import com.example.ui.components.WrapUpPane
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import kotlinx.coroutines.launch

private val TODAY_TABS = listOf("Plan", "Execute", "Wrap up")

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
  val scope = rememberCoroutineScope()

  val pagerState = rememberPagerState(initialPage = 1, pageCount = { TODAY_TABS.size })

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = "My Day",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
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
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
        PrimaryTabRow(
          selectedTabIndex = pagerState.currentPage,
          containerColor = MaterialTheme.colorScheme.surface,
        ) {
          TODAY_TABS.forEachIndexed { i, label ->
            Tab(
              selected = pagerState.currentPage == i,
              onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
              selectedContentColor = MaterialTheme.colorScheme.primary,
              unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              text = {
                Text(
                  label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (pagerState.currentPage == i) FontWeight.SemiBold else FontWeight.Medium,
                )
              },
            )
          }
        }
      }
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
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.setQuickAddOpen(true) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      ) {
        Icon(Icons.Default.Add, contentDescription = "New task")
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      key = { it },
    ) { page ->
      when (page) {
        0 -> PlanPane(
          onToday = uiState.onTodayTasks,
          browse = uiState.planBrowseTasks,
          selectedFilter = uiState.selectedPlanFilter,
          filterCount = { uiState.planFilterCount(it) },
          onSelectFilter = viewModel::selectPlanFilter,
          onToggleToday = viewModel::toggleMyDay,
          onToggleComplete = viewModel::toggleTaskCompletion,
          onOpenTask = viewModel::openTaskDetail,
          onAddTask = { viewModel.setQuickAddOpen(true) },
          onStartFocusing = { scope.launch { pagerState.animateScrollToPage(1) } },
        )
        1 -> ExecutePane(
          focusSession = focusSession,
          upNext = uiState.upNextTasks,
          doneToday = uiState.doneTodayTasks,
          onStartFocus = { task -> viewModel.startFocus(context, task) },
          onPauseFocus = { viewModel.pauseFocus(context) },
          onResumeFocus = { viewModel.resumeFocus(context) },
          onStopFocus = { viewModel.stopFocus(context) },
          onToggleComplete = viewModel::toggleTaskCompletion,
          onOpenTask = viewModel::openTaskDetail,
          onGoToPlan = { scope.launch { pagerState.animateScrollToPage(0) } },
        )
        2 -> WrapUpPane(
          doneCount = uiState.doneTodayTasks.size,
          focusedSeconds = uiState.focusedSecondsToday,
          openTasks = uiState.openTodayTasks,
          onToggleComplete = viewModel::toggleTaskCompletion,
          onMoveTaskTomorrow = viewModel::postponeOverdueTask,
          onMoveAllTomorrow = viewModel::moveAllOpenToTomorrow,
          onOpenTask = viewModel::openTaskDetail,
          onWriteJournal = viewModel::openTodayJournal,
        )
      }
    }
  }

  if (uiState.isQuickAddOpen) {
    QuickAddBottomSheet(
      onDismiss = { viewModel.setQuickAddOpen(false) },
      onSaveTask = { name, projectId, priority, isMyDay -> viewModel.addNewTask(name, projectId, priority, isMyDay) },
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
