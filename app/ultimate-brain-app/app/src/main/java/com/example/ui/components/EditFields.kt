package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A "Label      value ▾" row that opens a menu of [options] (plus a clear). */
@Composable
fun OptionRow(
  label: String,
  value: String?,
  options: List<String>,
  onSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
  allowClear: Boolean = true,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .clickable { open = true }
        .padding(vertical = 12.dp, horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
      Text(
        value ?: "—",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
      )
      Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      if (allowClear) DropdownMenuItem(text = { Text("None") }, onClick = { open = false; onSelect(null) })
      options.forEach { opt ->
        DropdownMenuItem(text = { Text(opt) }, onClick = { open = false; onSelect(opt) })
      }
    }
  }
}

/** A "Label      Sat, Sep 6 ▾" row that opens a date picker; tap the value to clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFieldRow(
  label: String,
  iso: String?,
  onPick: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var open by remember { mutableStateOf(false) }
  val date = iso?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Text(
      date?.format(DateTimeFormatter.ofPattern("EEE, MMM d")) ?: "Set date",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = if (date == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
      modifier = Modifier.clickable { open = true }.padding(horizontal = 4.dp),
    )
    if (date != null) {
      TextButton(onClick = { onPick(null) }) { Text("Clear") }
    }
  }

  if (open) {
    val state = rememberDatePickerState(
      initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
      onDismissRequest = { open = false },
      confirmButton = {
        TextButton(onClick = {
          open = false
          state.selectedDateMillis?.let {
            onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
          }
        }) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
    ) { DatePicker(state = state) }
  }
}
