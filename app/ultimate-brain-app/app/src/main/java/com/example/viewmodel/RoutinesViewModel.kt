package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hermes.HermesJob
import com.example.data.hermes.HermesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutinesUiState(
  val loading: Boolean = false,
  val jobs: List<HermesJob> = emptyList(),
  val error: String? = null,
  val saving: Boolean = false,
)

/** Wraps the `/api/jobs` (cron) surface — Hermes's scheduled-prompt "Routines". */
class RoutinesViewModel : ViewModel() {
  private val repo = HermesRepository()
  private val _state = MutableStateFlow(RoutinesUiState())
  val state: StateFlow<RoutinesUiState> = _state.asStateFlow()

  fun refresh() {
    if (!repo.isConfigured) return
    _state.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
      runCatching { repo.listJobs() }
        .onSuccess { list -> _state.update { it.copy(loading = false, jobs = list) } }
        .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Couldn't load routines") } }
    }
  }

  fun create(name: String, schedule: String, prompt: String) {
    if (name.isBlank() || schedule.isBlank() || prompt.isBlank()) return
    _state.update { it.copy(saving = true) }
    viewModelScope.launch {
      runCatching { repo.createJob(name.trim(), schedule.trim(), prompt.trim()) }
        .onSuccess { refresh() }
        .onFailure { e -> _state.update { it.copy(error = e.message ?: "Couldn't create routine") } }
      _state.update { it.copy(saving = false) }
    }
  }

  fun toggle(job: HermesJob) {
    val optimistic = job.copy(enabled = !job.enabled)
    _state.update { s -> s.copy(jobs = s.jobs.map { if (it.id == job.id) optimistic else it }) }
    viewModelScope.launch {
      runCatching { if (job.enabled) repo.pauseJob(job.id) else repo.resumeJob(job.id) }
        .onFailure { e -> _state.update { it.copy(error = e.message ?: "Couldn't update routine") } }
      refresh()
    }
  }

  fun runNow(job: HermesJob) {
    viewModelScope.launch {
      runCatching { repo.runJobNow(job.id) }
        .onFailure { e -> _state.update { it.copy(error = e.message ?: "Couldn't run routine") } }
      refresh()
    }
  }

  fun delete(job: HermesJob) {
    _state.update { s -> s.copy(jobs = s.jobs.filterNot { it.id == job.id }) }
    viewModelScope.launch {
      runCatching { repo.deleteJob(job.id) }
        .onFailure { refresh() }
    }
  }

  fun clearError() = _state.update { it.copy(error = null) }
}
