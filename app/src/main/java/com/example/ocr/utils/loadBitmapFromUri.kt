package com.example.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlin.math.ceil
import kotlin.math.max

suspend fun loadBitmapFromUri(
  context: Context,
  uri: Uri,
  maxDecodeSizePx: Int = 2048
): Bitmap? {
  val cr = context.contentResolver

  return if (Build.VERSION.SDK_INT >= 28) {
    val src = ImageDecoder.createSource(cr, uri)
    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
      // ★ set allocator to ALLOCATOR_SOFTWARE for software decoding
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      decoder.isMutableRequired = true

      // Down sampling (ensuring the larger dimension does not exceed maxDecodeSizePx)
      val (w, h) = info.size.let { it.width to it.height }
      val sample = max(1, ceil(max(w, h) / maxDecodeSizePx.toFloat()).toInt())
      decoder.setTargetSampleSize(sample)
    }
  } else {
    // API < 28: use BitmapFactory with inSampleSize
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val inSample = calcInSampleSize(bounds.outWidth, bounds.outHeight, maxDecodeSizePx)

    val opts = BitmapFactory.Options().apply {
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
      inSampleSize = inSample
    }
    cr.openInputStream(uri)?.use { input ->
      BitmapFactory.decodeStream(input, null, opts)!!
    }
  }
}

private fun calcInSampleSize(w: Int, h: Int, maxSize: Int): Int {
  if (w <= 0 || h <= 0) return 1
  var sample = 1
  var cw = w
  var ch = h
  while (max(cw, ch) > maxSize) {
    cw /= 2; ch /= 2; sample *= 2
  }
  return max(1, sample)
}