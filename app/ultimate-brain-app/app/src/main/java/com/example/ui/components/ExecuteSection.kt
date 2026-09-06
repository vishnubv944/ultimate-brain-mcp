package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Priority
import com.example.model.Task
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent
import com.example.ui.theme.successContainer
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.viewmodel.ExecuteFilter

@Composable
fun ExecuteSection(
  doingTasks: List<Task>,
  todoTasks: List<Task>,
  doneTasks: List<Task>,
  formattedTimer: String,
  isTimerRunning: Boolean,
  selectedFilter: ExecuteFilter,
  isDoneExpanded: Boolean,
  // Active focus session — driven by ViewModel, not hardcoded (audit Finding 10).
  // When `activeFocusTask` is null, the focus banner collapses (no fake data).
  activeFocusTask: Task? = null,
  activeFocusProjectName: String? = null,
  onToggleTimer: () -> Unit,
  onSelectFilter: (ExecuteFilter) -> Unit,
  onToggleDoneAccordion: () -> Unit,
  onToggleTaskCompletion: (String) -> Unit,
  onTaskClick: ((String) -> Unit)? = null,
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
          imageVector = Icons.Default.Bolt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.execute_title),
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.successContainer
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.success)
          )
          Text(
            text = stringResource(R.string.execute_focus_active),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSuccessContainer
          )
        }
      }
    }

    // Active Focus Session Sticky Banner
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Timer Icon Circle with green status dot
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Box(
              modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.success)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .align(Alignment.TopEnd)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = stringResource(R.string.myday_focus_running),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
              // Project label: derive from active focus, fallback to none.
              if (activeFocusProjectName != null) {
                Text(
                  text = " · $activeFocusProjectName",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Text(
              text = activeFocusTask?.name ?: stringResource(R.string.myday_no_active_focus),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = if (activeFocusTask != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Timer display
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
          ) {
            Text(
              text = formattedTimer,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          // Play / Pause toggle
          IconButton(
            onClick = onToggleTimer,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
              .testTag("pause_timer_btn")
          ) {
            Icon(
              imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isTimerRunning) stringResource(R.string.action_pause_timer) else stringResource(R.string.action_resume_timer),
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // Sub-view Execution Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      ExecuteChip(
        label = stringResource(R.string.execute_chip_my_day),
        icon = Icons.Default.WbSunny,
        isSelected = selectedFilter == ExecuteFilter.MY_DAY,
        onClick = { onSelectFilter(ExecuteFilter.MY_DAY) }
      )
      ExecuteChip(
        label = stringResource(R.string.execute_chip_time),
        icon = Icons.Default.Schedule,
        isSelected = selectedFilter == ExecuteFilter.TIME,
        onClick = { onSelectFilter(ExecuteFilter.TIME) }
      )
      ExecuteChip(
        label = stringResource(R.string.execute_chip_energy),
        icon = Icons.Default.Bolt,
        isSelected = selectedFilter == ExecuteFilter.ENERGY,
        onClick = { onSelectFilter(ExecuteFilter.ENERGY) }
      )
      ExecuteChip(
        label = stringResource(R.string.execute_chip_location),
        icon = Icons.Default.LocationOn,
        isSelected = selectedFilter == ExecuteFilter.LOCATION,
        onClick = { onSelectFilter(ExecuteFilter.LOCATION) }
      )
    }

    // Status Bucket: DOING (In Progress)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = stringResource(R.string.execute_section_doing, doingTasks.size),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      val doingVisible = rememberVisibleCount(selectedFilter)
      doingTasks.page(doingVisible.intValue).forEach { task ->
        ExecuteTaskCard(
          task = task,
          isActiveSession = task.isActiveSession,
          liveTimer = formattedTimer,
          onComplete = { onToggleTaskCompletion(task.id) },
          onClick = { onTaskClick?.invoke(task.id) }
        )
      }
      ShowMoreRow(doingTasks.size - doingVisible.intValue, doingVisible)
    }

    // Status Bucket: TO DO
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
        Surface(
          shape = CircleShape,
          color = Color.Transparent,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
          )
        ) {
          Text(
            text = stringResource(R.string.execute_section_todo, todoTasks.size),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      val todoVisible = rememberVisibleCount(selectedFilter)
      todoTasks.page(todoVisible.intValue).forEach { task ->
        ExecuteTaskCard(
          task = task,
          isActiveSession = false,
          liveTimer = null,
          onComplete = { onToggleTaskCompletion(task.id) },
          onClick = { onTaskClick?.invoke(task.id) }
        )
      }
      ShowMoreRow(todoTasks.size - todoVisible.intValue, todoVisible)
    }

    // Status Bucket: DONE (Collapsed Accordion)
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggleDoneAccordion() }
        .testTag("done_accordion")
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.success,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = stringResource(R.string.execute_section_done_today),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.successContainer
            ) {
              Text(
                text = "${doneTasks.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSuccessContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Text(
              text = if (isDoneExpanded) stringResource(R.string.execute_action_hide) else stringResource(R.string.execute_action_show),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.primary
            )
            Icon(
              imageVector = if (isDoneExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        AnimatedVisibility(
          visible = isDoneExpanded,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val doneVisible = rememberVisibleCount(selectedFilter)
            doneTasks.page(doneVisible.intValue).forEach { task ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onToggleTaskCompletion(task.id) }
                  .padding(vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = stringResource(R.string.action_done),
                  tint = MaterialTheme.colorScheme.success,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  if (task.projectName != null) {
                    Text(
                      text = task.projectName,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.outline
                    )
                  }
                }
              }
            }
            ShowMoreRow(doneTasks.size - doneVisible.intValue, doneVisible)
          }
        }
      }
    }
  }
}

@Composable
fun ExecuteChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
  val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  val border = if (isSelected) {
    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
  } else {
    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = border,
    modifier = Modifier.clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(15.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = textColor
      )
    }
  }
}

@Composable
fun ExecuteTaskCard(
  task: Task,
  isActiveSession: Boolean,
  liveTimer: String?,
  onComplete: () -> Unit,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val border = if (isActiveSession) {
    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
  } else {
    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    border = border,
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Circular Checkbox target
      Box(
        modifier = Modifier
          .size(36.dp)
          .clickable { onComplete() }
          .testTag("task_check_${task.id}"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.RadioButtonUnchecked,
          contentDescription = stringResource(R.string.action_complete_task),
          tint = if (isActiveSession) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            // Priority dot
            val priorityDotColor = when (task.priority) {
              Priority.HIGH -> MaterialTheme.colorScheme.errorAccent
              Priority.MEDIUM -> MaterialTheme.colorScheme.warning
              Priority.LOW -> MaterialTheme.colorScheme.outline
              null -> if (task.labels.contains("Habits")) MaterialTheme.colorScheme.entityGoals else MaterialTheme.colorScheme.outline
            }
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(priorityDotColor)
            )
            Text(
              text = task.name,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isActiveSession) FontWeight.SemiBold else FontWeight.Medium
              ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = task.dueDisplay,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata Tags Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (isActiveSession && liveTimer != null) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.successContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.success,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = liveTimer,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                  ),
                  color = MaterialTheme.colorScheme.onSuccessContainer
                )
              }
            }
          }

          if (task.projectName != null) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
              )
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.entityProjects,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = task.projectName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.entityProjects
                )
              }
            }
          }

          if (task.labels.contains("Habits")) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.SelfImprovement,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.entityGoals,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = stringResource(R.string.execute_habits_label),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.entityGoals
                )
              }
            }
          }

          task.labels.filter { it != "Habits" }.forEach { label ->
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          if (task.isRecurring && task.recurrenceText != null) {
            Text(
              text = task.recurrenceText,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
