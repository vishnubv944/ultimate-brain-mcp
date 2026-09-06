package com.example.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Date bucketing for tasks. The Task model carries both `due` (ISO 8601, "yyyy-MM-dd")
 * and `dueDisplay` (human-readable, e.g. "Today", "5:00 PM"). All bucketing must run on
 * `due` — never string-match on `dueDisplay`.
 */
object DateUtils {

  /**
   * Parse an ISO `yyyy-MM-dd` deadline. Returns null on bad input or empty string —
   * callers must treat null as "no date" rather than bucket it into Today.
   */
  fun parseIsoDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
      LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
      null
    }
  }

  enum class DueBucket {
    OVERDUE,
    TODAY,
    TOMORROW,
    UPCOMING,
    NONE,       // no due date
  }

  /**
   * Classify a task by due date relative to today. `today` is injectable so tests can pin it.
   * A task with no due date returns NONE — never falls into Today.
   */
  fun bucket(dueIso: String?, today: LocalDate = LocalDate.now()): DueBucket {
    val date = parseIsoDate(dueIso) ?: return DueBucket.NONE
    return when {
      date.isBefore(today) -> DueBucket.OVERDUE
      date == today -> DueBucket.TODAY
      date == today.plusDays(1) -> DueBucket.TOMORROW
      else -> DueBucket.UPCOMING
    }
  }

  /**
   * Human-readable label for a due date. Replaces the hard-coded "Today"/"Tomorrow"/"Sep 10"
   * patterns that were being string-matched elsewhere.
   *
   *  - today            → "Today"
   *  - tomorrow         → "Tomorrow"
   *  - within 7 days    → "Wed" (short weekday)
   *  - same year        → "Sep 30"
   *  - other            → "Sep 30, 2027"
   */
  fun displayLabel(dueIso: String?, today: LocalDate = LocalDate.now()): String {
    val date = parseIsoDate(dueIso?.substringBefore('T')) ?: return ""
    return when {
      date == today -> "Today"
      date == today.plusDays(1) -> "Tomorrow"
      date == today.minusDays(1) -> "Yesterday"
      // Upcoming within a week → short weekday. Past dates never do this
      // (a bare "Sat" reads like the future) — they get a real date.
      date.isAfter(today) && date.isBefore(today.plusDays(7)) ->
        date.format(DateTimeFormatter.ofPattern("EEE"))
      date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
      else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
  }
}
