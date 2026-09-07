package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.NoteModel
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityNotes
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.NoteFilter

private val NOTE_FILTERS = listOf(
  NoteFilter.ALL to "All",
  NoteFilter.MEETING to "Meeting",
  NoteFilter.JOURNAL to "Journal",
  NoteFilter.IDEA to "Idea",
  NoteFilter.REFERENCE to "Reference",
  NoteFilter.BOOK to "Book",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val list = uiState.notesMatching(uiState.selectedChipKey(com.example.model.FilterScope.NOTES))

  ScreenScaffold(
    title = "Notes",
    viewModel = viewModel,
    active = BottomNavDestination.NOTES,
    modifier = modifier,
    actions = {
      IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
        Icon(Icons.Default.Search, contentDescription = "Search")
      }
    },
    fab = {
      ExtendedFloatingActionButton(
        onClick = { viewModel.createNewNote() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New note", fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      )
    },
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        com.example.ui.components.FilterBar(viewModel, com.example.model.FilterScope.NOTES)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item { EmptyLine("No notes here yet.", Modifier.padding(horizontal = TodayPad)) }
      } else {
        itemsIndexed(list, key = { _, n -> n.id }) { index, note ->
          EntityRow(
            title = note.title,
            meta = noteMeta(note),
            leadingDot = MaterialTheme.colorScheme.entityNotes,
            onClick = { viewModel.openNoteDetail(note.id) },
            trailing = {
              IconButton(onClick = { viewModel.toggleNoteFavorite(note.id) }) {
                Icon(
                  imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                  contentDescription = if (note.isFavorite) "Unfavorite" else "Favorite",
                  tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (index < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun noteMeta(n: NoteModel): String {
  val parts = mutableListOf(n.type)
  n.projectName?.let { parts += it }
  if (n.date.isNotBlank() && n.date != "—") parts += n.date
  return parts.joinToString("  ·  ")
}
