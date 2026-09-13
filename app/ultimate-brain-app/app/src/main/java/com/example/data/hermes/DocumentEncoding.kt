package com.example.data.hermes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DocumentAttachment(val name: String, val text: String)

private const val MAX_DOC_CHARS = 20_000

/**
 * Reads a picked document as plain text to inline into the chat message.
 *
 * Hermes's API server only accepts text and image content parts today — it
 * explicitly rejects file/document parts (`unsupported_content_type` in
 * `_normalize_multimodal_content`). Until that changes server-side, this is
 * the practical way to "attach a doc": read a text-like file locally and
 * fold its content into the outgoing message text, instead of a real binary
 * upload. Returns null for anything that doesn't decode as text (a PDF, an
 * image, etc.) — the caller surfaces that as "only text files are supported".
 */
suspend fun readDocumentForUpload(context: Context, uri: Uri): DocumentAttachment? = withContext(Dispatchers.IO) {
  runCatching {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
      if (c.moveToFirst()) c.getString(0) else null
    } ?: uri.lastPathSegment ?: "document"

    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
    val text = bytes.toString(Charsets.UTF_8)
    // Heuristic: heavy replacement/control-character content means this
    // almost certainly isn't text (a PDF, a photo picked by mistake, etc.).
    val badChars = text.count { it == '�' || (it.code < 32 && it != '\n' && it != '\r' && it != '\t') }
    if (text.isBlank() || badChars > text.length / 20) return@runCatching null

    DocumentAttachment(name, text.take(MAX_DOC_CHARS))
  }.getOrNull()
}
