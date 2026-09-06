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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectModel
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val project = uiState.selectedProject ?: return

  val projectTasks = uiState.tasks.filter { it.projectId == project.id }
  val doingTasks = projectTasks.filter { it.status == TaskStatus.DOING }
  val todoTasks = projectTasks.filter { it.status == TaskStatus.TODO && !it.isDone }
  val doneTasks = projectTasks.filter { it.isDone || it.status == TaskStatus.DONE }
  val projectNotes = uiState.notes.filter { it.projectName == project.name }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Project Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.openEditProject(project.id) }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = MaterialTheme.colorScheme.primary)
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
            .testTag("project_hero_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              ProjectStatusPill(status = project.status)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.deadline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Text(
              text = project.name,
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            if (project.goalName != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    val matchedGoal = uiState.goals.find { it.name == project.goalName }
                    viewModel.openGoalDetail(matchedGoal?.id ?: "g-1")
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.entityGoals, modifier = Modifier.size(16.dp))
                  Column {
                    Text("Linked Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.goalName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }
            }

            // Progress Bento
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Project Completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.progressText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.entityProjects)
              }
              LinearProgressIndicator(
                progress = { project.progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.entityProjects,
                trackColor = MaterialTheme.colorScheme.surfaceContainer
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("${project.doneTasks} of ${project.totalTasks} tasks done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${project.doingTasks} doing · ${project.todoTasks} to do", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      // Grouped Tasks Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tasks (${projectTasks.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          OutlinedButton(
            onClick = { viewModel.setQuickAddOpen(true) },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Add Task", style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      // Section: Doing
      if (doingTasks.isNotEmpty()) {
        item {
          Text(
            text = "DOING (${doingTasks.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        items(doingTasks) { task ->
          ProjectTaskItem(
            task = task,
            onToggle = { viewModel.toggleTaskStatus(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) }
          )
        }
      }

      // Section: To Do
      if (todoTasks.isNotEmpty()) {
        item {
          Text(
            text = "TO DO (${todoTasks.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        items(todoTasks) { task ->
          ProjectTaskItem(
            task = task,
            onToggle = { viewModel.toggleTaskStatus(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) }
          )
        }
      }

      // Section: Done
      if (doneTasks.isNotEmpty()) {
        item {
          Text(
            text = "DONE (${doneTasks.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.success,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        items(doneTasks) { task ->
          ProjectTaskItem(
            task = task,
            onToggle = { viewModel.toggleTaskStatus(task.id) },
            onClick = { viewModel.openTaskDetail(task.id) }
          )
        }
      }

      // Notes Section
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Linked Notes (${projectNotes.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          OutlinedButton(
            onClick = { viewModel.createNewNote(project.id) },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Add Note", style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      items(projectNotes) { note ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openNoteDetail(note.id) }
            .testTag("project_note_${note.id}")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.entityNotes, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(note.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
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

@Composable
fun ProjectTaskItem(
  task: Task,
  onToggle: () -> Unit,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      IconButton(
        onClick = onToggle,
        modifier = Modifier.size(48.dp)
      ) {
        Icon(
          imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = null,
          tint = if (task.isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = task.name,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
          ),
          color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        if (task.dueDisplay.isNotEmpty() || task.labels.isNotEmpty()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(task.dueDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            task.labels.forEach { label ->
              Text("#$label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }
  }
}
