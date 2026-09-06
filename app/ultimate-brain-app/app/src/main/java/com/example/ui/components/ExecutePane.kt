package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.FocusSession
import com.example.focus.formatFocusElapsed
import com.example.model.Task
import kotlinx.coroutines.delay

@Composable
fun ExecutePane(
  focusSession: FocusSession?,
  upNext: List<Task>,
  doneToday: List<Task>,
  onStartFocus: (Task) -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onStopFocus: () -> Unit,
  onToggleComplete: (String) -> Unit,
  onOpenTask: (String) -> Unit,
  onGoToPlan: () -> Unit,
) {
  var doneExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
  ) {
    Spacer(Modifier.height(16.dp))

    if (focusSession != null) {
      FocusCard(
        session = focusSession,
        onPause = onPauseFocus,
        onResume = onResumeFocus,
        onStop = onStopFocus,
        onOpenTask = { onOpenTask(focusSession.taskId) },
      )
    }

    SectionHeader("Up next", upNext.size)
    if (upNext.isEmpty()) {
      EmptyLine("Nothing on today yet.")
      Text(
        text = "Plan your day →",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onGoToPlan).padding(vertical = 8.dp),
      )
    } else {
      upNext.forEachIndexed { i, task ->
        val isActive = focusSession?.taskId == task.id
        TaskRow(
          task = task,
          onToggleComplete = { onToggleComplete(task.id) },
          onClick = { onOpenTask(task.id) },
          trailing = if (isActive) null else {
            {
              IconButton(onClick = { onStartFocus(task) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start focus", tint = MaterialTheme.colorScheme.primary)
              }
            }
          },
        )
        if (i < upNext.lastIndex) ThinDivider()
      }
    }

    if (doneToday.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp).clickable { doneExpanded = !doneExpanded },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Done today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        Text("${doneToday.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Icon(
          imageVector = if (doneExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = doneExpanded) {
        Column {
          doneToday.forEachIndexed { i, task ->
            TaskRow(
              task = task,
              onToggleComplete = { onToggleComplete(task.id) },
              onClick = { onOpenTask(task.id) },
            )
            if (i < doneToday.lastIndex) ThinDivider()
          }
        }
      }
    }

    Spacer(Modifier.height(96.dp))
  }
}

@Composable
private fun FocusCard(
  session: FocusSession,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStop: () -> Unit,
  onOpenTask: () -> Unit,
) {
  val elapsed by produceState(initialValue = session.elapsedMs(), session) {
    while (true) {
      value = session.elapsedMs()
      delay(1000)
    }
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = session.taskName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 2,
        modifier = Modifier.clickable(onClick = onOpenTask),
      )
      if (session.projectName != null) {
        Text(
          text = session.projectName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = formatFocusElapsed(elapsed),
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      if (session.isPaused) {
        Text("Paused", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
      }
      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = if (session.isPaused) onResume else onPause) {
          Icon(
            imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Text(if (session.isPaused) "  Resume" else "  Pause")
        }
        Button(
          onClick = onStop,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("  Stop")
        }
      }
    }
  }
}
