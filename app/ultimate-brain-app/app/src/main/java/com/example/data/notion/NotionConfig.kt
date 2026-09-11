package com.example.data.notion

import com.example.BuildConfig

/**
 * Notion Direct-API configuration.
 *
 * The token + data-source IDs are injected at build time by the Secrets Gradle
 * Plugin from `app/ultimate-brain-app/.env` (gitignored) and surface here as
 * `BuildConfig.*` fields. `.env.example` documents the keys.
 *
 * SECURITY NOTE: the integration token is baked into the APK. That is
 * acceptable for a personal single-user client. If this app is ever
 * distributed, move to Notion OAuth and rotate this token.
 */
object NotionConfig {

  val integrationToken: String = buildConfigOrEmpty("NOTION_INTEGRATION_SECRET")

  val tasksDsId: String = buildConfigOrEmpty("UB_TASKS_DS_ID")
  val projectsDsId: String = buildConfigOrEmpty("UB_PROJECTS_DS_ID")
  val notesDsId: String = buildConfigOrEmpty("UB_NOTES_DS_ID")
  val tagsDsId: String = buildConfigOrEmpty("UB_TAGS_DS_ID")
  val goalsDsId: String = buildConfigOrEmpty("UB_GOALS_DS_ID")
  val milestonesDsId: String = buildConfigOrEmpty("UB_MILESTONES_DS_ID")
  val workSessionsDsId: String = buildConfigOrEmpty("UB_WORK_SESSIONS_DS_ID")
  val peopleDsId: String = buildConfigOrEmpty("UB_PEOPLE_DS_ID")
  val booksDsId: String = buildConfigOrEmpty("UB_BOOKS_DS_ID")
  val readingLogDsId: String = buildConfigOrEmpty("UB_READING_LOG_DS_ID")
  val genresDsId: String = buildConfigOrEmpty("UB_GENRES_DS_ID")
  val recipesDsId: String = buildConfigOrEmpty("UB_RECIPES_DS_ID")
  val mealPlannerDsId: String = buildConfigOrEmpty("UB_MEAL_PLANNER_DS_ID")

  /** When false, the app has no data source and shows empty states. */
  val isConfigured: Boolean
    get() = integrationToken.isNotBlank() &&
      tasksDsId.isNotBlank() &&
      projectsDsId.isNotBlank() &&
      notesDsId.isNotBlank() &&
      goalsDsId.isNotBlank()

  const val BASE_URL = "https://api.notion.com/v1/"
  const val API_VERSION = "2025-09-03"

  /**
   * Reflectively reads a generated `BuildConfig` string field, returning "" if
   * the Secrets plugin didn't emit it (e.g. key absent from both .env files).
   * Avoids a hard compile dependency on fields that may not exist yet.
   */
  private fun buildConfigOrEmpty(field: String): String = try {
    BuildConfig::class.java.getField(field).get(null) as? String ?: ""
  } catch (_: Throwable) {
    ""
  }
}
