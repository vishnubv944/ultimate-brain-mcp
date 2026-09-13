package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.DetailTabs
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityHubHeader
import com.example.ui.components.HubTab
import com.example.ui.components.MarkdownBody
import com.example.ui.components.PropertyChipRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.components.TaskRow
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val note = uiState.selectedNote

  DetailScaffold(
    title = "Note",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      if (note != null) {
        IconButton(onClick = { viewModel.toggleNoteFavorite(note.id) }) {
          Icon(
            if (note.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
            contentDescription = "Favorite",
            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = { viewModel.openNoteEditor(note.id) }) {
          Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
      }
    },
  ) { innerPadding ->
    if (note == null) {
      EmptyLine("Note not found.", Modifier.padding(innerPadding).padding(TodayPad))
      return@DetailScaffold
    }
    val linkedTasks = uiState.tasks.filter { note.id in it.noteIds }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
     Column(
      modifier = Modifier
        .widthIn(max = com.example.ui.components.DETAIL_MAX_WIDTH)
        .fillMaxWidth()
        .padding(horizontal = TodayPad),
     ) {
      EntityHubHeader(
        title = note.title,
        onRename = { viewModel.renameNote(note.id, it) },
        leading = {
          Icon(
            Icons.Default.Description, contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        viewDetails = { NoteViewDetails(note, uiState, viewModel) },
        propertyStrip = {
          Column {
            com.example.ui.components.OptionRow(
              "Type", note.type,
              uiState.optionsFor("note.Type", listOf("Journal", "Meeting", "Web Clip", "Voice Note", "Lecture", "Reference", "Book", "Idea", "Plan", "Recipe", "Daily")),
              { it?.let { t -> viewModel.setNoteType(note.id, t) } },
              allowClear = false, icon = Icons.Default.Description,
            )
            com.example.ui.components.DateFieldRow("Date", note.dateIso, { viewModel.setNoteDate(note.id, it) }, icon = Icons.Default.Event)
            com.example.ui.components.OptionRow(
              "Project", note.projectName,
              uiState.projects.filter { !it.isArchived }.map { it.name },
              { name -> viewModel.setNoteProjectRelation(note.id, uiState.projects.firstOrNull { it.name == name }?.id) },
              icon = Icons.Default.Folder,
            )
            com.example.ui.components.ToggleFieldRow(
              "Favorite", note.isFavorite, { viewModel.toggleNoteFavorite(note.id) }, icon = Icons.Default.Star,
            )
          }
        },
      )

      var tab by remember(note.id) { mutableStateOf(0) }
      val tabs = listOf(HubTab("Content"), HubTab("Tasks"))
      DetailTabs(tabs, tab, { tab = it })

      when (tab) {
        0 -> {
          when {
            uiState.detailBodyLoading -> CircularProgressIndicator(Modifier.padding(vertical = 24.dp))
            !uiState.detailBody.isNullOrBlank() -> MarkdownBody(uiState.detailBody!!)
            note.rawMarkdown.isNotBlank() -> MarkdownBody(note.rawMarkdown)
            else -> EmptyLine("This note has no content yet. Tap edit to add some.")
          }
        }
        1 -> {
          if (linkedTasks.isEmpty()) {
            EmptyLine("No tasks linked to this note.")
          } else {
            linkedTasks.forEachIndexed { i, t ->
              TaskRow(
                task = t,
                onToggleComplete = { viewModel.toggleTaskCompletion(t.id) },
                onClick = { viewModel.openTaskDetail(t.id) },
                onLongClick = { viewModel.openTaskQuickEdit(t.id) },
              )
              if (i < linkedTasks.lastIndex) ThinDivider()
            }
          }
        }
      }
      Spacer(Modifier.height(96.dp))
     }
    }
  }
}

@Composable
private fun NoteViewDetails(
  note: com.example.model.NoteModel,
  uiState: com.example.viewmodel.MyDayUiState,
  viewModel: MyDayViewModel,
) {
  Column {
    com.example.ui.components.DateFieldRow("Review date", note.reviewDateIso, { viewModel.setNoteReviewDate(note.id, it) })
    var urlDraft by remember(note.id, note.url) { mutableStateOf(note.url) }
    androidx.compose.material3.OutlinedTextField(
      value = urlDraft,
      onValueChange = { urlDraft = it },
      label = { Text("URL") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    if (urlDraft != note.url) {
      androidx.compose.material3.TextButton(onClick = { viewModel.setNoteUrl(note.id, urlDraft.trim()) }) { Text("Save URL") }
    }
    if (uiState.tags.isNotEmpty()) {
      Text("Tags", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
      Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
      ) {
        uiState.tags.forEach { tag ->
          FilterChip(
            selected = tag.id in note.tagIds,
            onClick = { viewModel.toggleNoteTag(note.id, tag.id) },
            label = { Text(tag.name) },
          )
        }
      }
    }
  }
}
