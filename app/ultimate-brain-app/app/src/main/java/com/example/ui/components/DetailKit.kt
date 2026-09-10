package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * An editable screen title: reads as a heading, becomes a single-line field on
 * tap, commits on done / focus loss. Detail screens open on the content, not on
 * a form — the title editing hides inside the heading itself.
 */
@Composable
fun DetailTitle(
  text: String,
  onCommit: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editing by remember(text) { mutableStateOf(false) }
  var draft by remember(text) { mutableStateOf(text) }

  if (!editing) {
    Text(
      text = text,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = modifier
        .fillMaxWidth()
        .clickable { draft = text; editing = true }
        .padding(vertical = 4.dp),
    )
  } else {
    TextField(
      value = draft,
      onValueChange = { draft = it },
      textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = {
        editing = false
        if (draft.trim().isNotBlank() && draft.trim() != text) onCommit(draft.trim())
      }),
      colors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
      ),
      modifier = modifier.fillMaxWidth().onFocusChanged {
        if (!it.isFocused && editing) {
          editing = false
          if (draft.trim().isNotBlank() && draft.trim() != text) onCommit(draft.trim())
        }
      },
    )
  }
}

/**
 * A tappable "Label ⌄" section toggle. Clipped to a rounded shape and inset a
 * little so the press / hover highlight is a contained pill, not an edge-to-edge
 * slab.
 */
@Composable
fun ExpanderHeader(
  label: String,
  expanded: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  count: Int? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 12.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onToggle)
      .padding(vertical = 10.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (count != null) {
      Spacer(Modifier.width(8.dp))
      Text(
        count.toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      )
    }
    Spacer(Modifier.weight(1f))
    Icon(
      if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** A horizontally-scrollable row of property chips. */
@Composable
fun PropertyChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) { content() }
}

/**
 * The Notion-style property block under a page title: a wrapping grid of quiet
 * `icon LABEL` / value cells. No chip fills, no clutter — the value is the
 * emphasis, the label is a muted caption, the whole cell is the tap target.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PropertyGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  androidx.compose.foundation.layout.FlowRow(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) { content() }
}

/** One cell of a [PropertyGrid]. [trailing] draws next to the value (e.g. a checkbox). */
@Composable
fun PropertyCell(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  valueColor: Color = Color.Unspecified,
  emptyText: String = "Empty",
  trailing: (@Composable () -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .widthIn(min = 116.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 2.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.width(5.dp))
      Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(3.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        value?.takeIf { it.isNotBlank() } ?: emptyText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (value.isNullOrBlank()) FontWeight.Normal else FontWeight.Medium,
        color = when {
          value.isNullOrBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
          valueColor != Color.Unspecified -> valueColor
          else -> MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
      )
      if (trailing != null) { Spacer(Modifier.width(6.dp)); trailing() }
    }
  }
}

/** [PropertyCell] backed by a single-select dropdown. */
@Composable
fun SelectCell(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String?,
  options: List<String>,
  onSelect: (String?) -> Unit,
  modifier: Modifier = Modifier,
  allowClear: Boolean = true,
  valueColor: Color = Color.Unspecified,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    PropertyCell(icon, label, value, { open = true }, modifier, valueColor)
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      if (allowClear) DropdownMenuItem(text = { Text("None") }, onClick = { open = false; onSelect(null) })
      options.forEach { opt ->
        DropdownMenuItem(text = { Text(opt) }, onClick = { open = false; onSelect(opt) })
      }
    }
  }
}

/** [PropertyCell] backed by a date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCell(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  iso: String?,
  onPick: (String?) -> Unit,
  modifier: Modifier = Modifier,
  overdue: Boolean = false,
) {
  var open by remember { mutableStateOf(false) }
  val date = iso?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
  PropertyCell(
    icon, label,
    date?.format(DateTimeFormatter.ofPattern("MMM d")),
    { open = true }, modifier,
    valueColor = if (overdue && date != null) MaterialTheme.colorScheme.error else Color.Unspecified,
  )
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
          if (date != null) TextButton(onClick = { onPick(null); open = false }) { Text("Clear") }
          TextButton(onClick = { open = false }) { Text("Cancel") }
        }
      },
    ) { DatePicker(state = state) }
  }
}

/** [PropertyCell] whose value is a boolean, shown as a checkbox; tap toggles. */
@Composable
fun ToggleCell(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  checked: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .widthIn(min = 116.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onToggle)
      .padding(vertical = 8.dp, horizontal = 2.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.width(5.dp))
      Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(1.dp))
    androidx.compose.material3.Checkbox(
      checked = checked, onCheckedChange = { onToggle() },
      modifier = Modifier.size(20.dp).padding(0.dp),
    )
  }
}

/**
 * One property as a chip. Set → a tonal chip showing the icon + value (with an
 * optional clear); unset → an outlined chip showing icon + label. One tap opens
 * the picker. No "Label ·" prefix — the icon carries the meaning.
 */
@Composable
fun PropertyChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String?,
  onClick: () -> Unit,
  onClear: (() -> Unit)? = null,
  valueColor: Color = Color.Unspecified,
) {
  if (value.isNullOrBlank()) {
    androidx.compose.material3.AssistChip(
      onClick = onClick,
      label = { Text(label) },
      leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
      colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    )
  } else {
    InputChip(
      selected = true,
      onClick = onClick,
      label = { Text(value, color = valueColor) },
      leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
      trailingIcon = if (onClear != null) {
        {
          Icon(
            Icons.Default.Close,
            contentDescription = "Clear $label",
            modifier = Modifier.size(16.dp).clickable { onClear() },
          )
        }
      } else null,
    )
  }
}

/** [PropertyChip] backed by a single-select dropdown. */
@Composable
fun SelectChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String?,
  options: List<String>,
  onSelect: (String?) -> Unit,
  allowClear: Boolean = true,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    PropertyChip(
      icon = icon,
      label = label,
      value = value,
      onClick = { open = true },
      onClear = if (allowClear && !value.isNullOrBlank()) ({ onSelect(null) }) else null,
    )
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      if (allowClear) DropdownMenuItem(text = { Text("None") }, onClick = { open = false; onSelect(null) })
      options.forEach { opt ->
        DropdownMenuItem(text = { Text(opt) }, onClick = { open = false; onSelect(opt) })
      }
    }
  }
}

/** [PropertyChip] backed by a date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  iso: String?,
  onPick: (String?) -> Unit,
  overdue: Boolean = false,
) {
  var open by remember { mutableStateOf(false) }
  val date = iso?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
  PropertyChip(
    icon = icon,
    label = label,
    value = date?.format(DateTimeFormatter.ofPattern("MMM d")),
    onClick = { open = true },
    onClear = if (date != null) ({ onPick(null) }) else null,
    valueColor = if (overdue && date != null) MaterialTheme.colorScheme.error else Color.Unspecified,
  )
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
          if (date != null) TextButton(onClick = { onPick(null); open = false }) { Text("Clear") }
          TextButton(onClick = { open = false }) { Text("Cancel") }
        }
      },
    ) { DatePicker(state = state) }
  }
}
