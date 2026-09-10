package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Task
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val DAY_START_HOUR = 5
private const val DAY_END_HOUR = 24
private val HOUR_HEIGHT = 64.dp
private const val SNAP_MIN = 15

/** Parsed local start/end for a task, or null when it has no time-of-day. */
private fun taskTimes(t: Task): Pair<LocalDateTime, LocalDateTime>? {
  val startRaw = t.dueStartIso ?: return null
  if (!startRaw.contains('T')) return null
  val start = runCatching { OffsetDateTime.parse(startRaw).toLocalDateTime() }.getOrNull()
    ?: runCatching { LocalDateTime.parse(startRaw) }.getOrNull()
    ?: return null
  val end = t.dueEndIso?.let {
    runCatching { OffsetDateTime.parse(it).toLocalDateTime() }.getOrNull()
      ?: runCatching { LocalDateTime.parse(it) }.getOrNull()
  } ?: start.plusMinutes(60)
  return start to end
}

private fun isoAt(day: LocalDate, minutesFromMidnight: Int): String {
  val t = LocalTime.ofSecondOfDay((minutesFromMidnight.coerceIn(0, 24 * 60 - 1) * 60).toLong())
  return day.atTime(t).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

@Composable
fun DayTimelineScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var day by remember { mutableStateOf(LocalDate.now()) }

  DetailScaffold(
    title = "Time-block",
    onBack = { viewModel.navigateBack() },
    modifier = modifier,
  ) { pad ->
    Column(Modifier.fillMaxSize().padding(pad)) {
      // Date bar
      Row(
        Modifier.fillMaxWidth().padding(horizontal = TodayPad, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          day.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { day = day.minusDays(1) }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        TextButton(onClick = { day = LocalDate.now() }) { Text("Today") }
        IconButton(onClick = { day = day.plusDays(1) }) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
      }

      val dayTasks = uiState.tasks.filter { !it.isDone && it.due == day.toString() }
      val scheduled = dayTasks.mapNotNull { t -> taskTimes(t)?.let { t to it } }
        .filter { it.second.first.toLocalDate() == day }
      val unscheduled = dayTasks.filter { taskTimes(it) == null }

      // Unscheduled tray
      if (unscheduled.isNotEmpty()) {
        Text(
          "Not yet blocked",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = TodayPad, vertical = 4.dp),
        )
        Row(
          Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = TodayPad, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          unscheduled.forEach { t ->
            AssistChip(
              onClick = {
                // Drop it at the next free-looking hour (9:00 fallback).
                val used = scheduled.map { it.second.first.hour }.toSet()
                val hour = (9..21).firstOrNull { it !in used } ?: 9
                viewModel.setTaskTimeBlock(t.id, isoAt(day, hour * 60), isoAt(day, hour * 60 + 60))
              },
              label = { Text(t.name, maxLines = 1) },
            )
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Timeline
      val density = LocalDensity.current
      val hourPx = with(density) { HOUR_HEIGHT.toPx() }
      val now = LocalDateTime.now()
      Box(
        Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState()),
      ) {
        Column {
          for (h in DAY_START_HOUR until DAY_END_HOUR) {
            Row(Modifier.fillMaxWidth().height(HOUR_HEIGHT)) {
              Text(
                LocalTime.of(h % 24, 0).format(DateTimeFormatter.ofPattern("h a")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp).padding(start = 8.dp, top = 2.dp),
              )
              HorizontalDivider(
                Modifier.padding(top = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
              )
            }
          }
        }

        // now-line
        if (day == LocalDate.now() && now.hour in DAY_START_HOUR until DAY_END_HOUR) {
          val topMin = (now.hour * 60 + now.minute) - DAY_START_HOUR * 60
          Box(
            Modifier
              .fillMaxWidth()
              .padding(start = 52.dp)
              .offset(y = with(density) { (topMin / 60f * hourPx).toDp() })
              .height(2.dp)
              .background(MaterialTheme.colorScheme.error),
          )
        }

        scheduled.forEach { (task, times) ->
          TimelineBlock(
            task = task,
            startMin = times.first.hour * 60 + times.first.minute - DAY_START_HOUR * 60,
            durationMin = java.time.Duration.between(times.first, times.second).toMinutes().toInt().coerceAtLeast(SNAP_MIN),
            hourPx = hourPx,
            onCommit = { newStartMin, newDurMin ->
              val absStart = newStartMin + DAY_START_HOUR * 60
              viewModel.setTaskTimeBlock(task.id, isoAt(day, absStart), isoAt(day, absStart + newDurMin))
            },
            onOpen = { viewModel.openTaskDetail(task.id) },
          )
        }

        if (scheduled.isEmpty() && unscheduled.isEmpty()) {
          EmptyLine(
            "Nothing due on this day. Add tasks from your lists, then block time here.",
            Modifier.padding(TodayPad).padding(top = 24.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun TimelineBlock(
  task: Task,
  startMin: Int,
  durationMin: Int,
  hourPx: Float,
  onCommit: (startMin: Int, durationMin: Int) -> Unit,
  onOpen: () -> Unit,
) {
  val density = LocalDensity.current
  var dragOffsetPx by remember(task.id, startMin) { mutableStateOf(0f) }
  var extraHeightPx by remember(task.id, durationMin) { mutableStateOf(0f) }
  fun snap(min: Int) = (Math.round(min / SNAP_MIN.toFloat()) * SNAP_MIN)

  val topDp = with(density) { (startMin / 60f * hourPx + dragOffsetPx).toDp() }
  val heightDp = with(density) { (durationMin / 60f * hourPx + extraHeightPx).coerceAtLeast(24f).toDp() }

  Box(
    Modifier
      .fillMaxWidth()
      .padding(start = 56.dp, end = 8.dp)
      .offset(y = topDp)
      .height(heightDp)
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
      .pointerInput(task.id) {
        detectDragGestures(
          onDragEnd = {
            val newStart = snap(startMin + Math.round(dragOffsetPx / hourPx * 60)).coerceIn(0, (DAY_END_HOUR - DAY_START_HOUR) * 60 - SNAP_MIN)
            onCommit(newStart, durationMin)
            dragOffsetPx = 0f
          },
          onDragCancel = { dragOffsetPx = 0f },
        ) { change, drag -> change.consume(); dragOffsetPx += drag.y }
      }
      .clickable(onClick = onOpen),
  ) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
      Text(
        task.name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 2,
        fontWeight = FontWeight.Medium,
      )
      val s = LocalTime.ofSecondOfDay(((startMin + DAY_START_HOUR * 60) * 60L))
      val e = LocalTime.ofSecondOfDay(((startMin + DAY_START_HOUR * 60 + durationMin).coerceAtMost(24 * 60 - 1) * 60L))
      Text(
        "${s.format(DateTimeFormatter.ofPattern("h:mm"))}–${e.format(DateTimeFormatter.ofPattern("h:mm a"))}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
      )
    }
    // resize handle
    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(14.dp)
        .pointerInput(task.id) {
          detectDragGestures(
            onDragEnd = {
              val newDur = snap(durationMin + Math.round(extraHeightPx / hourPx * 60)).coerceAtLeast(SNAP_MIN)
              onCommit(startMin, newDur)
              extraHeightPx = 0f
            },
            onDragCancel = { extraHeightPx = 0f },
          ) { change, drag -> change.consume(); extraHeightPx += drag.y }
        },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier.width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
      )
    }
  }
}
