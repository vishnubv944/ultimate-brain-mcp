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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  // Audit Finding 16: dynamicColor is now read from the VM (which seeds the
  // theme at the root via MyApplicationTheme(dynamicColor = ...)). Local
  // toggle mutates VM state; the previous `var dynamicColor by remember` was
  // a static switch because nothing ever read it.
  val uiState by viewModel.uiState.collectAsState()
  var dailyReminder by remember { mutableStateOf(true) }
  var recurringSchedule by remember { mutableStateOf(true) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // User Profile Bento
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_profile_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(50.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text("PS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Priya Sharma", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text("PRO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }
              Text("priya@company.internal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Workspace: Priya — Work (PARA Hub v2.4)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // Notion Sync Status Card
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.success, modifier = Modifier.size(22.dp))
              Column {
                Text("Notion Backend Synced", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text("Last synced 2 minutes ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            IconButton(onClick = { /* sync */ }) {
              Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // Appearance Section
      item {
        SettingsSectionHeader(title = "Appearance & Theme")
      }

      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.ColorLens,
              title = "Dynamic Color (Material You)",
              subtitle = "Derives palette from wallpaper",
              trailing = {
                Switch(
                  checked = uiState.dynamicColorEnabled,
                  onCheckedChange = { viewModel.setDynamicColorEnabled(it) }
                )
              }
            )
            SettingsDivider()
            SettingsItemRow(
              icon = Icons.Default.DarkMode,
              title = "Theme",
              subtitle = "System Default (Follow OS)",
              trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            )
          }
        }
      }

      // App Behavior Section
      item {
        SettingsSectionHeader(title = "App Behavior & Rituals")
      }

      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.Notifications,
              title = "Daily Review Reminder",
              subtitle = "Every day at 9:00 AM",
              trailing = {
                Switch(checked = dailyReminder, onCheckedChange = { dailyReminder = it })
              }
            )
            SettingsDivider()
            SettingsItemRow(
              icon = Icons.Default.Schedule,
              title = "Recurring Tasks Schedule",
              subtitle = "Auto-place onto My Day when due",
              trailing = {
                Switch(checked = recurringSchedule, onCheckedChange = { recurringSchedule = it })
              }
            )
            SettingsDivider()
            SettingsItemRow(
              icon = Icons.Default.Public,
              title = "Timezone",
              subtitle = "Asia/Kolkata (UTC+05:30)",
              trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            )
          }
        }
      }

      // Data & Storage Section
      item {
        SettingsSectionHeader(title = "Data & Storage")
      }

      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.Storage,
              title = "Offline Cache",
              subtitle = "24.2 MB cached",
              trailing = {
                OutlinedButton(onClick = { /* clear cache */ }, shape = RoundedCornerShape(8.dp)) {
                  Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
              }
            )
            SettingsDivider()
            SettingsItemRow(
              icon = Icons.Default.Archive,
              title = "Archived Items",
              subtitle = "14 archived projects & tasks",
              trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            )
            SettingsDivider()
            SettingsItemRow(
              icon = Icons.Default.FileDownload,
              title = "Export Data",
              subtitle = "Download full JSON backup",
              trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            )
          }
        }
      }

      // About Section
      item {
        SettingsSectionHeader(title = "About")
      }

      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          SettingsItemRow(
            icon = Icons.Default.Info,
            title = "Ultimate Brain for Android",
            subtitle = "Version 1.0.4 (Client for Notion)",
            trailing = {
              Text("Up to date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.success)
            }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}

@Composable
fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp)
  )
}

@Composable
fun SettingsItemRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  trailing: @Composable () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
      Column {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    trailing()
  }
}

@Composable
fun SettingsDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  )
}
