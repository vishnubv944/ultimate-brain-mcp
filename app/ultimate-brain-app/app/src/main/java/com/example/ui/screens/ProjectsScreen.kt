package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ProjectModel
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityProjects
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.ProjectFilter

private val PROJECT_FILTERS = listOf(
  ProjectFilter.ALL to "All",
  ProjectFilter.ACTIVE to "Active",
  ProjectFilter.DOING to "Doing",
  ProjectFilter.DONE to "Done",
  ProjectFilter.ARCHIVED to "Archived",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val list = uiState.projectsMatching(uiState.selectedChipKey(com.example.model.FilterScope.PROJECTS))
  var showCreate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
  if (showCreate) com.example.ui.components.NameDialog("project", { showCreate = false }) { viewModel.createNewProject(it) }

  ScreenScaffold(
    title = "Projects",
    viewModel = viewModel,
    active = BottomNavDestination.PROJECTS,
    modifier = modifier,
    actions = {
      IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
        Icon(Icons.Default.Search, contentDescription = "Search")
      }
    },
    fab = {
      ExtendedFloatingActionButton(
        onClick = { showCreate = true },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New project", fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
      )
    },
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        com.example.ui.components.FilterBar(viewModel, com.example.model.FilterScope.PROJECTS)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item {
          EmptyLine(
            "No projects in this view yet.",
            Modifier.padding(horizontal = TodayPad),
            actionLabel = "New project",
            onAction = { showCreate = true },
          )
        }
      } else {
        itemsIndexed(list, key = { _, p -> p.id }) { index, project ->
          EntityRow(
            title = project.name,
            meta = projectMeta(project),
            leadingIcon = Icons.Default.Folder,
            leadingIconTint = MaterialTheme.colorScheme.entityProjects,
            strikethrough = project.status == "Done",
            onClick = { viewModel.openProjectDetail(project.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (index < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun projectMeta(p: ProjectModel): String {
  val parts = mutableListOf<String>()
  when {
    p.doneTasks > 0 && p.totalTasks > 0 -> parts += "${p.doneTasks}/${p.totalTasks} tasks"
    p.totalTasks > 0 -> parts += "${p.totalTasks} tasks"
  }
  if (p.progressText.isNotBlank() && p.progressText != "0%") parts += p.progressText
  p.goalName?.let { parts += it }
  if (p.deadline.isNotBlank() && p.deadline != "—") parts += p.deadline
  return parts.joinToString("  ·  ")
}
