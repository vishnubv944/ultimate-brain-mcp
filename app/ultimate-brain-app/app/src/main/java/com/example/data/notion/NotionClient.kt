package com.example.data.notion

import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the [NotionApi] and adds the two cross-cutting behaviours from
 * `notion_client.py`: a ~3 req/s spacing limiter and retry-with-backoff on
 * 429 / 5xx. Also exposes a paginated `queryAll` helper.
 */
class NotionClient(
  token: String,
  enableLogging: Boolean = false,
) {
  private val rateLimiter = TokenSpacedLimiter(minIntervalMs = 350L)

  private val authInterceptor = Interceptor { chain ->
    val req = chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $token")
      .addHeader("Notion-Version", NotionConfig.API_VERSION)
      .addHeader("Content-Type", "application/json")
      .build()
    chain.proceed(req)
  }

  private val okHttp: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(authInterceptor)
    .apply {
      if (enableLogging) {
        addInterceptor(
          okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
          }
        )
      }
    }
    .build()

  private val moshi: Moshi = Moshi.Builder().build()

  val api: NotionApi = Retrofit.Builder()
    .baseUrl(NotionConfig.BASE_URL)
    .client(okHttp)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
    .create(NotionApi::class.java)

  /** Rate-limited + retrying wrapper for arbitrary API calls. */
  suspend fun <T> call(block: suspend (NotionApi) -> T): T {
    var attempt = 0
    var backoff = 1000L
    while (true) {
      rateLimiter.await()
      try {
        return block(api)
      } catch (e: HttpException) {
        val code = e.code()
        val retryable = code == 429 || code in 500..504
        if (!retryable || attempt >= MAX_RETRIES) throw e
        val retryAfter = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()?.times(1000)
        delay(retryAfter ?: backoff)
        backoff = (backoff * 2).coerceAtMost(30_000L)
        attempt++
      }
    }
  }

  /**
   * Query a data source, following `has_more` cursors up to [maxPages].
   * [filter] / [sorts] are raw Notion query-body values (see the API docs);
   * null omits them.
   */
  suspend fun queryAll(
    dsId: String,
    filter: Map<String, Any>? = null,
    sorts: List<Map<String, Any>>? = null,
    maxPages: Int = 6,
  ): List<NotionPage> {
    val out = ArrayList<NotionPage>()
    var cursor: String? = null
    repeat(maxPages) {
      val body = buildMap<String, Any> {
        put("page_size", 100)
        filter?.let { put("filter", it) }
        sorts?.let { put("sorts", it) }
        cursor?.let { put("start_cursor", it) }
      }
      val resp = call { it.queryDataSource(dsId, body) }
      out += resp.results
      if (!resp.hasMore) return out
      cursor = resp.nextCursor
    }
    return out
  }

  private companion object {
    const val MAX_RETRIES = 4
  }
}

/** Serialises callers to at most one request per [minIntervalMs]. */
private class TokenSpacedLimiter(private val minIntervalMs: Long) {
  private val lock = Mutex()
  private var nextOk = 0L

  suspend fun await() {
    lock.withLock {
      val now = System.currentTimeMillis()
      val wait = nextOk - now
      if (wait > 0) delay(wait)
      nextOk = maxOf(now, nextOk) + minIntervalMs
    }
  }
}
