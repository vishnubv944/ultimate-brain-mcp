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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkSessionModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.success
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSessionsScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  val todaySessions = uiState.workSessions.filter { it.isToday }
  val yesterdaySessions = uiState.workSessions.filter { !it.isToday }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text("Work Sessions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.MORE_HUB) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        activeDestination = BottomNavDestination.MORE,
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
        onClick = { /* log manual session */ },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Log Session", fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("new_session_fab")
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Live Timer Hero Card
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("live_work_session_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(if (uiState.isWorkSessionActive) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                  text = if (uiState.isWorkSessionActive) "FOCUS SESSION LIVE" else "PAUSED",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (uiState.isWorkSessionActive) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text("Target: 5:00 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Big Timer
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = uiState.formattedActiveSessionTimer,
                  style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Cut release branch and tag",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text("Q3 launch — v2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              // Timer Controls
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                  onClick = { viewModel.toggleWorkSession() },
                  colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                ) {
                  Icon(
                    imageVector = if (uiState.isWorkSessionActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Timer"
                  )
                }
                FilledIconButton(
                  onClick = { viewModel.toggleWorkSession() },
                  colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                  )
                ) {
                  Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
              }
            }
          }
        }
      }

      // Daily Focus Target Bento
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Daily Focus Target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("3h 49m / 4h 00m (95%)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
              progress = { 0.95f },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Text("🔥 5 days streak of meeting your focus target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      // Today's Sessions
      item {
        Text("Today", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(todaySessions) { session ->
        WorkSessionCard(session = session, onTaskClick = { viewModel.openTaskDetail(session.taskId) })
      }

      // Yesterday's Sessions
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text("Yesterday", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      }

      items(yesterdaySessions) { session ->
        WorkSessionCard(session = session, onTaskClick = { viewModel.openTaskDetail(session.taskId) })
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}

@Composable
fun WorkSessionCard(
  session: WorkSessionModel,
  onTaskClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onTaskClick() }
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = Icons.Default.Timelapse,
          contentDescription = null,
          tint = if (session.isRunning) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text(session.taskName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
          Text("${session.projectName} · ${session.timeRange}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
      ) {
        Text(
          text = session.duration,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}
