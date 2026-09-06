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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val tag = uiState.selectedTag ?: return

  val tagProjects = uiState.projects.filter { it.tags.any { t -> t.contains(tag.name.removePrefix("#")) } }
  val bareName = tag.name.removePrefix("#")
  val tagTasks = uiState.tasks.filter { task ->
    task.labels.any { it.equals(bareName, ignoreCase = true) || it.equals(tag.name, ignoreCase = true) } ||
      tagProjects.any { it.id == task.projectId }
  }
  val tagNotes = uiState.notes.filter { note ->
    note.tags.any { t -> t.contains(tag.name.removePrefix("#")) } ||
      tagProjects.any { it.name == note.projectName }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Tag Detail", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { /* favorite toggle */ }) {
            Icon(
              imageVector = if (tag.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Favorite",
              tint = if (tag.isFavorite) MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hero Card
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tag_hero_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.15f)
            ) {
              Text(
                text = "${tag.type} Tag",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.entityTagArea,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }

            Text(
              text = tag.name,
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tag.totalItems}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("Total Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tagProjects.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityProjects)
                Text("Projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tagTasks.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text("Tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tagNotes.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityNotes)
                Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      // Projects Section
      item {
        Text("Projects in ${tag.name} (${tagProjects.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(tagProjects) { proj ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openProjectDetail(proj.id) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.entityProjects, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(proj.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
              Text("${proj.status} · ${proj.progressText} complete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      // Active Tasks Section
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text("Active Tasks (${tagTasks.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(tagTasks) { task ->
        Surface(
          shape = RoundedCornerShape(10.dp),
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
            IconButton(
              onClick = { viewModel.toggleTaskStatus(task.id) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(task.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
              Text(task.projectName ?: "Task", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      // Linked Notes Section
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text("Linked Notes (${tagNotes.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(tagNotes) { note ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openNoteDetail(note.id) }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.entityNotes, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(note.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
              Text("${note.type} · ${note.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
