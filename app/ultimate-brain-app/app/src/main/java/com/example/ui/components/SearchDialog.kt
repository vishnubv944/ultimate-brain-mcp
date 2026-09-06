package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.ui.theme.entityProjects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  searchResults: List<Task>,
  onTaskClick: (Task) -> Unit,
  onDismiss: () -> Unit
) {
  BasicAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("search_dialog")
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search tasks, projects, tags...") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchResults.isEmpty()) {
          Text(
            text = "No tasks found matching \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp)
          ) {
            items(searchResults) { task ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onTaskClick(task)
                    onDismiss()
                  }
                  .padding(vertical = 8.dp)
              ) {
                Text(
                  text = task.name,
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (task.projectName != null) {
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
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.entityProjects
                      )
                    }
                  }
                  Text(
                    text = task.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
          }
        }
      }
    }
  }
}
