@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.ocr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.screen.ocr.components.SelectableCard

@Composable
fun OcrScreen(
  viewModel: OcrViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(viewModel) {
    viewModel.processImage(context)
  }

  val scrollState = rememberScrollState()


  val text by viewModel.text.collectAsStateWithLifecycle()
  val headerBitmap by viewModel.headerPreview.collectAsStateWithLifecycle()
  val bodyBitmap by viewModel.bodyPreview.collectAsStateWithLifecycle()
  val logic1 by viewModel.logic1.collectAsStateWithLifecycle()
  val logic2 by viewModel.logic2.collectAsStateWithLifecycle()
  val logic3 by viewModel.logic3.collectAsStateWithLifecycle()
  val logic4 by viewModel.logic4.collectAsStateWithLifecycle()
  val logic5 by viewModel.logic5.collectAsStateWithLifecycle()

  val dateCells by viewModel.dateCells.collectAsStateWithLifecycle()
  val workCells by viewModel.workCells.collectAsStateWithLifecycle()

  if (text == null) {
    Text("No Text")
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(text = "OCR Screen") },
        navigationIcon = {
          IconButton(onClick = { onBack() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
      )
    }
  ) { paddingValues ->
    Surface(modifier = Modifier.padding(paddingValues)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(16.dp)

      ) {
        Text(text = "Select appropriate card:")

        if (headerBitmap != null) {
          Image(headerBitmap!!.asImageBitmap(), contentDescription = null)
        }
        if (bodyBitmap != null) {
          Image(bodyBitmap!!.asImageBitmap(), contentDescription = null)
        }

        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = logic1,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.ADAPTIVE)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = logic2,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMPEAK)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = logic3,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMWIDTH)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = logic4,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMVALLEY)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = logic5,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.ENFORCE)
            )
          }
        )

        Spacer(modifier = Modifier.padding(15.dp))
        Text(text = "Detected date cells:")
        Spacer(modifier = Modifier.padding(8.dp))
        dateCells.forEach { cellBitmap ->
          Image(cellBitmap.asImageBitmap(), contentDescription = null)
          Spacer(modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.padding(15.dp))
        workCells.forEach { cellBitmap ->
          Image(cellBitmap.asImageBitmap(), contentDescription = null)
          Spacer(modifier = Modifier.padding(8.dp))
        }
      }
    }
  }
}

data class Peak(val x: Int, val prom: Double)

private fun medianD(xs: List<Double>): Double {
  if (xs.isEmpty()) return 0.0
  val s = xs.sorted()
  val m = s.size / 2
  return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
}

private fun smoothInt(signal: IntArray, r: Int): IntArray {
  if (signal.isEmpty()) return signal
  val n = signal.size
  val rr = r.coerceAtLeast(1)
  val out = IntArray(n)
  var sum = 0L
  var count = 0
  var L = 0
  var R = -1
  for (i in 0 until n) {
    val targetR = (i + rr).coerceAtMost(n - 1)
    while (R < targetR) {
      R++; sum += signal[R]; count++
    }
    val targetL = (i - rr)
    while (L < targetL) {
      sum -= signal[L]; L++; count--
    }
    out[i] = (sum / count).toInt()
  }
  return out
}

/**
 * More tolerant column boundary detector.
 *
 * @param s vertical projection over the header row (IntArray of width)
 * @param imgWidth ROI width
 * @param charWidthPx rough character width in px (≈ rowHeight * 0.6..0.8 is often good)
 * @param minCellW minimum practical cell width (px)
 * @param expectedCols optional expected number of columns (dates); used only to fill obvious missing splits
 */
fun pickColumnBoundariesAdaptive(
  s: IntArray,
  imgWidth: Int,
  charWidthPx: Int,
  minCellW: Int = 36,
  expectedCols: Int? = null
): List<Int> {
  if (s.isEmpty()) return listOf(0, imgWidth)
  val n = s.size

  // 1) High-pass to remove background/tint trend
  val bg = smoothInt(s, r = (charWidthPx * 4).coerceAtLeast(imgWidth / 32))
  val hp = DoubleArray(n) { (s[it] - bg[it]).toDouble() } // can be negative

  // Helper to collect extrema against local mean
  fun collect(lookForValleys: Boolean): MutableList<Peak> {
    val R = (charWidthPx).coerceAtLeast(6)
    fun localAvg(i: Int): Double {
      val a = (i - R).coerceAtLeast(0)
      val b = (i + R).coerceAtMost(n - 1)
      var sum = 0.0
      for (k in a..b) sum += hp[k]
      return sum / (b - a + 1)
    }

    val cand = mutableListOf<Peak>()
    for (i in 1 until n - 1) {
      val v = hp[i]
      val isExtrema =
        if (lookForValleys) (v <= hp[i - 1] && v <= hp[i + 1]) else (v >= hp[i - 1] && v >= hp[i + 1])
      if (!isExtrema) continue
      val lavg = localAvg(i)
      val prom = if (lookForValleys) (lavg - v) else (v - lavg) // prominence vs local baseline
      cand += Peak(i, prom)
    }
    // Adaptive threshold: keep strongest by distribution
    val proms = cand.map { it.prom }
    val med = medianD(proms)
    val mad = medianD(proms.map { kotlin.math.abs(it - med) })
    val thr = maxOf(0.05, med + 0.5 * mad) // tune 0.3..0.8 * MAD if needed
    return cand.filter { it.prom >= thr }.toMutableList()
  }

  val valleys = collect(lookForValleys = true)
  val peaks = collect(lookForValleys = false)

  // Union candidates, then NMS with a gentler minGap
  val minGap = maxOf((charWidthPx * 1.3).toInt(), imgWidth / 24, minCellW)
  val all = (valleys + peaks).sortedByDescending { it.prom }
  val picked = mutableListOf<Int>()
  for (p in all) {
    if (picked.none { kotlin.math.abs(it - p.x) < minGap }) picked += p.x
  }
  picked.sort()

  // Always include edges
  val edges = mutableListOf(0)
  edges += picked
  if (edges.last() != imgWidth) edges += imgWidth

  // 2) Enforce min cell width & fill missing splits in over-wide spans
  // Remove edges that create too narrow cells
  val cleaned = mutableListOf<Int>()
  var i = 0
  while (i < edges.size - 1) {
    var j = i + 1
    while (j < edges.size && edges[j] - edges[i] < minCellW) j++
    cleaned += edges[i]
    i = j
  }
  if (cleaned.last() != edges.last()) cleaned += edges.last()

  // Optional filling: if we expect more columns, split the widest spans
  val wantCells = expectedCols ?: 0
  fun cellsCount(list: List<Int>) = (list.size - 1)
  var out = cleaned.toMutableList()
  if (wantCells > 0 && cellsCount(out) < wantCells) {
    while (cellsCount(out) < wantCells) {
      // Find widest span
      var bestIdx = -1
      var bestW = 0
      for (k in 0 until out.size - 1) {
        val w = out[k + 1] - out[k]
        if (w > bestW) {
          bestW = w; bestIdx = k
        }
      }
      if (bestIdx < 0 || bestW < minCellW * 2) break // nothing reasonable to split
      val mid = (out[bestIdx] + out[bestIdx + 1]) / 2
      out.add(mid)
      out.sort()
    }
  }
  return out
}