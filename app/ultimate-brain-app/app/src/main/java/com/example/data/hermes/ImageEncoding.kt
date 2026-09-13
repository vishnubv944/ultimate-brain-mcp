package com.example.data.hermes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Downscales a picked image to something reasonable for a chat turn (long
 * edge capped, re-encoded as JPEG) and base64-encodes it for the
 * `image_url` data-URL content part `sendStream` sends to Hermes.
 */
private const val MAX_EDGE_PX = 1280
private const val JPEG_QUALITY = 82

suspend fun encodeImageForUpload(context: Context, uri: Uri): ImageAttachment? = withContext(Dispatchers.IO) {
  runCatching {
    val resolver = context.contentResolver

    // First pass: read bounds only, to pick a cheap inSampleSize before
    // decoding the full bitmap (avoids OOM on a 12MP camera photo).
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val (w, h) = bounds.outWidth to bounds.outHeight
    if (w <= 0 || h <= 0) return@runCatching null

    var sample = 1
    while (w / (sample * 2) >= MAX_EDGE_PX || h / (sample * 2) >= MAX_EDGE_PX) sample *= 2

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
      ?: return@runCatching null

    val bitmap = if (maxOf(decoded.width, decoded.height) > MAX_EDGE_PX) {
      val scale = MAX_EDGE_PX.toFloat() / maxOf(decoded.width, decoded.height)
      Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    } else decoded

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    ImageAttachment(base64 = b64, mimeType = "image/jpeg")
  }.getOrNull()
}
