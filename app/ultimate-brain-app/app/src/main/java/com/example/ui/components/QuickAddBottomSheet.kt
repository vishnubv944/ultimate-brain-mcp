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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.ui.theme.entityProjects
import com.example.ui.theme.errorAccent
import com.example.ui.theme.warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
  onDismiss: () -> Unit,
  onSaveTask: (name: String, projectId: String?, priority: Priority?, isMyDay: Boolean) -> Unit,
  projects: List<ProjectModel> = emptyList()
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var taskName by remember { mutableStateOf("") }
  var isMyDay by remember { mutableStateOf(false) }
  var selectedProject by remember { mutableStateOf<ProjectModel?>(null) }
  var projectMenuOpen by remember { mutableStateOf(false) }
  var selectedPriority by remember { mutableStateOf<Priority?>(null) }
  var dueDisplay by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    modifier = Modifier.testTag("quick_add_modal_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp)
        .padding(bottom = 24.dp)
    ) {
      // Header: "New Task" and Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "New Task",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp).testTag("close_sheet_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      TextField(
        value = taskName,
        onValueChange = { taskName = it },
        placeholder = { Text("What task are you planning?") },
        modifier = Modifier.fillMaxWidth().testTag("task_input_field"),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = MaterialTheme.colorScheme.primary,
          unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Smart attribute chips — all native M3 chips so the row reads as one
      // consistent control set (previously a 160dp text field sat among
      // hand-rolled pills of a different height).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Due date — cycles none → Today → Tomorrow → +1 week
        FilterChip(
          selected = dueDisplay != null,
          onClick = {
            dueDisplay = when (dueDisplay) {
              null -> "Today"
              "Today" -> "Tomorrow"
              "Tomorrow" -> "+1 week"
              else -> null
            }
          },
          label = { Text(dueDisplay ?: "Set due") },
          leadingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        )

        // Project — chip anchors a dropdown menu of the supplied projects.
        Box {
          FilterChip(
            selected = selectedProject != null,
            onClick = { projectMenuOpen = true },
            label = { Text(selectedProject?.name ?: "Project") },
            leadingIcon = {
              Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("project_dropdown_field")
          )
          DropdownMenu(
            expanded = projectMenuOpen,
            onDismissRequest = { projectMenuOpen = false }
          ) {
            DropdownMenuItem(
              text = { Text("None") },
              onClick = { selectedProject = null; projectMenuOpen = false }
            )
            projects.forEach { p ->
              DropdownMenuItem(
                text = { Text(p.name) },
                onClick = { selectedProject = p; projectMenuOpen = false }
              )
            }
          }
        }

        // Priority — cycles none → High → Medium → Low
        FilterChip(
          selected = selectedPriority != null,
          onClick = {
            selectedPriority = when (selectedPriority) {
              Priority.HIGH -> Priority.MEDIUM
              Priority.MEDIUM -> Priority.LOW
              Priority.LOW -> null
              null -> Priority.HIGH
            }
          },
          label = {
            Text(selectedPriority?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Priority")
          },
          leadingIcon = {
            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        )

        // My Day toggle
        FilterChip(
          selected = isMyDay,
          onClick = { isMyDay = !isMyDay },
          label = { Text("My Day") },
          leadingIcon = {
            Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        )

      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          if (taskName.isNotBlank()) onSaveTask(taskName, selectedProject?.id, selectedPriority, isMyDay)
        },
        enabled = taskName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth().testTag("save_task_btn"),
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save task", style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
