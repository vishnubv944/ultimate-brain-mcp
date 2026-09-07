package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChipRowConfig
import com.example.model.FilterScope
import com.example.model.customId
import com.example.model.isCustomKey
import com.example.viewmodel.BuiltinFilters
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterCustomizeSheet(
  viewModel: MyDayViewModel,
  scope: FilterScope,
  onDismiss: () -> Unit,
  onNewFilter: () -> Unit,
  onEditFilter: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Working copy of the layout — committed on every edit.
  val natural = remember(uiState.customFilters) {
    BuiltinFilters.keys(scope) + uiState.scopeCustomFilters(scope).map { "cf:" + it.id }
  }
  val stored = uiState.chipConfigs[scope.name] ?: ChipRowConfig()
  var order by remember(stored, natural) {
    mutableStateOf(
      if (stored.order.isEmpty()) natural
      else stored.order.filter { it in natural } + natural.filterNot { it in stored.order },
    )
  }
  var hidden by remember(stored) { mutableStateOf(stored.hidden) }
  var defaultKey by remember(stored) { mutableStateOf(stored.defaultKey) }

  fun commit() = viewModel.saveChipConfig(scope, ChipRowConfig(order, hidden, defaultKey))

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
    ) {
      Text("Customize filters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      Text(
        "Reorder, hide the chips you don't use, or pick the one that opens by default.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
      )

      order.forEachIndexed { index, key ->
        val isCustom = key.isCustomKey()
        val label =
          if (isCustom) uiState.customFilters.firstOrNull { it.id == key.customId() }?.name ?: "Filter"
          else BuiltinFilters.label(key)
        val visible = key !in hidden

        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { defaultKey = if (defaultKey == key) null else key; commit() },
            enabled = visible,
          ) {
            Icon(
              if (defaultKey == key) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
              contentDescription = "Default filter",
              tint = if (defaultKey == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          if (isCustom) {
            IconButton(onClick = { onEditFilter(key.customId()) }) {
              Icon(Icons.Default.Edit, contentDescription = "Edit filter", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.deleteCustomFilter(scope, key.customId()) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete filter", modifier = Modifier.size(18.dp))
            }
          }
          IconButton(
            onClick = { if (index > 0) { order = order.toMutableList().apply { add(index - 1, removeAt(index)) }; commit() } },
            enabled = index > 0,
          ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp)) }
          IconButton(
            onClick = { if (index < order.lastIndex) { order = order.toMutableList().apply { add(index + 1, removeAt(index)) }; commit() } },
            enabled = index < order.lastIndex,
          ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp)) }
          Switch(
            checked = visible,
            onCheckedChange = {
              hidden = if (it) hidden - key else hidden + key
              if (!visible && defaultKey == key) defaultKey = null
              commit()
            },
          )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TextButton(onClick = onNewFilter) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("  New filter")
        }
        TextButton(onClick = {
          order = natural; hidden = emptySet(); defaultKey = null; commit()
        }) { Text("Reset") }
      }
    }
  }
}
