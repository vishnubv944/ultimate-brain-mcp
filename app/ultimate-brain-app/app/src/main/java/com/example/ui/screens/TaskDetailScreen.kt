package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Priority
import com.example.model.TaskStatus
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.errorAccent
import com.example.ui.theme.successContainer
import com.example.ui.theme.onSuccessContainer
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.ui.theme.warningContainer
import com.example.ui.theme.onWarningContainer
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
  viewModel: MyDayViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val task = uiState.selectedTask
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  var showStatusMenu by remember { mutableStateOf(false) }
  var showPriorityMenu by remember { mutableStateOf(false) }
  var showAddSubTaskDialog by remember { mutableStateOf(false) }
  var newSubTaskName by remember { mutableStateOf("") }

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(viewModel.formatSnackbarMessage(context, msg))
      viewModel.clearSnackbar()
    }
  }

  if (task == null) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(stringResource(R.string.task_detail_no_task))
    }
    return
  }

  // Live session pulsing animation
  val infiniteTransition = rememberInfiniteTransition(label = "live_session_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.task_detail_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("detail_back_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.task_detail_back_cd)
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              viewModel.toggleTaskCompletion(task.id)
              onNavigateBack()
            }
          ) {
            Icon(
              imageVector = Icons.Default.Archive,
              contentDescription = stringResource(R.string.task_detail_archive_cd),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { /* Share */ }
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = stringResource(R.string.task_detail_share_cd),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { /* More options */ }
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = stringResource(R.string.task_detail_more_cd)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      // Fixed bottom action bar (Mark Complete)
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // M3 Filled button — replaces the previous iOS-style CircleShape
          // pill. State layer and content padding come from the design system.
          Button(
            onClick = {
              viewModel.toggleTaskCompletion(task.id)
              onNavigateBack()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("mark_complete_bottom_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (task.isDone) stringResource(R.string.task_detail_mark_incomplete) else stringResource(R.string.task_detail_mark_complete),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {

      // HERO HEADER & TITLE
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Priority Dot with ring
          Box(
            modifier = Modifier
              .padding(top = 6.dp)
              .size(16.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorAccent)
            )
          }

          Text(
            text = task.name,
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp,
              lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // INTERACTIVE STATUS ROW
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Status Pill Dropdown
          Box {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.clickable { showStatusMenu = true }.testTag("status_pill_btn")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = when (task.status) {
                    TaskStatus.DOING -> stringResource(R.string.status_doing)
                    TaskStatus.TODO -> stringResource(R.string.status_todo)
                    TaskStatus.DONE -> stringResource(R.string.status_done)
                  },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            DropdownMenu(
              expanded = showStatusMenu,
              onDismissRequest = { showStatusMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.status_doing)) },
                onClick = {
                  viewModel.updateTaskStatus(task.id, TaskStatus.DOING)
                  showStatusMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.status_todo)) },
                onClick = {
                  viewModel.updateTaskStatus(task.id, TaskStatus.TODO)
                  showStatusMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.status_done)) },
                onClick = {
                  viewModel.updateTaskStatus(task.id, TaskStatus.DONE)
                  showStatusMenu = false
                }
              )
            }
          }

          // Priority Pill Dropdown
          Box {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.errorContainer,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.errorAccent.copy(alpha = 0.2f)),
              modifier = Modifier.clickable { showPriorityMenu = true }.testTag("priority_pill_btn")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Flag,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.errorAccent,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = when (task.priority) {
                    Priority.HIGH -> stringResource(R.string.priority_high)
                    Priority.MEDIUM -> stringResource(R.string.priority_medium)
                    Priority.LOW -> stringResource(R.string.priority_low)
                    null -> stringResource(R.string.value_none)
                  },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.errorAccent
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.errorAccent,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            DropdownMenu(
              expanded = showPriorityMenu,
              onDismissRequest = { showPriorityMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.priority_high)) },
                onClick = {
                  viewModel.updateTaskPriority(task.id, Priority.HIGH)
                  showPriorityMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.priority_medium)) },
                onClick = {
                  viewModel.updateTaskPriority(task.id, Priority.MEDIUM)
                  showPriorityMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.priority_low)) },
                onClick = {
                  viewModel.updateTaskPriority(task.id, Priority.LOW)
                  showPriorityMenu = false
                }
              )
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          // My Day Toggle Pill
          Surface(
            shape = CircleShape,
            color = if (task.isMyDay) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (task.isMyDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.clickable { viewModel.toggleMyDay(task.id) }.testTag("detail_my_day_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.warning,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = stringResource(R.string.tasks_chip_my_day),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      }

      // ACTIVE WORK SESSION BANNER
      if (task.isActiveSession) {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.successContainer.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.success.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Pulsing Dot
              Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.success.copy(alpha = 0.25f))
                )
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.success)
                )
              }

              Column {
                Text(
                  text = stringResource(R.string.task_detail_live_session),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                  ),
                  color = MaterialTheme.colorScheme.success
                )
                Text(
                  text = uiState.formattedTimer,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.errorAccent.copy(alpha = 0.25f)),
              modifier = Modifier.clickable { viewModel.endLiveSession(task.id) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.StopCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.errorAccent,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = stringResource(R.string.task_detail_end_session),
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.errorAccent
                )
              }
            }
          }
        }
        }
      }

      // CORE METADATA PROPERTY ROWS (Clean M3 List Tiles)
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
          modifier = Modifier.padding(4.dp)
        ) {
          // Schedule & Time-block
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { /* Edit time */ }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.task_detail_schedule),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = task.timeBlock ?: stringResource(R.string.task_detail_time_block_format, task.dueDisplay),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = stringResource(R.string.task_detail_edit_cd),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

          // Project link
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { /* Navigate to project */ }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.entityProjects,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.task_detail_project),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.entityProjects.copy(alpha = 0.12f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = task.projectName ?: stringResource(R.string.value_none),
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.entityProjects
                    )
                    Icon(
                      imageVector = Icons.Default.ArrowForward,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.entityProjects,
                      modifier = Modifier.size(13.dp)
                    )
                  }
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

          // Labels
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Label,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.task_detail_labels),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                task.labels.forEach { label ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.task_detail_remove_label_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                      )
                    }
                  }
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  modifier = Modifier.clickable { /* Add label */ }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(13.dp)
                    )
                    Text(
                      text = stringResource(R.string.task_detail_add_label),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

          // Taxonomy Area
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Tag,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.entityTagArea,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.task_detail_taxonomy_area),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.entityTagArea.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.entityTagArea),
                modifier = Modifier.padding(top = 2.dp)
              ) {
                Text(
                  text = task.taxonomyArea ?: stringResource(R.string.task_detail_taxonomy_default),
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.entityTagArea,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

          // Repeat / Recurrence
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { /* Repeat sheet */ }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Repeat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.task_detail_repeat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = if (task.isRecurring) task.recurrenceText ?: stringResource(R.string.task_detail_repeat_daily) else stringResource(R.string.task_detail_repeat_none),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      }

      // SUB-TASKS SECTION (Collapsible with Count)
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                imageVector = Icons.Default.Checklist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = stringResource(R.string.task_detail_subtasks),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            val completedCount = task.subTasks.count { it.isCompleted }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainer
            ) {
              Text(
                text = stringResource(R.string.task_detail_subtasks_progress_format, completedCount, task.subTasks.size),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          task.subTasks.forEach { subTask ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.toggleSubTask(task.id, subTask.id) }
                .padding(vertical = 4.dp, horizontal = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (subTask.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (subTask.isCompleted) stringResource(R.string.task_detail_completed_cd) else stringResource(R.string.task_detail_incomplete_cd),
                tint = if (subTask.isCompleted) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = subTask.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Normal,
                  textDecoration = if (subTask.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                ),
                color = if (subTask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { /* Subtask menu */ },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          TextButton(
            onClick = { showAddSubTaskDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("add_subtask_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.task_detail_add_subtask),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }
      }

      // NOTES / BODY SECTION (Rendered Markdown Preview)
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth()
        ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.entityNotes,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = stringResource(R.string.task_detail_notes_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = stringResource(R.string.task_detail_edit_markdown),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.clickable { /* Edit markdown */ }
            )
          }

          Text(
            text = task.implementationNotes ?: stringResource(R.string.task_detail_notes_default),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Code snippet container — uses the M3 inverse-surface token pair
          // (audit #12). Was previously a hardcoded `Color(0xFF283044)` /
          // `Color(0xFFEEF0FF)` pair that bypassed the theme; the tokens are
          // now defined on both light and dark schemes in Theme.kt.
          task.codeSnippet?.let { snippet ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.inverseSurface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = snippet,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          // Acceptance Criteria Checklist
          if (task.acceptanceCriteria.isNotEmpty()) {
            Column(
              modifier = Modifier.padding(top = 4.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = stringResource(R.string.task_detail_acceptance),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )

              task.acceptanceCriteria.forEach { criterion ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleAcceptanceCriterion(task.id, criterion.id) }
                    .padding(vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (criterion.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (criterion.isChecked) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = criterion.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  criterion.codeHighlight?.let { code ->
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                      Text(
                        text = code,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontFamily = FontFamily.Monospace,
                          color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }

  // Add subtask dialog
  if (showAddSubTaskDialog) {
    AlertDialog(
      onDismissRequest = { showAddSubTaskDialog = false },
      title = { Text(stringResource(R.string.task_detail_add_subtask_dialog_title)) },
      text = {
        OutlinedTextField(
          value = newSubTaskName,
          onValueChange = { newSubTaskName = it },
          placeholder = { Text(stringResource(R.string.task_detail_add_subtask_dialog_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("subtask_input")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newSubTaskName.isNotBlank()) {
              viewModel.addSubTask(task.id, newSubTaskName)
              newSubTaskName = ""
              showAddSubTaskDialog = false
            }
          }
        ) {
          Text(stringResource(R.string.task_detail_add_subtask_dialog_add))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddSubTaskDialog = false }) {
          Text(stringResource(R.string.task_detail_dialog_cancel))
        }
      }
    )
  }
}
