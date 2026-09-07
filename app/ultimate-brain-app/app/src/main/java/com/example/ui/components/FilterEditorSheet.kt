package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.domain.FieldKind
import com.example.domain.FilterCatalog
import com.example.model.CustomFilter
import com.example.model.FilterClause
import com.example.model.FilterOp
import com.example.model.FilterScope
import com.example.model.FilterSort
import com.example.model.MatchMode
import com.example.viewmodel.MyDayUiState
import com.example.viewmodel.MyDayViewModel
import java.util.UUID

private fun opLabel(op: FilterOp): String = when (op) {
  FilterOp.IS -> "is"
  FilterOp.IS_NOT -> "is not"
  FilterOp.CONTAINS -> "contains"
  FilterOp.NOT_CONTAINS -> "doesn't contain"
  FilterOp.IS_EMPTY -> "is empty"
  FilterOp.IS_NOT_EMPTY -> "is set"
  FilterOp.IS_TRUE -> "is yes"
  FilterOp.IS_FALSE -> "is no"
  FilterOp.BEFORE -> "before"
  FilterOp.AFTER -> "after"
  FilterOp.ON -> "on"
  FilterOp.WITHIN_DAYS -> "within N days"
  FilterOp.OVERDUE_BY -> "overdue"
  FilterOp.GT -> "greater than"
  FilterOp.LT -> "less than"
}

private fun opNeedsValue(op: FilterOp) = when (op) {
  FilterOp.IS_EMPTY, FilterOp.IS_NOT_EMPTY, FilterOp.IS_TRUE, FilterOp.IS_FALSE -> false
  else -> true
}

/** Options to offer for a SELECT/MULTI field, resolving dynamic sources. */
private fun editorOptions(state: MyDayUiState, optionsKey: String?): List<String> = when (optionsKey) {
  null -> emptyList()
  "task.Project" -> state.projects.filter { !it.isArchived }.map { it.name }.distinct()
  "task.Labels" -> (state.optionsFor("task.Labels", emptyList()) +
    state.tasks.flatMap { it.labels }).distinct().sorted()
  "note.Tags" -> state.notes.flatMap { it.tags }.distinct().sorted()
  "task.Status" -> state.optionsFor("task.Status", listOf("To Do", "Doing", "Done"))
  "task.Priority" -> state.optionsFor("task.Priority", listOf("High", "Medium", "Low"))
  "task.Energy" -> state.optionsFor("task.Energy", listOf("High", "Low"))
  "project.Status" -> state.optionsFor("project.Status", listOf("Planned", "On Hold", "Doing", "Ongoing", "Done"))
  "note.Type" -> state.optionsFor("note.Type", listOf("Note", "Meeting", "Journal", "Idea", "Reference", "Book", "Recipe"))
  "goal.Status" -> state.optionsFor("goal.Status", listOf("Dream", "Active", "Achieved"))
  "tag.Type" -> state.optionsFor("tag.Type", listOf("Area", "Resource", "Entity"))
  else -> state.optionsFor(optionsKey, emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEditorSheet(
  viewModel: MyDayViewModel,
  scope: FilterScope,
  existingId: String?,
  onDismiss: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val fields = remember(scope) { FilterCatalog.fieldsFor(scope) }
  val existing = existingId?.let { id -> uiState.customFilters.firstOrNull { it.id == id } }

  var name by remember { mutableStateOf(existing?.name ?: "") }
  var match by remember { mutableStateOf(existing?.match ?: MatchMode.ALL) }
  var clauses by remember {
    mutableStateOf(existing?.clauses ?: listOf(FilterClause(fields.first().key, fields.first().ops.first(), null)))
  }
  var sortField by remember { mutableStateOf(existing?.sort?.field) }
  var sortDesc by remember { mutableStateOf(existing?.sort?.descending ?: false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
    ) {
      Text(
        if (existing == null) "New filter" else "Edit filter",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Filter name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      )

      Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = match == MatchMode.ALL, onClick = { match = MatchMode.ALL }, label = { Text("Match all") })
        FilterChip(selected = match == MatchMode.ANY, onClick = { match = MatchMode.ANY }, label = { Text("Match any") })
      }

      clauses.forEachIndexed { index, clause ->
        val def = FilterCatalog.fieldDef(scope, clause.field) ?: fields.first()
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Condition ${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (clauses.size > 1) {
              IconButton(onClick = { clauses = clauses.toMutableList().apply { removeAt(index) } }) {
                Icon(Icons.Default.Close, contentDescription = "Remove condition", modifier = Modifier.size(18.dp))
              }
            }
          }
          MenuField(
            value = def.label,
            options = fields.map { it.label },
            onSelect = { picked ->
              val newDef = fields.first { it.label == picked }
              clauses = clauses.toMutableList().apply {
                set(index, FilterClause(newDef.key, newDef.ops.first(), null))
              }
            },
          )
          MenuField(
            value = opLabel(clause.op),
            options = def.ops.map { opLabel(it) },
            onSelect = { picked ->
              val op = def.ops.first { opLabel(it) == picked }
              clauses = clauses.toMutableList().apply { set(index, clause.copy(op = op, value = null)) }
            },
            modifier = Modifier.padding(top = 6.dp),
          )
          if (opNeedsValue(clause.op)) {
            val opts = if (def.kind == FieldKind.SELECT || def.kind == FieldKind.MULTI) editorOptions(uiState, def.optionsKey) else emptyList()
            if (opts.isNotEmpty()) {
              MenuField(
                value = clause.value ?: "Choose…",
                options = opts,
                onSelect = { clauses = clauses.toMutableList().apply { set(index, clause.copy(value = it)) } },
                modifier = Modifier.padding(top = 6.dp),
              )
            } else {
              OutlinedTextField(
                value = clause.value ?: "",
                onValueChange = { clauses = clauses.toMutableList().apply { set(index, clause.copy(value = it)) } },
                label = {
                  Text(
                    when (clause.op) {
                      FilterOp.WITHIN_DAYS, FilterOp.OVERDUE_BY -> "Number of days"
                      FilterOp.BEFORE, FilterOp.AFTER, FilterOp.ON -> "Date (YYYY-MM-DD)"
                      else -> "Value"
                    },
                  )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
              )
            }
          }
        }
      }

      TextButton(
        onClick = {
          clauses = clauses + FilterClause(fields.first().key, fields.first().ops.first(), null)
        },
        modifier = Modifier.padding(top = 4.dp),
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  Add condition")
      }

      Text("Sort by", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
      MenuField(
        value = sortField?.let { k -> fields.firstOrNull { it.key == k }?.label } ?: "Default",
        options = listOf("Default") + fields.map { it.label },
        onSelect = { picked -> sortField = if (picked == "Default") null else fields.first { it.label == picked }.key },
        modifier = Modifier.padding(top = 6.dp),
      )
      if (sortField != null) {
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = !sortDesc, onClick = { sortDesc = false }, label = { Text("Ascending") })
          FilterChip(selected = sortDesc, onClick = { sortDesc = true }, label = { Text("Descending") })
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
          onClick = {
            viewModel.saveCustomFilter(
              CustomFilter(
                id = existing?.id ?: UUID.randomUUID().toString(),
                scope = scope,
                name = name.trim().ifBlank { "Untitled filter" },
                match = match,
                clauses = clauses.filter { it.value != null || !opNeedsValue(it.op) },
                sort = sortField?.let { FilterSort(it, sortDesc) },
              ),
            )
            onDismiss()
          },
          modifier = Modifier.weight(1f),
        ) { Text("Save filter") }
      }
    }
  }
}

@Composable
private fun MenuField(
  value: String,
  options: List<String>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var open by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
      Text(value, modifier = Modifier.weight(1f))
      Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      options.forEach { opt ->
        DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); open = false })
      }
    }
  }
}
