package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.DateFieldRow
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.OptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

/**
 * A lean rename / lifecycle screen. Every other project field is edited
 * inline on [ProjectDetailScreen] now, so this only carries the things that
 * screen doesn't: renaming and archiving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val project = uiState.selectedProject

  DetailScaffold(title = "Edit project", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    if (project == null) {
      EmptyLine("Project not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold
    }
    var name by remember(project.id) { mutableStateOf(project.name) }

    Column(
      Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Project name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      if (name.isNotBlank() && name != project.name) {
        TextButton(onClick = { viewModel.saveProject(project.copy(name = name.trim())) }) { Text("Save name") }
      }

      SectionHeader("Lifecycle")
      OptionRow(
        "Status", project.status,
        uiState.optionsFor("project.Status", listOf("Planned", "On Hold", "Doing", "Ongoing", "Done")),
        { it?.let { s -> viewModel.setProjectStatus(project.id, s) } },
        allowClear = false,
      )
      DateFieldRow("Deadline", project.deadlineIso, { viewModel.setProjectDeadline(project.id, it) })

      Spacer(Modifier.height(24.dp))
      if (!project.isArchived) {
        OutlinedButton(onClick = { viewModel.archiveProject(project.id) }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
          Text("Archive project")
        }
      } else {
        Text("This project is archived.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(Modifier.height(96.dp))
    }
  }
}
