package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
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
        com.example.ui.components.DetailTitle(
          goal.name,
          { viewModel.renameGoal(goal.id, it) },
          Modifier.padding(horizontal = TodayPad),
        )
        val meta = buildList {
          add(goal.status)
          if (goal.deadline.isNotBlank() && goal.deadline != "—") add(goal.deadline)
          if (goal.tagArea.isNotBlank()) add(goal.tagArea)
        }
        Text(
          meta.joinToString("  ·  "),
          style = MaterialTheme.typography.bodyMedium,
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
        Spacer(Modifier.height(8.dp))
        com.example.ui.components.PropertyChipRow(Modifier.padding(horizontal = TodayPad)) {
          com.example.ui.components.SelectChip(
            "Status", goal.status,
            uiState.optionsFor("goal.Status", listOf("Dream", "Active", "Achieved")),
            { it?.let { s -> viewModel.setGoalStatusValue(goal.id, s) } },
            allowClear = false,
          )
          com.example.ui.components.DateChip("Deadline", goal.deadlineIso) { viewModel.setGoalDeadline(goal.id, it) }
          com.example.ui.components.DateChip("Goal set", goal.goalSetIso) { viewModel.setGoalSetDate(goal.id, it) }
          com.example.ui.components.SelectChip(
            "Area", uiState.tags.firstOrNull { it.id == goal.tagId }?.name,
            uiState.tags.map { it.name },
            { name -> viewModel.setGoalTagRelation(goal.id, uiState.tags.firstOrNull { it.name == name }?.id) },
          )
        }
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

      val goalMilestones = uiState.milestones
        .filter { it.goalId == goal.id }
        .sortedBy { it.targetDateIso ?: "9999-99-99" }
      if (goalMilestones.isNotEmpty()) {
        item {
          SectionHeader(
            "Milestones",
            goalMilestones.count { it.status == "Completed" },
            Modifier.padding(horizontal = TodayPad),
          )
        }
        itemsIndexed(goalMilestones, key = { _, m -> m.id }) { i, ms ->
          val done = ms.status == "Completed"
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = TodayPad - 4.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable { viewModel.toggleMilestoneStatus(ms.id) }
              .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
              contentDescription = null,
              tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(
                ms.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null,
              )
              val date = ms.targetDateIso?.substringBefore('T')?.let { DateUtils.displayLabel(it) }
              if (date != null) {
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
          if (i < goalMilestones.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }

      if (!uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == goal.id &&
        com.example.ui.components.markdownHasRenderableContent(uiState.detailBody!!)
      ) {
        item {
          SectionHeader("Notes", modifier = Modifier.padding(horizontal = TodayPad))
          MarkdownBody(uiState.detailBody!!, Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}
