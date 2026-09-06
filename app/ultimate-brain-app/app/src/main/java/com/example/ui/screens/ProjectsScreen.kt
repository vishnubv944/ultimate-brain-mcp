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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ProjectModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.entityTagResource
import com.example.ui.theme.success
import com.example.ui.theme.successContainer
import com.example.ui.theme.onSuccessContainer
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.ProjectFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.projects_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        actions = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = stringResource(R.string.action_search),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = { /* Sort options */ }) {
            Icon(
              imageVector = Icons.Default.Sort,
              contentDescription = stringResource(R.string.action_sort),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = { /* More options */ }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = stringResource(R.string.action_more),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        activeDestination = BottomNavDestination.PROJECTS,
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
        onClick = { viewModel.openEditProject("p-work-1") },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.projects_fab_new_project), fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        // Audit Finding 6: standardize FAB elevation + bottom padding so the
        // docked action sits predictably across Goals/Tasks/Notes/Projects.
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("new_project_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Horizontal Filter Chips (lives below the MediumTopAppBar in the body
      // so it scrolls with content; topBar slot stays reserved for the title).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        ProjectFilterChip(
          label = stringResource(R.string.projects_chip_all_format, uiState.projects.size),
          isSelected = uiState.selectedProjectFilter == ProjectFilter.ALL,
          hasCheck = true,
          onClick = { viewModel.selectProjectFilter(ProjectFilter.ALL) }
        )
        ProjectFilterChip(
          label = stringResource(R.string.projects_chip_status_doing_format, 2),
          isSelected = uiState.selectedProjectFilter == ProjectFilter.STATUS_DOING,
          hasDropdown = true,
          onClick = { viewModel.selectProjectFilter(ProjectFilter.STATUS_DOING) }
        )
        ProjectFilterChip(
          label = stringResource(R.string.projects_chip_tag_format, "Work"),
          isSelected = uiState.selectedProjectFilter == ProjectFilter.TAG_WORK,
          hasClose = true,
          onClick = { viewModel.selectProjectFilter(ProjectFilter.TAG_WORK) }
        )
        ProjectFilterChip(
          label = stringResource(R.string.projects_chip_goal_format, "Q3 v2.0"),
          isSelected = uiState.selectedProjectFilter == ProjectFilter.GOAL_Q3,
          onClick = { viewModel.selectProjectFilter(ProjectFilter.GOAL_Q3) }
        )
        // Add filter
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color.Transparent,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          modifier = Modifier
            .clickable { /* open filter dialog */ }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_filter), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(uiState.filteredProjects, key = { it.id }) { project ->
          ProjectCard(
            project = project,
            onClick = { viewModel.openProjectDetail(project.id) }
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
fun ProjectCard(
  project: ProjectModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDone = project.status == "Done"

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isDone) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("project_card_${project.id}")
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
          // Status Pill
          ProjectStatusPill(status = project.status)

          // Deadline
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
              contentDescription = null,
              tint = if (isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = project.deadline,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Icon(
          imageVector = Icons.Default.Folder,
          contentDescription = null,
          tint = if (isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.entityProjects,
          modifier = Modifier.size(20.dp)
        )
      }

      // Title
      Text(
        text = project.name,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.SemiBold,
          textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
        ),
        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
      )

      // Linked Goal Badge
      if (project.goalName != null && !isDone) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.TrackChanges,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.entityGoals,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = stringResource(R.string.projects_goal_format, project.goalName),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Progress bar
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isDone) stringResource(R.string.projects_completed) else stringResource(R.string.projects_progress),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = project.progressText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurface
          )
        }
        LinearProgressIndicator(
          progress = { project.progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = if (isDone) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.entityProjects,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
      }

      // Footer: Stats & Tags
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.FormatListBulleted,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (project.notesCount > 0)
              stringResource(R.string.projects_tasks_with_notes_format, project.totalTasks, project.notesCount)
            else
              stringResource(R.string.projects_tasks_only_format, project.totalTasks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Tags
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (project.isArchived) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.successContainer
            ) {
              Text(
                text = stringResource(R.string.projects_archived),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSuccessContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          project.tags.forEach { tag ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.08f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.4f))
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
fun ProjectStatusPill(status: String) {
  val (bgColor, textColor) = when (status) {
    "Doing" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    "Ongoing" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    "Done" -> MaterialTheme.colorScheme.success to MaterialTheme.colorScheme.onSuccessContainer
    else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    shape = RoundedCornerShape(4.dp),
    color = bgColor,
    border = if (status == "Not Started") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
  ) {
    Text(
      text = status,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun ProjectFilterChip(
  label: String,
  isSelected: Boolean,
  hasCheck: Boolean = false,
  hasDropdown: Boolean = false,
  hasClose: Boolean = false,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .clickable { onClick() }
      .padding(vertical = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (hasCheck && isSelected) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (hasDropdown) {
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (hasClose) {
        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
