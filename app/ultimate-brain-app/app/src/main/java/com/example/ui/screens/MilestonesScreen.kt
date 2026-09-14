package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.MilestoneModel
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.viewmodel.MilestoneFilter
import com.example.viewmodel.MyDayViewModel

private val MS_FILTERS = listOf(
  MilestoneFilter.ALL to "All",
  MilestoneFilter.IN_PROGRESS to "In progress",
  MilestoneFilter.COMPLETED to "Completed",
  MilestoneFilter.PENDING to "Pending",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val list = uiState.milestonesMatching(uiState.selectedChipKey(com.example.model.FilterScope.MILESTONES))

  DetailScaffold(title = "Milestones", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    LazyColumn(modifier = Modifier.padding(pad)) {
      item {
        com.example.ui.components.FilterBar(viewModel, com.example.model.FilterScope.MILESTONES)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item { EmptyLine("No milestones here.", Modifier.padding(horizontal = TodayPad)) }
      } else {
        itemsIndexed(list, key = { _, m -> m.id }) { i, m ->
          EntityRow(
            title = m.name,
            meta = listOfNotNull(m.goalName.ifBlank { null }, m.targetDateText.ifBlank { null }).joinToString("  ·  "),
            leadingMilestone = m.status == "Completed",
            onLeadingClick = { viewModel.toggleMilestoneStatus(m.id) },
            strikethrough = m.status == "Completed",
            onClick = { viewModel.openMilestoneDetail(m.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (i < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun msMatches(m: MilestoneModel, f: MilestoneFilter) = when (f) {
  MilestoneFilter.ALL -> true
  MilestoneFilter.IN_PROGRESS -> m.status == "In Progress"
  MilestoneFilter.COMPLETED -> m.status == "Completed"
  MilestoneFilter.PENDING -> m.status == "Pending"
}
