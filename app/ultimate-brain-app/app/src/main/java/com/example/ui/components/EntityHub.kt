package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Keeps detail content to a readable column on wide screens. */
val DETAIL_MAX_WIDTH = 720.dp

/**
 * The shared head of a detail page: an inline leading marker (a completion
 * check or a small page icon), an editable title, the primary property grid,
 * a quiet "More properties" expander for the rest, and an optional Relations
 * quick-add row.
 */
@Composable
fun EntityHubHeader(
  title: String,
  onRename: (String) -> Unit,
  modifier: Modifier = Modifier,
  leading: (@Composable () -> Unit)? = null,
  viewDetails: (@Composable ColumnScope.() -> Unit)? = null,
  relations: (@Composable () -> Unit)? = null,
  propertyStrip: @Composable () -> Unit,
) {
  var detailsOpen by remember(title) { mutableStateOf(false) }
  Column(modifier.fillMaxWidth()) {
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (leading != null) {
        leading()
        Spacer(Modifier.width(10.dp))
      }
      DetailTitle(title, onRename, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    propertyStrip()
    if (viewDetails != null) {
      TextButton(
        onClick = { detailsOpen = !detailsOpen },
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
      ) {
        Text(
          if (detailsOpen) "Fewer properties" else "More properties",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
          if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = detailsOpen) {
        Column(Modifier.padding(top = 2.dp, bottom = 8.dp)) { viewDetails() }
      }
    }
    if (relations != null) {
      Spacer(Modifier.height(8.dp))
      Text(
        "Relations",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) { relations() }
    }
    Spacer(Modifier.height(6.dp))
  }
}

/** One tab in [DetailTabs]. */
data class HubTab(val label: String, val icon: ImageVector? = null)

/** Native Material underline tabs. Caller renders the body for [selected]. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DetailTabs(
  tabs: List<HubTab>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null,
) {
  Column(modifier.fillMaxWidth()) {
    androidx.compose.material3.SecondaryScrollableTabRow(
      selectedTabIndex = selected.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
      containerColor = Color.Transparent,
      edgePadding = 0.dp,
      divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
    ) {
      tabs.forEachIndexed { i, tab ->
        Tab(
          selected = i == selected,
          onClick = { onSelect(i) },
          text = {
            Text(
              tab.label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
            )
          },
        )
      }
    }
    if (action != null) {
      Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) { action() }
    }
    Spacer(Modifier.height(8.dp))
  }
}

/**
 * A titled section for the stacked-section detail pages (Project / Goal / Tag):
 * header with an optional count, a row of view chips, and an action, then the
 * body. Meant to be used inside a scrolling Column.
 */
@Composable
fun HubSection(
  title: String,
  modifier: Modifier = Modifier,
  count: Int? = null,
  views: List<String> = emptyList(),
  selectedView: String? = null,
  onView: (String) -> Unit = {},
  action: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  Column(modifier.fillMaxWidth().padding(top = 18.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (count != null) {
        Spacer(Modifier.width(8.dp))
        Text(
          count.toString(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
      Spacer(Modifier.weight(1f))
      if (action != null) action()
    }
    if (views.isNotEmpty()) {
      Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        views.forEach { v ->
          val active = v == selectedView
          Surface(
            onClick = { onView(v) },
            shape = RoundedCornerShape(8.dp),
            color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          ) {
            Text(v, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
          }
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    content()
  }
}
