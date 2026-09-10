package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hermes.ChatEvent
import com.example.data.hermes.HermesConfig
import com.example.data.hermes.HermesRepository
import com.example.data.hermes.HermesSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A single message in the chat thread UI. */
data class ChatTurn(
  val role: String,        // "user" | "assistant"
  val text: String,
  val tools: List<String> = emptyList(),
  val streaming: Boolean = false,
)

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
)

class ChatViewModel : ViewModel() {
  private val repo = HermesRepository()
  private val _state = MutableStateFlow(ChatUiState())
  val state: StateFlow<ChatUiState> = _state.asStateFlow()

  private var streamJob: Job? = null
  private var currentRunId: String? = null

  fun onResume() {
    _state.update { it.copy(configured = HermesConfig.isConfigured) }
    if (HermesConfig.isConfigured) refreshSessions()
  }

  fun setInput(v: String) = _state.update { it.copy(input = v) }
  fun clearError() = _state.update { it.copy(error = null) }

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
    _state.update { it.copy(activeSessionId = null, activeTitle = null, turns = emptyList(), streaming = false) }
  }

  fun send() {
    val text = _state.value.input.trim()
    if (text.isEmpty() || _state.value.streaming) return
    _state.update {
      it.copy(
        input = "",
        turns = it.turns + ChatTurn("user", text) + ChatTurn("assistant", "", streaming = true),
        streaming = true,
        error = null,
      )
    }
    viewModelScope.launch {
      val sid = _state.value.activeSessionId ?: run {
        val created = runCatching { repo.createSession(defaultTitle(text)) }.getOrNull()
        if (created == null) {
          failStream("Couldn't start a chat. Check the connection in Settings.")
          return@launch
        }
        _state.update { it.copy(activeSessionId = created.id, activeTitle = created.title) }
        refreshSessions()
        created.id
      }
      streamTurn(sid, text)
    }
  }

  private fun streamTurn(sid: String, text: String) {
    streamJob?.cancel()
    streamJob = viewModelScope.launch {
      repo.sendStream(sid, text).collect { ev ->
        when (ev) {
          is ChatEvent.RunStarted -> currentRunId = ev.runId
          is ChatEvent.Delta -> appendToLast(ev.text)
          is ChatEvent.ToolStarted -> addToolToLast("Running ${ev.tool}…")
          is ChatEvent.ToolProgress -> if (ev.tool != "_thinking") addToolToLast("${ev.tool}…")
          is ChatEvent.ToolCompleted -> addToolToLast("${ev.tool} ✓")
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

  private fun addToolToLast(label: String) = _state.update { s ->
    val t = s.turns.toMutableList()
    val i = t.indexOfLast { it.role == "assistant" }
    if (i >= 0 && t[i].tools.lastOrNull() != label) t[i] = t[i].copy(tools = t[i].tools + label)
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
