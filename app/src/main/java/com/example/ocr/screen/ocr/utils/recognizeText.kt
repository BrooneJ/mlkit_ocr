package com.example.ocr.screen.ocr.utils

import android.graphics.Bitmap
import com.example.ocr.screen.ocr.cluster1D
import com.example.ocr.screen.ocr.dateRegex
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

suspend fun recognizeText(bitmap: Bitmap): Text {
  val input = InputImage.fromBitmap(bitmap, 0)
  val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
  return withContext(Dispatchers.IO) {
    Tasks.await(recognizer.process(input))
  }
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

enum class RowType { Header, Body }

fun detectEdgesInRow(
  bitmap: Bitmap,
  row: RectI,
  rowType: RowType,
  minCellW: Int = 36
): List<Int> {
  val proj = verticalProjection(bitmap, row)

  // Estimate “character scale” more generously than row.height/4
  val r = maxOf((row.height * 0.5f).toInt(), 8)           // ~half the row height, clamp ≥8px
  val short = smooth(proj, radius = r)                     // removes stroke jitter
  val long = smooth(proj, radius = r * 3)                 // local baseline (tinted backgrounds)
  val detail = IntArray(proj.size) { short[it] - long[it] } // high-pass

  val lookForValleys =
    (rowType == RowType.Header)         // Header → valleys, Body (with grid) → peaks
  val raw = pickBoundariesWithWidth(detail, row.width, r, lookForValleys)

  return enforceMinCellWidth(raw, minCellW).map { row.left + it }
}

private fun pickBoundariesWithWidth(
  s: IntArray,
  imgWidth: Int,
  charPx: Int,
  lookForValleys: Boolean
): List<Int> {
  if (s.isEmpty()) return listOf(0, imgWidth)

  // Normalize 0..1
  val min = s.minOrNull()!!.toDouble()
  val max = s.maxOrNull()!!.toDouble()
  val range = (max - min).coerceAtLeast(1.0)
  val n = s.size
  val norm = DoubleArray(n) { (s[it] - min) / range }

  // Local average window (for prominence)
  val R = maxOf(charPx, 8)
  fun localAvg(i: Int): Double {
    val a = (i - R).coerceAtLeast(0)
    val b = (i + R).coerceAtMost(n - 1)
    var sum = 0.0
    for (k in a..b) sum += norm[k]
    return sum / (b - a + 1)
  }

  // Find extrema
  data class Cand(val x: Int, val prom: Double, val width: Int)

  val cand = mutableListOf<Cand>()
  for (i in 1 until n - 1) {
    val v = norm[i]
    val isExt =
      if (lookForValleys) v <= norm[i - 1] && v <= norm[i + 1]
      else v >= norm[i - 1] && v >= norm[i + 1]
    if (!isExt) continue

    val base = localAvg(i)
    val prom = if (lookForValleys) (base - v) else (v - base)
    if (prom <= 0.0) continue

    // WIDTH: expand left/right until we cross back toward base
    val thresh = if (lookForValleys) base - prom * 0.5 else base + prom * 0.5
    var l = i; while (l > 0 && (if (lookForValleys) norm[l] <= thresh else norm[l] >= thresh)) l--
    var r =
      i; while (r < n - 1 && (if (lookForValleys) norm[r] <= thresh else norm[r] >= thresh)) r++
    val width = (r - l).coerceAtLeast(1)

    // Reject narrow strokes: need at least ~0.6×char width
    if (width >= (charPx * 0.6).toInt()) cand += Cand(i, prom, width)
  }

  if (cand.isEmpty()) return listOf(0, imgWidth)

  // Adaptive prominence: keep stronger structures
  val proms = cand.map { it.prom }.sorted()
  val med = proms[proms.size / 2]
  val mad = proms.map { kotlin.math.abs(it - med) }.sorted()[proms.size / 2]
  val promThresh = (med + 0.30 * mad).coerceAtLeast(0.12)  // floor ~0.12

  val filtered = cand
    .filter { it.prom >= promThresh }
    .sortedByDescending { it.prom }

  // Non-max suppression by distance
  val minGap = maxOf(charPx * 2, imgWidth / 16)
  val picked = mutableListOf<Int>()
  for (c in filtered) if (picked.none { kotlin.math.abs(it - c.x) < minGap }) picked += c.x
  picked.sort()

  return buildList { add(0); addAll(picked); add(imgWidth) }
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
    strokeWidth = 10f
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

fun splitHeadBandByEdges(header: Bitmap, edges: List<Int>): List<Bitmap> {
  val cells = mutableListOf<Bitmap>()
  for (i in 0 until edges.size - 1) {
    val x1 = edges[i].coerceIn(0, header.width)
    val x2 = edges[i + 1].coerceIn(0, header.width)
    if (x2 - x1 < 8) continue
    val cell = Bitmap.createBitmap(header, x1, 0, x2 - x1, header.height)
    cells += cell
  }
  return cells
}

fun minCellWidth(row: RectI, charPx: Int, expectedCols: Int? = null): Int {
  val byChar = (1.6f * charPx).toInt()                // ~1.4–1.8 × char width
  val byPercent = (0.03f * row.width).toInt()         // ~3% of row width (scale with resolution)
  val byLayout = expectedCols?.let {                  // if you know there are ~7 or ~31 columns
    (0.35f * row.width / it).toInt()                  // cells shouldn’t be tinier than this
  }

  // pick a conservative floor, but don’t explode if byLayout is null
  val base = maxOf(byChar, byPercent, 24)
  return if (byLayout != null) minOf(base, (row.width / expectedCols)/*upper guard*/)
  else base
}
