package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.ui.theme.entityProjects

@Composable
fun WrapUpSection(
  overdueTasks: List<Task>,
  currentStep: Int,
  onStepSelected: (Int) -> Unit,
  onClearMyDay: () -> Unit,
  onPostponeTask: (String) -> Unit,
  onRescheduleAll: () -> Unit,
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
          imageVector = Icons.Default.TaskAlt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Wrap Up",
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
          text = "Evening Ritual",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Quick Action Buttons: Clear My Day & Journal Shortcut
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .weight(1f)
          .clickable { onClearMyDay() }
          .testTag("clear_my_day_btn")
      ) {
        Row(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.CleaningServices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Clear My Day",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .weight(1f)
          .clickable { /* Open journal draft */ }
          .testTag("journal_entry_btn")
      ) {
        Row(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.entityProjects,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Journal Entry",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.entityProjects
          )
        }
      }
    }

    // 3-Step Guided Evening Review Card
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "3-STEP EVENING REVIEW",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Steps Grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val steps = listOf("Clear Day", "Calendar", "Tomorrow")
          steps.forEachIndexed { index, stepName ->
            val stepNumber = index + 1
            val isActive = (currentStep == stepNumber)
            val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            val textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = bg,
              modifier = Modifier
                .weight(1f)
                .clickable { onStepSelected(stepNumber) }
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "$stepNumber",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = textColor
                )
                Text(
                  text = stepName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                  ),
                  color = textColor,
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Review Table / Leftovers Card
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (overdueTasks.size == 1) "Remaining in Queue (1 item)"
                   else "Remaining in Queue (${overdueTasks.size} items)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Reschedule all",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onRescheduleAll() }
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (overdueTasks.isEmpty()) {
          Text(
            text = "No remaining tasks! You're all wrapped up for today.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        } else {
          overdueTasks.forEach { task ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
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
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                      ) {
                        Text(
                          text = "Overdue",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                          ),
                          color = MaterialTheme.colorScheme.onErrorContainer,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                      Text(
                        text = task.dueDisplay,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                  ),
                  modifier = Modifier.clickable { onPostponeTask(task.id) }
                ) {
                  Text(
                    text = "+1 Day",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
