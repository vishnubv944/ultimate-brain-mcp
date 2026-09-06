package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One option in a [SegmentedFilter]. [count] null hides the badge. */
data class FilterOption<T>(val value: T, val label: String, val count: Int? = null)

/**
 * A single scrollable row of M3 `FilterChip`s with optional count badges.
 * Replaces the five hand-rolled filter rows across the list screens.
 */
@Composable
fun <T> SegmentedFilter(
  options: List<FilterOption<T>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = TodayPad, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEach { opt ->
      FilterChip(
        selected = selected == opt.value,
        onClick = { onSelect(opt.value) },
        label = {
          Text(if (opt.count != null && opt.count > 0) "${opt.label}  ${opt.count}" else opt.label)
        },
      )
    }
  }
}
