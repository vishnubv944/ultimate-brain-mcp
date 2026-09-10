package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.EntityHubHeader
import com.example.ui.components.HubSection
import com.example.ui.components.OptionRow
import com.example.ui.components.PropertyChip
import com.example.ui.components.PropertyChipRow
import com.example.ui.components.SelectChip
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val tag = uiState.selectedTag

  DetailScaffold(title = "Tag", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    if (tag == null) {
      EmptyLine("Tag not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold
    }
    val subTags = uiState.tags.filter { it.parentId == tag.id }
    val projects = uiState.projects.filter { it.tags.any { t -> t.contains(tag.name, true) } }
    val notes = uiState.notes.filter { it.tags.any { t -> t.contains(tag.name, true) } }
    val goals = uiState.goals.filter { it.tagId == tag.id || it.tagArea.equals(tag.name, true) }
    val cProjects = MaterialTheme.colorScheme.entityProjects
    val cNotes = MaterialTheme.colorScheme.entityNotes
    val cGoals = MaterialTheme.colorScheme.entityGoals

    LazyColumn(modifier = Modifier.padding(pad)) {
      item {
        Spacer(Modifier.height(4.dp))
        EntityHubHeader(
          title = tag.name,
          onRename = { viewModel.renameTag(tag.id, it) },
          modifier = Modifier.padding(horizontal = TodayPad),
          leading = {
            Icon(Icons.Default.Folder, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.entityProjects)
          },
          viewDetails = {
            OptionRow(
              "Parent tag", tag.parentName,
              uiState.tags.filter { it.id != tag.id }.map { it.name },
              { name -> viewModel.setTagParent(tag.id, uiState.tags.firstOrNull { it.name == name }?.id) },
            )
          },
          propertyStrip = {
            com.example.ui.components.PropertyGrid {
              com.example.ui.components.SelectCell(
                Icons.Outlined.Flag, "Type", tag.type,
                uiState.optionsFor("tag.Type", listOf("Area", "Resource", "Entity")),
                { it?.let { t -> viewModel.setTagType(tag.id, t) } },
                allowClear = false,
              )
              com.example.ui.components.ToggleCell(
                Icons.Default.Star, "Favorite", tag.isFavorite, { viewModel.toggleTagFavorite(tag.id) },
              )
            }
          },
        )
      }

      hubList("Sub-tags", subTags.map { it.name to it.id }, cProjects) { viewModel.openTagDetail(it) }
      item { HubSection("Projects", Modifier.padding(horizontal = TodayPad), count = projects.size) {} }
      tagRows(projects.map { it.name to it.id }, projects.map { it.status }, cProjects) { viewModel.openProjectDetail(it) }
      item { HubSection("Notes", Modifier.padding(horizontal = TodayPad), count = notes.size) {} }
      tagRows(notes.map { it.title to it.id }, notes.map { it.type }, cNotes) { viewModel.openNoteDetail(it) }
      if (goals.isNotEmpty()) {
        item { HubSection("Goals", Modifier.padding(horizontal = TodayPad), count = goals.size) {} }
        tagRows(goals.map { it.name to it.id }, goals.map { it.status }, cGoals) { viewModel.openGoalDetail(it) }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.hubList(
  title: String,
  rows: List<Pair<String, String>>,
  dot: androidx.compose.ui.graphics.Color,
  onClick: (String) -> Unit,
) {
  if (rows.isEmpty()) return
  item { HubSection(title, Modifier.padding(horizontal = TodayPad), count = rows.size) {} }
  itemsIndexed(rows, key = { _, r -> "$title:${r.second}" }) { i, r ->
    EntityRow(
      title = r.first, leadingDot = dot,
      onClick = { onClick(r.second) },
      modifier = Modifier.padding(horizontal = TodayPad),
    )
    if (i < rows.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tagRows(
  rows: List<Pair<String, String>>,
  metas: List<String>,
  dot: androidx.compose.ui.graphics.Color,
  onClick: (String) -> Unit,
) {
  if (rows.isEmpty()) {
    item { EmptyLine("None.", Modifier.padding(horizontal = TodayPad)) }
    return
  }
  itemsIndexed(rows, key = { _, r -> "row:${r.second}" }) { i, r ->
    EntityRow(
      title = r.first, meta = metas.getOrNull(i), leadingDot = dot,
      onClick = { onClick(r.second) },
      modifier = Modifier.padding(horizontal = TodayPad),
    )
    if (i < rows.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
  }
}
