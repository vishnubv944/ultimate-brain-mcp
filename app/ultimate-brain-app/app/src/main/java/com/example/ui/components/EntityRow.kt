package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The generalised list row for non-task entities (project, note, goal, tag,
 * milestone, work session, person, book, …). Same shape as [TaskRow]: a
 * leading marker, a title, one muted metadata line, an optional trailing
 * slot. Plain row on the page background — no per-item card.
 *
 * Provide exactly one leading marker: [leadingDot] (a colored dot) or
 * [leadingIcon].
 */
@Composable
fun EntityRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  meta: String? = null,
  leadingDot: Color? = null,
  leadingIcon: ImageVector? = null,
  leadingIconTint: Color = Color.Unspecified,
  strikethrough: Boolean = false,
  trailing: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 56.dp)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
      when {
        leadingIcon != null -> Icon(
          leadingIcon,
          contentDescription = null,
          tint = if (leadingIconTint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else leadingIconTint,
          modifier = Modifier.size(20.dp),
        )
        leadingDot != null -> Box(Modifier.size(10.dp).clip(CircleShape).background(leadingDot))
      }
    }
    Spacer(Modifier.width(4.dp))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (strikethrough) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!meta.isNullOrBlank()) {
        Text(
          text = meta,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (trailing != null) {
      Spacer(Modifier.width(8.dp))
      trailing()
    }
  }
}
