package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Default number of rows a list section shows before "Show more". */
const val DEFAULT_PAGE_SIZE = 25

/**
 * Remembers how many items a capped list currently shows. Survives config
 * changes; resets when [key] changes (e.g. the active filter).
 */
@Composable
fun rememberVisibleCount(key: Any? = Unit, pageSize: Int = DEFAULT_PAGE_SIZE): MutableIntState =
  rememberSaveable(key) { mutableIntStateOf(pageSize) }

/** Take at most [count] items — the windowed slice a capped section renders. */
fun <T> List<T>.page(count: Int): List<T> = if (size <= count) this else subList(0, count)

/**
 * "Show N more" row. Renders nothing when [remaining] <= 0. Bumps the shared
 * [visibleCount] by [pageSize] on tap.
 */
@Composable
fun ShowMoreRow(
  remaining: Int,
  visibleCount: MutableIntState,
  modifier: Modifier = Modifier,
  pageSize: Int = DEFAULT_PAGE_SIZE,
) {
  if (remaining <= 0) return
  TextButton(
    onClick = { visibleCount.intValue += pageSize },
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
  ) {
    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
    Text(
      text = "Show $remaining more",
      style = MaterialTheme.typography.labelLarge,
    )
  }
}
