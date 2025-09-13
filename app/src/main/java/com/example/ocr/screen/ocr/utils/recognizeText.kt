package com.example.ocr.screen.ocr.utils

import android.graphics.Bitmap
import android.util.Log
import com.example.ocr.screen.ocr.cluster1D
import com.example.ocr.screen.ocr.dateRegex
import com.example.ocr.screen.ocr.weekdayRegex
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RectI(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  val width get() = right - left
  val height get() = bottom - top
}

data class RowBand(val index: Int, val rect: RectI)

data class TableLayout(
  val headerBand: RectI,
  val columns: List<Int>,
  val rows: List<RectI>,
)

suspend fun recognizeText(bitmap: Bitmap): Text {
  val input = InputImage.fromBitmap(bitmap, 0)
  val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
  return withContext(Dispatchers.IO) {
    Tasks.await(recognizer.process(input))
  }
}

fun headerBandOf(bitmap: Bitmap, ratio: Float = 0.15f): RectI {
  val h = bitmap.height
  val bandHeight = (h * ratio).toInt().coerceAtLeast(40)
  return RectI(0, 0, bitmap.width, bandHeight.coerceAtMost(h))
}

private fun median(values: List<Float>): Float {
  if (values.isEmpty()) return 0f
  val s = values.sorted()
  val m = s.size / 2
  return if (s.size % 2 == 1) s[m] else ((s[m - 1] + s[m]) / 2f)
}

fun headerBandFromWords(words: List<OcrWord>, imageWidth: Int): RectI? {
  if (words.isEmpty()) return null

  val hMed = median(words.map { it.h })
  val rowGap = if (hMed > 0f) hMed * 0.9f else 24f

  val rows: List<List<OcrWord>> = cluster1D(words, key = { it.cy }, gap = rowGap)

  data class RowScore(val rowIdx: Int, val score: Int, val ys: Pair<Int, Int>)

  val rowScores = rows.mapIndexed { idx, row ->
    val hitsDate = row.count { dateRegex.matches(it.text.trim()) }
    val hitsDayOnly = row.count { Regex("""^\d{1,2}$""").matches(it.text.trim()) }
    val score = hitsDate * 2 + hitsDayOnly
    val yTop = row.minOf { it.top }
    val yBot = row.maxOf { it.bottom }
    RowScore(idx, score, yTop to yBot)
  }

  val best = rowScores.maxByOrNull { it.score }
  val selected = when {
    best == null -> return null
    best.score > 0 -> best
    else -> rowScores.minByOrNull { it.ys.first }!!
  }

  val margin = (hMed * 0.5f).toInt().coerceAtLeast(8)
  val y1 = (selected.ys.first - margin).coerceAtLeast(0)
  val y2 = (selected.ys.second + margin)

  return RectI(0, y1, imageWidth, y2)
}

fun bodyBandFromWords(
  words: List<OcrWord>,
  headerBand: RectI,
  imageWidth: Int,
  imageHeight: Int,
  margin: Int = 8
): RectI? {
  val bodyWords = words.filter { it.bottom > headerBand.bottom }
  if (bodyWords.isEmpty()) return null
  val top = (bodyWords.minOf { it.top } - margin).coerceAtLeast(0)
  val bottom = (bodyWords.maxOf { it.bottom } + margin).coerceAtMost(imageHeight)
  return RectI(0, top, imageWidth, bottom)
}

fun verticalProjection(bitmap: Bitmap, roi: RectI): IntArray {
  val r = clampToBitmap(roi, bitmap.width, bitmap.height)
  val width = r.width
  val height = r.height
  val pixels = IntArray(width * height)
  bitmap.getPixels(pixels, 0, width, r.left, r.top, width, height)
  val colSums = IntArray(width)
  for (y in 0 until height) {
    for (x in 0 until width) {
      val p = pixels[y * width + x]
      val rC = (p shr 16) and 0xFF
      val gC = (p shr 8) and 0xFF
      val bC = p and 0xFF
      val luma = (0.299 * rC + 0.587 * gC + 0.114 * bC).toInt()
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
  val min = smoothed.minOrNull()!!
  val max = smoothed.maxOrNull()!!
  val minGap = maxOf(charWidthPx * 2, imgWidth / 16)

  val depthThresh = min + (max - min) * 0.25

  val candidates = mutableListOf<Int>()
  for (x in 1 until smoothed.lastIndex) {
    val v = smoothed[x]
    if (v <= smoothed[x - 1] && v <= smoothed[x + 1] && v <= depthThresh) {
      val leftPeak = ((x - charWidthPx).coerceAtLeast(0)..x).maxOf { smoothed[it] }
      val rightPeak = (x..(x + charWidthPx).coerceAtMost(smoothed.lastIndex)).maxOf { smoothed[it] }
      val prominence = minOf(leftPeak - v, rightPeak - v)
      if (prominence >= (max - min) * 0.10) { // 최소 돌출도
        candidates += x
      }
    }
  }

  val picked = mutableListOf<Int>()
  for (c in candidates) if (picked.isEmpty() || c - picked.last() >= minGap) picked += c

  return buildList {
    add(0)
    addAll(picked)
    add(imgWidth)
  }
}

data class Peak(val x: Int, val prom: Double)

fun pickColumnBoundariesRobust(
  s: IntArray,
  imgWidth: Int,
  charWidthPx: Int,
  lookForValleys: Boolean = true
): List<Int> {
  if (s.isEmpty()) return listOf(0, imgWidth)

  // 1) 0..1 정규화
  val min = s.minOrNull()!!.toDouble()
  val max = s.maxOrNull()!!.toDouble()
  val range = (max - min).coerceAtLeast(1.0)
  val n = s.size
  val norm = DoubleArray(n) { (s[it] - min) / range }

  val R = charWidthPx.coerceAtLeast(8)
  fun localAvg(i: Int): Double {
    val a = (i - R).coerceAtLeast(0)
    val b = (i + R).coerceAtMost(n - 1)
    var sum = 0.0
    for (k in a..b) sum += norm[k]
    return sum / (b - a + 1)
  }

  val cand = mutableListOf<Peak>()
  for (i in 1 until n - 1) {
    val v = norm[i]
    val isExtrema =
      if (lookForValleys) (v <= norm[i - 1] && v <= norm[i + 1])
      else (v >= norm[i - 1] && v >= norm[i + 1])

    if (!isExtrema) continue

    val lavg = localAvg(i)
    val prom = if (lookForValleys) (lavg - v) else (v - lavg)
    if (prom >= 0.10) {
      cand += Peak(i, prom)
    }
  }

  if (cand.isEmpty()) return listOf(0, imgWidth)

  val minGap = maxOf(charWidthPx * 2, imgWidth / 16)

  val picked = mutableListOf<Int>()
  for (p in cand.sortedByDescending { it.prom }) {
    if (picked.none { kotlin.math.abs(it - p.x) < minGap }) {
      picked += p.x
    }
  }

  picked.sort()
  return buildList {
    add(0); addAll(picked); add(imgWidth)
  }
}

fun roughCharWidthPx(bitmap: Bitmap, header: RectI): Int {
  return (bitmap.width / 48f).toInt().coerceAtLeast(6)
}

suspend fun recoverLayout(bitmap: Bitmap): TableLayout {
  val header = headerBandOf(bitmap)
  val proj = withContext(Dispatchers.Default) { verticalProjection(bitmap, header) }
  val charWidth = roughCharWidthPx(bitmap, header)
  val smoothed = smooth(proj, radius = charWidth)

  val a = pickColumnBoundaries(smoothed, header.width, charWidth)
  val b = pickColumnBoundariesRobust(proj, header.width, charWidth)

  val raw = if (b.size <= a.size) b else a

  val columns = enforceMinCellWidth(raw, minWidth = 36)

  return TableLayout(headerBand = header, columns = columns, rows = emptyList())
}

fun cropBitmap(src: Bitmap, rect: RectI): Bitmap =
  Bitmap.createBitmap(src, rect.left, rect.top, rect.width, rect.height)

fun clampRectToMin(r: RectI, imgW: Int, imgH: Int, minW: Int, minH: Int, pad: Int = 6): RectI {
  var x1 = r.left
  var x2 = r.right
  var y1 = r.top
  var y2 = r.bottom

  if (x2 - x1 < minW) {
    val need = minW - (x2 - x1)
    x1 = (x1 - need / 2 - pad).coerceAtLeast(0)
    x2 = (x2 + need / 2 + pad).coerceAtMost(imgW)
  }
  if (y2 - y1 < minH) {
    val need = minH - (y2 - y1)
    y1 = (y1 - need / 2 - pad).coerceAtLeast(0)
    y2 = (y2 + need / 2 + pad).coerceAtMost(imgH)
  }
  return RectI(x1, y1, x2, y2)
}

suspend fun ocrCellText(bitmap: Bitmap, cell: RectI): String {
  val minDim = 32
  val safe = clampRectToMin(cell, bitmap.width, bitmap.height, minDim, minDim, pad = 6)
  if (safe.width < minDim || safe.height < minDim) return "" // 최후의 방어
  val sub = cropBitmap(bitmap, safe)
  val text = recognizeText(sub).text
  return text.replace("[()（）\\[\\]{}]".toRegex(), " ").trim()
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

fun isHeaderWord(word: OcrWord): Boolean {
  val text = word.text.replace(" ", "").trim()
  return dateRegex.matches(text) || weekdayRegex.matches(text)
}

fun buildRowBands(
  words: List<OcrWord>,
  imageWidth: Int,
  gapY: Float? = null,
  padding: Int = 8,
): List<RowBand> {
  if (words.isEmpty()) return emptyList()

  val avgHeight = (words.sumOf { it.h.toDouble() } / words.size).toFloat()
  val gY = gapY ?: (avgHeight * 0.9f)

  val rows = cluster1D(items = words, key = { it.cy }, gap = gY)
  Log.d("RowBands", "Detected ${rows.size} rows with gapY=$gY (avgH=$avgHeight)")
  return rows.mapIndexed { idx, group ->
    val top = (group.minOf { it.top } - padding).coerceAtLeast(0)
    val bottom = group.maxOf { it.bottom } + padding
    RowBand(idx, RectI(0, top, imageWidth, bottom))
  }
}

fun enforceMinCellWidth(edges: List<Int>, minWidth: Int): List<Int> {
  if (edges.size <= 2) return edges
  val out = mutableListOf<Int>()
  var i = 0
  while (i < edges.size - 1) {
    var j = i + 1
    while (j < edges.size && edges[j] - edges[i] < minWidth) j++
    out += edges[i]
    i = j
  }
  if (out.last() != edges.last()) out += edges.last()
  return out
}

fun roughCharWidth(row: RectI) = maxOf(row.height, 6) // 경험값

// 2) 행별 열 경계
fun detectEdgesInRow(
  bitmap: Bitmap,
  row: RectI,
  usePeaksFirst: Boolean = true,
  minCellW: Int = 36
): List<Int> {
  val proj = verticalProjection(bitmap, row)
  val r = roughCharWidth(row)
  val smoothed = smooth(proj, radius = r)

  val edgesPeak = pickColumnBoundariesRobust(proj, row.width, r, lookForValleys = false)
  val edgesValley = pickColumnBoundaries(smoothed, row.width, r) // 밸리 버전
  val raw = when {
    usePeaksFirst && edgesPeak.size >= 3 -> edgesPeak
    else -> edgesValley
  }
  return enforceMinCellWidth(raw, minCellW).map { row.left + it } // ROI→절대좌표
}

// 3) 셀 OCR
suspend fun ocrCell(bitmap: Bitmap, x1: Int, x2: Int, row: RectI): String {
  val shrink = 3 // 그리드선 회피
  val cell = RectI(
    (x1 + shrink).coerceAtMost(x2),
    row.top,
    (x2 - shrink).coerceAtLeast(x1),
    row.bottom
  )
  return ocrCellText(bitmap, cell) // 내부에서 32x32 보정
}

suspend fun readHeaderDates(
  bitmap: Bitmap,
  header: RectI,
//  columns: List<Int>,           // recoverLayout()에서 나온 열 경계들
  defaultYear: Int,
  defaultMonth: Int
): List<ParsedDate?> {
  val columns = recoverLayout(bitmap).columns
  val results = mutableListOf<ParsedDate?>()
  for (i in 0 until columns.size - 1) {
    val x1 = columns[i];
    val x2 = columns[i + 1]
    if (x2 - x1 < 12) {
      results += null; continue
    } // 너무 좁은 칸은 스킵
    val text = ocrCell(bitmap, x1, x2, header)
    results += parseDate(text, defaultYear, defaultMonth) // "9/1" 또는 "1" → (month, day)
  }
  return results
}

fun clampToBitmap(r: RectI, imageWidth: Int, imageHeight: Int): RectI {
  val x1 = r.left.coerceIn(0, imageWidth)
  val x2 = r.right.coerceIn(0, imageWidth)
  val y1 = r.top.coerceIn(0, imageHeight)
  val y2 = r.bottom.coerceIn(0, imageHeight)

  val w = (x2 - x1).coerceAtLeast(1)
  val h = (y2 - y1).coerceAtLeast(1)
  return RectI(x1, y1, x1 + w, y1 + h)
}

fun cropToBitmap(src: Bitmap, rect: RectI): Bitmap {
  val safe = clampToBitmap(rect, src.width, src.height)
  return Bitmap.createBitmap(src, safe.left, safe.top, safe.width, safe.height)
}

fun drawColumnDebug(src: Bitmap, roi: RectI, edgesAbsX: List<Int>): Bitmap {
  // Make a mutable copy to draw on
  val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
  val c = android.graphics.Canvas(bmp)

  val boxPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.GREEN
    style = android.graphics.Paint.Style.STROKE
    strokeWidth = 5f
  }
  c.drawRect(
    android.graphics.Rect(roi.left, roi.top, roi.right, roi.bottom),
    boxPaint
  )

  val edgePaint = android.graphics.Paint().apply {
    color = android.graphics.Color.RED
    strokeWidth = 5f
  }
  edgesAbsX.forEach { x ->
    c.drawLine(
      x.toFloat(), roi.top.toFloat(),
      x.toFloat(), roi.bottom.toFloat(),
      edgePaint
    )
  }
  return bmp
}