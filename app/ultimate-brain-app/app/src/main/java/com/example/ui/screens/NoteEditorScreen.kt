package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.FilterOption
import com.example.ui.components.MarkdownBody
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

private val NOTE_TYPES = listOf("Note", "Meeting", "Journal", "Idea", "Reference", "Book", "Recipe")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val note = uiState.selectedNote ?: return

  var title by remember(note.id) { mutableStateOf(note.title) }
  var type by remember(note.id) { mutableStateOf(note.type) }
  var body by remember(note.id) { mutableStateOf(note.rawMarkdown) }
  var tab by remember { mutableIntStateOf(0) }

  // Prefill from the fetched Notion body when the local copy is empty.
  LaunchedEffect(uiState.detailBody, note.id) {
    if (body.isBlank() && uiState.detailBodyForId == note.id && !uiState.detailBody.isNullOrBlank()) {
      body = uiState.detailBody!!
    }
  }

  DetailScaffold(
    title = "Edit note",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      TextButton(onClick = {
        viewModel.saveNote(note.copy(title = title.ifBlank { "Untitled note" }, type = type, rawMarkdown = body))
      }) { Text("Save", fontWeight = FontWeight.SemiBold) }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("Title") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = TodayPad, vertical = 8.dp),
      )
      SegmentedFilter(
        options = NOTE_TYPES.map { FilterOption(it, it) },
        selected = type,
        onSelect = { type = it },
      )
      PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Write") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Preview") })
      }
      Spacer(Modifier.height(8.dp))
      if (tab == 0) {
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          placeholder = { Text("Write in Markdown…\n\n# Heading\n- [ ] Task\n> Quote") },
          modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = TodayPad),
        )
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
        ) {
          if (body.isBlank()) {
            Text("Nothing to preview yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            MarkdownBody(body)
          }
        }
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}
