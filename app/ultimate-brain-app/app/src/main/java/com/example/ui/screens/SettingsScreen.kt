package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.DetailScaffold
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.success
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()

  DetailScaffold(title = "Settings", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    Column(
      modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad),
    ) {
      SectionHeader("Notion")
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          if (uiState.isRemote) "Connected" else "Not connected — using sample data",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = if (uiState.isRemote) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (uiState.isSyncing) CircularProgressIndicator(Modifier.height(20.dp))
      }
      uiState.syncError?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
      }
      Spacer(Modifier.height(12.dp))
      StatCard(
        listOf(
          Stat(uiState.tasks.size.toString(), "tasks"),
          Stat(uiState.projects.size.toString(), "projects"),
          Stat(uiState.notes.size.toString(), "notes"),
        )
      )
      Spacer(Modifier.height(12.dp))
      OutlinedButton(
        onClick = { viewModel.refreshFromNotion() },
        enabled = uiState.isRemote && !uiState.isSyncing,
        modifier = Modifier.fillMaxWidth(),
      ) { Text("Sync now") }

      SectionHeader("Appearance")
      SettingSwitch("Dynamic colour (Material You)", uiState.dynamicColorEnabled) {
        viewModel.setDynamicColorEnabled(it)
      }

      SectionHeader("About")
      Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text("Version", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
          "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.height(96.dp))
    }
  }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onChange)
  }
}
