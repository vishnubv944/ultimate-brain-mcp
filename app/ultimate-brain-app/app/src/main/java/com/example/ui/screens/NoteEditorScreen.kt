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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteModel
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val note = uiState.selectedNote ?: return

  var title by remember { mutableStateOf(note.title) }
  var type by remember { mutableStateOf(note.type) }
  var rawMarkdown by remember { mutableStateOf(note.rawMarkdown) }
  var previewTab by remember { mutableStateOf(0) } // 0: Edit Markdown, 1: Preview

  val categories = listOf("Meeting", "Reference", "Idea", "Journal", "Book", "Recipe")

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Edit Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
          }
        },
        actions = {
          Button(
            onClick = {
              viewModel.saveNote(
                note.copy(
                  title = title,
                  type = type,
                  rawMarkdown = rawMarkdown,
                  excerpt = rawMarkdown.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: ""
                )
              )
            },
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text("Done", fontWeight = FontWeight.Bold)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Formatting toolbar
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          listOf("H1", "H2", "B", "I", "• List", "1. List", "☑ Task", "Quote").forEach { tool ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.clickable {
                rawMarkdown = when (tool) {
                  "H1" -> "$rawMarkdown\n# "
                  "H2" -> "$rawMarkdown\n## "
                  "B" -> "$rawMarkdown **bold** "
                  "I" -> "$rawMarkdown *italic* "
                  "• List" -> "$rawMarkdown\n- "
                  "1. List" -> "$rawMarkdown\n1. "
                  "☑ Task" -> "$rawMarkdown\n- [ ] "
                  "Quote" -> "$rawMarkdown\n> "
                  else -> rawMarkdown
                }
              }
            ) {
              Text(
                text = tool,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Category Fast-switch
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.forEach { cat ->
          val isSelected = type == cat
          FilterChip(
            selected = isSelected,
            onClick = { type = cat },
            label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      // Title input
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("Note Title") },
        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("note_title_input"),
        shape = RoundedCornerShape(12.dp)
      )

      // Relations chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.entityProjects.copy(alpha = 0.1f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityProjects.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.entityProjects, modifier = Modifier.size(16.dp))
            Text(note.projectName ?: "+ Project", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.entityProjects)
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.1f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(note.tags.firstOrNull() ?: "+ Tag", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.entityTagArea)
          }
        }
      }

      // Tab: Edit Markdown vs Preview
      TabRow(
        selectedTabIndex = previewTab,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(selected = previewTab == 0, onClick = { previewTab = 0 }, text = { Text("Edit Markdown") })
        Tab(selected = previewTab == 1, onClick = { previewTab = 1 }, text = { Text("Preview") })
      }

      // Content area
      if (previewTab == 0) {
        OutlinedTextField(
          value = rawMarkdown,
          onValueChange = { rawMarkdown = it },
          placeholder = { Text("Write in Markdown...\n\n# Heading\n- [ ] Task\n> Quote") },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("note_markdown_input"),
          shape = RoundedCornerShape(12.dp)
        )
      } else {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Preview Document", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rawMarkdown, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }
  }
}
