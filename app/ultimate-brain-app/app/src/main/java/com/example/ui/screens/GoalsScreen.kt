package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GoalModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GoalFilter
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  // Filter options, now enum-driven so the chip click handler can pass typed
  // values to ViewModel.selectGoalFilter(...) instead of raw strings.
  val filterOptions = listOf(
    GoalFilter.ACTIVE to R.string.goals_chip_active,
    GoalFilter.ACHIEVED to R.string.goals_chip_achieved,
    GoalFilter.DROPPED to R.string.goals_chip_dropped
  )

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  // Audit Finding 7/9: derive macro KPI counts from real state.
  // The "51% / 3 Active · 1 Achieved" line previously hardcoded literal numbers —
  // it now reads from the actual goals list so it changes when the user adds /
  // achieves a goal.
  val allGoals = uiState.filteredGoals
  val activeGoals = allGoals.count { it.status == "Active" }
  val achievedGoals = allGoals.count { it.status == "Achieved" }
  val totalGoalMilestones = allGoals.sumOf { it.totalMilestonesCount }
  val completedGoalMilestones = allGoals.sumOf { it.completedMilestonesCount }
  val overallProgressPercent = if (totalGoalMilestones > 0)
    (completedGoalMilestones * 100 / totalGoalMilestones)
  else
    0

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.goals_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        actions = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { viewModel.createNewGoal() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.goals_fab_new_goal), fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        // Audit Finding 6: standardize FAB elevation + bottom padding so the
        // docked action sits predictably across Goals/Tasks/Notes/Projects.
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("new_goal_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Horizontal filter chips (lives below the MediumTopAppBar in the body
      // so it scrolls with content; topBar slot stays reserved for the title).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        filterOptions.forEach { (filter, labelRes) ->
          val isSelected = uiState.selectedGoalFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectGoalFilter(filter) },
            label = { Text(stringResource(labelRes), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Macro KPI Bento
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(stringResource(R.string.goals_kpi_overall_progress), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = stringResource(R.string.goals_kpi_percent_format, overallProgressPercent),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.entityGoals
              )
              Text(
                text = stringResource(R.string.goals_kpi_active_achieved_format, activeGoals, achievedGoals),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.MILESTONES) }
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.entityGoals, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.goals_milestones_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.goals_view_all), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }
      }

      items(uiState.filteredGoals, key = { it.id }) { goal ->
        GoalCard(
          goal = goal,
          onClick = { viewModel.openGoalDetail(goal.id) },
          onProjectClick = { viewModel.openProjectDetail(it) }
        )
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
      }
    }
  }
}

@Composable
fun GoalCard(
  goal: GoalModel,
  onClick: () -> Unit,
  onProjectClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val isAchieved = goal.status == "Achieved"

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("goal_card_${goal.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isAchieved) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.primary
          ) {
            Text(
              text = goal.status,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              // Was hardcoded Color.White (audit Finding 6). M3 maps
              // primary → onPrimary and success → onSuccessContainer.
              color = if (isAchieved) MaterialTheme.colorScheme.onSuccessContainer
                      else MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
          Text(
            text = goal.tagArea,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.entityTagArea
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = if (isAchieved) goal.completionDate ?: stringResource(R.string.goals_achieved_label) else stringResource(R.string.goals_deadline_left_format, goal.deadline, goal.daysRemaining),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Title
      Text(
        text = goal.name,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      // Aggregated progress bar
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(stringResource(R.string.goals_aggregated_progress), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(goal.aggregatedProgressText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityGoals)
        }
        LinearProgressIndicator(
          progress = { goal.aggregatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = if (isAchieved) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.entityGoals,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Text(
          text = stringResource(
            R.string.goals_tasks_closed_format,
            goal.closedTasks,
            goal.totalTasks,
            goal.completedMilestonesCount,
            goal.totalMilestonesCount
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Linked Projects
      if (goal.linkedProjects.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(stringResource(R.string.goals_linked_projects_format, goal.linkedProjects.size), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
          goal.linkedProjects.forEach { proj ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onProjectClick(proj.id) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(proj.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  Text(proj.tasksSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(proj.progressText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityProjects)
              }
            }
          }
        }
      }
    }
  }
}
