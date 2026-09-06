package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
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
    val hash = "#${tag.name}"
    val projects = uiState.projects.filter { it.tags.any { t -> t.equals(hash, true) || t.equals(tag.name, true) } }
    val notes = uiState.notes.filter { it.tags.any { t -> t.contains(tag.name, true) } }

    LazyColumn(modifier = Modifier.padding(pad)) {
      item {
        Spacer(Modifier.height(8.dp))
        Text(tag.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = TodayPad))
        Text(tag.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = TodayPad))
        SectionHeader("Details", modifier = Modifier.padding(horizontal = TodayPad))
        com.example.ui.components.OptionRow(
          "Type", tag.type,
          uiState.optionsFor("tag.Type", listOf("Area", "Resource", "Entity")),
          { it?.let { t -> viewModel.setTagType(tag.id, t) } },
          Modifier.padding(horizontal = TodayPad), allowClear = false,
        )
        com.example.ui.components.OptionRow(
          "Parent tag", tag.parentName,
          uiState.tags.filter { it.id != tag.id }.map { it.name },
          { name -> viewModel.setTagParent(tag.id, uiState.tags.firstOrNull { it.name == name }?.id) },
          Modifier.padding(horizontal = TodayPad),
        )
        val subTags = uiState.tags.filter { it.parentId == tag.id }
        if (subTags.isNotEmpty()) {
          SectionHeader("Sub-tags", subTags.size, Modifier.padding(horizontal = TodayPad))
          subTags.forEach { st ->
            EntityRow(
              title = st.name, meta = st.type,
              leadingDot = MaterialTheme.colorScheme.entityProjects,
              onClick = { viewModel.openTagDetail(st.id) },
              modifier = Modifier.padding(horizontal = TodayPad),
            )
          }
        }
      }

      item { SectionHeader("Projects", projects.size, Modifier.padding(horizontal = TodayPad)) }
      if (projects.isEmpty()) item { EmptyLine("None.", Modifier.padding(horizontal = TodayPad)) }
      else itemsIndexed(projects, key = { _, p -> "p:${p.id}" }) { i, p ->
        EntityRow(
          title = p.name, meta = p.status,
          leadingDot = MaterialTheme.colorScheme.entityProjects,
          onClick = { viewModel.openProjectDetail(p.id) },
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < projects.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }

      item { SectionHeader("Notes", notes.size, Modifier.padding(horizontal = TodayPad)) }
      if (notes.isEmpty()) item { EmptyLine("None.", Modifier.padding(horizontal = TodayPad)) }
      else itemsIndexed(notes, key = { _, n -> "n:${n.id}" }) { i, n ->
        EntityRow(
          title = n.title, meta = n.type,
          leadingDot = MaterialTheme.colorScheme.entityNotes,
          onClick = { viewModel.openNoteDetail(n.id) },
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < notes.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}
