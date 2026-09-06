package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Priority
import com.example.model.Task
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent
import com.example.viewmodel.PlanFilter

// Short-form priority label for inline task rows (P1/P2/P3). Kept conservative
// for now — callers can swap this in once the design wants MEDIUM/LOW visible.
private fun priorityShortLabel(p: Priority?): String = when (p) {
  Priority.HIGH -> "P1"
  Priority.MEDIUM -> "P2"
  Priority.LOW -> "P3"
  null -> ""
}

@Composable
fun PlanSection(
  tasks: List<Task>,
  selectedFilter: PlanFilter,
  onSelectFilter: (PlanFilter) -> Unit,
  onToggleMyDay: (String) -> Unit,
  onAddTaskClick: () -> Unit,
  inboxCount: Int = 0,
  overdueCount: Int = 0,
  phaseLabel: String = stringResource(R.string.plan_ritual_default),
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.EditCalendar,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.plan_section_title),
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = phaseLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Quick Action Card: Journal & Meeting Notes
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { /* Opens notes drawer or view */ }
        .testTag("plan_notes_shortcut")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.entityProjects,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = stringResource(R.string.plan_journal_meeting_notes),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = stringResource(R.string.plan_open_notes_cd),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Horizontal Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_today),
        icon = Icons.Default.WbSunny,
        isSelected = selectedFilter == PlanFilter.TODAY,
        onClick = { onSelectFilter(PlanFilter.TODAY) }
      )
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_active_projects),
        icon = Icons.Default.Folder,
        iconTint = MaterialTheme.colorScheme.entityProjects,
        isSelected = selectedFilter == PlanFilter.ACTIVE_PROJECTS,
        onClick = { onSelectFilter(PlanFilter.ACTIVE_PROJECTS) }
      )
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_inbox, inboxCount),
        icon = Icons.Default.Inbox,
        isSelected = selectedFilter == PlanFilter.INBOX,
        onClick = { onSelectFilter(PlanFilter.INBOX) }
      )
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_week),
        icon = Icons.Default.DateRange,
        isSelected = selectedFilter == PlanFilter.WEEK,
        onClick = { onSelectFilter(PlanFilter.WEEK) }
      )
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_overdue, overdueCount),
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.errorAccent,
        isSelected = selectedFilter == PlanFilter.OVERDUE,
        textColor = MaterialTheme.colorScheme.errorAccent,
        onClick = { onSelectFilter(PlanFilter.OVERDUE) }
      )
      PlanFilterChip(
        label = stringResource(R.string.plan_chip_recurring),
        icon = Icons.Default.Sync,
        isSelected = selectedFilter == PlanFilter.RECURRING,
        onClick = { onSelectFilter(PlanFilter.RECURRING) }
      )
    }

    // Task Planning Stack Card
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val planTasks = tasks.filter { !it.isDone && !it.isOverdue }.take(3)
        planTasks.forEachIndexed { index, task ->
          PlanTaskRow(
            task = task,
            onToggleMyDay = { onToggleMyDay(task.id) }
          )
          if (index < planTasks.size - 1) {
            HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              thickness = 1.dp,
              modifier = Modifier.padding(vertical = 4.dp)
            )
          }
        }
      }
    }

    // Add Task to Plan Button
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(8.dp)
        )
        .clickable { onAddTaskClick() }
        .padding(vertical = 10.dp)
        .testTag("add_task_to_plan_btn"),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.plan_add_task),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFilterChip(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconTint: Color? = null,
  textColor: Color? = null
) {
  // Real M3 FilterChip — replaces the previous hand-rolled Surface clickable
  // implementation so chip state, ripple, role semantics, and elevation come
  // from the design system instead of being duplicated by hand.
  FilterChip(
    selected = isSelected,
    onClick = onClick,
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
      )
    },
    leadingIcon = {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint ?: LocalContentColor.current,
        modifier = Modifier.size(18.dp),
      )
    },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
      selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
      labelColor = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
      iconColor = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    modifier = modifier,
  )
}

@Composable
fun PlanTaskRow(
  task: Task,
  onToggleMyDay: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onToggleMyDay() }
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.Top
    ) {
      Checkbox(
        checked = task.isMyDay,
        onCheckedChange = { onToggleMyDay() },
        colors = CheckboxDefaults.colors(
          checkedColor = MaterialTheme.colorScheme.primary,
          checkmarkColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .size(24.dp)
          .padding(top = 2.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = task.name,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (task.projectName != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.entityProjects,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = task.projectName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.entityProjects,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          if (task.priority == Priority.HIGH) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.errorAccent,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = priorityShortLabel(task.priority),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.errorAccent,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (task.labels.isNotEmpty()) {
            Text(
              text = task.labels.first(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Text(
            text = task.dueDisplay,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (task.isMyDay) {
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = stringResource(R.string.plan_day_pill),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
  }
}
