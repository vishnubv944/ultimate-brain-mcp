package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.ThinDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focus.formatFocusElapsed
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSessionsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val session by viewModel.focusSession.collectAsState()
  val secs = uiState.focusedSecondsToday
  val h = secs / 3600
  val m = (secs % 3600) / 60

  DetailScaffold(title = "Work sessions", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    Column(modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = TodayPad).verticalScroll(rememberScrollState())) {
      Spacer(Modifier.height(12.dp))
      StatCard(
        listOf(
          Stat(if (h > 0) "${h}h ${m}m" else "${m}m", "focused today"),
          Stat(if (session != null) "1" else "0", "running"),
          Stat("4h", "daily target"),
        )
      )

      SectionHeader("Now")
      if (session != null) {
        Text(session!!.taskName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
          formatFocusElapsed(session!!.elapsedMs()) + (session!!.projectName?.let { "  ·  $it" } ?: ""),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        EmptyLine("No session running. Start one from a task in Execute.")
      }

      SectionHeader("History", uiState.workSessions.size)
      if (uiState.workSessions.isEmpty()) {
        EmptyLine("No logged sessions yet. Completed focus blocks are written here when you stop the timer.")
      } else {
        uiState.workSessions.forEach { s ->
          Column(Modifier.padding(vertical = 8.dp)) {
            Text(
              s.taskName.ifBlank { "Session" },
              style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
            )
            val meta = buildList {
              s.startIso?.let { add(com.example.data.DateUtils.displayLabel(it.substringBefore('T'))) }
              s.durationMinutes?.let { add(if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m") }
            }.joinToString("  ·  ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          ThinDivider()
        }
      }
      Spacer(Modifier.height(96.dp))
    }
  }
}
