package com.example.data.hermes

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A chat conversation on the Hermes server. */
@JsonClass(generateAdapter = true)
data class HermesSession(
  val id: String,
  val title: String? = null,
  val source: String? = null,
  val model: String? = null,
  @Json(name = "message_count") val messageCount: Int = 0,
  @Json(name = "started_at") val startedAt: Double? = null,
  @Json(name = "last_active") val lastActive: Double? = null,
  @Json(name = "ended_at") val endedAt: Double? = null,
  val preview: String? = null,
)

@JsonClass(generateAdapter = true)
data class HermesSessionList(val data: List<HermesSession> = emptyList())

/** Wrapper Hermes returns from create / patch: {object, session:{...}} or the session inline. */
@JsonClass(generateAdapter = true)
data class HermesSessionEnvelope(
  val session: HermesSession? = null,
  val id: String? = null,
  val title: String? = null,
) {
  fun resolve(): HermesSession? = session ?: id?.let { HermesSession(id = it, title = title) }
}

@JsonClass(generateAdapter = true)
data class HermesMessage(
  val id: Long? = null,
  val role: String,
  val content: String? = null,
  @Json(name = "tool_name") val toolName: String? = null,
  @Json(name = "finish_reason") val finishReason: String? = null,
  val timestamp: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HermesMessageList(val data: List<HermesMessage> = emptyList())

/** One decoded frame from the /chat/stream SSE. */
sealed interface ChatEvent {
  data class RunStarted(val runId: String) : ChatEvent
  data class Delta(val text: String) : ChatEvent
  data class ToolProgress(val tool: String, val text: String) : ChatEvent
  data class ToolStarted(val tool: String, val preview: String?) : ChatEvent
  data class ToolCompleted(val tool: String) : ChatEvent
  data class ToolFailed(val tool: String) : ChatEvent
  data class ApprovalRequired(val runId: String, val summary: String) : ChatEvent
  data class Completed(val content: String) : ChatEvent
  data class Error(val message: String) : ChatEvent
  data object Done : ChatEvent
}

/** GET /v1/skills — a skill the API-server agent can invoke, surfaced in the "/" palette. */
@JsonClass(generateAdapter = true)
data class HermesSkill(
  val name: String,
  val description: String? = null,
  val category: String? = null,
)

@JsonClass(generateAdapter = true)
data class HermesSkillList(val data: List<HermesSkill> = emptyList())

/** One row from GET /api/jobs — a scheduled ("cron") Hermes prompt. */
@JsonClass(generateAdapter = true)
data class HermesJob(
  val id: String,
  val name: String,
  val prompt: String = "",
  @Json(name = "schedule_display") val scheduleDisplay: String? = null,
  val deliver: String? = null,
  val enabled: Boolean = true,
  val state: String? = null,
  @Json(name = "next_run_at") val nextRunAt: String? = null,
  @Json(name = "last_run_at") val lastRunAt: String? = null,
  @Json(name = "last_status") val lastStatus: String? = null,
)

@JsonClass(generateAdapter = true)
data class HermesJobList(val jobs: List<HermesJob> = emptyList())

@JsonClass(generateAdapter = true)
data class HermesJobEnvelope(val job: HermesJob? = null)
