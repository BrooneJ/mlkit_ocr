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
      val (width, height) = info.size.width to info.size.height
      // not allow under 1f
      val scale = max(1f, max(width, height) / maxSizePx.toFloat())
      val targetWidth = (width / scale).toInt().coerceAtLeast(1)
      val targetHeight = (height / scale).toInt().coerceAtLeast(1)
      decoder.setTargetSize(targetWidth, targetHeight)
    }
  } else {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, bounds)
    }
    val inSample = run {
      var sample = 1
      val width = bounds.outWidth
      val height = bounds.outHeight
      while (width / sample > maxSizePx || height / sample > maxSizePx) sample *= 2
      sample
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
    context.contentResolver.openInputStream(uri)!!.use {
      BitmapFactory.decodeStream(it, null, opts)!!
    }
  }
}