package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class BottomNavDestination(val label: String, val icon: ImageVector) {
  TODAY("Today", Icons.Default.WbSunny),
  TASKS("Tasks", Icons.Default.Checklist),
  PROJECTS("Projects", Icons.Default.Folder),
  NOTES("Notes", Icons.Default.Description),
  MORE("More", Icons.Default.MoreHoriz)
}

@Composable
fun BottomNavBar(
  activeDestination: BottomNavDestination = BottomNavDestination.TODAY,
  onDestinationSelected: (BottomNavDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 3.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    BottomNavDestination.values().forEach { destination ->
      val isSelected = (destination == activeDestination)
      NavigationBarItem(
        selected = isSelected,
        onClick = { onDestinationSelected(destination) },
        icon = {
          Icon(
            imageVector = destination.icon,
            contentDescription = destination.label
          )
        },
        label = {
          Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
      )
    }
  }
}

