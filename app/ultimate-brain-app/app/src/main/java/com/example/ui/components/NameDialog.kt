package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** "New <label>" dialog with a single name field. Shared by every create flow. */
@Composable
fun NameDialog(label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  FieldDialog("New $label", "", "Create", onDismiss, onConfirm)
}

/** "Rename <label>" dialog, pre-filled with [current]. */
@Composable
fun RenameDialog(label: String, current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  FieldDialog("Rename $label", current, "Save", onDismiss, onConfirm)
}

@Composable
private fun FieldDialog(
  title: String,
  initial: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var text by remember { mutableStateOf(initial) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text("Name") },
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(text.trim()); onDismiss() }, enabled = text.isNotBlank()) { Text(confirmLabel) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
