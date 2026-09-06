package com.example.data.notion

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A single page (row) returned by a data-source query or page GET. */
@JsonClass(generateAdapter = true)
data class NotionPage(
  val id: String = "",
  val url: String? = null,
  val archived: Boolean = false,
  @Json(name = "in_trash") val inTrash: Boolean = false,
  val properties: Map<String, NotionProperty> = emptyMap(),
)

/**
 * Union of every property shape we read. Every field is nullable; `type` tells
 * you which one is populated. Mirrors `formatters.py` extractors.
 */
@JsonClass(generateAdapter = true)
data class NotionProperty(
  val id: String? = null,
  val type: String? = null,
  val title: List<RichText>? = null,
  @Json(name = "rich_text") val richText: List<RichText>? = null,
  val select: NotionOption? = null,
  val status: NotionOption? = null,
  @Json(name = "multi_select") val multiSelect: List<NotionOption>? = null,
  val date: NotionDate? = null,
  val checkbox: Boolean? = null,
  val number: Double? = null,
  val url: String? = null,
  val email: String? = null,
  @Json(name = "phone_number") val phoneNumber: String? = null,
  val relation: List<NotionRef>? = null,
  @Json(name = "has_more") val hasMore: Boolean? = null,
  val formula: NotionFormula? = null,
  val people: List<NotionRef>? = null,
)

@JsonClass(generateAdapter = true)
data class RichText(
  @Json(name = "plain_text") val plainText: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotionOption(
  val name: String? = null,
  val color: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotionDate(
  val start: String? = null,
  val end: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotionRef(
  val id: String? = null,
  val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotionFormula(
  val type: String? = null,
  val string: String? = null,
  val number: Double? = null,
  val boolean: Boolean? = null,
  val date: NotionDate? = null,
)

@JsonClass(generateAdapter = true)
data class DataSourceSchema(
  val properties: Map<String, SchemaProperty> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
  val id: String? = null,
  val type: String? = null,
  val select: SchemaOptions? = null,
  val status: SchemaOptions? = null,
  @Json(name = "multi_select") val multiSelect: SchemaOptions? = null,
) {
  fun optionNames(): List<String> = when (type) {
    "select" -> select?.options
    "status" -> status?.options
    "multi_select" -> multiSelect?.options
    else -> null
  }.orEmpty().mapNotNull { it.name }
}

@JsonClass(generateAdapter = true)
data class SchemaOptions(val options: List<NotionOption> = emptyList())

@JsonClass(generateAdapter = true)
data class MarkdownResponse(
  val markdown: String? = null,
  val truncated: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class QueryResponse(
  val results: List<NotionPage> = emptyList(),
  @Json(name = "has_more") val hasMore: Boolean = false,
  @Json(name = "next_cursor") val nextCursor: String? = null,
)

// --- convenience accessors ---------------------------------------------------

fun NotionProperty.plainTitle(): String =
  (title ?: richText).orEmpty().joinToString("") { it.plainText.orEmpty() }

fun NotionProperty.selectName(): String? = select?.name ?: status?.name

fun NotionProperty.multiNames(): List<String> = multiSelect.orEmpty().mapNotNull { it.name }

fun NotionProperty.relationIds(): List<String> = relation.orEmpty().mapNotNull { it.id }

fun NotionProperty.dateStart(): String? = date?.start

fun NotionProperty.isChecked(): Boolean = checkbox == true

fun NotionProperty.formulaValue(): Any? = when (formula?.type) {
  "string" -> formula.string
  "number" -> formula.number
  "boolean" -> formula.boolean
  "date" -> formula.date?.start
  else -> null
}

fun Map<String, NotionProperty>.prop(vararg names: String): NotionProperty? {
  for (n in names) this[n]?.let { return it }
  return null
}
