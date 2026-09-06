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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoalModel
import com.example.model.MilestoneModel
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val goal = uiState.selectedGoal ?: return
  val goalMilestones = uiState.milestones.filter { it.goalId == goal.id || it.goalName == goal.name }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Goal Detail", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { /* edit goal */ }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Sticky Action Bar
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = { viewModel.dropGoal(goal.id) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Drop Goal", fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = { viewModel.achieveGoal(goal.id) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
              // Audit Finding 6: pair success with onSuccessContainer so dark
              // mode keeps contrast (raw SuccessGreen + Color.White legibility
              // is good, but the dark-mode M3 palette maps success →
              // onSuccessContainer; staying on the standard token preserves
              // consistency with every other "Done" CTA in the app).
              containerColor = MaterialTheme.colorScheme.success,
              contentColor = MaterialTheme.colorScheme.onSuccessContainer
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark Achieved", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hero Card
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_hero_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (goal.status == "Achieved") MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.primary
              ) {
                Text(
                  text = goal.status,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  // Audit Finding 6: was hardcoded Color.White. Map onto the
                  // proper "on*" tokens so dark mode stays legible (success →
                  // onSuccessContainer, primary → onPrimary).
                  color = if (goal.status == "Achieved")
                    MaterialTheme.colorScheme.onSuccessContainer
                  else
                    MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
              Text(
                text = "${goal.deadline} · ${goal.daysRemaining}d left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Text(
              text = goal.name,
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            // Progress Bento
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Aggregated Completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(goal.aggregatedProgressText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityGoals)
              }
              LinearProgressIndicator(
                progress = { goal.aggregatedProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.entityGoals,
                trackColor = MaterialTheme.colorScheme.surfaceContainer
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("${goal.closedTasks} of ${goal.totalTasks} tasks closed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${goal.completedMilestonesCount} of ${goal.totalMilestonesCount} milestones done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      // Linked Projects
      item {
        Text("Linked Projects (${goal.linkedProjects.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(goal.linkedProjects) { proj ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openProjectDetail(proj.id) }
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(proj.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Text(proj.progressText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityProjects)
            }
            LinearProgressIndicator(
              progress = { proj.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = MaterialTheme.colorScheme.entityProjects,
              trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Text(proj.tasksSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      // Key Milestones
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Key Milestones (${goalMilestones.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
          Text(
            text = "View Milestones →",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.MILESTONES) }
          )
        }
      }

      items(goalMilestones) { ms ->
        val isDone = ms.status == "Completed"
        val isInProgress = ms.status == "In Progress"

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.toggleMilestoneStatus(ms.id) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
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
              modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
              Text(ms.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
              Text(ms.targetDateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              if (ms.note != null) {
                Text(ms.note, style = MaterialTheme.typography.labelSmall, color = if (isInProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.success)
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}
