package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DailyRitualPhase

/**
 * Native Material 3 segmented control for switching between the three daily
 * ritual phases (Plan / Execute / Wrap Up). Replaces the earlier hand-rolled
 * Box + clickable pill strip (UI audit Finding 5). The caller owns the outer
 * padding — this composable adds none of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitualJumpBar(
  selectedPhase: DailyRitualPhase,
  onPhaseSelected: (DailyRitualPhase) -> Unit,
  modifier: Modifier = Modifier
) {
  val phases = DailyRitualPhase.values()
  SingleChoiceSegmentedButtonRow(
    modifier = modifier
      .fillMaxWidth()
      .testTag("ritual_jump_bar")
  ) {
    phases.forEachIndexed { index, phase ->
      val icon = when (phase) {
        DailyRitualPhase.PLAN -> Icons.Default.DateRange
        DailyRitualPhase.EXECUTE -> Icons.Default.Bolt
        DailyRitualPhase.WRAP_UP -> Icons.Default.CheckCircle
      }
      SegmentedButton(
        selected = phase == selectedPhase,
        onClick = { onPhaseSelected(phase) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = phases.size),
        icon = {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.width(18.dp)
          )
        },
        modifier = Modifier.testTag("phase_tab_${phase.name.lowercase()}")
      ) {
        Text(
          text = phase.label,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
