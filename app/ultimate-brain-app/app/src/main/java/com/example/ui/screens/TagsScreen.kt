package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.TagModel
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.groupSections
import com.example.ui.components.groupedRows
import com.example.viewmodel.BuiltinFilters
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.entityTagEntity
import com.example.ui.theme.entityTagResource
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.TagFilter

private val TAG_FILTERS = listOf(
  TagFilter.ALL to "All",
  TagFilter.AREAS to "Areas",
  TagFilter.RESOURCES to "Resources",
  TagFilter.ENTITIES to "Entities",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val selKey = uiState.selectedChipKey(com.example.model.FilterScope.TAGS)
  val list = uiState.tagsMatching(selKey)
  val sections = groupSections(list, BuiltinFilters.TAG_GROUP_ORDER) {
    BuiltinFilters.tagGroup(selKey, it)
  }
  val groupExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

  DetailScaffold(title = "Tags & Areas", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    LazyColumn(modifier = Modifier.padding(pad)) {
      item {
        com.example.ui.components.FilterBar(viewModel, com.example.model.FilterScope.TAGS)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item { EmptyLine("No tags here.", Modifier.padding(horizontal = TodayPad)) }
      } else if (sections != null) {
        groupedRows(
          sections = sections,
          expanded = groupExpanded,
          rowKey = { it.id },
          headerPadding = Modifier.padding(horizontal = TodayPad),
          dividerPadding = Modifier.padding(horizontal = TodayPad),
        ) { tag -> TagRow(tag, viewModel) }
      } else {
        itemsIndexed(list, key = { _, t -> t.id }) { i, tag ->
          TagRow(tag, viewModel)
          if (i < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

@Composable
private fun TagRow(tag: TagModel, viewModel: MyDayViewModel) {
  EntityRow(
    title = tag.name,
    meta = tag.type + (if (tag.totalItems > 0) "  ·  ${tag.totalItems} items" else ""),
    leadingDot = tagColor(tag.type),
    onClick = { viewModel.openTagDetail(tag.id) },
    modifier = Modifier.padding(horizontal = TodayPad),
    trailing = {
      IconButton(onClick = { viewModel.toggleTagFavorite(tag.id) }) {
        Icon(
          if (tag.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
          contentDescription = "Favorite",
          tint = if (tag.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
}

@Composable
private fun tagColor(type: String) = when (type.lowercase()) {
  "area" -> MaterialTheme.colorScheme.entityTagArea
  "resource" -> MaterialTheme.colorScheme.entityTagResource
  else -> MaterialTheme.colorScheme.entityTagEntity
}

private fun tagMatches(t: TagModel, f: TagFilter) = when (f) {
  TagFilter.ALL -> true
  TagFilter.AREAS -> t.type == "Area"
  TagFilter.RESOURCES -> t.type == "Resource"
  TagFilter.ENTITIES -> t.type == "Entity"
}
