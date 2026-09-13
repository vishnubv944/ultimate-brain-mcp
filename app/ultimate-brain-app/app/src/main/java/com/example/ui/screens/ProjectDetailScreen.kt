package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.MarkdownBody
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.TaskRow
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val project = uiState.selectedProject

  DetailScaffold(
    title = "Project",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      if (project != null && !project.isArchived) {
        IconButton(onClick = { viewModel.archiveProject(project.id) }) {
          Icon(Icons.Default.Archive, contentDescription = "Archive project")
        }
      }
    },
  ) { innerPadding ->
    if (project == null) {
      EmptyLine("Project not found.", Modifier.padding(innerPadding).padding(TodayPad))
      return@DetailScaffold
    }
    val tasks = uiState.tasks.filter { it.projectId == project.id }
    val open = tasks.filter { !it.isDone }.sortedByDescending { it.status == TaskStatus.DOING }
    val done = tasks.filter { it.isDone }
    val notes = uiState.notes.filter { it.projectName == project.name }

    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        Spacer(Modifier.height(4.dp))
        com.example.ui.components.EntityHubHeader(
          title = project.name,
          onRename = { viewModel.renameProject(project.id, it) },
          modifier = Modifier.padding(horizontal = TodayPad),
          leading = {
            Icon(Icons.Default.Rocket, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.entityProjects)
          },
          viewDetails = { ProjectViewDetails(project, uiState, viewModel) },
          propertyStrip = {
            androidx.compose.foundation.layout.Column {
              com.example.ui.components.OptionRow(
                "Status", project.status,
                uiState.optionsFor("project.Status", listOf("Planned", "On Hold", "Doing", "Ongoing", "Done")),
                { it?.let { s -> viewModel.setProjectStatus(project.id, s) } },
                allowClear = false, icon = Icons.Outlined.CheckCircle,
              )
              com.example.ui.components.DateFieldRow(
                "Deadline", project.deadlineIso, { viewModel.setProjectDeadline(project.id, it) }, icon = Icons.Default.Event,
              )
              com.example.ui.components.FieldRow(
                "Progress", project.progressText.ifBlank { "0%" }, icon = Icons.Default.TrendingUp,
              )
              com.example.ui.components.OptionRow(
                "Goal", project.goalName, uiState.goals.map { it.name },
                { name -> viewModel.setProjectGoalRelation(project.id, uiState.goals.firstOrNull { it.name == name }?.id) },
                icon = Icons.Default.TrackChanges,
              )
            }
          },
        )
        val pct = project.progress.coerceIn(0f, 1f)
        androidx.compose.material3.LinearProgressIndicator(
          progress = { pct },
          modifier = Modifier.fillMaxWidth().padding(horizontal = TodayPad).padding(top = 10.dp),
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
          "${done.size} of ${done.size + open.size} tasks done",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = TodayPad, vertical = 4.dp),
        )
      }

      item {
        com.example.ui.components.HubSection(
          "Tasks", Modifier.padding(horizontal = TodayPad), count = open.size + done.size,
        ) {}
      }
      section("Open", open, viewModel)
      section("Done", done, viewModel)

      item {
        com.example.ui.components.HubSection(
          "Notes", Modifier.padding(horizontal = TodayPad), count = notes.size,
        ) {}
      }
      if (notes.isEmpty()) {
        item { EmptyLine("No linked notes.", Modifier.padding(horizontal = TodayPad)) }
      } else {
        itemsIndexed(notes, key = { _, n -> "note:${n.id}" }) { i, note ->
          EntityRow(
            title = note.title,
            meta = note.type,
            leadingDot = MaterialTheme.colorScheme.entityNotes,
            onClick = { viewModel.openNoteDetail(note.id) },
            onLongClick = { viewModel.openNoteQuickEdit(note.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (i < notes.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }

      if (!uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == project.id &&
        com.example.ui.components.markdownHasRenderableContent(uiState.detailBody!!)
      ) {
        item {
          SectionHeader("About", modifier = Modifier.padding(horizontal = TodayPad))
          MarkdownBody(uiState.detailBody!!, Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

@Composable
private fun ProjectViewDetails(
  project: com.example.model.ProjectModel,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
) {
          androidx.compose.foundation.layout.Column {
            var reviewDraft by remember(project.id, project.reviewNotes) { mutableStateOf(project.reviewNotes) }
            Text("Review notes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            androidx.compose.material3.OutlinedTextField(
              value = reviewDraft,
              onValueChange = { reviewDraft = it },
              placeholder = { Text("What's the current state / next review?") },
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            if (reviewDraft != project.reviewNotes) {
              androidx.compose.material3.TextButton(onClick = { viewModel.setProjectReviewNotes(project.id, reviewDraft) }) {
                Text("Save review notes")
              }
            }
            if (uiState.tags.isNotEmpty()) {
              Text("Tags", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
              Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.tags.forEach { tag ->
                  FilterChip(selected = tag.id in project.tagIds, onClick = { viewModel.toggleProjectTag(project.id, tag.id) }, label = { Text(tag.name) })
                }
              }
            }
            if (uiState.people.isNotEmpty()) {
              Text("People", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
              Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.people.forEach { person ->
                  FilterChip(selected = person.id in project.personIds, onClick = { viewModel.toggleProjectPerson(project.id, person.id) }, label = { Text(person.name) })
                }
              }
            }
            if (project.templateName != null) {
              Text("Template: ${project.templateName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
          }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
  title: String,
  tasks: List<Task>,
  viewModel: MyDayViewModel,
) {
  if (tasks.isEmpty()) return
  item(key = "hdr:$title") { SectionHeader(title, tasks.size, Modifier.padding(horizontal = TodayPad)) }
  itemsIndexed(tasks, key = { _, t -> "$title:${t.id}" }) { i, task ->
    TaskRow(
      task = task,
      onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
      onClick = { viewModel.openTaskDetail(task.id) },
      onLongClick = { viewModel.openTaskQuickEdit(task.id) },
      showProject = false,
      modifier = Modifier.padding(horizontal = TodayPad),
    )
    if (i < tasks.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
  }
}
