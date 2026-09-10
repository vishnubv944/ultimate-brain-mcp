package com.example.data.hermes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Holds a [HermesClient] built from the current [HermesConfig] and rebuilds it
 * when the connection settings change.
 */
class HermesRepository {
  private var client: HermesClient? = null
  private var forBase: String = ""
  private var forKey: String = ""

  private fun client(): HermesClient? {
    if (!HermesConfig.isConfigured) return null
    if (client == null || forBase != HermesConfig.baseUrl || forKey != HermesConfig.apiKey) {
      forBase = HermesConfig.baseUrl
      forKey = HermesConfig.apiKey
      client = HermesClient(forBase, forKey)
    }
    return client
  }

  val isConfigured get() = HermesConfig.isConfigured

  suspend fun health(): Boolean = client()?.health() ?: false
  suspend fun listSessions() = client()?.listSessions().orEmpty()
  suspend fun createSession(title: String?) = client()?.createSession(title)
  suspend fun renameSession(id: String, title: String) { client()?.renameSession(id, title) }
  suspend fun deleteSession(id: String) { client()?.deleteSession(id) }
  suspend fun messages(id: String) = client()?.messages(id).orEmpty()
  suspend fun stopRun(runId: String) { client()?.stopRun(runId) }
  suspend fun resolveApproval(runId: String, approved: Boolean) { client()?.resolveApproval(runId, approved) }

  fun sendStream(sessionId: String, input: String): Flow<ChatEvent> =
    client()?.sendStream(sessionId, input) ?: emptyFlow()
}
