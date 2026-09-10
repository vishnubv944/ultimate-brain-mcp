package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.Priority
import com.example.model.TaskStatus
import com.example.ui.components.DateFieldRow
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.MarkdownBody
import com.example.ui.components.OptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
  viewModel: MyDayViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val task = uiState.selectedTask
  var newSubtask by remember { mutableStateOf("") }
  var statusMenu by remember { mutableStateOf(false) }
  var priorityMenu by remember { mutableStateOf(false) }
  var descDraft by remember(task?.id, task?.description) { mutableStateOf(task?.description ?: "") }

  DetailScaffold(
    title = "Task",
    onBack = onNavigateBack,
    modifier = modifier,
  ) { innerPadding ->
    if (task == null) {
      EmptyLine("Task not found.", Modifier.padding(innerPadding).padding(TodayPad))
      return@DetailScaffold
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
     Column(
      modifier = Modifier
        .widthIn(max = com.example.ui.components.DETAIL_MAX_WIDTH)
        .fillMaxWidth()
        .padding(horizontal = TodayPad),
     ) {
      com.example.ui.components.EntityHubHeader(
        title = task.name,
        onRename = { viewModel.renameTask(task.id, it) },
        leading = {
          Icon(
            imageVector = if (task.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (task.isDone) "Mark not done" else "Mark done",
            tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .clickable { viewModel.toggleTaskCompletion(task.id) },
          )
        },
        viewDetails = { TaskViewDetails(task, uiState, viewModel) },
        propertyStrip = {
          com.example.ui.components.PropertyGrid {
            com.example.ui.components.SelectCell(
              Icons.Outlined.CheckCircle, "Status", statusLabel(task.status),
              listOf("To Do", "Doing", "Done"),
              { name -> name?.let { viewModel.updateTaskStatus(task.id, statusFromLabel(it)) } },
              allowClear = false,
            )
            com.example.ui.components.SelectCell(
              Icons.Default.Folder, "Project", task.projectName,
              uiState.projects.filter { !it.isArchived }.map { it.name },
              { name -> viewModel.setTaskProjectRelation(task.id, uiState.projects.firstOrNull { it.name == name }?.id) },
            )
            com.example.ui.components.DateCell(
              Icons.Default.Event, "Due", task.due, { viewModel.setTaskDueDate(task.id, it) },
              overdue = task.isOverdue,
            )
            com.example.ui.components.SelectCell(
              Icons.Outlined.Flag, "Priority", task.priority?.let { priorityLabel(it) },
              listOf("High", "Medium", "Low"),
              { name -> viewModel.updateTaskPriority(task.id, name?.let { priorityFromLabel(it) }) },
            )
            com.example.ui.components.ToggleCell(
              Icons.Default.WbSunny, "My Day", task.isMyDay, { viewModel.toggleMyDay(task.id) },
            )
          }
        },
      )

      val hasHistory = task.isRecurring || task.occurrenceIds.isNotEmpty()
      val tabs = buildList {
        add(com.example.ui.components.HubTab("Content"))
        add(com.example.ui.components.HubTab("Sub-Tasks"))
        if (hasHistory) add(com.example.ui.components.HubTab("History"))
        add(com.example.ui.components.HubTab("Time"))
      }
      var tab by remember(task.id) { mutableStateOf(0) }
      val tabName = tabs.getOrNull(tab)?.label ?: "Content"
      androidx.compose.runtime.LaunchedEffect(task.id, tabName) {
        if (tabName == "History") viewModel.loadTaskOccurrences(task.id)
      }
      com.example.ui.components.DetailTabs(tabs, tab, { tab = it })

      when (tabName) {
        "Content" -> TaskContentTab(task, uiState, viewModel, descDraft, { descDraft = it })
        "Sub-Tasks" -> TaskSubtasksTab(task, viewModel, newSubtask, { newSubtask = it })
        "History" -> TaskHistoryTab(task, uiState, viewModel)
        "Time" -> TaskTimeTab(task, uiState, viewModel)
      }
      Spacer(Modifier.height(120.dp))
     }
    }
  }
}

@Composable
private fun TaskViewDetails(
  task: com.example.model.Task,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
) {
        Column {
          OptionRow("Energy", task.energy, uiState.optionsFor("task.Energy", listOf("High", "Low")), { viewModel.setTaskEnergy(task.id, it) })
          OptionRow("Location", task.location, uiState.optionsFor("task.Location", listOf("Home", "Office", "Errand")), { viewModel.setTaskLocation(task.id, it) })
          OptionRow("Smart list", task.smartList, uiState.optionsFor("task.Smart List", listOf("Do Next", "Delegated", "Someday")), { viewModel.setTaskSmartList(task.id, it) })
          OptionRow(
            "Repeats", task.recurUnit,
            uiState.optionsFor("task.Recur Unit", listOf("Day(s)", "Week(s)", "Month(s)", "Year(s)")),
            { viewModel.setTaskRecurrence(task.id, it, task.recurInterval.coerceAtLeast(1)) },
          )
          if (task.recurUnit != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Every", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              androidx.compose.material3.TextButton(onClick = { viewModel.setTaskRecurrence(task.id, task.recurUnit, (task.recurInterval - 1).coerceAtLeast(1)) }) { Text("−") }
              Text("${task.recurInterval}", style = MaterialTheme.typography.bodyLarge)
              androidx.compose.material3.TextButton(onClick = { viewModel.setTaskRecurrence(task.id, task.recurUnit, task.recurInterval + 1) }) { Text("+") }
              Text(task.recurUnit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val dayOpts = uiState.optionsFor(
              "task.Days",
              listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
            )
            Text("On days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              dayOpts.forEach { d ->
                FilterChip(
                  selected = d in task.recurDays,
                  onClick = { viewModel.toggleTaskRecurDay(task.id, d) },
                  label = { Text(d.take(3)) },
                )
              }
            }
          }

          DateFieldRow("Snooze until", task.snoozeIso, { viewModel.setTaskSnooze(task.id, it) })
          DateFieldRow("Wait date", task.waitIso, { viewModel.setTaskWaitDate(task.id, it) })
          OptionRow("Focus type", task.processImmersive, uiState.optionsFor("task.P/I", listOf("Process", "Immersive")), { viewModel.setTaskProcessImmersive(task.id, it) })

          run {
            val labelOpts = uiState.optionsFor("task.Labels", task.labels)
            if (labelOpts.isNotEmpty()) {
              Text("Labels", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
              Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labelOpts.forEach { l ->
                  FilterChip(
                    selected = l in task.labels,
                    onClick = {
                      val next = if (l in task.labels) task.labels - l else task.labels + l
                      viewModel.setTaskLabelSet(task.id, next)
                    },
                    label = { Text(l) },
                  )
                }
              }
            }
          }

          if (uiState.workspaceUsers.isNotEmpty()) {
            Text("Assignee", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              uiState.workspaceUsers.forEach { (uid, uname) ->
                FilterChip(
                  selected = uid in task.assigneeIds,
                  onClick = { viewModel.toggleTaskAssignee(task.id, uid, uname) },
                  label = { Text(uname) },
                )
              }
            }
          }
          if (uiState.people.isNotEmpty()) {
            Text("People", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              uiState.people.forEach { person ->
                FilterChip(
                  selected = person.id in task.personIds,
                  onClick = { viewModel.toggleTaskPerson(task.id, person.id) },
                  label = { Text(person.name) },
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = task.enforceSchedule, onCheckedChange = { viewModel.setTaskEnforceSchedule(task.id, it) })
            Text("Enforce schedule", style = MaterialTheme.typography.bodyMedium)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = task.shoppingList, onCheckedChange = { viewModel.setTaskShoppingList(task.id, it) })
            Text("Shopping list", style = MaterialTheme.typography.bodyMedium)
          }
          if (task.taxonomyArea != null) DetailField("Area", task.taxonomyArea)

          if (task.noteIds.isNotEmpty()) {
            SectionHeader("Linked notes", task.noteIds.size)
            task.noteIds.forEach { nid ->
              val n = uiState.notes.firstOrNull { it.id == nid }
              androidx.compose.material3.TextButton(onClick = { viewModel.openNoteDetail(nid) }) {
                Text((n?.title ?: "Open note") + "  →")
              }
            }
          }
          if (task.projectName != null) {
            androidx.compose.material3.TextButton(onClick = { task.projectId?.let { viewModel.openProjectDetail(it) } }) {
              Text("Open ${task.projectName}  →")
            }
          }
        }
}

@Composable
private fun TaskContentTab(
  task: com.example.model.Task,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
  descDraft: String,
  onDescChange: (String) -> Unit,
) {
  Column {
    // A borderless, always-there description field — reads as page body, not a form.
    androidx.compose.material3.TextField(
      value = descDraft,
      onValueChange = onDescChange,
      placeholder = { Text("Write a description…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
      textStyle = MaterialTheme.typography.bodyMedium,
      colors = androidx.compose.material3.TextFieldDefaults.colors(
        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
      ),
      modifier = Modifier.fillMaxWidth(),
    )
    if (descDraft.trim() != task.description.trim()) {
      androidx.compose.material3.TextButton(onClick = { viewModel.setTaskDescription(task.id, descDraft.trim()) }) {
        Text("Save description")
      }
    }

    when {
      uiState.detailBodyLoading -> {
        Spacer(Modifier.height(12.dp))
        Text("Loading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      !uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == task.id -> {
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        MarkdownBody(uiState.detailBody!!)
      }
    }
  }
}

@Composable
private fun TaskSubtasksTab(
  task: com.example.model.Task,
  viewModel: MyDayViewModel,
  newSubtask: String,
  onNewSubtaskChange: (String) -> Unit,
) {
  Column {
    if (task.subTasks.isNotEmpty()) {
      SectionHeader("Subtasks " + task.subTasks.count { it.isCompleted } + "/" + task.subTasks.size)
    }
    task.subTasks.forEach { st ->
      Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
          .clickable { viewModel.toggleSubTask(task.id, st.id) }.padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          if (st.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
          contentDescription = null,
          tint = if (st.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
          st.name,
          style = MaterialTheme.typography.bodyMedium,
          color = if (st.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    OutlinedTextField(
      value = newSubtask,
      onValueChange = onNewSubtaskChange,
      placeholder = { Text("New subtask") },
      singleLine = true,
      keyboardActions = KeyboardActions(onDone = {
        if (newSubtask.isNotBlank()) { viewModel.addSubTask(task.id, newSubtask.trim()); onNewSubtaskChange("") }
      }),
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
  }
}

@Composable
private fun TaskHistoryTab(
  task: com.example.model.Task,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
) {
  val occ = if (uiState.taskOccurrencesForId == task.id) uiState.taskOccurrences else emptyList()
  Column {
    Text(
      "Past occurrences of this recurring task.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(vertical = 8.dp),
    )
    when {
      uiState.taskOccurrencesLoading && occ.isEmpty() ->
        Text("Loading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      occ.isEmpty() ->
        EmptyLine("No occurrences logged yet.")
      else -> occ.forEach { o ->
        Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { viewModel.openTaskDetail(o.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            if (o.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (o.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.size(10.dp))
          Column(Modifier.weight(1f)) {
            Text(o.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            val meta = listOfNotNull(
              o.completionDate?.substringBefore('T')?.let { com.example.data.DateUtils.displayLabel(it) }?.ifBlank { null }
                ?: o.due?.let { com.example.data.DateUtils.displayLabel(it) }?.ifBlank { null },
              statusLabel(o.status),
            ).joinToString("  ·  ")
            if (meta.isNotBlank()) {
              Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TaskTimeTab(
  task: com.example.model.Task,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
) {
  val logged = uiState.workSessions.filter { it.taskId == task.id }
  val mins = logged.sumOf { s -> s.durationMinutes ?: 0 }
  Column {
    val summary = when {
      logged.isEmpty() -> "No sessions yet"
      mins >= 60 -> "${mins / 60}h ${mins % 60}m  ·  ${logged.size} session${if (logged.size == 1) "" else "s"}"
      else -> "${mins}m  ·  ${logged.size} session${if (logged.size == 1) "" else "s"}"
    }
    Text(summary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
    androidx.compose.material3.TextButton(onClick = { viewModel.openTaskWorkSessions(task.id) }) {
      Text("Open full history & charts  →")
    }
    logged.take(10).forEach { s ->
      Text(
        listOfNotNull(s.timeRange.ifBlank { null }, s.duration.ifBlank { null }).joinToString("  ·  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun DetailField(label: String, value: String) {
  SectionHeader(label)
  Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 4.dp))
}

private fun statusLabel(s: TaskStatus) = when (s) {
  TaskStatus.TODO -> "To Do"
  TaskStatus.DOING -> "Doing"
  TaskStatus.DONE -> "Done"
}

private fun statusFromLabel(l: String) = when (l) {
  "Doing" -> TaskStatus.DOING
  "Done" -> TaskStatus.DONE
  else -> TaskStatus.TODO
}

private fun priorityLabel(p: Priority) = when (p) {
  Priority.HIGH -> "High"
  Priority.MEDIUM -> "Medium"
  Priority.LOW -> "Low"
}

private fun priorityFromLabel(l: String) = when (l) {
  "High" -> Priority.HIGH
  "Low" -> Priority.LOW
  else -> Priority.MEDIUM
}
