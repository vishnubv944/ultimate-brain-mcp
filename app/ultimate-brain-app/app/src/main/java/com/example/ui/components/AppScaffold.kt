package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

/** Thin strip shown under the top bar when Notion writes are queued offline. */
@Composable
fun SyncBanner(viewModel: MyDayViewModel) {
  val state by viewModel.uiState.collectAsState()
  if (state.pendingWriteCount <= 0) return
  androidx.compose.foundation.layout.Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.errorContainer)
      .clickable { viewModel.drainPendingWrites() }
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
  ) {
    Text(
      "${state.pendingWriteCount} change(s) waiting to sync — tap to retry",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
  }
}

/** Standard bottom-nav wiring shared by every top-level screen. */
fun bottomNavHandler(viewModel: MyDayViewModel): (BottomNavDestination) -> Unit = { dest ->
  when (dest) {
    BottomNavDestination.TODAY -> viewModel.navigateTo(AppScreen.TODAY)
    BottomNavDestination.TASKS -> viewModel.navigateTo(AppScreen.TASKS)
    BottomNavDestination.PROJECTS -> viewModel.navigateTo(AppScreen.PROJECTS)
    BottomNavDestination.CHAT -> viewModel.navigateTo(AppScreen.CHAT)
    BottomNavDestination.MORE -> viewModel.navigateTo(AppScreen.MORE_HUB)
  }
}

/**
 * The one scaffold for a top-level (bottom-nav) screen: small pinned
 * `TopAppBar` with a bold `titleLarge`, an actions slot, the shared bottom
 * nav, an optional FAB, and a snackbar host. Body gets [PaddingValues].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
  title: String,
  viewModel: MyDayViewModel,
  active: BottomNavDestination,
  modifier: Modifier = Modifier,
  actions: @Composable () -> Unit = {},
  fab: @Composable () -> Unit = {},
  snackbarHost: SnackbarHostState? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      androidx.compose.foundation.layout.Column {
        TopAppBar(
          title = {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
          },
          actions = { actions() },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SyncBanner(viewModel)
      }
    },
    bottomBar = {
      BottomNavBar(activeDestination = active, onDestinationSelected = bottomNavHandler(viewModel))
    },
    floatingActionButton = fab,
    snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) },
    containerColor = MaterialTheme.colorScheme.background,
    content = content,
  )
}

/**
 * The one scaffold for a detail / sub screen: back arrow, title, optional
 * overflow actions, optional bottom bar (e.g. a primary action), snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable () -> Unit = {},
  bottomBar: @Composable () -> Unit = {},
  fab: @Composable () -> Unit = {},
  snackbarHost: SnackbarHostState? = null,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      androidx.compose.foundation.layout.Column {
        TopAppBar(
          title = {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = { actions() },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      }
    },
    bottomBar = bottomBar,
    floatingActionButton = fab,
    snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) },
    containerColor = MaterialTheme.colorScheme.background,
    content = content,
  )
}
