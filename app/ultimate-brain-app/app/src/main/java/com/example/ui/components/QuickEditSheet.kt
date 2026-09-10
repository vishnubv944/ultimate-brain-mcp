package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Priority
import com.example.model.TaskStatus
import com.example.viewmodel.MyDayViewModel

/**
 * Long-press quick-edit for a task or a note — a bottom sheet of property chips
 * (Todoist / TickTick style), so common edits don't need the full detail page.
 * Reads the target id from [MyDayUiState.quickEditTaskId] / quickEditNoteId.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditSheetHost(viewModel: MyDayViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val taskId = uiState.quickEditTaskId
  val noteId = uiState.quickEditNoteId
  if (taskId == null && noteId == null) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = { viewModel.closeQuickEdit() }, sheetState = sheetState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
      when {
        taskId != null -> TaskQuickEdit(viewModel, taskId)
        noteId != null -> NoteQuickEdit(viewModel, noteId)
      }
    }
  }
}

@Composable
private fun TaskQuickEdit(viewModel: MyDayViewModel, taskId: String) {
  val uiState by viewModel.uiState.collectAsState()
  val task = uiState.tasks.firstOrNull { it.id == taskId } ?: run {
    Text("Task not found."); return
  }
  Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3)
  Spacer(Modifier.height(12.dp))
  PropertyChipRow {
    SelectChip(
      Icons.Outlined.CheckCircle, "Status", statusLabel(task.status),
      listOf("To Do", "Doing", "Done"),
      { it?.let { l -> viewModel.updateTaskStatus(taskId, statusFromLabel(l)) } },
      allowClear = false,
    )
    DateChip(Icons.Default.Event, "Due", task.due, { viewModel.setTaskDueDate(taskId, it) }, overdue = task.isOverdue)
    SelectChip(
      Icons.Default.Folder, "Project", task.projectName,
      uiState.projects.filter { !it.isArchived }.map { it.name },
      { name -> viewModel.setTaskProjectRelation(taskId, uiState.projects.firstOrNull { it.name == name }?.id) },
    )
    SelectChip(
      Icons.Outlined.Flag, "Priority", task.priority?.let { priorityLabel(it) },
      listOf("High", "Medium", "Low"),
      { l -> viewModel.updateTaskPriority(taskId, l?.let { priorityFromLabel(it) }) },
    )
  }
  Spacer(Modifier.height(8.dp))
  FilterChip(
    selected = task.isMyDay,
    onClick = { viewModel.toggleMyDay(taskId) },
    label = { Text("My Day") },
    leadingIcon = { Icon(Icons.Default.WbSunny, null, Modifier.size(16.dp)) },
  )
  Spacer(Modifier.height(4.dp))
  androidx.compose.foundation.layout.Row {
    TextButton(onClick = {
      viewModel.toggleTaskCompletion(taskId); viewModel.closeQuickEdit()
    }) { Text(if (task.isDone) "Reopen" else "Complete") }
    TextButton(onClick = {
      viewModel.closeQuickEdit(); viewModel.openTaskDetail(taskId)
    }) {
      Icon(Icons.Default.OpenInFull, null, Modifier.size(16.dp))
      Spacer(Modifier.size(6.dp))
      Text("Open details")
    }
  }
}

@Composable
private fun NoteQuickEdit(viewModel: MyDayViewModel, noteId: String) {
  val uiState by viewModel.uiState.collectAsState()
  val note = uiState.notes.firstOrNull { it.id == noteId } ?: run {
    Text("Note not found."); return
  }
  Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3)
  Spacer(Modifier.height(12.dp))
  PropertyChipRow {
    SelectChip(
      Icons.Default.Description, "Type", note.type,
      uiState.optionsFor("note.Type", listOf("Journal", "Meeting", "Web Clip", "Voice Note", "Reference", "Book", "Idea", "Daily")),
      { it?.let { t -> viewModel.setNoteType(noteId, t) } },
      allowClear = false,
    )
    DateChip(Icons.Default.Event, "Date", note.dateIso, { viewModel.setNoteDate(noteId, it) })
    SelectChip(
      Icons.Default.Folder, "Project", note.projectName,
      uiState.projects.filter { !it.isArchived }.map { it.name },
      { name -> viewModel.setNoteProjectRelation(noteId, uiState.projects.firstOrNull { it.name == name }?.id) },
    )
  }
  Spacer(Modifier.height(8.dp))
  FilterChip(
    selected = note.isFavorite,
    onClick = { viewModel.toggleNoteFavorite(noteId) },
    label = { Text("Favorite") },
    leadingIcon = {
      Icon(if (note.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, Modifier.size(16.dp))
    },
  )
  Spacer(Modifier.height(4.dp))
  TextButton(onClick = { viewModel.closeQuickEdit(); viewModel.openNoteDetail(noteId) }) {
    Icon(Icons.Default.OpenInFull, null, Modifier.size(16.dp))
    Spacer(Modifier.size(6.dp))
    Text("Open details")
  }
}

private fun statusLabel(s: TaskStatus) = when (s) {
  TaskStatus.TODO -> "To Do"; TaskStatus.DOING -> "Doing"; TaskStatus.DONE -> "Done"
}

private fun statusFromLabel(l: String) = when (l) {
  "Doing" -> TaskStatus.DOING; "Done" -> TaskStatus.DONE; else -> TaskStatus.TODO
}

private fun priorityLabel(p: Priority) = when (p) {
  Priority.HIGH -> "High"; Priority.MEDIUM -> "Medium"; Priority.LOW -> "Low"
}

private fun priorityFromLabel(l: String) = when (l) {
  "High" -> Priority.HIGH; "Low" -> Priority.LOW; else -> Priority.MEDIUM
}
