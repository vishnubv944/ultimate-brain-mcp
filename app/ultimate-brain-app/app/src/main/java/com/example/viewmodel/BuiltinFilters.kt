package com.example.viewmodel

import com.example.data.DateUtils
import com.example.model.FilterScope
import com.example.model.GoalModel
import com.example.model.MilestoneModel
import com.example.model.NoteModel
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.model.TagModel
import com.example.model.Task
import com.example.model.TaskStatus
import java.time.LocalDate

/**
 * The stock filter "views" each list screen ships with. A key is a stable
 * string; beyond a match predicate it can also carry a sort (see [*Sort]) and,
 * for a grouped view, a per-row group name (see [*Group]) that the screen
 * renders as an accordion. User [com.example.model.CustomFilter]s still layer
 * in alongside these via [com.example.model.ChipRowConfig].
 */
object BuiltinFilters {

  fun keys(scope: FilterScope): List<String> = when (scope) {
    FilterScope.TASKS -> listOf(
      "INBOX", "TODAY", "ACTIVE_PROJECTS", "WEEK", "MONTH", "OVERDUE",
      "SCHEDULED", "RECURRING", "NO_DUE", "ALL_PROJECTS", "DO_NEXT", "ALL", "DONE",
    )
    FilterScope.PROJECTS -> listOf("ALL", "PLANNED", "ON_HOLD", "DOING", "ONGOING", "DONE", "ARCHIVED")
    FilterScope.NOTES -> listOf("INBOX", "NOTES", "FAV", "CLIPS", "VOICE", "JOURNAL", "MEETINGS", "ALL")
    FilterScope.GOALS -> listOf("BY_ACTIVITY", "BY_DEADLINE", "BY_TAG", "ACHIEVED", "DROPPED")
    FilterScope.TAGS -> listOf("FAV", "A_Z", "TYPES")
    FilterScope.MILESTONES -> listOf("ALL", "IN_PROGRESS", "COMPLETED", "PENDING")
  }

  /**
   * The chip a screen cold-opens on when the user hasn't chosen one. Kept
   * independent of chip *display* order — Inbox leads the row everywhere
   * now, but Tasks/Notes should still land on Today/Notes, not Inbox.
   */
  fun defaultKey(scope: FilterScope): String = when (scope) {
    FilterScope.TASKS -> "TODAY"
    FilterScope.NOTES -> "NOTES"
    FilterScope.TAGS -> "A_Z"          // "Fav" is usually empty
    else -> keys(scope).first()
  }

  fun label(key: String): String = when (key) {
    "ALL" -> "All"
    "MY_DAY" -> "My Day"
    "TODAY" -> "Today"
    "INBOX" -> "Inbox"
    "WEEK" -> "Week"
    "MONTH" -> "Month"
    "SCHEDULED" -> "Scheduled"
    "NO_DUE" -> "No due"
    "ACTIVE_PROJECTS" -> "Active projects"
    "ALL_PROJECTS" -> "All projects"
    "DO_NEXT" -> "Do next"
    "OVERDUE" -> "Overdue"
    "HIGH_PRIORITY" -> "High priority"
    "RECURRING" -> "Recurring"
    "DONE" -> "Done"
    "ACTIVE" -> "Active"
    "PLANNED" -> "Planned"
    "ON_HOLD" -> "On hold"
    "DOING" -> "Doing"
    "ONGOING" -> "Ongoing"
    "ARCHIVED" -> "Archived"
    "NOTES" -> "Notes"
    "FAV" -> "Fav"
    "CLIPS" -> "Clips"
    "VOICE" -> "Voice"
    "JOURNAL" -> "Journal"
    "MEETINGS" -> "Meetings"
    "MEETING" -> "Meeting"
    "REFERENCE" -> "Reference"
    "IDEA" -> "Idea"
    "BOOK" -> "Book"
    "BY_ACTIVITY" -> "By activity"
    "BY_DEADLINE" -> "By deadline"
    "BY_TAG" -> "By tag"
    "ACHIEVED" -> "Achieved"
    "DROPPED" -> "Dropped"
    "A_Z" -> "A–Z"
    "TYPES" -> "Types"
    "AREAS" -> "Areas"
    "RESOURCES" -> "Resources"
    "ENTITIES" -> "Entities"
    "IN_PROGRESS" -> "In progress"
    "COMPLETED" -> "Completed"
    "PENDING" -> "Pending"
    else -> key
  }

  // ---- Tasks ---------------------------------------------------------------

  fun taskMatches(key: String, t: Task): Boolean {
    if (key == "DONE") return t.isDone
    if (t.isDone) return false
    val today = LocalDate.now()
    val date = DateUtils.parseIsoDate(t.due)
    return when (key) {
      "ALL" -> true
      "MY_DAY" -> t.isMyDay
      "TODAY" -> date == today
      "INBOX" -> t.projectId == null && t.due == null
      "WEEK" -> date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(7))
      "MONTH" -> date != null && java.time.YearMonth.from(date) == java.time.YearMonth.from(today)
      "SCHEDULED" -> t.due != null
      "NO_DUE" -> t.due == null
      "ALL_PROJECTS" -> t.projectId != null
      // ACTIVE_PROJECTS needs the live project list; MyDayUiState.tasksMatching
      // resolves it. This fallback keeps custom-filter / count paths sane.
      "ACTIVE_PROJECTS" -> t.projectId != null
      "DO_NEXT" -> t.parentTaskId == null &&
        t.snoozeIso.futureDate(today) == null &&
        t.waitIso.futureDate(today) == null &&
        (date == null || !date.isAfter(today))
      "OVERDUE" -> date != null && date.isBefore(today)
      "HIGH_PRIORITY" -> t.priority == Priority.HIGH
      "RECURRING" -> t.isRecurring
      else -> true
    }
  }

  private fun String?.futureDate(today: LocalDate): LocalDate? =
    this?.substringBefore('T')
      ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
      ?.takeIf { it.isAfter(today) }

  // ---- Projects -----------------------------------------------------------

  fun projectMatches(key: String, p: ProjectModel): Boolean = when (key) {
    "ALL", "ACTIVE" -> !p.isArchived
    "PLANNED" -> !p.isArchived && p.status == "Planned"
    "ON_HOLD" -> !p.isArchived && p.status == "On Hold"
    "DOING" -> !p.isArchived && p.status == "Doing"
    "ONGOING" -> !p.isArchived && p.status == "Ongoing"
    "DONE" -> !p.isArchived && p.status == "Done"
    "ARCHIVED" -> p.isArchived
    else -> !p.isArchived
  }

  /** Group header for the grouped "All" projects view; null = not grouped. */
  fun projectGroup(key: String, p: ProjectModel): String? =
    if (key != "ALL") null else when (p.status) {
      "Planned", "On Hold" -> "To-do"
      "Doing", "Ongoing" -> "In progress"
      "Done" -> "Complete"
      else -> "Other"
    }

  val PROJECT_GROUP_ORDER = listOf("In progress", "To-do", "Complete", "Other")

  // ---- Notes -------------------------------------------------------------

  fun noteMatches(key: String, n: NoteModel): Boolean = when (key) {
    "NOTES", "ALL" -> true
    "INBOX" -> n.projectName.isNullOrBlank()
    "FAV" -> n.isFavorite
    "CLIPS" -> n.type.equals("Web Clip", ignoreCase = true) || n.type.equals("Clip", ignoreCase = true)
    "VOICE" -> n.type.equals("Voice Note", ignoreCase = true) || n.type.equals("Voice", ignoreCase = true)
    "JOURNAL" -> n.type.equals("Journal", ignoreCase = true) || n.type.equals("Daily", ignoreCase = true)
    "MEETINGS" -> n.type.equals("Meeting", ignoreCase = true)
    else -> n.type.equals(label(key), ignoreCase = true)
  }

  // ---- Goals -----------------------------------------------------------

  fun goalMatches(key: String, g: GoalModel): Boolean = when (key) {
    "BY_ACTIVITY", "BY_DEADLINE", "BY_TAG", "ACTIVE" ->
      !g.isArchived && (g.status == "Active" || g.status == "Dream")
    "ACHIEVED" -> !g.isArchived && g.status == "Achieved"
    "DROPPED" -> g.isArchived
    else -> !g.isArchived
  }

  fun goalSort(key: String): Comparator<GoalModel>? = when (key) {
    "BY_DEADLINE" -> compareBy({ it.deadlineIso ?: "9999-12-31" }, { it.name.lowercase() })
    "BY_TAG" -> compareBy({ it.tagArea.lowercase() }, { it.name.lowercase() })
    "BY_ACTIVITY" -> compareByDescending<GoalModel> { it.goalSetIso ?: "" }.thenBy { it.name.lowercase() }
    else -> null
  }

  fun goalGroup(key: String, g: GoalModel): String? =
    if (key != "BY_TAG") null else g.tagArea.ifBlank { "No area" }

  // ---- Tags ----------------------------------------------------------

  fun tagMatches(key: String, tag: TagModel): Boolean = when (key) {
    "FAV" -> tag.isFavorite
    "A_Z", "TYPES", "ALL" -> true
    "AREAS" -> tag.type == "Area"
    "RESOURCES" -> tag.type == "Resource"
    "ENTITIES" -> tag.type == "Entity"
    else -> true
  }

  fun tagSort(key: String): Comparator<TagModel>? = when (key) {
    "A_Z", "TYPES", "FAV" -> compareBy { it.name.lowercase() }
    else -> null
  }

  fun tagGroup(key: String, tag: TagModel): String? =
    if (key != "TYPES") null else tag.type.ifBlank { "Other" }

  val TAG_GROUP_ORDER = listOf("Area", "Resource", "Entity", "Other")

  // ---- Milestones --------------------------------------------------

  fun milestoneMatches(key: String, m: MilestoneModel): Boolean = when (key) {
    "IN_PROGRESS" -> m.status == "In Progress"
    "COMPLETED" -> m.status == "Completed"
    "PENDING" -> m.status == "Pending"
    else -> true
  }
}
