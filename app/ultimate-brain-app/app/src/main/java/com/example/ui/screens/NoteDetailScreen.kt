package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.MarkdownBody
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(8.dp))
      com.example.ui.components.DetailTitle(note.title, { viewModel.renameNote(note.id, it) })
      Text(
        buildList {
          add(note.type)
          note.projectName?.let { add(it) }
          if (note.date.isNotBlank() && note.date != "—") add(note.date)
        }.joinToString("  ·  "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      com.example.ui.components.PropertyChipRow {
        com.example.ui.components.SelectChip(
          Icons.Default.Description,
          "Type", note.type,
          uiState.optionsFor("note.Type", listOf("Journal", "Meeting", "Web Clip", "Lecture", "Reference", "Book", "Idea", "Plan", "Recipe", "Voice Note", "Daily")),
          { it?.let { t -> viewModel.setNoteType(note.id, t) } },
          allowClear = false,
        )
        com.example.ui.components.DateChip(
          Icons.Default.Event,
          "Date", note.dateIso, { viewModel.setNoteDate(note.id, it) },
        )
        com.example.ui.components.SelectChip(
          Icons.Default.Folder,
          "Project", note.projectName,
          uiState.projects.map { it.name },
          { name -> viewModel.setNoteProjectRelation(note.id, uiState.projects.firstOrNull { it.name == name }?.id) },
        )
      }

      Spacer(Modifier.height(20.dp))

      when {
        uiState.detailBodyLoading -> {
          CircularProgressIndicator(Modifier.padding(vertical = 24.dp))
        }
        !uiState.detailBody.isNullOrBlank() -> {
          MarkdownBody(uiState.detailBody!!)
        }
        note.rawMarkdown.isNotBlank() -> {
          MarkdownBody(note.rawMarkdown)
        }
        else -> EmptyLine("This note has no content yet. Tap edit to add some.")
      }

      var infoOpen by androidx.compose.runtime.remember(note.id) { androidx.compose.runtime.mutableStateOf(false) }
      com.example.ui.components.ExpanderHeader("Note info", infoOpen, { infoOpen = !infoOpen })
      androidx.compose.animation.AnimatedVisibility(visible = infoOpen) {
        Column {
          com.example.ui.components.DateFieldRow("Review date", note.reviewDateIso, { viewModel.setNoteReviewDate(note.id, it) })
          var urlDraft by androidx.compose.runtime.remember(note.id, note.url) { androidx.compose.runtime.mutableStateOf(note.url) }
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
            androidx.compose.foundation.layout.Row(
              Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
              uiState.tags.forEach { tag ->
                androidx.compose.material3.FilterChip(
                  selected = tag.id in note.tagIds,
                  onClick = { viewModel.toggleNoteTag(note.id, tag.id) },
                  label = { Text(tag.name) },
                )
              }
            }
          }
        }
      }
      Spacer(Modifier.height(96.dp))
    }
  }
}
