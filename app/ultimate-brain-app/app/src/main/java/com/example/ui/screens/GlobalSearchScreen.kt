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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  val showTasks = uiState.globalSearchScope == "All" || uiState.globalSearchScope == "Tasks"
  val showProjects = uiState.globalSearchScope == "All" || uiState.globalSearchScope == "Projects"
  val showNotes = uiState.globalSearchScope == "All" || uiState.globalSearchScope == "Notes"

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.TODAY) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          // Search input lives in the title slot so the title bar collapses
          // cleanly with the M3 AppBar layout. The leading search icon is
          // drawn inside the field via leadingIcon; back is provided by
          // navigationIcon above.
          OutlinedTextField(
            value = uiState.globalSearchQuery,
            onValueChange = { viewModel.setGlobalSearchQuery(it) },
            placeholder = { Text("Search tasks, projects, notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.globalSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.setGlobalSearchQuery("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                  }
                }
                IconButton(onClick = { /* mic */ }) {
                  Icon(Icons.Default.Mic, contentDescription = "Voice", modifier = Modifier.size(18.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("global_search_input"),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent
            )
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Scope Filter Chips (lives below the TopAppBar in the body so it
      // scrolls with content; topBar slot stays reserved for the search field).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          "All" to "All (${uiState.totalSearchMatchesCount})",
          "Tasks" to "Tasks (${uiState.globalSearchTasks.size})",
          "Projects" to "Projects (${uiState.globalSearchProjects.size})",
          "Notes" to "Notes (${uiState.globalSearchNotes.size})"
        ).forEach { (scopeKey, label) ->
          val isSelected = uiState.globalSearchScope == scopeKey
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.setGlobalSearchScope(scopeKey) },
            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
        // Tasks Matches
        if (showTasks && uiState.globalSearchTasks.isNotEmpty()) {
          item {
            Text("TASKS (${uiState.globalSearchTasks.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
          }
          items(uiState.globalSearchTasks) { task ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openTaskDetail(task.id) }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (task.isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(task.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  Text("${task.projectName ?: "Inbox"} · ${task.dueDisplay}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }

        // Projects Matches
        if (showProjects && uiState.globalSearchProjects.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("PROJECTS (${uiState.globalSearchProjects.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityProjects)
          }
          items(uiState.globalSearchProjects) { project ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openProjectDetail(project.id) }
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.entityProjects, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(project.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  Text("${project.status} · ${project.progressText} complete · ${project.totalTasks} tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }

        // Notes Matches
        if (showNotes && uiState.globalSearchNotes.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("NOTES (${uiState.globalSearchNotes.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityNotes)
          }
          items(uiState.globalSearchNotes) { note ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openNoteDetail(note.id) }
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.entityNotes, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(note.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  Text(note.excerpt.take(80) + "...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(40.dp))
        }
      }
    }
  }
}
