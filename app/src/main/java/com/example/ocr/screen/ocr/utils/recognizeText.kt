package com.example.ocr.screen.ocr.utils

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

data class RectI(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  val width get() = right - left
  val height get() = bottom - top
}

data class TableLayout(
  val headerBand: RectI,
  val columns: List<Int>,
  val rows: List<RectI>,
)

data class DayCell(val x1: Int, val y1: Int, val x2: Int, val y2: Int)
data class ShiftCell(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

suspend fun recognizeText(bitmap: Bitmap): Text {
  val input = InputImage.fromBitmap(bitmap, 0)
  val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
  return Tasks.await(recognizer.process(input))
}

fun headerBandOf(bitmap: Bitmap, ratio: Float = 0.15f): RectI {
  val h = bitmap.height
  val bandHeight = (h * ratio).toInt().coerceAtLeast(40)
  return RectI(0, 0, bitmap.width, bandHeight.coerceAtMost(h))
}

fun verticalProjection(bitmap: Bitmap, roi: RectI): IntArray {
  val width = roi.width
  val height = roi.height
  val pixels = IntArray(width * height)
  bitmap.getPixels(pixels, 0, width, roi.left, roi.top, width, height)

  val colSums = IntArray(width)
  for (y in 0 until height) {
    for (x in 0 until width) {
      val pixel = pixels[y * width + x]
      val r = (pixel shr 16 and 0xFF)
      val g = (pixel shr 8 and 0xFF)
      val b = pixel and 0xFF
      val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
      val darkness = 255 - luma
      colSums[x] += darkness
    }
  }
  return colSums
}

fun smooth(signal: IntArray, radius: Int): IntArray {
  val n = signal.size
  val out = IntArray(n)
  val r = radius.coerceAtLeast(1)
  var acc = 0L
  var cnt = 0

  // sliding window
  var left = 0
  var right = 0
  acc = signal[0].toLong()
  cnt = 1
  out[0] = acc.toInt()

  fun add(i: Int) {
    acc += signal[i]; cnt++
  }

  fun remove(i: Int) {
    acc -= signal[i]; cnt--
  }

  for (i in 1 until n) {
    while (right < (i + r) && right + 1 < n) {
      right++; add(right)
    }
    while (left < (i - r)) {
      remove(left); left++
    }
    out[i] = (acc / cnt).toInt()
  }
  return out
}

fun pickColumnBoundaries(
  smoothed: IntArray,
  imgWidth: Int,
  charWidthPx: Int,
): List<Int> {
  val avg = smoothed.average()
  val minGap = maxOf(charWidthPx * 2, imgWidth / 16)
  val depthThresh = avg * 0.6

  val minima = mutableListOf<Int>()
  for (x in 1 until smoothed.lastIndex) {
    if (smoothed[x] <= smoothed[x - 1] && smoothed[x] <= smoothed[x + 1] && smoothed[x] < depthThresh) {
      minima += x
    }
  }
  val picked = mutableListOf<Int>()
  for (m in minima) {
    if (picked.isEmpty() || m - picked.last() >= minGap) picked += m
  }
  return buildList {
    add(0)
    addAll(picked)
    add(imgWidth)
  }
}

fun roughCharWidthPx(bitmap: Bitmap, header: RectI): Int {
  return (bitmap.width / 48f).toInt().coerceAtLeast(6)
}

fun recoverLayout(bitmap: Bitmap): TableLayout {
  val header = headerBandOf(bitmap)
  val projection = verticalProjection(bitmap, header)
  val charWidth = roughCharWidthPx(bitmap, header)
  val smoothed = smooth(projection, radius = charWidth)
  val columns = pickColumnBoundaries(smoothed, bitmap.width, charWidth)
  return TableLayout(headerBand = header, columns = columns, rows = emptyList())
}

fun cropBitmap(src: Bitmap, rect: RectI): Bitmap =
  Bitmap.createBitmap(src, rect.left, rect.top, rect.width, rect.height)

suspend fun ocrCellText(bitmap: Bitmap, cell: RectI): String {
  val sub = cropBitmap(bitmap, cell)
  val text = recognizeText(sub).text
  return text.replace("[()（）\\[\\]{}]".toRegex(), " ")
    .trim()
}

data class ParsedDate(val month: Int, val day: Int)

fun parseDate(text: String, defaultYear: Int, defaultMonth: Int): ParsedDate? {
  val cleaned = text.replace("[^0-9/]".toRegex(), " ").trim()
  val monthAndDay = "(\\d{1,2})\\s*/\\s*(\\d{1,2})".toRegex().find(cleaned)
  if (monthAndDay != null) {
    val (month, day) = monthAndDay.destructured
    return ParsedDate(month.toInt(), day.toInt())
  }
  val dayOnly = "^\\s*(\\d{1,2})\\s*$".toRegex().find(cleaned)?.groupValues?.get(1)
  if (dayOnly != null) return ParsedDate(defaultMonth, dayOnly.toInt())
  return null
}

suspend fun extractScheduleJson(
  bitmap: Bitmap,
  defaultYear: Int,
  defaultMonth: Int,
  targetRowBand: RectI,
): String {
  val layout = recoverLayout(bitmap)
  val header = layout.headerBand
  val xs = layout.columns

  data class Entry(val date: String, val shift: String)

  val entries = mutableListOf<Entry>()
  for (i in 0 until xs.size - 1) {
    val x1 = xs[i];
    val x2 = xs[i + 1]
    if (x2 - x1 < 12) continue

    val headerCell = RectI(x1, header.top, x2, header.bottom)
    val headerText = ocrCellText(bitmap, headerCell)
    val parsed = parseDate(headerText, defaultYear, defaultMonth) ?: continue

    val shiftCell = RectI(x1, targetRowBand.top, x2, targetRowBand.bottom)
    val shiftTextRaw = ocrCellText(bitmap, shiftCell)
    val shiftText = shiftTextRaw
      .replace("[月火水木金土日]".toRegex(), "")   // 요일 제거
      .replace("\\s+".toRegex(), " ")
      .trim()

    val yyyy = defaultYear
    val mm = parsed.month.toString().padStart(2, '0')
    val dd = parsed.day.toString().padStart(2, '0')
    entries += Entry(date = "$yyyy-$mm-$dd", shift = shiftText)
  }

  val jsonEntries = entries.joinToString(",") { """{"date":"${it.date}","shift":"${it.shift}"}""" }
  return """{"year":$defaultYear,"month":$defaultMonth,"entries":[$jsonEntries]}"""
}