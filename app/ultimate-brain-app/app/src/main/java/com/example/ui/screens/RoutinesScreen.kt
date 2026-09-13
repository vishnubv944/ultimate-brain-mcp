package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.hermes.HermesJob
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.RoutinesViewModel

/** Wraps GET/POST /api/jobs — scheduled Hermes prompts, unreachable anywhere else in the app until now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(viewModel: MyDayViewModel, routinesViewModel: RoutinesViewModel, modifier: Modifier = Modifier) {
  val s by routinesViewModel.state.collectAsState()
  var showCreate by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { routinesViewModel.refresh() }

  DetailScaffold(
    title = "Routines",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
    actions = {
      IconButton(onClick = { showCreate = true }) {
        Icon(Icons.Default.Add, contentDescription = "New routine")
      }
    },
  ) { pad ->
    Column(Modifier.fillMaxWidth().padding(pad)) {
      s.error?.let {
        Row(
          Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = TodayPad, vertical = 6.dp),
        ) {
          Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
      }
      when {
        s.loading && s.jobs.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
        s.jobs.isEmpty() -> EmptyLine(
          "No routines yet — scheduled prompts Hermes runs on its own.",
          actionLabel = "New routine",
          onAction = { showCreate = true },
        )
        else -> LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(TodayPad)) {
          items(s.jobs, key = { it.id }) { job ->
            RoutineRow(job, onToggle = { routinesViewModel.toggle(job) }, onRunNow = { routinesViewModel.runNow(job) }, onDelete = { routinesViewModel.delete(job) })
            Spacer(Modifier.height(8.dp))
          }
        }
      }
    }
  }

  if (showCreate) {
    CreateRoutineDialog(
      saving = s.saving,
      onDismiss = { showCreate = false },
      onCreate = { name, schedule, prompt ->
        routinesViewModel.create(name, schedule, prompt)
        showCreate = false
      },
    )
  }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RoutineRow(job: HermesJob, onToggle: () -> Unit, onRunNow: () -> Unit, onDelete: () -> Unit) {
  var menuOpen by remember { mutableStateOf(false) }
  androidx.compose.material3.Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
      .combinedClickable(onClick = {}, onLongClick = { menuOpen = true }),
  ) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(job.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(
          job.scheduleDisplay ?: "—",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Default.PlayArrow, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(text = { Text("Run now") }, onClick = { menuOpen = false; onRunNow() })
          DropdownMenuItem(
            text = { Text("Delete") },
            onClick = { menuOpen = false; onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
          )
        }
      }
      Switch(
        checked = job.enabled,
        onCheckedChange = { onToggle() },
        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
      )
    }
  }
}

@Composable
private fun CreateRoutineDialog(saving: Boolean, onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var schedule by remember { mutableStateOf("") }
  var prompt by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New routine") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = schedule, onValueChange = { schedule = it },
          label = { Text("Schedule") },
          placeholder = { Text("e.g. daily at 7am") },
          singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = prompt, onValueChange = { prompt = it },
          label = { Text("Prompt") },
          placeholder = { Text("What should Hermes do?") },
          minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onCreate(name, schedule, prompt) },
        enabled = !saving && name.isNotBlank() && schedule.isNotBlank() && prompt.isNotBlank(),
      ) { Text("Create") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
