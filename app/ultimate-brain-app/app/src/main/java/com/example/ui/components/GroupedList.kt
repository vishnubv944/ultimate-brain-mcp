package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier

/** One collapsible section in a [groupedRows] list. */
data class EntityGroup<T>(val title: String, val items: List<T>)

/**
 * Split [items] into ordered [EntityGroup]s using [groupOf]. Returns null when
 * [groupOf] yields null for any row (i.e. this view isn't grouped). Groups are
 * emitted in [order] first, then any leftover group names alphabetically.
 */
fun <T> groupSections(
  items: List<T>,
  order: List<String>,
  groupOf: (T) -> String?,
): List<EntityGroup<T>>? {
  if (items.isEmpty()) return emptyList()
  val byGroup = LinkedHashMap<String, MutableList<T>>()
  for (it in items) {
    val g = groupOf(it) ?: return null
    byGroup.getOrPut(g) { mutableListOf() }.add(it)
  }
  val names = order.filter { it in byGroup } + byGroup.keys.filterNot { it in order }.sortedBy { it.lowercase() }
  return names.map { EntityGroup(it, byGroup.getValue(it)) }
}

/**
 * Render [sections] as an accordion: an [ExpanderHeader] per group (open by
 * default) followed by its rows. [expanded] persists open/closed state across
 * recompositions — pass a `remember { mutableStateMapOf<String, Boolean>() }`.
 */
fun <T> LazyListScope.groupedRows(
  sections: List<EntityGroup<T>>,
  expanded: SnapshotStateMap<String, Boolean>,
  rowKey: (T) -> Any,
  headerPadding: Modifier = Modifier,
  dividerPadding: Modifier = Modifier,
  row: @Composable (T) -> Unit,
) {
  sections.forEach { section ->
    val isOpen = expanded[section.title] ?: true
    item(key = "grp:${section.title}") {
      ExpanderHeader(
        label = section.title,
        expanded = isOpen,
        onToggle = { expanded[section.title] = !isOpen },
        modifier = headerPadding,
        count = section.items.size,
      )
    }
    if (isOpen) {
      itemsIndexed(section.items, key = { _, it -> "row:${section.title}:${rowKey(it)}" }) { i, it ->
        row(it)
        if (i < section.items.lastIndex) ThinDivider(dividerPadding)
      }
    }
  }
}
