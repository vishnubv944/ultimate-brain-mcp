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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
    bottomBar = {
      if (task != null) {
        Button(
          onClick = { viewModel.toggleTaskCompletion(task.id) },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (task.isDone) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
            contentColor = if (task.isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier.fillMaxWidth().padding(horizontal = TodayPad, vertical = 12.dp).imePadding(),
        ) {
          Text(if (task.isDone) "Reopen task" else "Mark complete", fontWeight = FontWeight.SemiBold)
        }
      }
    },
  ) { innerPadding ->
    if (task == null) {
      EmptyLine("Task not found.", Modifier.padding(innerPadding).padding(TodayPad))
      return@DetailScaffold
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(8.dp))
      com.example.ui.components.DetailTitle(task.name, { viewModel.renameTask(task.id, it) })
      run {
        val meta = buildList {
          add(statusLabel(task.status))
          if (task.isOverdue && task.dueDisplay.isNotBlank()) add("Overdue · ${task.dueDisplay}")
          else if (task.dueDisplay.isNotBlank() && task.due != null) add(task.dueDisplay)
          task.projectName?.let { add(it) }
        }
        if (meta.isNotEmpty()) {
          Text(
            meta.joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(Modifier.height(10.dp))

      com.example.ui.components.PropertyChipRow {
        com.example.ui.components.SelectChip(
          "Status", statusLabel(task.status),
          listOf("To Do", "Doing", "Done"),
          { name -> name?.let { viewModel.updateTaskStatus(task.id, statusFromLabel(it)) } },
          allowClear = false,
        )
        com.example.ui.components.DateChip("Due", task.due) { viewModel.setTaskDueDate(task.id, it) }
        com.example.ui.components.SelectChip(
          "Project", task.projectName,
          uiState.projects.filter { !it.isArchived }.map { it.name },
          { name -> viewModel.setTaskProjectRelation(task.id, uiState.projects.firstOrNull { it.name == name }?.id) },
        )
        com.example.ui.components.SelectChip(
          "Priority", task.priority?.let { priorityLabel(it) },
          listOf("High", "Medium", "Low"),
          { name -> viewModel.updateTaskPriority(task.id, name?.let { priorityFromLabel(it) }) },
        )
        FilterChip(
          selected = task.isMyDay,
          onClick = { viewModel.toggleMyDay(task.id) },
          label = { Text("My Day") },
        )
      }

      // Time tracked — a quiet read.
      run {
        val logged = uiState.workSessions.filter { it.taskId == task.id }
        val mins = logged.sumOf { s -> s.durationMinutes ?: 0 }
        val summary = when {
          logged.isEmpty() -> "No sessions yet"
          mins >= 60 -> "${mins / 60}h ${mins % 60}m  ·  ${logged.size} session${if (logged.size == 1) "" else "s"}"
          else -> "${mins}m  ·  ${logged.size} session${if (logged.size == 1) "" else "s"}"
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openTaskWorkSessions(task.id) }
            .padding(vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f)) {
            Text("Time tracked", style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open work sessions",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      // Everything below is set-once-and-forget config — hidden until asked for.
      var moreOpen by remember(task.id) { mutableStateOf(false) }
      Row(
        modifier = Modifier.fillMaxWidth().clickable { moreOpen = !moreOpen }.padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "More details",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Icon(
          if (moreOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      androidx.compose.animation.AnimatedVisibility(visible = moreOpen) {
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
        }
      }

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

      SectionHeader("Description")
      OutlinedTextField(
        value = descDraft,
        onValueChange = { descDraft = it },
        placeholder = { Text("Add a description") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      )
      if (descDraft != task.description) {
        androidx.compose.material3.TextButton(onClick = { viewModel.setTaskDescription(task.id, descDraft) }) {
          Text("Save description")
        }
      }

      // Sub-tasks
      SectionHeader("Sub-tasks", task.subTasks.size)
      task.subTasks.forEach { st ->
        Row(
          modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSubTask(task.id, st.id) }.padding(vertical = 6.dp),
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
        onValueChange = { newSubtask = it },
        placeholder = { Text("Add a sub-task") },
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = {
          if (newSubtask.isNotBlank()) { viewModel.addSubTask(task.id, newSubtask.trim()); newSubtask = "" }
        }),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      )

      // Notes — the real Notion page body.
      SectionHeader("Notes")
      when {
        uiState.detailBodyLoading -> Text("Loading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        !uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == task.id -> MarkdownBody(uiState.detailBody!!)
        else -> EmptyLine("No notes on this task.")
      }
      Spacer(Modifier.height(120.dp))
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
