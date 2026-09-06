package com.example.data.notion

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Notion REST surface (API version 2025-09-03). Mirrors the subset of
 * `notion_client.py` the app needs: data-source query, page create/get/update.
 */
interface NotionApi {

  @POST("data_sources/{dsId}/query")
  suspend fun queryDataSource(
    @Path("dsId") dsId: String,
    @Body body: Map<String, @JvmSuppressWildcards Any>,
  ): QueryResponse

  @POST("pages")
  suspend fun createPage(@Body body: Map<String, @JvmSuppressWildcards Any>): NotionPage

  @GET("pages/{pageId}")
  suspend fun getPage(@Path("pageId") pageId: String): NotionPage

  @PATCH("pages/{pageId}")
  suspend fun updatePage(
    @Path("pageId") pageId: String,
    @Body body: Map<String, @JvmSuppressWildcards Any>,
  ): NotionPage

  // Page body as Markdown — needs the newer API version, pinned per-call.
  @GET("pages/{pageId}/markdown")
  suspend fun getPageMarkdown(
    @Path("pageId") pageId: String,
    @Header("Notion-Version") version: String = "2026-03-11",
  ): MarkdownResponse

  @PATCH("pages/{pageId}/markdown")
  suspend fun replacePageMarkdown(
    @Path("pageId") pageId: String,
    @Body body: Map<String, @JvmSuppressWildcards Any>,
    @Header("Notion-Version") version: String = "2026-03-11",
  ): MarkdownResponse
}
