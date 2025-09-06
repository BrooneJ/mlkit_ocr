package com.example.ocr.screen.ocr.utils

import android.graphics.Bitmap

fun verticalProjectionCuts(bitmap: Bitmap, threshold: Int = 230): List<Int> {
  val width = bitmap.width
  val height = bitmap.height
  val ink = IntArray(width)
  for (x in 0 until width) {
    var sum = 0
    for (y in 0 until height) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16 and 0xFF)
      val g = (pixel shr 8 and 0xFF)
      val b = pixel and 0xFF
      val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
      if (luma < threshold) sum++
    }
    ink[x] = sum
  }

  val cuts = mutableListOf<Int>()
  var inGap = false
  for (x in 0 until width) {
    val gap = ink[x] == 0
    if (gap && !inGap) {
      cuts += x; inGap = true
    }
    if (!gap && inGap) {
      cuts += x; inGap = false
    }
  }

  if (cuts.isEmpty()) return emptyList()
  return cuts
}
