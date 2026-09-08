package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focus.FocusSession
import com.example.focus.formatFocusElapsed
import kotlinx.coroutines.delay

/** The running-focus-session card. Shown at the top of Today while a timer runs. */
@Composable
fun FocusCard(
  session: FocusSession,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStop: () -> Unit,
  onOpenTask: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val elapsed by produceState(initialValue = session.elapsedMs(), session) {
    while (true) {
      value = session.elapsedMs()
      delay(1000)
    }
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = session.taskName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 2,
        modifier = Modifier.clickable(onClick = onOpenTask),
      )
      if (session.projectName != null) {
        Text(
          text = session.projectName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = formatFocusElapsed(elapsed),
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      if (session.isPaused) {
        Text(
          "Paused",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
      }
      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = if (session.isPaused) onResume else onPause) {
          Icon(
            imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Text(if (session.isPaused) "  Resume" else "  Pause")
        }
        Button(
          onClick = onStop,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("  Stop")
        }
      }
    }
  }
}
