package com.example.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

suspend fun loadBitmapFromUri(
  context: Context,
  uri: Uri,
  maxSizePx: Int = 2048
): Bitmap = withContext(Dispatchers.IO) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val src = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
      val (w, h) = info.size.width to info.size.height
      val scale = max(1f, max(w, h) / maxSizePx.toFloat())
      val targetWidth = (w / scale).toInt().coerceAtLeast(1)
      val targetHeight = (h / scale).toInt().coerceAtLeast(1)
      decoder.setTargetSize(targetWidth, targetHeight)
    }
  } else {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, bounds)
    }
    val inSample = run {
      var sample = 1
      val w = bounds.outWidth
      val h = bounds.outHeight
      while (w / sample > maxSizePx || h / sample > maxSizePx) sample *= 2
      sample
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
    context.contentResolver.openInputStream(uri)!!.use {
      BitmapFactory.decodeStream(it, null, opts)!!
    }
  }
}