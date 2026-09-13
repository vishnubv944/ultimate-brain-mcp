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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Shared "Label   value" row anatomy — an optional leading icon, a label that
 * takes the remaining width, and trailing content the caller supplies (a
 * value, a chevron, a Switch...). Every property row on a detail page is
 * built from this so the primary (always-visible) properties and the
 * "More properties" ones read as one system instead of two: the earlier
 * version showed the primary block as a wrapping grid of little chip-like
 * cells (Notion-ish, but not a native Android pattern) while the secondary
 * block already used this plain full-width row — visually two different
 * screens stitched together. This is the Google Calendar / Contacts detail
 * row shape: icon, label, value, all in one line, full width.
 */
@Composable
private fun FieldRowScaffold(
  icon: ImageVector?,
  label: String,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  trailing: @Composable () -> Unit,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .let { if (onClick != null) it.clickable(onClick = onClick) else it }
      .padding(vertical = 12.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
      androidx.compose.foundation.layout.Spacer(Modifier.width(14.dp))
    }
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    trailing()
  }
}

/** A "Label      value ▾" row that opens a menu of [options] (plus a clear). */
@Composable
fun OptionRow(
  label: String,
  value: String?,
  options: List<String>,
  onSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
  allowClear: Boolean = true,
  icon: ImageVector? = null,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    FieldRowScaffold(icon, label, onClick = { open = true }, modifier = modifier) {
      Text(
        value ?: "—",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
      )
      androidx.compose.foundation.layout.Spacer(Modifier.width(2.dp))
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
  icon: ImageVector? = null,
  overdue: Boolean = false,
) {
  var open by remember { mutableStateOf(false) }
  val date = iso?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
  FieldRowScaffold(icon, label, onClick = { open = true }, modifier = modifier) {
    Text(
      date?.format(DateTimeFormatter.ofPattern("EEE, MMM d")) ?: "Set date",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = when {
        date == null -> MaterialTheme.colorScheme.onSurfaceVariant
        overdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
      },
    )
    if (date != null) {
      androidx.compose.foundation.layout.Spacer(Modifier.width(2.dp))
      Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
      dismissButton = {
        Row {
          if (date != null) TextButton(onClick = { open = false; onPick(null) }) { Text("Clear") }
          TextButton(onClick = { open = false }) { Text("Cancel") }
        }
      },
    ) { DatePicker(state = state) }
  }
}

/** A "Label      value" row that opens a menu of [options]. */
@Composable
fun ToggleFieldRow(
  label: String,
  checked: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
) {
  FieldRowScaffold(icon, label, onClick = { onToggle(!checked) }, modifier = modifier) {
    Switch(checked = checked, onCheckedChange = onToggle)
  }
}

/** A read-only "Label      value" row — no picker, no click, just a fact (e.g. computed progress). */
@Composable
fun FieldRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
  FieldRowScaffold(icon, label, onClick = null, modifier = modifier) {
    Text(
      value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = if (valueColor != androidx.compose.ui.graphics.Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
    )
  }
}
