package com.example.domain

import com.example.model.CustomFilter
import com.example.model.FilterClause
import com.example.model.FilterOp
import com.example.model.FilterScope
import com.example.model.FilterSort
import com.example.model.GoalModel
import com.example.model.MatchMode
import com.example.model.MilestoneModel
import com.example.model.NoteModel
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.model.TagModel
import com.example.model.Task
import com.example.model.TaskStatus
import java.time.LocalDate

/** A typed value pulled out of an entity for one filter field. */
sealed interface FVal {
  data class Txt(val s: String?) : FVal
  data class Bool(val b: Boolean) : FVal
  data class Num(val n: Double?) : FVal
  data class DateV(val iso: String?) : FVal
  data class Multi(val items: List<String>) : FVal
}

enum class FieldKind { TEXT, SELECT, BOOL, DATE, NUMBER, MULTI }

data class FilterFieldDef(
  val key: String,
  val label: String,
  val kind: FieldKind,
  val ops: List<FilterOp>,
  /** For SELECT / MULTI: which option set the editor should offer (see uiState.optionsFor). */
  val optionsKey: String? = null,
)

private val TEXT_OPS = listOf(FilterOp.CONTAINS, FilterOp.NOT_CONTAINS, FilterOp.IS, FilterOp.IS_NOT)
private val SELECT_OPS = listOf(FilterOp.IS, FilterOp.IS_NOT, FilterOp.IS_EMPTY, FilterOp.IS_NOT_EMPTY)
private val BOOL_OPS = listOf(FilterOp.IS_TRUE, FilterOp.IS_FALSE)
private val DATE_OPS = listOf(
  FilterOp.ON, FilterOp.BEFORE, FilterOp.AFTER, FilterOp.WITHIN_DAYS,
  FilterOp.OVERDUE_BY, FilterOp.IS_EMPTY, FilterOp.IS_NOT_EMPTY,
)
private val MULTI_OPS = listOf(FilterOp.CONTAINS, FilterOp.NOT_CONTAINS, FilterOp.IS_EMPTY, FilterOp.IS_NOT_EMPTY)
private val NUM_OPS = listOf(FilterOp.IS, FilterOp.GT, FilterOp.LT)

/** The fields a custom filter can be built from, per screen. */
object FilterCatalog {
  fun fieldsFor(scope: FilterScope): List<FilterFieldDef> = when (scope) {
    FilterScope.TASKS -> listOf(
      FilterFieldDef("status", "Status", FieldKind.SELECT, SELECT_OPS, "task.Status"),
      FilterFieldDef("priority", "Priority", FieldKind.SELECT, SELECT_OPS, "task.Priority"),
      FilterFieldDef("due", "Due date", FieldKind.DATE, DATE_OPS),
      FilterFieldDef("myDay", "My Day", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("recurring", "Recurring", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("done", "Completed", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("project", "Project", FieldKind.SELECT, SELECT_OPS, "task.Project"),
      FilterFieldDef("label", "Label", FieldKind.MULTI, MULTI_OPS, "task.Labels"),
      FilterFieldDef("energy", "Energy", FieldKind.SELECT, SELECT_OPS, "task.Energy"),
      FilterFieldDef("name", "Name", FieldKind.TEXT, TEXT_OPS),
    )
    FilterScope.PROJECTS -> listOf(
      FilterFieldDef("status", "Status", FieldKind.SELECT, SELECT_OPS, "project.Status"),
      FilterFieldDef("archived", "Archived", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("hasOpenTasks", "Has open tasks", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("goal", "Goal", FieldKind.TEXT, TEXT_OPS),
      FilterFieldDef("deadline", "Deadline", FieldKind.DATE, DATE_OPS),
      FilterFieldDef("name", "Name", FieldKind.TEXT, TEXT_OPS),
    )
    FilterScope.NOTES -> listOf(
      FilterFieldDef("type", "Type", FieldKind.SELECT, SELECT_OPS, "note.Type"),
      FilterFieldDef("favorite", "Favorite", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("hasUrl", "Has URL", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("tag", "Tag", FieldKind.MULTI, MULTI_OPS, "note.Tags"),
      FilterFieldDef("date", "Note date", FieldKind.DATE, DATE_OPS),
      FilterFieldDef("name", "Title", FieldKind.TEXT, TEXT_OPS),
    )
    FilterScope.GOALS -> listOf(
      FilterFieldDef("status", "Status", FieldKind.SELECT, SELECT_OPS, "goal.Status"),
      FilterFieldDef("archived", "Archived", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("deadline", "Deadline", FieldKind.DATE, DATE_OPS),
      FilterFieldDef("name", "Name", FieldKind.TEXT, TEXT_OPS),
    )
    FilterScope.TAGS -> listOf(
      FilterFieldDef("type", "Type", FieldKind.SELECT, SELECT_OPS, "tag.Type"),
      FilterFieldDef("favorite", "Favorite", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("hasParent", "Has parent", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("name", "Name", FieldKind.TEXT, TEXT_OPS),
    )
    FilterScope.MILESTONES -> listOf(
      FilterFieldDef("completed", "Completed", FieldKind.BOOL, BOOL_OPS),
      FilterFieldDef("goal", "Goal", FieldKind.TEXT, TEXT_OPS),
      FilterFieldDef("target", "Target date", FieldKind.DATE, DATE_OPS),
      FilterFieldDef("name", "Name", FieldKind.TEXT, TEXT_OPS),
    )
  }

  fun fieldDef(scope: FilterScope, key: String): FilterFieldDef? = fieldsFor(scope).firstOrNull { it.key == key }
}

// --- entity → row projections ------------------------------------------------

fun Task.filterRow(): Map<String, FVal> = mapOf(
  "status" to FVal.Txt(
    when (status) { TaskStatus.TODO -> "To Do"; TaskStatus.DOING -> "Doing"; TaskStatus.DONE -> "Done" },
  ),
  "priority" to FVal.Txt(priority?.let { it.name.lowercase().replaceFirstChar(Char::uppercase) }),
  "due" to FVal.DateV(due),
  "myDay" to FVal.Bool(isMyDay),
  "recurring" to FVal.Bool(isRecurring),
  "done" to FVal.Bool(isDone),
  "project" to FVal.Txt(projectName?.takeIf { it.isNotBlank() }),
  "label" to FVal.Multi(labels),
  "energy" to FVal.Txt(energy),
  "name" to FVal.Txt(name),
)

fun ProjectModel.filterRow(): Map<String, FVal> = mapOf(
  "status" to FVal.Txt(status),
  "archived" to FVal.Bool(isArchived),
  "hasOpenTasks" to FVal.Bool((totalTasks - doneTasks) > 0),
  "goal" to FVal.Txt(goalName),
  "deadline" to FVal.DateV(deadlineIso),
  "name" to FVal.Txt(name),
)

fun NoteModel.filterRow(): Map<String, FVal> = mapOf(
  "type" to FVal.Txt(type),
  "favorite" to FVal.Bool(isFavorite),
  "hasUrl" to FVal.Bool(url.isNotBlank()),
  "tag" to FVal.Multi(tags),
  "date" to FVal.DateV(dateIso),
  "name" to FVal.Txt(title),
)

fun GoalModel.filterRow(): Map<String, FVal> = mapOf(
  "status" to FVal.Txt(status),
  "archived" to FVal.Bool(isArchived),
  "deadline" to FVal.DateV(deadlineIso),
  "name" to FVal.Txt(name),
)

fun TagModel.filterRow(): Map<String, FVal> = mapOf(
  "type" to FVal.Txt(type),
  "favorite" to FVal.Bool(isFavorite),
  "hasParent" to FVal.Bool(parentId != null),
  "name" to FVal.Txt(name),
)

fun MilestoneModel.filterRow(): Map<String, FVal> = mapOf(
  "completed" to FVal.Bool(status.equals("Completed", ignoreCase = true)),
  "goal" to FVal.Txt(goalName),
  "target" to FVal.DateV(targetDateIso),
  "name" to FVal.Txt(name),
)

// --- evaluation -------------------------------------------------------------

object FilterEngine {
  fun matches(row: Map<String, FVal>, filter: CustomFilter, today: LocalDate = LocalDate.now()): Boolean {
    if (filter.clauses.isEmpty()) return true
    val res = filter.clauses.map { evalClause(row[it.field], it, today) }
    return if (filter.match == MatchMode.ANY) res.any { it } else res.all { it }
  }

  private fun parseDate(s: String?): LocalDate? =
    s?.substringBefore('T')?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

  private fun evalClause(v: FVal?, c: FilterClause, today: LocalDate): Boolean {
    val target = c.value?.trim().orEmpty()
    return when (v) {
      null -> c.op == FilterOp.IS_EMPTY
      is FVal.Txt -> {
        val s = v.s
        when (c.op) {
          FilterOp.IS -> s.equals(target, ignoreCase = true)
          FilterOp.IS_NOT -> !s.equals(target, ignoreCase = true)
          FilterOp.CONTAINS -> s?.contains(target, ignoreCase = true) == true
          FilterOp.NOT_CONTAINS -> s?.contains(target, ignoreCase = true) != true
          FilterOp.IS_EMPTY -> s.isNullOrBlank()
          FilterOp.IS_NOT_EMPTY -> !s.isNullOrBlank()
          else -> false
        }
      }
      is FVal.Bool -> when (c.op) {
        FilterOp.IS_TRUE -> v.b
        FilterOp.IS_FALSE -> !v.b
        else -> false
      }
      is FVal.Multi -> {
        val items = v.items
        when (c.op) {
          FilterOp.CONTAINS -> items.any { it.equals(target, ignoreCase = true) }
          FilterOp.NOT_CONTAINS -> items.none { it.equals(target, ignoreCase = true) }
          FilterOp.IS_EMPTY -> items.isEmpty()
          FilterOp.IS_NOT_EMPTY -> items.isNotEmpty()
          else -> false
        }
      }
      is FVal.Num -> {
        val n = v.n
        val t = target.toDoubleOrNull()
        when (c.op) {
          FilterOp.IS -> n != null && t != null && n == t
          FilterOp.GT -> n != null && t != null && n > t
          FilterOp.LT -> n != null && t != null && n < t
          else -> false
        }
      }
      is FVal.DateV -> {
        val d = parseDate(v.iso)
        when (c.op) {
          FilterOp.IS_EMPTY -> d == null
          FilterOp.IS_NOT_EMPTY -> d != null
          FilterOp.ON -> d != null && d == parseDate(target)
          FilterOp.BEFORE -> d != null && parseDate(target)?.let { d.isBefore(it) } == true
          FilterOp.AFTER -> d != null && parseDate(target)?.let { d.isAfter(it) } == true
          FilterOp.WITHIN_DAYS -> {
            val n = target.toLongOrNull() ?: return false
            d != null && !d.isBefore(today) && !d.isAfter(today.plusDays(n))
          }
          FilterOp.OVERDUE_BY -> {
            if (d == null) return false
            val n = target.toLongOrNull()
            if (n == null) d.isBefore(today) else !d.isBefore(today.minusDays(n)) && d.isBefore(today)
          }
          else -> false
        }
      }
    }
  }

  /** Sort [items] by a custom-filter sort spec, using each item's projected row. */
  fun <T> sorted(items: List<T>, sort: FilterSort?, rowOf: (T) -> Map<String, FVal>): List<T> {
    sort ?: return items
    val cmp = compareBy<T> { sortKey(rowOf(it)[sort.field]) }
    return items.sortedWith(if (sort.descending) cmp.reversed() else cmp)
  }

  /** A single string key that orders every FVal kind sensibly enough for a chip list. */
  private fun sortKey(v: FVal?): String = when (v) {
    is FVal.Txt -> (v.s ?: "￿").lowercase()
    is FVal.Bool -> if (v.b) "0" else "1"
    is FVal.Num -> "%020.4f".format(v.n ?: Double.MAX_VALUE)
    is FVal.DateV -> parseDate(v.iso)?.toString() ?: "9999-99-99"
    is FVal.Multi -> (v.items.firstOrNull() ?: "￿").lowercase()
    null -> "￿"
  }
}
