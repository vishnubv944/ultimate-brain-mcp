package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.ui.components.DateFieldRow
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.OptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val ms = uiState.selectedMilestone

  DetailScaffold(
    title = "Milestone",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      if (ms != null) {
        IconButton(onClick = { viewModel.toggleMilestoneStatus(ms.id) }) {
          Icon(
            Icons.Default.Check,
            contentDescription = "Toggle complete",
            tint = if (ms.status == "Completed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
  ) { pad ->
    if (ms == null) {
      EmptyLine("Milestone not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold
    }
    Column(
      Modifier
        .fillMaxSize()
        .padding(pad)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(8.dp))
      Text(ms.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(
        ms.status,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SectionHeader("Details")
      OptionRow(
        "Goal", ms.goalName.ifBlank { null },
        uiState.goals.map { it.name },
        { name -> viewModel.setMilestoneGoal(ms.id, uiState.goals.firstOrNull { it.name == name }?.id) },
      )
      DateFieldRow("Target date", ms.targetDateIso, { viewModel.setMilestoneDate(ms.id, it) })

      if (ms.goalId.isNotBlank()) {
        androidx.compose.material3.TextButton(onClick = { viewModel.openGoalDetail(ms.goalId) }) {
          Text("Open ${ms.goalName}  →")
        }
      }
      Spacer(Modifier.height(96.dp))
    }
  }
}
