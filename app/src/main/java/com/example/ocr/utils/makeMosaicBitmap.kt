package com.example.ocr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

fun makeMosaicBitmap(src: Bitmap, downscaleFactor: Int = 16): Bitmap {
  require(downscaleFactor >= 2)
  val smallWidth = (src.width / downscaleFactor).coerceAtLeast(1)
  val smallHeight = (src.height / downscaleFactor).coerceAtLeast(1)
  val smallSizeBitmap = Bitmap.createBitmap(smallWidth, smallHeight, Bitmap.Config.ARGB_8888)
  Canvas(smallSizeBitmap).drawBitmap(
    src,
    Rect(0, 0, src.width, src.height),
    Rect(0, 0, smallWidth, smallHeight),
    null
  )
  return Bitmap.createScaledBitmap(smallSizeBitmap, src.width, src.height, false)
}