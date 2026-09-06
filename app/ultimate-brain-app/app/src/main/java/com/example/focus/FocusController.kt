package com.example.focus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide holder for the single active focus session. The ViewModel
 * mutates it; [FocusTimerService] renders it into an ongoing notification and
 * keeps the process alive while a session runs. UI observes [session] and
 * derives elapsed time with [FocusSession.elapsedMs].
 *
 * One session at a time — [start] replaces any previous one.
 */
object FocusController {

  private val _session = MutableStateFlow<FocusSession?>(null)
  val session: StateFlow<FocusSession?> = _session.asStateFlow()

  val isRunning: Boolean get() = _session.value?.isPaused == false

  fun start(taskId: String, taskName: String, projectName: String?) {
    val now = System.currentTimeMillis()
    _session.value = FocusSession(
      taskId = taskId,
      taskName = taskName,
      projectName = projectName,
      startedAtMs = now,
      pausedTotalMs = 0L,
      pauseStartedAtMs = null,
    )
  }

  fun pause() {
    val s = _session.value ?: return
    if (s.pauseStartedAtMs != null) return
    _session.value = s.copy(pauseStartedAtMs = System.currentTimeMillis())
  }

  fun resume() {
    val s = _session.value ?: return
    val pStart = s.pauseStartedAtMs ?: return
    _session.value = s.copy(
      pausedTotalMs = s.pausedTotalMs + (System.currentTimeMillis() - pStart),
      pauseStartedAtMs = null,
    )
  }

  /** Ends the session and returns it for persistence (Work Session write). */
  fun stop(): FocusSession? {
    val s = _session.value ?: return null
    _session.value = null
    return s
  }
}

data class FocusSession(
  val taskId: String,
  val taskName: String,
  val projectName: String?,
  val startedAtMs: Long,
  val pausedTotalMs: Long,
  val pauseStartedAtMs: Long?,
) {
  val isPaused: Boolean get() = pauseStartedAtMs != null

  /** Wall-clock time spent focusing, excluding paused stretches. */
  fun elapsedMs(now: Long = System.currentTimeMillis()): Long {
    val pausedNow = pauseStartedAtMs?.let { now - it } ?: 0L
    return (now - startedAtMs - pausedTotalMs - pausedNow).coerceAtLeast(0L)
  }

  val startIso: String get() = java.time.Instant.ofEpochMilli(startedAtMs).toString()
}

fun formatFocusElapsed(ms: Long): String {
  val total = ms / 1000
  val h = total / 3600
  val m = (total % 3600) / 60
  val s = total % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
