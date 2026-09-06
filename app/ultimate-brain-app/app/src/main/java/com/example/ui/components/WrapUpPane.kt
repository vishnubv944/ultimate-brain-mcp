package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WrapUpPane(
  doneCount: Int,
  focusedSeconds: Long,
  openTasks: List<Task>,
  onToggleComplete: (String) -> Unit,
  onMoveTaskTomorrow: (String) -> Unit,
  onMoveAllTomorrow: () -> Unit,
  onOpenTask: (String) -> Unit,
  onWriteJournal: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
  ) {
    Text(
      text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(12.dp))

    StatCard(
      listOf(
        Stat(doneCount.toString(), "done"),
        Stat(formatFocus(focusedSeconds), "focused"),
        Stat(openTasks.size.toString(), "still open"),
      )
    )

    SectionHeader("Still open", openTasks.size)
    if (openTasks.isEmpty()) {
      EmptyLine("All clear. Nice work.")
    } else {
      openTasks.forEachIndexed { i, task ->
        TaskRow(
          task = task,
          onToggleComplete = { onToggleComplete(task.id) },
          onClick = { onOpenTask(task.id) },
          trailing = {
            IconButton(onClick = { onMoveTaskTomorrow(task.id) }) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Move to tomorrow",
                modifier = Modifier.height(18.dp),
              )
            }
          },
        )
        if (i < openTasks.lastIndex) ThinDivider()
      }
      Spacer(Modifier.height(12.dp))
      OutlinedButton(onClick = onMoveAllTomorrow, modifier = Modifier.fillMaxWidth()) {
        Text("Move all to tomorrow")
      }
    }

    Spacer(Modifier.height(28.dp))
    OutlinedButton(onClick = onWriteJournal, modifier = Modifier.fillMaxWidth()) {
      Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.height(18.dp))
      Text("  Write journal entry")
    }
    Spacer(Modifier.height(96.dp))
  }
}

private fun formatFocus(seconds: Long): String {
  if (seconds <= 0) return "0m"
  val h = seconds / 3600
  val m = (seconds % 3600) / 60
  return if (h > 0) "${h}h ${m}m" else "${m}m"
}
