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
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New $label") },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text("Name") },
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(text.trim()); onDismiss() }, enabled = text.isNotBlank()) { Text("Create") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
