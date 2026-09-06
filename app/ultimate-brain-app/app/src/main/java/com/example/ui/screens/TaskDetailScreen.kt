package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.MarkdownBody
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
      Text(task.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
          FilterChip(
            selected = true,
            onClick = { statusMenu = true },
            label = { Text(statusLabel(task.status)) },
          )
          DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
            listOf(TaskStatus.TODO, TaskStatus.DOING, TaskStatus.DONE).forEach { s ->
              DropdownMenuItem(text = { Text(statusLabel(s)) }, onClick = {
                statusMenu = false; viewModel.updateTaskStatus(task.id, s)
              })
            }
          }
        }
        Box {
          FilterChip(
            selected = task.priority != null,
            onClick = { priorityMenu = true },
            label = { Text(task.priority?.let { priorityLabel(it) } ?: "Priority") },
          )
          DropdownMenu(expanded = priorityMenu, onDismissRequest = { priorityMenu = false }) {
            (listOf<Priority?>(Priority.HIGH, Priority.MEDIUM, Priority.LOW, null)).forEach { p ->
              DropdownMenuItem(text = { Text(p?.let { priorityLabel(it) } ?: "None") }, onClick = {
                priorityMenu = false; viewModel.updateTaskPriority(task.id, p)
              })
            }
          }
        }
        FilterChip(
          selected = task.isMyDay,
          onClick = { viewModel.toggleMyDay(task.id) },
          label = { Text("My Day") },
        )
      }

      // Schedule / project / labels / repeat — read-only for now.
      if (task.dueDisplay.isNotBlank() || task.timeBlock != null) {
        DetailField("When", listOfNotNull(task.dueDisplay.ifBlank { null }, task.timeBlock).joinToString(" · "))
      }
      if (task.projectName != null) {
        SectionHeader("Project")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { task.projectId?.let { viewModel.openProjectDetail(it) } }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.entityProjects, CircleShape))
          Spacer(Modifier.size(8.dp))
          Text(task.projectName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          Spacer(Modifier.weight(1f))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
      }
      if (task.labels.isNotEmpty()) {
        SectionHeader("Labels")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          task.labels.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
      }
      if (task.isRecurring) {
        DetailField("Repeats", task.recurrenceText ?: "Recurring")
      }
      if (task.taxonomyArea != null) {
        DetailField("Area", task.taxonomyArea)
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

private fun priorityLabel(p: Priority) = when (p) {
  Priority.HIGH -> "High"
  Priority.MEDIUM -> "Medium"
  Priority.LOW -> "Low"
}
