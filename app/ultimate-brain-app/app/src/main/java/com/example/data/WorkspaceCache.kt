package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import java.io.File

/**
 * Disk cache of the last successfully synced [WorkspaceData].
 *
 * Without this, every cold start showed a blank/loading app until the Notion
 * round-trip finished — the fix isn't a spinner, it's not needing one: the
 * app renders the last known-good snapshot instantly, then [MyDayViewModel]
 * kicks off a silent background refresh that swaps in live data once the
 * sync completes. A spinner is only shown on the very first launch ever,
 * when there's no snapshot yet to show.
 *
 * Same object-with-`init(context)` shape as [FilterStore] — a plain
 * `MyDayViewModel()` (unit tests) just sees a cache miss.
 */
object WorkspaceCache {
  private var file: File? = null
  private val moshi = Moshi.Builder().build()
  private val adapter = moshi.adapter(WorkspaceData::class.java)

  fun init(context: Context) {
    if (file == null) file = File(context.applicationContext.filesDir, "workspace_cache.json")
  }

  /** Synchronous — a local JSON read, fast enough to call from `ViewModel.init`. */
  fun load(): WorkspaceData? = runCatching {
    val f = file?.takeIf { it.exists() } ?: return null
    adapter.fromJson(f.readText())
  }.getOrNull()

  /** Call from a background dispatcher — this does blocking file I/O. */
  fun save(data: WorkspaceData) {
    val f = file ?: return
    runCatching { f.writeText(adapter.toJson(data)) }
  }
}
