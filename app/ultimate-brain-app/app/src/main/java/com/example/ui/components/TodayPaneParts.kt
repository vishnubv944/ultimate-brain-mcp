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
 * A section header: medium-weight text with a muted count, generous space
 * above. No eyebrow, no caps, no pill, no card.
 */
@Composable
fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    if (count != null) {
      Spacer(Modifier.width(8.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Quiet inline empty state — a sentence and (optionally) a follow-on action. */
@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Start,
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
  )
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
  androidx.compose.material3.HorizontalDivider(
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    modifier = modifier,
  )
}
