package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The shared head of every detail page (Task / Note / Project / Goal / Tag):
 * an icon, an editable title, a quiet "View details" expander for the full
 * property list, a horizontally-scrolling strip of the primary property chips,
 * and an optional Relations quick-add row.
 */
@Composable
fun EntityHubHeader(
  title: String,
  onRename: (String) -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  iconTint: Color = Color.Unspecified,
  viewDetails: (@Composable ColumnScope.() -> Unit)? = null,
  relations: (@Composable () -> Unit)? = null,
  propertyStrip: @Composable () -> Unit,
) {
  var detailsOpen by remember(title) { mutableStateOf(false) }
  Column(modifier.fillMaxWidth()) {
    if (icon != null) {
      Spacer(Modifier.height(4.dp))
      Icon(
        icon,
        contentDescription = null,
        tint = if (iconTint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else iconTint,
        modifier = Modifier.size(30.dp),
      )
      Spacer(Modifier.height(4.dp))
    }
    DetailTitle(title, onRename)
    if (viewDetails != null) {
      Text(
        text = if (detailsOpen) "Hide details" else "View details",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .clickable { detailsOpen = !detailsOpen }
          .padding(vertical = 4.dp, horizontal = 2.dp),
      )
      AnimatedVisibility(visible = detailsOpen) {
        Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) { viewDetails() }
      }
    }
    Spacer(Modifier.height(8.dp))
    propertyStrip()
    if (relations != null) {
      Spacer(Modifier.height(10.dp))
      Text(
        "Relations",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
      )
      Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) { relations() }
    }
    Spacer(Modifier.height(10.dp))
    ThinDivider()
  }
}

/** One tab in [DetailTabs]. */
data class HubTab(val label: String, val icon: ImageVector? = null)

/**
 * A scrollable pill tab row with an optional trailing action slot (e.g. "New").
 * The caller renders the body for [selected] itself.
 */
@Composable
fun DetailTabs(
  tabs: List<HubTab>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      Modifier.weight(1f).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      tabs.forEachIndexed { i, tab ->
        val active = i == selected
        Surface(
          onClick = { onSelect(i) },
          shape = RoundedCornerShape(10.dp),
          color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
          contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (tab.icon != null) {
              Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
            }
            Text(
              tab.label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            )
          }
        }
      }
    }
    if (action != null) {
      Spacer(Modifier.width(8.dp))
      action()
    }
  }
}

/**
 * A titled section for the stacked-section detail pages (Project / Goal / Tag):
 * a header with an optional count, a row of view chips, and an action, then the
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
  Column(modifier.fillMaxWidth().padding(top = 16.dp)) {
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
            Text(
              v,
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }
    }
    Spacer(Modifier.height(4.dp))
    content()
  }
}
