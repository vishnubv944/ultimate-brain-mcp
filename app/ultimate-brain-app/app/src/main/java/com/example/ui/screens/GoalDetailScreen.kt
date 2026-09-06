package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.MarkdownBody
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityProjects
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val goal = uiState.selectedGoal

  DetailScaffold(
    title = "Goal",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      if (goal != null && goal.status == "Active") {
        IconButton(onClick = { viewModel.achieveGoal(goal.id) }) {
          Icon(Icons.Default.Check, contentDescription = "Mark achieved")
        }
        IconButton(onClick = { viewModel.dropGoal(goal.id) }) {
          Icon(Icons.Outlined.Cancel, contentDescription = "Drop goal")
        }
      }
    },
  ) { innerPadding ->
    if (goal == null) {
      EmptyLine("Goal not found.", Modifier.padding(innerPadding).padding(TodayPad))
      return@DetailScaffold
    }
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        Spacer(Modifier.height(8.dp))
        Text(goal.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = TodayPad))
        Text(
          buildList {
            add(goal.status)
            if (goal.deadline.isNotBlank() && goal.deadline != "—") add(goal.deadline)
            if (goal.tagArea.isNotBlank()) add(goal.tagArea)
          }.joinToString("  ·  "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        Spacer(Modifier.height(12.dp))
        StatCard(
          listOf(
            Stat(goal.aggregatedProgressText.ifBlank { "0%" }, "progress"),
            Stat(goal.linkedProjects.size.toString(), "projects"),
            Stat("${goal.closedTasks}/${goal.totalTasks}", "tasks"),
          ),
          Modifier.padding(horizontal = TodayPad),
        )
        SectionHeader("Details", modifier = Modifier.padding(horizontal = TodayPad))
        com.example.ui.components.OptionRow(
          "Status", goal.status,
          uiState.optionsFor("goal.Status", listOf("Dream", "Active", "Achieved")),
          { it?.let { s -> viewModel.setGoalStatusValue(goal.id, s) } },
          Modifier.padding(horizontal = TodayPad), allowClear = false,
        )
        com.example.ui.components.DateFieldRow(
          "Deadline", goal.deadlineIso,
          { viewModel.setGoalDeadline(goal.id, it) },
          Modifier.padding(horizontal = TodayPad),
        )
      }

      item { SectionHeader("Projects", goal.linkedProjects.size, Modifier.padding(horizontal = TodayPad)) }
      if (goal.linkedProjects.isEmpty()) {
        item { EmptyLine("No projects linked to this goal.", Modifier.padding(horizontal = TodayPad)) }
      } else {
        itemsIndexed(goal.linkedProjects, key = { _, p -> p.id }) { i, p ->
          EntityRow(
            title = p.name,
            meta = "${p.progressText}  ·  ${p.tasksSummary}",
            leadingIcon = Icons.Default.Folder,
            leadingIconTint = MaterialTheme.colorScheme.entityProjects,
            onClick = { viewModel.openProjectDetail(p.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (i < goal.linkedProjects.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }

      if (!uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == goal.id) {
        item {
          SectionHeader("Notes", modifier = Modifier.padding(horizontal = TodayPad))
          MarkdownBody(uiState.detailBody!!, Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}
