package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NoteModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.noteTypeColors
import com.example.ui.theme.warning
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.NoteFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  // Filter chips: enum-driven so click handler passes typed values to VM.
  val categories = listOf(
    NoteFilter.ALL to R.string.notes_chip_all_format,
    NoteFilter.MEETING to R.string.notes_chip_meeting,
    NoteFilter.REFERENCE to R.string.notes_chip_reference,
    NoteFilter.IDEA to R.string.notes_chip_idea,
    NoteFilter.JOURNAL to R.string.notes_chip_journal,
    NoteFilter.BOOK to R.string.notes_chip_book
  )

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        actions = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = { /* Grid/List toggle */ }) {
            Icon(Icons.Default.GridView, contentDescription = stringResource(R.string.notes_view_toggle_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = { /* More options */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      BottomNavBar(
        activeDestination = BottomNavDestination.NOTES,
        onDestinationSelected = { dest ->
          when (dest) {
            BottomNavDestination.TODAY -> viewModel.navigateTo(AppScreen.TODAY)
            BottomNavDestination.TASKS -> viewModel.navigateTo(AppScreen.TASKS)
            BottomNavDestination.PROJECTS -> viewModel.navigateTo(AppScreen.PROJECTS)
            BottomNavDestination.NOTES -> viewModel.navigateTo(AppScreen.NOTES)
            BottomNavDestination.MORE -> viewModel.navigateTo(AppScreen.MORE_HUB)
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { viewModel.createNewNote() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.notes_fab_new_note), fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        // Audit Finding 6: standardize FAB elevation + bottom padding so the
        // docked action sits predictably across Goals/Tasks/Notes/Projects.
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("new_note_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Horizontal Category Chips (lives below the MediumTopAppBar in the body
      // so it scrolls with content; topBar slot stays reserved for the title).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        categories.forEach { (filter, labelRes) ->
          val isSelected = uiState.selectedNoteFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectNoteFilter(filter) },
            label = {
              val text = if (filter == NoteFilter.ALL) {
                stringResource(R.string.notes_chip_all_format, uiState.notes.size)
              } else {
                stringResource(labelRes)
              }
              Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(uiState.filteredNotes, key = { it.id }) { note ->
          NoteCard(
            note = note,
            onFavoriteToggle = { viewModel.toggleNoteFavorite(note.id) },
            onClick = { viewModel.openNoteDetail(note.id) }
          )
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }
}

@Composable
fun NoteCard(
  note: NoteModel,
  onFavoriteToggle: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("note_card_${note.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top meta row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          NoteTypeBadge(type = note.type)
          Text(
            text = note.date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onFavoriteToggle,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = stringResource(R.string.action_favorite),
            tint = if (note.isFavorite) MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Title
      Text(
        text = note.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )

      // Project link if any
      if (note.projectName != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.entityProjects,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = note.projectName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.entityProjects
          )
        }
      }

      // Excerpt preview
      if (note.excerpt.isNotEmpty()) {
        Text(
          text = note.excerpt,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Tags Row
      if (note.tags.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          note.tags.forEach { tag ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.08f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.3f))
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.entityTagArea,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NoteTypeBadge(type: String) {
  // Audit Finding 11: hardcoded Color(0x...) container/onContainer pairs
  // replaced by the noteTypeColors(type, colorScheme) helper in ColorScheme
  // Extensions.kt. Dark mode now uses properly tinted containers instead of
  // bright pale pills, and the family lives in the theme, not the screen.
  val palette = noteTypeColors(type, MaterialTheme.colorScheme)

  Surface(
    shape = RoundedCornerShape(4.dp),
    color = palette.container
  ) {
    Text(
      text = type,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = palette.onContainer,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
  }
}
