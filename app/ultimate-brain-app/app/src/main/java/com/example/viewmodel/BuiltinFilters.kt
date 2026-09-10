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

/**
 * The stock filter chips each list screen ships with. Keys are stable strings
 * (mostly the old per-screen enum names) so a [com.example.model.ChipRowConfig]
 * can reorder / hide them and mix them with user [com.example.model.CustomFilter]s.
 */
object BuiltinFilters {

  fun keys(scope: FilterScope): List<String> = when (scope) {
    FilterScope.TASKS -> listOf(
      "TODAY", "INBOX", "WEEK", "MONTH", "SCHEDULED", "NO_DUE", "RECURRING", "ACTIVE_PROJECTS", "ALL", "DONE",
    )
    FilterScope.PROJECTS -> listOf("ALL", "ACTIVE", "DOING", "DONE", "ARCHIVED")
    FilterScope.NOTES -> listOf("ALL", "MEETING", "REFERENCE", "IDEA", "JOURNAL", "BOOK")
    FilterScope.GOALS -> listOf("ACTIVE", "ACHIEVED", "DROPPED")
    FilterScope.TAGS -> listOf("ALL", "AREAS", "RESOURCES", "ENTITIES")
    FilterScope.MILESTONES -> listOf("ALL", "IN_PROGRESS", "COMPLETED", "PENDING")
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
    "OVERDUE" -> "Overdue"
    "HIGH_PRIORITY" -> "High priority"
    "RECURRING" -> "Recurring"
    "DONE" -> "Done"
    "ACTIVE" -> "Active"
    "DOING" -> "Doing"
    "ARCHIVED" -> "Archived"
    "MEETING" -> "Meeting"
    "REFERENCE" -> "Reference"
    "IDEA" -> "Idea"
    "JOURNAL" -> "Journal"
    "BOOK" -> "Book"
    "ACHIEVED" -> "Achieved"
    "DROPPED" -> "Archived"
    "AREAS" -> "Areas"
    "RESOURCES" -> "Resources"
    "ENTITIES" -> "Entities"
    "IN_PROGRESS" -> "In progress"
    "COMPLETED" -> "Completed"
    "PENDING" -> "Pending"
    else -> key
  }

  fun taskMatches(key: String, t: Task): Boolean {
    if (key == "DONE") return t.isDone
    if (t.isDone) return false
    val today = java.time.LocalDate.now()
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
      // ACTIVE_PROJECTS needs the live project list; MyDayUiState.tasksMatching
      // handles it. This fallback keeps custom-filter / count paths sane.
      "ACTIVE_PROJECTS" -> t.projectId != null
      "OVERDUE" -> date != null && date.isBefore(today)
      "HIGH_PRIORITY" -> t.priority == Priority.HIGH
      "RECURRING" -> t.isRecurring
      else -> true
    }
  }

  fun projectMatches(key: String, p: ProjectModel): Boolean = when (key) {
    "ALL" -> !p.isArchived
    "ACTIVE" -> !p.isArchived && p.status != "Done"
    "DOING" -> !p.isArchived && p.status == "Doing"
    "DONE" -> !p.isArchived && p.status == "Done"
    "ARCHIVED" -> p.isArchived
    else -> true
  }

  fun noteMatches(key: String, n: NoteModel): Boolean = when (key) {
    "ALL" -> true
    else -> n.type.equals(label(key), ignoreCase = true)
  }

  fun goalMatches(key: String, g: GoalModel): Boolean = when (key) {
    "ACTIVE" -> !g.isArchived && (g.status == "Active" || g.status == "Dream")
    "ACHIEVED" -> !g.isArchived && g.status == "Achieved"
    "DROPPED" -> g.isArchived
    else -> true
  }

  fun tagMatches(key: String, tag: TagModel): Boolean = when (key) {
    "AREAS" -> tag.type == "Area"
    "RESOURCES" -> tag.type == "Resource"
    "ENTITIES" -> tag.type == "Entity"
    else -> true
  }

  fun milestoneMatches(key: String, m: MilestoneModel): Boolean = when (key) {
    "IN_PROGRESS" -> m.status == "In Progress"
    "COMPLETED" -> m.status == "Completed"
    "PENDING" -> m.status == "Pending"
    else -> true
  }
}
