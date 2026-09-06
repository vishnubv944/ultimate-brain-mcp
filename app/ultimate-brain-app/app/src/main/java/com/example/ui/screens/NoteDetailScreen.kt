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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteActionItem
import com.example.model.NoteModel
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val note = uiState.selectedNote ?: return

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.toggleNoteFavorite(note.id) }) {
            Icon(
              imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Favorite",
              tint = if (note.isFavorite) MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = { viewModel.openNoteEditor(note.id) }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = MaterialTheme.colorScheme.primary)
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
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Header Meta
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            NoteTypeBadge(type = note.type)
            Text(note.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Text(
            text = note.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          if (note.projectName != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.clickable { viewModel.openProjectDetail("p-work-1") }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.entityProjects, modifier = Modifier.size(16.dp))
                Text("Project: ${note.projectName}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.entityProjects)
              }
            }
          }

          if (note.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              note.tags.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.08f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.3f))
                ) {
                  Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.entityTagArea, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }
            }
          }
        }
      }

      // Attendees
      if (note.attendees.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Attendees", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              note.attendees.forEach { name ->
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(name.take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }
            }
          }
        }
      }

      // Agenda
      if (note.agenda.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Agenda", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            note.agenda.forEachIndexed { index, item ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text(item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }
      }

      // Notes Bullets
      if (note.noteBullets.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            note.noteBullets.forEach { (actionTag, text) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("•", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Column {
                  Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
          }
        }
      }

      // Decisions Callout
      if (note.decisions != null) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Decisions", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
              border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(
                  text = note.decisions,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Action Items
      if (note.actionItems.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Action Items", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            note.actionItems.forEach { item ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { viewModel.toggleNoteActionItem(note.id, item.id) }
                  .testTag("action_item_${item.id}")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (item.isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }

      // Fallback Raw Markdown
      if (note.attendees.isEmpty() && note.rawMarkdown.isNotEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = note.rawMarkdown,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(16.dp)
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}
