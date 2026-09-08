package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkSessionModel
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private fun WorkSessionModel.minutes(): Int {
  durationMinutes?.let { return it }
  val s = startIso ?: return 0
  val e = endIso ?: return 0
  return try {
    ChronoUnit.MINUTES.between(OffsetDateTime.parse(s), OffsetDateTime.parse(e)).toInt().coerceAtLeast(0)
  } catch (_: Exception) {
    0
  }
}

private fun WorkSessionModel.day(): LocalDate? =
  startIso?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun fmtDuration(mins: Int): String = when {
  mins <= 0 -> "0m"
  mins >= 60 -> "${mins / 60}h ${mins % 60}m"
  else -> "${mins}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskWorkSessionsScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val task = uiState.selectedTask
  val sessions = uiState.taskWorkSessions

  DetailScaffold(title = "Time tracked", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(pad)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = TodayPad),
    ) {
      Spacer(Modifier.height(8.dp))
      Text(
        task?.name ?: "Task",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(16.dp))

      if (uiState.taskWorkSessionsLoading && sessions.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
        return@DetailScaffold
      }

      if (sessions.isEmpty()) {
        Spacer(Modifier.height(24.dp))
        EmptyLine("No focus sessions logged for this task yet. Start the timer from Execute or the task screen.")
        Spacer(Modifier.height(120.dp))
        return@DetailScaffold
      }

      val totalMin = sessions.sumOf { it.minutes() }
      val avgMin = (totalMin.toDouble() / sessions.size).roundToInt()
      val longest = sessions.maxOfOrNull { it.minutes() } ?: 0
      val activeDays = sessions.mapNotNull { it.day() }.distinct().size

      Text(
        fmtDuration(totalMin),
        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        "across ${sessions.size} session${if (sessions.size == 1) "" else "s"}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))

      StatCard(
        listOf(
          Stat(fmtDuration(avgMin), "avg session"),
          Stat(fmtDuration(longest), "longest"),
          Stat("$activeDays", if (activeDays == 1) "active day" else "active days"),
        ),
      )

      SectionHeader("Last 14 days")
      DailyBars(sessions)

      SectionHeader("By month")
      MonthBars(sessions)

      SectionHeader("Sessions", sessions.size)
      sessions.forEach { s ->
        Column(Modifier.padding(vertical = 10.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              s.day()?.format(DateTimeFormatter.ofPattern("EEE, MMM d")) ?: "—",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium,
            )
            Text(
              fmtDuration(s.minutes()),
              style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          if (s.timeRange.isNotBlank()) {
            Text(s.timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        ThinDivider()
      }
      Spacer(Modifier.height(120.dp))
    }
  }
}

/** Minutes-per-day bars for the trailing 14 days. */
@Composable
private fun DailyBars(sessions: List<WorkSessionModel>) {
  val today = remember { LocalDate.now() }
  val days = remember(sessions) {
    val byDay = sessions.mapNotNull { s -> s.day()?.let { d -> d to s.minutes() } }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.sum() }
    (0 until 14).map { i ->
      val d = today.minusDays((13 - i).toLong())
      d to (byDay[d] ?: 0)
    }
  }
  val maxV = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
  val barColor = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val n = days.size
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (n - 1)) / n
        days.forEachIndexed { i, (_, v) ->
          val x = i * (barW + gap)
          val h = size.height * (v.toFloat() / maxV)
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(x, 0f),
            size = Size(barW, size.height),
            cornerRadius = CornerRadius(4f, 4f),
          )
          if (v > 0) {
            drawRoundRect(
              color = barColor,
              topLeft = Offset(x, size.height - h),
              size = Size(barW, h),
              cornerRadius = CornerRadius(4f, 4f),
            )
          }
        }
      }
      Spacer(Modifier.height(6.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(days.first().first.format(DateTimeFormatter.ofPattern("MMM d")), fontSize = 11.sp, color = labelColor)
        Text("today", fontSize = 11.sp, color = labelColor)
      }
    }
  }
}

/** Horizontal total-minutes bars per calendar month. */
@Composable
private fun MonthBars(sessions: List<WorkSessionModel>) {
  val months = remember(sessions) {
    val byMonth = sessions.mapNotNull { s -> s.day()?.let { YearMonth.from(it) to s.minutes() } }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.sum() }
    if (byMonth.isEmpty()) {
      emptyList()
    } else {
      val end = byMonth.keys.max()
      val start = byMonth.keys.min()
      val span = ChronoUnit.MONTHS.between(start, end).toInt().coerceIn(0, 11)
      (0..span).map { i -> val m = end.minusMonths((span - i).toLong()); m to (byMonth[m] ?: 0) }
    }
  }
  if (months.isEmpty()) return
  val maxV = (months.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
  val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      months.forEach { (m, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            m.format(DateTimeFormatter.ofPattern("MMM ''yy")),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp),
          )
          Box(
            Modifier
              .weight(1f)
              .height(14.dp)
              .padding(end = 8.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(trackColor),
          ) {
            Box(
              Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (v.toFloat() / maxV).coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(7.dp))
                .background(barColor),
            )
          }
          Text(
            fmtDuration(v),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
