package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.model.ProjectModel
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityTagArea
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val project = uiState.selectedProject ?: return

  var name by remember { mutableStateOf(project.name) }
  var status by remember { mutableStateOf(project.status) }
  var deadline by remember { mutableStateOf(project.deadline) }
  var goalName by remember { mutableStateOf(project.goalName) }
  var tags by remember { mutableStateOf(project.tags) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Edit Project", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
          }
        },
        actions = {
          Button(
            onClick = {
              viewModel.saveProject(
                project.copy(
                  name = name,
                  status = status,
                  deadline = deadline,
                  goalName = goalName,
                  tags = tags
                )
              )
            },
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text("Save", fontWeight = FontWeight.Bold)
          }
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
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Name
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Project Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_project_name_input"),
          shape = RoundedCornerShape(12.dp)
        )
      }

      // Lifecycle Status Segmented
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Lifecycle Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("Not Started", "Doing", "Ongoing", "Done").forEach { st ->
            val isSelected = status == st
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .weight(1f)
                .clickable { status = st }
            ) {
              Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = st,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Deadline
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Target Deadline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = deadline,
          onValueChange = { deadline = it },
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
      }

      // Linked Goal
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Linked Macro Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.entityGoals, modifier = Modifier.size(18.dp))
              Text(
                text = goalName ?: "No goal linked",
                style = MaterialTheme.typography.bodyMedium,
                color = if (goalName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (goalName != null) {
              IconButton(onClick = { goalName = null }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // Tags Section
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags (Areas & Resources)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("#Work", "#Engineering", "#Design").forEach { tag ->
            val isSelected = tags.contains(tag)
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.entityTagArea else MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.clickable {
                tags = if (isSelected) tags - tag else tags + tag
              }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.entityTagArea, modifier = Modifier.size(14.dp))
                }
                Text(tag, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = if (isSelected) MaterialTheme.colorScheme.entityTagArea else MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }
      }

      // Stats Bento
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Project Statistics", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
          Text("12 Tasks (${project.doneTasks} done, ${project.doingTasks} doing, ${project.todoTasks} todo)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${project.notesCount} Linked notes · Template: ${project.templateName ?: "None"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Actions: Archive & Delete
      OutlinedButton(
        onClick = { viewModel.archiveProject(project.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Archive Project")
      }

      Button(
        onClick = { viewModel.archiveProject(project.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
      ) {
        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete Project")
      }

      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
