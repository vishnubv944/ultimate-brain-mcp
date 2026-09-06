package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Stat(val value: String, val label: String)

/**
 * The one summary card per screen — a horizontal row of big-number / small-
 * label stats. This is the *only* place a card/elevation is used on a list
 * screen.
 */
@Composable
fun StatCard(stats: List<Stat>, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      stats.forEach { s ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = s.value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = s.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
