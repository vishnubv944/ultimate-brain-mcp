package com.example.data.hermes

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * Connection settings for the Hermes agent API server (running on the user's
 * Raspberry Pi, reached over Tailscale).
 *
 * Seeded at build time from `app/ultimate-brain-app/.env` via the Secrets
 * Gradle plugin (`HERMES_BASE_URL`, `HERMES_API_KEY` → `BuildConfig.*`), and
 * overridable at runtime from Settings (persisted to SharedPreferences).
 *
 * SECURITY: the bearer key is baked into the debug APK — personal single-user
 * client only, never distribute.
 */
object HermesConfig {
  private const val PREFS = "ub_hermes"
  private const val K_BASE = "base_url"
  private const val K_KEY = "api_key"

  private var prefs: SharedPreferences? = null

  fun init(context: Context) {
    prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  }

  private fun buildConfig(field: String): String = try {
    BuildConfig::class.java.getField(field).get(null) as? String ?: ""
  } catch (_: Throwable) {
    ""
  }

  /** Base URL, no trailing slash. e.g. http://100.123.210.71:8642 */
  val baseUrl: String
    get() = (prefs?.getString(K_BASE, null)?.takeIf { it.isNotBlank() }
      ?: buildConfig("HERMES_BASE_URL")).trimEnd('/')

  val apiKey: String
    get() = prefs?.getString(K_KEY, null)?.takeIf { it.isNotBlank() }
      ?: buildConfig("HERMES_API_KEY")

  val isConfigured: Boolean
    get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

  fun save(baseUrl: String?, apiKey: String?) {
    prefs?.edit()?.apply {
      if (baseUrl != null) putString(K_BASE, baseUrl.trim().trimEnd('/'))
      if (apiKey != null) putString(K_KEY, apiKey.trim())
      apply()
    }
  }
}
