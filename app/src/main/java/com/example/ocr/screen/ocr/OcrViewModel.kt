package com.example.ocr.screen.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ocr.navigation.OcrRoute
import com.example.ocr.screen.ocr.utils.OcrWord
import com.example.ocr.screen.ocr.utils.RectI
import com.example.ocr.screen.ocr.utils.buildRowBands
import com.example.ocr.screen.ocr.utils.extractScheduleJson
import com.example.ocr.screen.ocr.utils.headerBandFromWords
import com.example.ocr.screen.ocr.utils.isHeaderWord
import com.example.ocr.screen.ocr.utils.recognizeWordsFromUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OcrViewModel(
  handle: SavedStateHandle,
) : ViewModel() {
  val args: OcrRoute = handle.toRoute<OcrRoute>()
  val targetUri: Uri? = args.uri

  private val _text = MutableStateFlow<String?>(null)
  val text = _text.asStateFlow()

  fun processImage(context: Context) {
    val uri = targetUri ?: return
    viewModelScope.launch {
      val bitmap = loadBitmap(context, uri) ?: return@launch
      val words = recognizeWordsFromUri(context, uri)
      words.forEach {
        Log.v("OcrViewModel", "Word: '${it.text}' @(${it.left},${it.top},${it.right},${it.bottom})")
      }
      val headerBand: RectI? = headerBandFromWords(words, imageWidth = bitmap.width)
//      val headerBand = headerBandFromWords(words, bitmap.width)
//        ?: RectI(0, 0, bitmap.width, (bitmap.height * 0.15f).toInt())
      val bodyWords = words.filter { !isHeaderWord(it) && it.bottom > headerBand!!.bottom }
      val rowBands = buildRowBands(bodyWords, imageWidth = bitmap.width)
      val targetBand: RectI = rowBands.maxByOrNull { it.rect.height }?.rect ?: run {
        RectI(0, headerBand!!.bottom, bitmap.width, bitmap.height)
      }
      val json = extractScheduleJson(
        bitmap,
        defaultYear = 2025,
        defaultMonth = 9,
        targetRowBand = targetBand
      )
      _text.value = json
      Log.d("ScheduleJson", json)
    }
  }
}

private fun loadBitmap(context: Context, uri: Uri, mutable: Boolean = false): Bitmap? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val src = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      decoder.isMutableRequired = mutable
    }
  } else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
  }
}

fun <T> cluster1D(
  items: List<T>,
  key: (T) -> Float,
  gap: Float
): List<List<T>> {
  if (items.isEmpty()) return emptyList()
  val sorted = items.sortedBy(key)
  val groups = mutableListOf<MutableList<T>>()
  var current = mutableListOf(sorted.first())
  for (i in 1 until sorted.size) {
    val prev = key(sorted[i - 1])
    val now = key(sorted[i])
    if (now - prev <= gap) {
      current += sorted[i]
    } else {
      groups += current
      current = mutableListOf(sorted[i])
    }
  }
  groups += current
  return groups
}

data class Stats(val avgW: Float, val avgH: Float)

fun stats(words: List<OcrWord>) = Stats(
  avgW = (words.sumOf { it.w.toDouble() } / words.size).toFloat(),
  avgH = (words.sumOf { it.h.toDouble() } / words.size).toFloat()
)

data class Column(val words: List<OcrWord>) {
  val cx = words.map { it.cx }.average().toFloat()
}

fun buildColumns(words: List<OcrWord>): List<Column> {
  val s = stats(words)
  val gapX = s.avgW * 1.2f
  val groups = cluster1D(words, key = { it.cx }, gap = gapX)
  return groups.map { Column(it) }.sortedBy { it.cx }
}

val dateRegex = Regex("""^\d{1,2}/\d{1,2}$""")
val weekdayRegex = Regex("""^[\(\（].+[\)\）]$""")

data class Cell(val rowIndex: Int, val text: String)
data class ColumnParsed(
  val dateText: String?, val weekdayText: String?,
  val cells: List<Cell>
)

fun parseColumn(col: Column): ColumnParsed {
  val header = mutableListOf<OcrWord>()
  val body = mutableListOf<OcrWord>()

  col.words.forEach { w ->
    val t = w.text.replace(" ", "").trim()
    when {
      dateRegex.matches(t) -> header += w
      weekdayRegex.matches(t) -> header += w
      else -> body += w
    }
  }

  val gapY = (stats(col.words).avgH) * 0.9f
  val rowGroups = cluster1D(body, key = { it.cy }, gap = gapY)
    .mapIndexed { idx, g ->
      val joined = g.sortedBy { it.cx }.joinToString("") { it.text }
      Cell(rowIndex = idx, text = joined)
    }

  val dateText = header.firstOrNull { dateRegex.matches(it.text) }?.text
  val weekdayText = header.firstOrNull { weekdayRegex.matches(it.text) }?.text
  return ColumnParsed(dateText, weekdayText, rowGroups)
}

data class DaySchedule(
  val date: String,
  val weekday: String?,
  val duty: String?
)

fun parseTable(words: List<OcrWord>): List<DaySchedule> {
  val columns = buildColumns(words)
  return columns.map { col ->
    val p = parseColumn(col)
    DaySchedule(
      date = p.dateText ?: "",
      weekday = p.weekdayText,
      duty = p.cells.lastOrNull()?.text
    )
  }
}