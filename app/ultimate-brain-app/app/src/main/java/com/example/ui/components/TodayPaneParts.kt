package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Screen horizontal margin used by every Today pane. */
val TodayPad = 20.dp

/**
 * A section label — the app's third type tier, deliberately quieter than a
 * screen title so hierarchy reads at a glance: title (22, bold, onSurface) »
 * section (14, medium, onSurfaceVariant) » body (16, onSurface). Count is
 * tabular and dimmer still.
 */
@Composable
fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 26.dp, bottom = 6.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (count != null) {
      Spacer(Modifier.width(8.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
      )
    }
  }
}

/**
 * Inline empty state: one line of *why* it's empty, and — where there is one —
 * a button for the single action that fills it. A blank screen should never
 * just announce that it's blank.
 */
@Composable
fun EmptyLine(
  text: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Start,
    )
    if (actionLabel != null && onAction != null) {
      androidx.compose.material3.FilledTonalButton(
        onClick = onAction,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Text(actionLabel, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
  androidx.compose.material3.HorizontalDivider(
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    modifier = modifier,
  )
}
