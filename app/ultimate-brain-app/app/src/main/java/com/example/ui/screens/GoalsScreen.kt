package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GoalModel
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.groupSections
import com.example.ui.components.groupedRows
import com.example.viewmodel.BuiltinFilters
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityGoals
import com.example.viewmodel.GoalFilter
import com.example.viewmodel.MyDayViewModel

private val GOAL_FILTERS = listOf(
  GoalFilter.ACTIVE to "Active",
  GoalFilter.ACHIEVED to "Achieved",
  GoalFilter.DROPPED to "Archived",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val selKey = uiState.selectedChipKey(com.example.model.FilterScope.GOALS)
  val list = uiState.goalsMatching(selKey)
  val sections = groupSections(list, emptyList()) { BuiltinFilters.goalGroup(selKey, it) }
  val groupExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
  val active = uiState.goals.count { it.status == "Active" }
  val achieved = uiState.goals.count { it.status == "Achieved" }
  var showCreate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
  if (showCreate) com.example.ui.components.NameDialog("goal", { showCreate = false }) { viewModel.createNewGoal(it) }

  DetailScaffold(
    title = "Goals",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    fab = {
      ExtendedFloatingActionButton(
        onClick = { showCreate = true },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New goal", fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      )
    },
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        Spacer(Modifier.height(12.dp))
        StatCard(
          listOf(
            Stat(uiState.goals.size.toString(), "goals"),
            Stat(active.toString(), "active"),
            Stat(achieved.toString(), "achieved"),
          ),
          Modifier.padding(horizontal = TodayPad),
        )
        com.example.ui.components.FilterBar(viewModel, com.example.model.FilterScope.GOALS)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item {
          EmptyLine(
            "No goals in this view yet.",
            Modifier.padding(horizontal = TodayPad),
            actionLabel = "New goal",
            onAction = { showCreate = true },
          )
        }
      } else if (sections != null) {
        groupedRows(
          sections = sections,
          expanded = groupExpanded,
          rowKey = { it.id },
          headerPadding = Modifier.padding(horizontal = TodayPad),
          dividerPadding = Modifier.padding(horizontal = TodayPad),
        ) { goal ->
          EntityRow(
            title = goal.name,
            meta = goalMeta(goal),
            leadingIcon = Icons.Default.Flag,
            leadingIconTint = MaterialTheme.colorScheme.entityGoals,
            strikethrough = goal.status == "Achieved",
            onClick = { viewModel.openGoalDetail(goal.id) },
            trailing = { goalProgressTrailing(goal) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
      } else {
        itemsIndexed(list, key = { _, g -> g.id }) { index, goal ->
          EntityRow(
            title = goal.name,
            meta = goalMeta(goal),
            leadingIcon = Icons.Default.Flag,
            leadingIconTint = MaterialTheme.colorScheme.entityGoals,
            strikethrough = goal.status == "Achieved",
            onClick = { viewModel.openGoalDetail(goal.id) },
            trailing = { goalProgressTrailing(goal) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (index < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun goalMatchesFilter(g: GoalModel, f: GoalFilter) = when (f) {
  GoalFilter.ACTIVE -> !g.isArchived && (g.status == "Active" || g.status == "Dream")
  GoalFilter.ACHIEVED -> !g.isArchived && g.status == "Achieved"
  GoalFilter.DROPPED -> g.isArchived
}

// Progress is pulled out as its own trailing stat (see goalProgressTrailing)
// instead of living in this line — Streaks/Spark's whole design lesson is
// that the one number that matters should lead, not get buried in a joined
// meta string alongside project count and deadline.
private fun goalMeta(g: GoalModel): String {
  val parts = mutableListOf<String>()
  if (g.linkedProjects.isNotEmpty()) parts += "${g.linkedProjects.size} projects"
  if (g.deadline.isNotBlank() && g.deadline != "—") parts += g.deadline
  return parts.joinToString("  ·  ")
}

@Composable
private fun goalProgressTrailing(g: GoalModel) {
  if (g.aggregatedProgressText.isBlank() || g.aggregatedProgressText == "0%") return
  Text(
    g.aggregatedProgressText,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.entityGoals,
  )
}
