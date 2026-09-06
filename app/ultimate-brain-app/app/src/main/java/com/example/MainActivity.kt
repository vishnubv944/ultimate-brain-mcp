package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.EditProjectScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.GoalDetailScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.MilestonesScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MyDayScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TagDetailScreen
import com.example.ui.screens.TagsScreen
import com.example.ui.screens.TaskDetailScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.WorkSessionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.NavIntent
import com.example.viewmodel.isTopLevelTab

class MainActivity : ComponentActivity() {
  private val viewModel: MyDayViewModel by viewModels()

  private val notifPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Focus-timer notification permission (Android 13+). Asked once up front so
    // the ongoing timer notification can actually show.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    setContent {
      val systemInDark = isSystemInDarkTheme()
      var isDarkTheme by remember { mutableStateOf(systemInDark) }
      val uiState by viewModel.uiState.collectAsState()
      val navController = rememberNavController()

      // Audit Finding 15 (Round 5): bridge ViewModel's NavIntent event stream
      // -> NavHost back stack. Previously this was keyed on `uiState.currentScreen`,
      // which mutates for many reasons unrelated to navigation intent (toggling a
      // task, posting a snackbar). Collecting the dedicated one-shot Flow with
      // LaunchedEffect(Unit) means we only navigate when the VM explicitly asks.
      // NavHost starts at "today", so the first emitted NAVIGATE(TODAY) is a no-op
      // (currentDestination is already "today").
      LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { intent ->
          when (intent) {
            is NavIntent.Navigate -> {
              val route = routeFor(intent.screen)
              if (navController.currentDestination?.route != route) {
                navController.navigate(route) {
                  launchSingleTop = true
                  if (intent.screen.isTopLevelTab) {
                    // Standard M3 bottom-nav behaviour: don't stack tabs on
                    // top of each other; pop back to the graph start and
                    // preserve/restore each tab's own scroll + nested state.
                    popUpTo(navController.graph.startDestinationId) {
                      saveState = true
                    }
                    restoreState = true
                  }
                }
              }
            }
            is NavIntent.Back -> {
              navController.popBackStack()
            }
          }
        }
      }

      MyApplicationTheme(
        darkTheme = isDarkTheme,
        // Audit Finding 16: SettingsScreen's "Dynamic Color (Material You)"
        // toggle now drives the theme's `dynamicColor` parameter. Was
        // permanently hardcoded to `false` (the Theme.kt default), so the
        // settings switch was a dead UI control.
        dynamicColor = uiState.dynamicColorEnabled,
      ) {
        NavHost(navController = navController, startDestination = "today") {
          composable("today") {
            MyDayScreen(
              viewModel = viewModel,
              isDarkTheme = isDarkTheme,
              onToggleTheme = { isDarkTheme = !isDarkTheme }
            )
          }
          composable("tasks") {
            TasksScreen(viewModel = viewModel)
          }
          composable("task_detail") {
            TaskDetailScreen(
              viewModel = viewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }
          composable("projects") {
            ProjectsScreen(viewModel = viewModel)
          }
          composable("project_detail") {
            ProjectDetailScreen(viewModel = viewModel)
          }
          composable("edit_project") {
            EditProjectScreen(viewModel = viewModel)
          }
          composable("notes") {
            NotesScreen(viewModel = viewModel)
          }
          composable("note_detail") {
            NoteDetailScreen(viewModel = viewModel)
          }
          composable("note_editor") {
            NoteEditorScreen(viewModel = viewModel)
          }
          composable("goals") {
            GoalsScreen(viewModel = viewModel)
          }
          composable("goal_detail") {
            GoalDetailScreen(viewModel = viewModel)
          }
          composable("milestones") {
            MilestonesScreen(viewModel = viewModel)
          }
          composable("tags") {
            TagsScreen(viewModel = viewModel)
          }
          composable("tag_detail") {
            TagDetailScreen(viewModel = viewModel)
          }
          composable("work_sessions") {
            WorkSessionsScreen(viewModel = viewModel)
          }
          composable("settings") {
            SettingsScreen(viewModel = viewModel)
          }
          composable("global_search") {
            GlobalSearchScreen(viewModel = viewModel)
          }
          composable("more_hub") {
            MoreHubScreen(viewModel = viewModel)
          }
        }
      }
    }
  }
}

private fun routeFor(screen: AppScreen): String = when (screen) {
  AppScreen.TODAY -> "today"
  AppScreen.TASKS -> "tasks"
  AppScreen.TASK_DETAIL -> "task_detail"
  AppScreen.PROJECTS -> "projects"
  AppScreen.PROJECT_DETAIL -> "project_detail"
  AppScreen.EDIT_PROJECT -> "edit_project"
  AppScreen.NOTES -> "notes"
  AppScreen.NOTE_DETAIL -> "note_detail"
  AppScreen.NOTE_EDITOR -> "note_editor"
  AppScreen.GOALS -> "goals"
  AppScreen.GOAL_DETAIL -> "goal_detail"
  AppScreen.MILESTONES -> "milestones"
  AppScreen.TAGS -> "tags"
  AppScreen.TAG_DETAIL -> "tag_detail"
  AppScreen.WORK_SESSIONS -> "work_sessions"
  AppScreen.SETTINGS -> "settings"
  AppScreen.GLOBAL_SEARCH -> "global_search"
  AppScreen.MORE_HUB -> "more_hub"
}