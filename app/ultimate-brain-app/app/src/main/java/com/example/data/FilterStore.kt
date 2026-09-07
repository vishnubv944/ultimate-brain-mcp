package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ChipRowConfig
import com.example.model.CustomFilter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Local, per-device persistence for the customizable filter bars: user-defined
 * [CustomFilter]s and per-scope [ChipRowConfig] (order / hidden / default).
 *
 * An object rather than an injected dependency to match [com.example.focus.FocusController]
 * — [init] is called once from MainActivity; every accessor is a no-op until then
 * (so plain `MyDayViewModel()` in unit tests just sees empty state).
 */
object FilterStore {
  private var prefs: SharedPreferences? = null
  private val moshi = Moshi.Builder().build()

  private val filtersAdapter =
    moshi.adapter<List<CustomFilter>>(Types.newParameterizedType(List::class.java, CustomFilter::class.java))
  private val configsAdapter =
    moshi.adapter<Map<String, ChipRowConfig>>(
      Types.newParameterizedType(Map::class.java, String::class.java, ChipRowConfig::class.java),
    )

  fun init(context: Context) {
    if (prefs == null) prefs = context.applicationContext.getSharedPreferences("ub_filters", Context.MODE_PRIVATE)
  }

  fun loadFilters(): List<CustomFilter> =
    runCatching { prefs?.getString(KEY_FILTERS, null)?.let { filtersAdapter.fromJson(it) } }.getOrNull().orEmpty()

  fun saveFilters(list: List<CustomFilter>) {
    prefs?.edit()?.putString(KEY_FILTERS, filtersAdapter.toJson(list))?.apply()
  }

  fun loadConfigs(): Map<String, ChipRowConfig> =
    runCatching { prefs?.getString(KEY_CONFIGS, null)?.let { configsAdapter.fromJson(it) } }.getOrNull().orEmpty()

  fun saveConfigs(map: Map<String, ChipRowConfig>) {
    prefs?.edit()?.putString(KEY_CONFIGS, configsAdapter.toJson(map))?.apply()
  }

  private const val KEY_FILTERS = "custom_filters"
  private const val KEY_CONFIGS = "chip_configs"
}
