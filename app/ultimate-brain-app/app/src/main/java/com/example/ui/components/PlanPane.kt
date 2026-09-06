package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.Task
import com.example.viewmodel.PlanFilter

private val PLAN_FILTERS = listOf(
  PlanFilter.TODAY to "Today",
  PlanFilter.WEEK to "This week",
  PlanFilter.OVERDUE to "Overdue",
  PlanFilter.ACTIVE_PROJECTS to "Active projects",
  PlanFilter.INBOX to "Inbox",
  PlanFilter.RECURRING to "Recurring",
)

@Composable
fun PlanPane(
  onToday: List<Task>,
  browse: List<Task>,
  selectedFilter: PlanFilter,
  filterCount: (PlanFilter) -> Int,
  onSelectFilter: (PlanFilter) -> Unit,
  onToggleToday: (String) -> Unit,
  onToggleComplete: (String) -> Unit,
  onOpenTask: (String) -> Unit,
  onAddTask: () -> Unit,
  onStartFocusing: () -> Unit,
) {
  val onTodayIds = remember(onToday) { onToday.map { it.id }.toSet() }
  val browseVisible = rememberVisibleCount(selectedFilter)

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
  ) {
    Text(
      text = "Pick what you'll focus on today.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 12.dp),
    )

    SectionHeader("On today", onToday.size)
    if (onToday.isEmpty()) {
      EmptyLine("Nothing yet. Add tasks from the list below to build today's shortlist.")
    } else {
      onToday.forEachIndexed { i, task ->
        TaskRow(
          task = task,
          onToggleComplete = { onToggleComplete(task.id) },
          onClick = { onOpenTask(task.id) },
          trailing = {
            IconButton(onClick = { onToggleToday(task.id) }) {
              Icon(Icons.Default.Close, contentDescription = "Remove from today", modifier = Modifier.size(18.dp))
            }
          },
        )
        if (i < onToday.lastIndex) ThinDivider()
      }
      Spacer(Modifier.height(12.dp))
      OutlinedButton(onClick = onStartFocusing, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  Start focusing")
      }
    }

    SectionHeader("Browse")
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PLAN_FILTERS.forEach { (filter, label) ->
        val n = filterCount(filter)
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { onSelectFilter(filter) },
          label = { Text(if (n > 0) "$label  $n" else label) },
        )
      }
    }

    if (browse.isEmpty()) {
      EmptyLine("No tasks match this filter.")
    } else {
      val shown = browse.page(browseVisible.intValue)
      shown.forEachIndexed { i, task ->
        val alreadyToday = task.id in onTodayIds
        TaskRow(
          task = task,
          onToggleComplete = { onToggleComplete(task.id) },
          onClick = { onOpenTask(task.id) },
          trailing = {
            IconButton(onClick = { onToggleToday(task.id) }) {
              Icon(
                imageVector = if (alreadyToday) Icons.Default.Check else Icons.Default.WbSunny,
                contentDescription = if (alreadyToday) "On today" else "Add to today",
                tint = if (alreadyToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          },
        )
        if (i < shown.lastIndex) ThinDivider()
      }
      ShowMoreRow(browse.size - browseVisible.intValue, browseVisible)
    }

    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onAddTask) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Text("  Add a task")
    }
    Spacer(Modifier.height(96.dp))
  }
}
