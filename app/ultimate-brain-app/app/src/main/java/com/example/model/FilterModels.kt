package com.example.model

import com.squareup.moshi.JsonClass

/** The list screens that carry a customizable filter bar. */
enum class FilterScope { TASKS, PROJECTS, NOTES, GOALS, TAGS, MILESTONES }

/** How the value of a clause is compared. Not every op is valid for every field. */
enum class FilterOp {
  IS, IS_NOT,
  CONTAINS, NOT_CONTAINS,
  IS_EMPTY, IS_NOT_EMPTY,
  IS_TRUE, IS_FALSE,
  BEFORE, AFTER, ON,
  WITHIN_DAYS,      // value = N; matches dates in [today, today+N]
  OVERDUE_BY,       // value = N; matches dates in [today-N, today-1]  (N blank = any past date)
  GT, LT,           // numeric
}

/** ALL clauses must match (AND) or ANY (OR). */
enum class MatchMode { ALL, ANY }

/** A single "<field> <op> <value>" condition. */
@JsonClass(generateAdapter = true)
data class FilterClause(
  val field: String,          // a key from FilterCatalog.fieldsFor(scope)
  val op: FilterOp,
  val value: String? = null,  // status name / project id / label text / N days / iso date …
)

@JsonClass(generateAdapter = true)
data class FilterSort(
  val field: String,
  val descending: Boolean = false,
)

/** A user-defined, named, persisted filter. */
@JsonClass(generateAdapter = true)
data class CustomFilter(
  val id: String,
  val scope: FilterScope,
  val name: String,
  val match: MatchMode = MatchMode.ALL,
  val clauses: List<FilterClause> = emptyList(),
  val sort: FilterSort? = null,
)

/**
 * Per-scope chip-row layout the user has arranged: which chips show, in what
 * order, and which is selected on a cold open. Keys are built-in enum names
 * (e.g. "TODAY") or "cf:<uuid>" for a [CustomFilter].
 */
@JsonClass(generateAdapter = true)
data class ChipRowConfig(
  val order: List<String> = emptyList(),   // empty → use the scope's natural order
  val hidden: Set<String> = emptySet(),    // built-in keys the user switched off
  val defaultKey: String? = null,          // null → first visible chip
) {
  companion object { const val CUSTOM_PREFIX = "cf:" }
}

fun customKey(id: String) = ChipRowConfig.CUSTOM_PREFIX + id
fun String.isCustomKey() = startsWith(ChipRowConfig.CUSTOM_PREFIX)
fun String.customId() = removePrefix(ChipRowConfig.CUSTOM_PREFIX)
