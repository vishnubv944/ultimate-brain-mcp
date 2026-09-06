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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MilestoneModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityGoals
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MilestoneFilter
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  val filters = listOf(
    MilestoneFilter.ALL to R.string.milestones_chip_all_format,
    MilestoneFilter.IN_PROGRESS to R.string.milestones_chip_in_progress,
    MilestoneFilter.COMPLETED to R.string.milestones_chip_completed,
    MilestoneFilter.PENDING to R.string.milestones_chip_pending
  )

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.milestones_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.GOALS) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
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
        onClick = { /* create milestone */ },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.milestones_fab_new), fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("new_milestone_fab")
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
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        filters.forEach { (filter, labelRes) ->
          val isSelected = uiState.selectedMilestoneFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectMilestoneFilter(filter) },
            label = {
              val text = if (filter == MilestoneFilter.ALL) {
                stringResource(R.string.milestones_chip_all_format, uiState.milestones.size)
              } else {
                stringResource(labelRes)
              }
              Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
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
        // Summary Bento
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
              Text(stringResource(R.string.milestones_health_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(stringResource(R.string.milestones_health_value_format, 3, 2), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text(stringResource(R.string.milestones_on_track), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.success)
            }
            Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.entityGoals, modifier = Modifier.size(28.dp))
          }
        }
      }

      items(uiState.filteredMilestones, key = { it.id }) { ms ->
        MilestoneCard(
          milestone = ms,
          onToggle = { viewModel.toggleMilestoneStatus(ms.id) },
          onGoalClick = { viewModel.openGoalDetail(ms.goalId) }
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
fun MilestoneCard(
  milestone: MilestoneModel,
  onToggle: () -> Unit,
  onGoalClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDone = milestone.status == "Completed"
  val isInProgress = milestone.status == "In Progress"

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("milestone_card_${milestone.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh
          ) {
            Text(
              text = milestone.goalCategory,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = when {
              isDone -> MaterialTheme.colorScheme.success
              isInProgress -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
          ) {
            Text(
              text = milestone.status,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              // Color.White → conditional on tokens (Finding 6). isDone uses
              // the success family, isInProgress uses the primary family; both
              // already pair with a tonal container so we don't need a literal.
              color = when {
                isDone -> MaterialTheme.colorScheme.onSuccessContainer
                isInProgress -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = when {
              isDone -> Icons.Default.CheckCircle
              isInProgress -> Icons.Default.Timelapse
              else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
              isDone -> MaterialTheme.colorScheme.success
              isInProgress -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
          )
        }
      }

      // Title
      Text(
        text = milestone.name,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      // Linked Goal
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onGoalClick() }
      ) {
        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.entityGoals, modifier = Modifier.size(14.dp))
        Text(milestone.goalName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.entityGoals)
      }

      // Target Date & Note
      Text(milestone.targetDateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      if (milestone.note != null) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isInProgress) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.success.copy(alpha = 0.08f)
        ) {
          Text(
            text = milestone.note,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isInProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.success,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Tasks progress
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(stringResource(R.string.milestones_tasks_linked_format, milestone.linkedTasksDone, milestone.linkedTasksTotal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          val pct = if (milestone.linkedTasksTotal > 0) (milestone.linkedTasksDone * 100 / milestone.linkedTasksTotal) else 0
          Text(stringResource(R.string.milestones_percent_format, pct), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
        LinearProgressIndicator(
          progress = { if (milestone.linkedTasksTotal > 0) milestone.linkedTasksDone.toFloat() / milestone.linkedTasksTotal else 0f },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
      }
    }
  }
}
