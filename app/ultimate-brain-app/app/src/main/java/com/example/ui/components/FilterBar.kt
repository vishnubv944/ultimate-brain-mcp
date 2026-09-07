package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.FilterScope
import com.example.model.customId
import com.example.model.isCustomKey
import com.example.viewmodel.MyDayViewModel

/**
 * The customizable filter row shared by every list screen. Renders the user's
 * arranged built-in + custom chips plus a trailing "Customize" chip. Tapping an
 * already-selected custom chip opens its editor.
 */
@Composable
fun FilterBar(
  viewModel: MyDayViewModel,
  scope: FilterScope,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val keys = uiState.visibleChipKeys(scope)
  val selected = uiState.selectedChipKey(scope)

  var customizing by remember { mutableStateOf(false) }
  var editingId by remember { mutableStateOf<String?>(null) }
  var creatingNew by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = TodayPad, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    keys.forEach { key ->
      val count = uiState.chipCount(scope, key)
      val label = uiState.chipLabel(scope, key)
      FilterChip(
        selected = selected == key,
        onClick = {
          if (selected == key && key.isCustomKey()) editingId = key.customId()
          else viewModel.selectFilterKey(scope, key)
        },
        label = { Text(if (count > 0) "$label  $count" else label) },
      )
    }
    AssistChip(
      onClick = { customizing = true },
      label = { Text("Customize") },
      leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
  }

  if (customizing) {
    FilterCustomizeSheet(
      viewModel = viewModel,
      scope = scope,
      onDismiss = { customizing = false },
      onNewFilter = { customizing = false; creatingNew = true },
      onEditFilter = { id -> customizing = false; editingId = id },
    )
  }
  if (creatingNew) {
    FilterEditorSheet(viewModel = viewModel, scope = scope, existingId = null, onDismiss = { creatingNew = false })
  }
  editingId?.let { id ->
    FilterEditorSheet(viewModel = viewModel, scope = scope, existingId = id, onDismiss = { editingId = null })
  }
}
