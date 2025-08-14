package com.example.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidPath
import com.example.ocr.screen.draw.Stroke
import com.example.ocr.screen.draw.Tool
import java.io.File
import java.io.FileOutputStream

fun renderToBitmap(
  baseBitmap: Bitmap,
  mosaicedBitmap: Bitmap,
  strokes: List<Stroke>
): Bitmap {
  val out = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(out)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
  }

  canvas.drawBitmap(baseBitmap, 0f, 0f, null)

  strokes.forEach { stroke ->
    when (stroke.tool) {
      Tool.White -> {
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = stroke.width
        paint.alpha = (stroke.alpha * 255).toInt()
        canvas.drawPath(stroke.path.asAndroidPath(), paint)
      }

      Tool.Mosaic -> {
        val checkpoint = canvas.save()
        canvas.clipPath(stroke.path.asAndroidPath())
        canvas.drawBitmap(mosaicedBitmap, 0f, 0f, null)
        canvas.restoreToCount(checkpoint)
      }

      Tool.Eraser -> {
        val checkpoint = canvas.save()
        canvas.clipPath(stroke.path.asAndroidPath())
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        canvas.restoreToCount(checkpoint)
      }
    }
  }
  return out
}

fun saveBitmapToCacheUri(
  context: Context,
  bitmap: Bitmap,
  prefix: String,
  suffix: String = ".jpg",
  quality: Int = 92
): Uri {
  val file = File.createTempFile(prefix, suffix, context.cacheDir)
  FileOutputStream(file).use { out ->
    val format = if (suffix.lowercase().endsWith(".png")) Bitmap.CompressFormat.PNG
    else Bitmap.CompressFormat.JPEG
    bitmap.compress(format, quality, out)
  }
  return Uri.fromFile(file)
}