package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
  val filter = uiState.selectedTagFilter
  val list = uiState.filteredTags
  val options = remember(uiState.tags) {
    TAG_FILTERS.map { (f, label) -> FilterOption(f, label, uiState.tags.count { tagMatches(it, f) }) }
  }

  DetailScaffold(title = "Tags & Areas", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    LazyColumn(modifier = Modifier.padding(pad)) {
      item {
        SegmentedFilter(options = options, selected = filter, onSelect = viewModel::selectTagFilter)
        Spacer(Modifier.height(8.dp))
      }
      if (list.isEmpty()) {
        item { EmptyLine("No tags here.", Modifier.padding(horizontal = TodayPad)) }
      } else {
        itemsIndexed(list, key = { _, t -> t.id }) { i, tag ->
          EntityRow(
            title = tag.name,
            meta = tag.type + (if (tag.totalItems > 0) "  ·  ${tag.totalItems} items" else ""),
            leadingDot = tagColor(tag.type),
            onClick = { viewModel.openTagDetail(tag.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
          if (i < list.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
        }
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
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
