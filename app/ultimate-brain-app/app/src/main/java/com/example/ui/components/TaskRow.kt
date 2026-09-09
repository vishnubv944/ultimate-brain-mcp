package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.model.Priority
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent

/**
 * The one task row used across the Today panes: a plain row on the page
 * background (no per-item card) — leading complete-toggle, title, and a single
 * muted metadata line. Optional [trailing] slot for a per-context action.
 */
@Composable
fun TaskRow(
  task: Task,
  onToggleComplete: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showProject: Boolean = true,
  trailing: @Composable (() -> Unit)? = null,
) {
  val overdue = !task.isDone && DateUtils.bucket(task.due) == DateUtils.DueBucket.OVERDUE
  val due = DateUtils.displayLabel(task.due)
  val metaSegments = buildList {
    if (showProject && task.projectName != null) add(task.projectName)
    when {
      overdue -> add(if (due.isNotBlank()) "Overdue · $due" else "Overdue")
      due.isNotBlank() && due != "Today" -> add(due)
      task.isRecurring -> add("Recurring")
    }
    if (task.status == TaskStatus.DOING && !task.isDone) add("In progress")
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .heightIn(min = 56.dp)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onToggleComplete),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (task.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = if (task.isDone) "Mark not done" else "Mark done",
        tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(Modifier.width(4.dp))

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (task.priority == Priority.HIGH && !task.isDone) {
          Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorAccent))
        }
        Text(
          text = task.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (metaSegments.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (showProject && task.projectName != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.entityProjects))
          }
          Text(
            text = metaSegments.joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = if (overdue) MaterialTheme.colorScheme.errorAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    if (trailing != null) {
      Spacer(Modifier.width(8.dp))
      trailing()
    }
  }
}
