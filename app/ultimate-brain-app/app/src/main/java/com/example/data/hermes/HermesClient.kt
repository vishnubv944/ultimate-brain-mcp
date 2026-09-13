package com.example.data.hermes

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One image attached to an outgoing message — already downscaled + base64-encoded. */
data class ImageAttachment(val base64: String, val mimeType: String = "image/jpeg")

/**
 * Thin HTTP layer over the Hermes API server (OpenAI-compatible + REST session
 * control). REST calls use Moshi; the chat turn is read as a raw SSE stream and
 * decoded into [ChatEvent]s.
 */
class HermesClient(
  private val baseUrl: String,
  private val apiKey: String,
) {
  private val json = "application/json".toMediaType()
  private val moshi = Moshi.Builder().build()

  private val http = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.MINUTES) // streaming turns can be long
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  private fun req(path: String) = Request.Builder()
    .url("$baseUrl$path")
    .header("Authorization", "Bearer $apiKey")

  // ---- REST -------------------------------------------------------------

  suspend fun health(): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      http.newCall(req("/health").build()).execute().use { it.isSuccessful }
    }.getOrDefault(false)
  }

  suspend fun listSessions(): List<HermesSession> = withContext(Dispatchers.IO) {
    http.newCall(req("/api/sessions").build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(errorMessage(body, resp.code))
      moshi.adapter(HermesSessionList::class.java).fromJson(body)?.data.orEmpty()
    }
  }

  suspend fun createSession(title: String?): HermesSession = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply { if (title != null) put("title", title) }.toString()
    http.newCall(req("/api/sessions").post(payload.toRequestBody(json)).build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(errorMessage(body, resp.code))
      moshi.adapter(HermesSessionEnvelope::class.java).fromJson(body)?.resolve()
        ?: throw HermesException("Malformed create-session response")
    }
  }

  suspend fun renameSession(id: String, title: String) = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("title", title).toString()
    http.newCall(req("/api/sessions/$id").patch(payload.toRequestBody(json)).build()).execute().use { resp ->
      if (!resp.isSuccessful) throw HermesException(errorMessage(resp.body?.string().orEmpty(), resp.code))
    }
  }

  suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
    http.newCall(req("/api/sessions/$id").delete().build()).execute().use { resp ->
      if (!resp.isSuccessful && resp.code != 404) {
        throw HermesException(errorMessage(resp.body?.string().orEmpty(), resp.code))
      }
    }
  }

  suspend fun messages(id: String): List<HermesMessage> = withContext(Dispatchers.IO) {
    http.newCall(req("/api/sessions/$id/messages").build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (resp.code == 404) return@withContext emptyList()
      if (!resp.isSuccessful) throw HermesException(errorMessage(body, resp.code))
      moshi.adapter(HermesMessageList::class.java).fromJson(body)?.data.orEmpty()
    }
  }

  suspend fun stopRun(runId: String) = withContext(Dispatchers.IO) {
    runCatching {
      http.newCall(req("/v1/runs/$runId/stop").post("".toRequestBody(json)).build()).execute().close()
    }
  }

  suspend fun resolveApproval(runId: String, approved: Boolean) = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("decision", if (approved) "approve" else "deny").toString()
    runCatching {
      http.newCall(req("/v1/runs/$runId/approval").post(payload.toRequestBody(json)).build()).execute().close()
    }
  }

  suspend fun forkSession(id: String, title: String? = null): HermesSession = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply { if (title != null) put("title", title) }.toString()
    http.newCall(req("/api/sessions/$id/fork").post(payload.toRequestBody(json)).build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(errorMessage(body, resp.code))
      moshi.adapter(HermesSessionEnvelope::class.java).fromJson(body)?.resolve()
        ?: throw HermesException("Malformed fork response")
    }
  }

  // ---- Discovery: skills, for the "/" palette ---------------------------

  suspend fun listSkills(): List<HermesSkill> = withContext(Dispatchers.IO) {
    http.newCall(req("/v1/skills").build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(errorMessage(body, resp.code))
      moshi.adapter(HermesSkillList::class.java).fromJson(body)?.data.orEmpty()
    }
  }

  // ---- Jobs (cron / "Routines") ------------------------------------------

  suspend fun listJobs(): List<HermesJob> = withContext(Dispatchers.IO) {
    http.newCall(req("/api/jobs").build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(jobErrorMessage(body, resp.code))
      moshi.adapter(HermesJobList::class.java).fromJson(body)?.jobs.orEmpty()
    }
  }

  suspend fun createJob(name: String, schedule: String, prompt: String): HermesJob =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().put("name", name).put("schedule", schedule).put("prompt", prompt).toString()
      http.newCall(req("/api/jobs").post(payload.toRequestBody(json)).build()).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw HermesException(jobErrorMessage(body, resp.code))
        moshi.adapter(HermesJobEnvelope::class.java).fromJson(body)?.job
          ?: throw HermesException("Malformed create-job response")
      }
    }

  suspend fun deleteJob(id: String) = withContext(Dispatchers.IO) {
    http.newCall(req("/api/jobs/$id").delete().build()).execute().use { resp ->
      if (!resp.isSuccessful && resp.code != 404) throw HermesException(jobErrorMessage(resp.body?.string().orEmpty(), resp.code))
    }
  }

  suspend fun pauseJob(id: String): HermesJob? = jobAction(id, "pause")
  suspend fun resumeJob(id: String): HermesJob? = jobAction(id, "resume")
  suspend fun runJobNow(id: String): HermesJob? = jobAction(id, "run")

  private suspend fun jobAction(id: String, action: String): HermesJob? = withContext(Dispatchers.IO) {
    http.newCall(req("/api/jobs/$id/$action").post("".toRequestBody(json)).build()).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) throw HermesException(jobErrorMessage(body, resp.code))
      moshi.adapter(HermesJobEnvelope::class.java).fromJson(body)?.job
    }
  }

  // ---- Streaming chat turn --------------------------------------------

  /**
   * Send [input] (with optional [images]) to [sessionId] and stream the turn.
   * Emits [ChatEvent]s until `done`/error. Cancelling the collector aborts
   * the HTTP call. When images are attached the message body becomes an
   * OpenAI-style content-part array (`text` + `image_url` data URLs) — the
   * same shape `/v1/chat/completions` accepts.
   */
  fun sendStream(sessionId: String, input: String, images: List<ImageAttachment> = emptyList()): Flow<ChatEvent> = callbackFlow {
    val messageValue: Any = if (images.isEmpty()) {
      input
    } else {
      JSONArray().apply {
        if (input.isNotBlank()) put(JSONObject().put("type", "text").put("text", input))
        images.forEach { img ->
          put(
            JSONObject().put("type", "image_url").put(
              "image_url",
              JSONObject().put("url", "data:${img.mimeType};base64,${img.base64}"),
            ),
          )
        }
      }
    }
    val payload = JSONObject().put("input", messageValue).toString()
    val call = http.newCall(
      req("/api/sessions/$sessionId/chat/stream")
        .header("Accept", "text/event-stream")
        .post(payload.toRequestBody(json))
        .build(),
    )
    val worker = Thread {
      try {
        call.execute().use { resp ->
          if (!resp.isSuccessful) {
            trySend(ChatEvent.Error(errorMessage(resp.body?.string().orEmpty(), resp.code)))
            close(); return@use
          }
          val src = resp.body?.source() ?: run { close(); return@use }
          var event = ""
          val data = StringBuilder()
          while (!src.exhausted()) {
            val line = src.readUtf8Line() ?: break
            when {
              line.startsWith("event:") -> event = line.substring(6).trim()
              line.startsWith("data:") -> data.append(line.substring(5).trim())
              line.isEmpty() -> {
                if (event.isNotEmpty()) decode(event, data.toString())?.let { trySend(it) }
                event = ""; data.setLength(0)
              }
            }
          }
          trySend(ChatEvent.Done)
        }
      } catch (e: Exception) {
        if (!call.isCanceled()) trySend(ChatEvent.Error(e.message ?: "Stream failed"))
      } finally {
        close()
      }
    }
    worker.start()
    awaitClose { call.cancel() }
  }.flowOn(Dispatchers.IO)

  private fun decode(event: String, data: String): ChatEvent? {
    val o = runCatching { JSONObject(data) }.getOrNull() ?: return null
    return when (event) {
      "run.started" -> ChatEvent.RunStarted(o.optString("run_id"))
      "assistant.delta" -> ChatEvent.Delta(o.optString("delta"))
      "tool.progress" -> ChatEvent.ToolProgress(o.optString("tool_name", "tool"), o.optString("delta"))
      "tool.started" -> ChatEvent.ToolStarted(o.optString("tool_name", "tool"), o.optString("preview").takeIf { it.isNotBlank() })
      "tool.completed" -> ChatEvent.ToolCompleted(o.optString("tool_name", "tool"))
      "tool.failed" -> ChatEvent.ToolFailed(o.optString("tool_name", "tool"))
      "approval.required", "run.approval_required" ->
        ChatEvent.ApprovalRequired(o.optString("run_id"), o.optString("summary", o.optString("tool_name", "a tool")))
      "assistant.completed" -> ChatEvent.Completed(o.optString("content"))
      "run.completed" -> {
        val msgs = o.optJSONArray("messages")
        val text = (0 until (msgs?.length() ?: 0))
          .mapNotNull { msgs?.optJSONObject(it)?.optString("content") }
          .lastOrNull { it.isNotBlank() }
        if (text != null) ChatEvent.Completed(text) else null
      }
      "error", "run.error" -> ChatEvent.Error(o.optString("message", "Run failed"))
      "done" -> ChatEvent.Done
      else -> null
    }
  }

  private fun errorMessage(body: String, code: Int): String =
    runCatching { JSONObject(body).getJSONObject("error").getString("message") }
      .getOrNull() ?: "HTTP $code"

  /** The Jobs API returns `{"error": "text"}` (flat string), not the nested OpenAI shape. */
  private fun jobErrorMessage(body: String, code: Int): String =
    runCatching { JSONObject(body).getString("error") }.getOrNull()
      ?: errorMessage(body, code)
}

class HermesException(message: String) : Exception(message)
