package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hermes.ChatEvent
import com.example.data.hermes.HermesConfig
import com.example.data.hermes.HermesRepository
import com.example.data.hermes.HermesSession
import com.example.data.hermes.HermesSkill
import com.example.data.hermes.ImageAttachment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ToolStatus { RUNNING, DONE, FAILED }

/** One tool-call card inside an assistant turn. */
data class ToolCall(
  val tool: String,
  val preview: String? = null,
  val status: ToolStatus = ToolStatus.RUNNING,
)

/** A single message in the chat thread UI. */
data class ChatTurn(
  val role: String,        // "user" | "assistant"
  val text: String,
  val tools: List<ToolCall> = emptyList(),
  val streaming: Boolean = false,
  val imagePreview: Uri? = null, // set on the user turn when an image was attached
)

/** A built-in "/" command — the Session + Turn-control groups from the palette plan. */
enum class PaletteCommand(val label: String, val description: String) {
  NEW("/new", "start a fresh chat"),
  RESUME("/resume", "reopen a past session"),
  TITLE("/title", "rename this chat"),
  FORK("/fork", "branch from here"),
  STOP("/stop", "cancel the current run"),
  RETRY("/retry", "resend the last message"),
}

sealed interface PaletteEntry {
  data class Cmd(val cmd: PaletteCommand) : PaletteEntry
  data class Skill(val skill: HermesSkill) : PaletteEntry
}

/** Prefix-matches the typed token against built-ins first, then discovered skills. */
fun paletteEntries(query: String, skills: List<HermesSkill>): List<PaletteEntry> {
  val q = query.removePrefix("/").lowercase()
  val cmds = PaletteCommand.values()
    .filter { it.label.removePrefix("/").lowercase().startsWith(q) }
    .map { PaletteEntry.Cmd(it) }
  val skillEntries = skills
    .filter { it.name.lowercase().startsWith(q) }
    .map { PaletteEntry.Skill(it) }
  return cmds + skillEntries
}

data class ChatUiState(
  val configured: Boolean = HermesConfig.isConfigured,
  val sessions: List<HermesSession> = emptyList(),
  val sessionsLoading: Boolean = false,
  val activeSessionId: String? = null,
  val activeTitle: String? = null,
  val turns: List<ChatTurn> = emptyList(),
  val historyLoading: Boolean = false,
  val streaming: Boolean = false,
  val input: String = "",
  val error: String? = null,
  val pendingApproval: Pair<String, String>? = null, // runId, summary
  val skills: List<HermesSkill> = emptyList(),
  val attachedImage: Uri? = null,
  // One-shot UI intents the ViewModel can't perform itself (drawer/dialog live in the Composable).
  val requestOpenHistory: Int = 0,
  val requestRename: Int = 0,
) {
  val lastUserMessage: String?
    get() = turns.lastOrNull { it.role == "user" }?.text
}

class ChatViewModel : ViewModel() {
  private val repo = HermesRepository()
  private val _state = MutableStateFlow(ChatUiState())
  val state: StateFlow<ChatUiState> = _state.asStateFlow()

  private var streamJob: Job? = null
  private var currentRunId: String? = null
  private var pendingImage: ImageAttachment? = null
  private var skillsLoaded = false

  fun onResume() {
    _state.update { it.copy(configured = HermesConfig.isConfigured) }
    if (HermesConfig.isConfigured) {
      refreshSessions()
      loadSkills()
    }
  }

  fun setInput(v: String) = _state.update { it.copy(input = v) }
  fun clearError() = _state.update { it.copy(error = null) }

  private fun loadSkills() {
    if (skillsLoaded) return
    skillsLoaded = true
    viewModelScope.launch {
      runCatching { repo.listSkills() }
        .onSuccess { list -> _state.update { it.copy(skills = list) } }
        .onFailure { skillsLoaded = false } // allow a retry on the next resume
    }
  }

  fun refreshSessions() {
    if (!repo.isConfigured) return
    _state.update { it.copy(sessionsLoading = true) }
    viewModelScope.launch {
      runCatching { repo.listSessions() }
        .onSuccess { list ->
          // Only the app's own chats — hide cron / other-platform sessions.
          val sorted = list
            .filter { it.source == "api_server" }
            .sortedByDescending { s -> s.lastActive ?: s.startedAt ?: 0.0 }
          _state.update { it.copy(sessions = sorted, sessionsLoading = false) }
        }
        .onFailure { e -> _state.update { it.copy(sessionsLoading = false, error = e.message) } }
    }
  }

  fun openSession(id: String, title: String?) {
    if (id == _state.value.activeSessionId) return
    streamJob?.cancel()
    _state.update {
      it.copy(activeSessionId = id, activeTitle = title, turns = emptyList(), historyLoading = true, streaming = false)
    }
    viewModelScope.launch {
      runCatching { repo.messages(id) }
        .onSuccess { msgs ->
          val turns = msgs
            .filter { it.role == "user" || it.role == "assistant" }
            .filter { !it.content.isNullOrBlank() }
            .map { ChatTurn(it.role, it.content!!.trim()) }
          _state.update { it.copy(turns = turns, historyLoading = false) }
        }
        .onFailure { e -> _state.update { it.copy(historyLoading = false, error = e.message) } }
    }
  }

  fun newChat() {
    streamJob?.cancel()
    setAttachedImage(null)
    _state.update { it.copy(activeSessionId = null, activeTitle = null, turns = emptyList(), streaming = false, input = "") }
  }

  /** "/fork" — branch the active session; the copy keeps the transcript, this one closes. */
  fun fork() {
    val sid = _state.value.activeSessionId ?: run {
      _state.update { it.copy(error = "Nothing to fork yet — send a message first.") }
      return
    }
    viewModelScope.launch {
      runCatching { repo.forkSession(sid) }
        .onSuccess { forked ->
          if (forked != null) {
            _state.update { it.copy(activeSessionId = forked.id, activeTitle = forked.title) }
            refreshSessions()
          }
        }
        .onFailure { e -> _state.update { it.copy(error = e.message ?: "Fork failed") } }
    }
  }

  fun setAttachedImage(uri: Uri?, attachment: ImageAttachment? = null) {
    pendingImage = attachment
    _state.update { it.copy(attachedImage = uri) }
  }

  /** Handles a palette selection. Callback-only commands (resume/title) bump a request counter the Composable observes. */
  fun onPaletteSelect(entry: PaletteEntry) {
    _state.update { it.copy(input = "") }
    when (entry) {
      is PaletteEntry.Cmd -> when (entry.cmd) {
        PaletteCommand.NEW -> newChat()
        PaletteCommand.STOP -> stop()
        PaletteCommand.RETRY -> retry()
        PaletteCommand.FORK -> fork()
        PaletteCommand.RESUME -> _state.update { it.copy(requestOpenHistory = it.requestOpenHistory + 1) }
        PaletteCommand.TITLE -> _state.update { it.copy(requestRename = it.requestRename + 1) }
      }
      is PaletteEntry.Skill -> _state.update {
        it.copy(input = "Use your ${entry.skill.name} skill to ")
      }
    }
  }

  fun retry() {
    val last = _state.value.lastUserMessage ?: return
    if (_state.value.streaming) return
    setInput(last)
    send()
  }

  fun send() {
    val text = _state.value.input.trim()
    val image = pendingImage
    val imagePreview = _state.value.attachedImage
    if ((text.isEmpty() && image == null) || _state.value.streaming) return
    pendingImage = null
    _state.update {
      it.copy(
        input = "",
        attachedImage = null,
        turns = it.turns + ChatTurn("user", text, imagePreview = imagePreview) + ChatTurn("assistant", "", streaming = true),
        streaming = true,
        error = null,
      )
    }
    viewModelScope.launch {
      val sid = _state.value.activeSessionId ?: run {
        val created = runCatching { repo.createSession(defaultTitle(text.ifBlank { "Image" })) }.getOrNull()
        if (created == null) {
          failStream("Couldn't start a chat. Check the connection in Settings.")
          return@launch
        }
        _state.update { it.copy(activeSessionId = created.id, activeTitle = created.title) }
        refreshSessions()
        created.id
      }
      streamTurn(sid, text, if (image != null) listOf(image) else emptyList())
    }
  }

  private fun streamTurn(sid: String, text: String, images: List<ImageAttachment>) {
    streamJob?.cancel()
    streamJob = viewModelScope.launch {
      repo.sendStream(sid, text, images).collect { ev ->
        when (ev) {
          is ChatEvent.RunStarted -> currentRunId = ev.runId
          is ChatEvent.Delta -> appendToLast(ev.text)
          is ChatEvent.ToolStarted -> addTool(ToolCall(ev.tool, ev.preview, ToolStatus.RUNNING))
          is ChatEvent.ToolProgress -> if (ev.tool != "_thinking") addTool(ToolCall(ev.tool, ev.text.takeIf { it.isNotBlank() }, ToolStatus.RUNNING))
          is ChatEvent.ToolCompleted -> markTool(ev.tool, ToolStatus.DONE)
          is ChatEvent.ToolFailed -> markTool(ev.tool, ToolStatus.FAILED)
          is ChatEvent.ApprovalRequired ->
            _state.update { it.copy(pendingApproval = ev.runId to ev.summary) }
          is ChatEvent.Completed -> replaceLast(ev.content)
          is ChatEvent.Error -> { failStream(ev.message); return@collect }
          ChatEvent.Done -> finishStream()
        }
      }
      finishStream()
    }
  }

  fun stop() {
    currentRunId?.let { rid -> viewModelScope.launch { repo.stopRun(rid) } }
    streamJob?.cancel()
    finishStream()
  }

  fun resolveApproval(approved: Boolean) {
    val rid = _state.value.pendingApproval?.first ?: return
    _state.update { it.copy(pendingApproval = null) }
    viewModelScope.launch { repo.resolveApproval(rid, approved) }
  }

  fun rename(id: String, title: String) {
    viewModelScope.launch {
      runCatching { repo.renameSession(id, title) }.onSuccess { refreshSessions() }
      if (id == _state.value.activeSessionId) _state.update { it.copy(activeTitle = title) }
    }
  }

  fun delete(id: String) {
    viewModelScope.launch {
      runCatching { repo.deleteSession(id) }
      if (id == _state.value.activeSessionId) newChat()
      refreshSessions()
    }
  }

  // ---- stream helpers ----

  private fun appendToLast(chunk: String) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0) t[i] = t[i].copy(text = t[i].text + chunk, streaming = true)
    s.copy(turns = t)
  }

  private fun replaceLast(full: String) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0) t[i] = t[i].copy(text = full, streaming = true)
    s.copy(turns = t)
  }

  private fun addTool(call: ToolCall) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0) t[i] = t[i].copy(tools = t[i].tools + call)
    s.copy(turns = t)
  }

  /** Marks the most recent still-running card for [tool] as finished (matched by name — the SSE stream carries no call id). */
  private fun markTool(tool: String, status: ToolStatus) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0) {
      val tools = t[i].tools.toMutableList()
      val j = tools.indexOfLast { it.tool == tool && it.status == ToolStatus.RUNNING }
      if (j >= 0) tools[j] = tools[j].copy(status = status)
      else tools.add(ToolCall(tool, status = status))
      t[i] = t[i].copy(tools = tools)
    }
    s.copy(turns = t)
  }

  private fun finishStream() = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0) {
      t[i] = t[i].copy(streaming = false)
      if (t[i].text.isBlank() && t[i].tools.isEmpty()) t.removeAt(i)
    }
    s.copy(turns = t, streaming = false)
  }

  private fun failStream(msg: String) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0 && t[i].text.isBlank()) t.removeAt(i)
    else if (i >= 0) t[i] = t[i].copy(streaming = false)
    s.copy(turns = t, streaming = false, error = msg)
  }

  private fun defaultTitle(firstMsg: String): String {
    val stub = firstMsg.take(40).replace("\n", " ").trim()
    return "$stub · ${System.currentTimeMillis() % 100000}"
  }
}
