package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DateUtils
import com.example.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAYS = listOf(
  DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
  DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

/**
 * A month grid over the (not-done) tasks: each day shows a dot when work is due,
 * red when that day is in the past. Tapping a day selects it; the caller renders
 * that day's tasks below.
 */
@Composable
fun TaskCalendar(
  tasks: List<Task>,
  month: YearMonth,
  selected: LocalDate,
  onMonth: (YearMonth) -> Unit,
  onSelect: (LocalDate) -> Unit,
  modifier: Modifier = Modifier,
) {
  val today = LocalDate.now()
  val dueByDay: Map<LocalDate, Int> = androidx.compose.runtime.remember(tasks) {
    tasks.asSequence()
      .filter { !it.isDone }
      .mapNotNull { DateUtils.parseIsoDate(it.due) }
      .groupingBy { it }
      .eachCount()
  }

  val first = month.atDay(1)
  val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())

  Column(modifier.fillMaxWidth().padding(horizontal = TodayPad)) {
    Row(
      Modifier.fillMaxWidth().padding(top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
      }
      TextButton(onClick = { onMonth(YearMonth.from(today)); onSelect(today) }) { Text("Today") }
      IconButton(onClick = { onMonth(month.plusMonths(1)) }) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
      }
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      WEEKDAYS.forEach { d ->
        Text(
          d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.weight(1f),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
      }
    }

    for (week in 0 until 6) {
      Row(Modifier.fillMaxWidth()) {
        for (dow in 0 until 7) {
          val date = gridStart.plusDays((week * 7 + dow).toLong())
          val inMonth = YearMonth.from(date) == month
          val isToday = date == today
          val isSelected = date == selected
          val count = dueByDay[date] ?: 0

          Box(
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .clickable { if (YearMonth.from(date) != month) onMonth(YearMonth.from(date)); onSelect(date) },
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                  when {
                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                    isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else -> Modifier
                  },
                ),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  date.dayOfMonth.toString(),
                  style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                  fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                  },
                )
                Box(
                  Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        count == 0 -> androidx.compose.ui.graphics.Color.Transparent
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        date.isBefore(today) -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                      },
                    ),
                )
              }
            }
          }
        }
      }
    }
  }
}
