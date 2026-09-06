package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.entityTagEntity
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  // Audit Finding 7/9: derive macro KPI counts from real state instead of
  // frozen literals. The "3 Active / 9 Tags / 6 milestones / Live: 02:37" row
  // is the first thing the user sees on the hub — these numbers used to never
  // change regardless of user action.
  val activeGoalCount = uiState.goals.count { it.status == "Active" }
  val achievedGoalCount = uiState.goals.count { it.status == "Achieved" }
  val activeTagCount = uiState.tags.size
  val activeMilestoneCount = uiState.milestones.size
  val milestonesTodayCount = uiState.milestones.count { it.isToday }
  val timerLabel = if (uiState.isTimerRunning)
    stringResource(R.string.more_hub_timer_live_format, uiState.formattedTimer)
  else
    stringResource(R.string.more_hub_timer_idle_format, uiState.formattedTimer)

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.more_hub_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        actions = {
          // PS avatar / settings shortcut sits in the actions slot so it
          // doesn't collide with the large title.
          Box(
            modifier = Modifier
              .padding(end = 12.dp)
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
              .clickable { viewModel.navigateTo(AppScreen.SETTINGS) }
              .testTag("hub_avatar"),
            contentAlignment = Alignment.Center
          ) {
            Text(stringResource(R.string.settings_avatar_initials), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
          }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      BottomNavBar(
        activeDestination = BottomNavDestination.MORE,
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
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Productivity Engines
      item {
        Text(stringResource(R.string.more_hub_section_engines), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
      }

      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            MoreHubItemRow(
              icon = Icons.Default.TrackChanges,
              iconTint = MaterialTheme.colorScheme.entityGoals,
              title = stringResource(R.string.more_hub_goals_title),
              subtitle = stringResource(R.string.more_hub_goals_subtitle_format, activeGoalCount, achievedGoalCount),
              badge = if (activeGoalCount > 0) stringResource(R.string.more_hub_goals_badge_format, activeGoalCount) else null,
              onClick = { viewModel.navigateTo(AppScreen.GOALS) },
              testTag = "hub_goals_item"
            )
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(
              icon = Icons.Default.Flag,
              iconTint = MaterialTheme.colorScheme.entityProjects,
              title = stringResource(R.string.more_hub_milestones_title),
              subtitle = stringResource(R.string.more_hub_milestones_subtitle_format, activeMilestoneCount),
              badge = if (milestonesTodayCount > 0) stringResource(R.string.more_hub_milestones_badge_format, milestonesTodayCount) else null,
              onClick = { viewModel.navigateTo(AppScreen.MILESTONES) },
              testTag = "hub_milestones_item"
            )
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(
              icon = Icons.Default.Tag,
              iconTint = MaterialTheme.colorScheme.entityTagArea,
              title = stringResource(R.string.more_hub_tags_title),
              subtitle = stringResource(R.string.more_hub_tags_subtitle),
              badge = if (activeTagCount > 0) stringResource(R.string.more_hub_tags_badge_format, activeTagCount) else null,
              onClick = { viewModel.navigateTo(AppScreen.TAGS) },
              testTag = "hub_tags_item"
            )
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(
              icon = Icons.Default.Timelapse,
              iconTint = MaterialTheme.colorScheme.primary,
              title = stringResource(R.string.more_hub_work_sessions_title),
              subtitle = stringResource(R.string.more_hub_work_sessions_subtitle),
              badge = timerLabel,
              onClick = { viewModel.navigateTo(AppScreen.WORK_SESSIONS) },
              testTag = "hub_work_sessions_item"
            )
          }
        }
      }

      // Library
      item {
        Text("Library", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
      }
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            MoreHubItemRow(Icons.Default.Person, MaterialTheme.colorScheme.entityTagEntity, "People", "Contacts & CRM", uiState.people.size.takeIf { it > 0 }?.let { "$it" }, { viewModel.navigateTo(AppScreen.PEOPLE) }, "hub_people")
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(Icons.Default.MenuBook, MaterialTheme.colorScheme.entityNotes, "Books", "Reading list & library", uiState.books.size.takeIf { it > 0 }?.let { "$it" }, { viewModel.navigateTo(AppScreen.BOOKS) }, "hub_books")
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(Icons.Default.Timeline, MaterialTheme.colorScheme.onSurfaceVariant, "Reading log", "Session-by-session progress", null, { viewModel.navigateTo(AppScreen.READING_LOG) }, "hub_readinglog")
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(Icons.Default.Restaurant, MaterialTheme.colorScheme.entityGoals, "Recipes", "Your recipe box", uiState.recipes.size.takeIf { it > 0 }?.let { "$it" }, { viewModel.navigateTo(AppScreen.RECIPES) }, "hub_recipes")
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primary, "Meal planner", "This week's meals", null, { viewModel.navigateTo(AppScreen.MEAL_PLANNER) }, "hub_meals")
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(Icons.Default.LocalOffer, MaterialTheme.colorScheme.onSurfaceVariant, "Genres", "Book genres", null, { viewModel.navigateTo(AppScreen.GENRES) }, "hub_genres")
          }
        }
      }

      // System & Preferences
      item {
        Text(stringResource(R.string.more_hub_section_system), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
      }

      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            MoreHubItemRow(
              icon = Icons.Default.Search,
              iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
              title = stringResource(R.string.more_hub_search_title),
              subtitle = stringResource(R.string.more_hub_search_subtitle),
              badge = null,
              onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) },
              testTag = "hub_search_item"
            )
            com.example.ui.components.ThinDivider()
            MoreHubItemRow(
              icon = Icons.Default.Settings,
              iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
              title = stringResource(R.string.more_hub_settings_title),
              subtitle = stringResource(R.string.more_hub_settings_subtitle),
              badge = null,
              onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
              testTag = "hub_settings_item"
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}

@Composable
fun MoreHubItemRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  badge: String?,
  onClick: () -> Unit,
  testTag: String = ""
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
      }

      Column {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      if (badge != null) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
