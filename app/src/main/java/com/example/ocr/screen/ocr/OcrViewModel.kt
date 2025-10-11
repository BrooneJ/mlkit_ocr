package com.example.ocr.screen.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ocr.navigation.OcrRoute
import com.example.ocr.network.ApiRepository
import com.example.ocr.screen.ocr.constant.CHATGPT_MODEL
import com.example.ocr.screen.ocr.constant.PROMPT_ANALYZE_SCHEDULE
import com.example.ocr.screen.ocr.utils.RectI
import com.example.ocr.screen.ocr.utils.RowType
import com.example.ocr.screen.ocr.utils.ScheduleParser
import com.example.ocr.screen.ocr.utils.bodyBandFromWords
import com.example.ocr.screen.ocr.utils.cropToBitmap
import com.example.ocr.screen.ocr.utils.detectEdgesInRow
import com.example.ocr.screen.ocr.utils.drawColumnDebug
import com.example.ocr.screen.ocr.utils.enforceMinCellWidth
import com.example.ocr.screen.ocr.utils.headerBandFromWords
import com.example.ocr.screen.ocr.utils.minCellWidth
import com.example.ocr.screen.ocr.utils.pickColumnBoundaries
import com.example.ocr.screen.ocr.utils.pickColumnBoundariesAdaptive
import com.example.ocr.screen.ocr.utils.pickColumnBoundariesRobust
import com.example.ocr.screen.ocr.utils.recognizeText
import com.example.ocr.screen.ocr.utils.recognizeWordsFromUri
import com.example.ocr.screen.ocr.utils.roughCharWidth
import com.example.ocr.screen.ocr.utils.smooth
import com.example.ocr.screen.ocr.utils.splitHeadBandByEdges
import com.example.ocr.screen.ocr.utils.verticalProjection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
  handle: SavedStateHandle,
  private val scheduleParser: ScheduleParser,
  private val repo: ApiRepository,
) : ViewModel() {
  val args: OcrRoute = handle.toRoute<OcrRoute>()
  val targetUri: Uri? = args.uri

  private val _text = MutableStateFlow<String?>(null)
  val text = _text.asStateFlow()

  private val _headerPreview = MutableStateFlow<Bitmap?>(null)
  val headerPreview = _headerPreview.asStateFlow()

  private val _bodyPreview = MutableStateFlow<Bitmap?>(null)
  val bodyPreview = _bodyPreview.asStateFlow()

  private val _adaptive = MutableStateFlow<Bitmap?>(null)
  val adaptive = _adaptive.asStateFlow()

  private val _fromPeak = MutableStateFlow<Bitmap?>(null)
  val fromPeak = _fromPeak.asStateFlow()

  private val _fromWidth = MutableStateFlow<Bitmap?>(null)
  val fromWidth = _fromWidth.asStateFlow()

  private val _fromValley = MutableStateFlow<Bitmap?>(null)
  val fromValley = _fromValley.asStateFlow()

  private val _enforce = MutableStateFlow<Bitmap?>(null)
  val enforce = _enforce.asStateFlow()

  private val _edges = MutableStateFlow<ColumnEdges?>(null)

  private val _dateCells = MutableStateFlow<List<Bitmap>>(emptyList())
  val dateCells = _dateCells.asStateFlow()

  private val _workCells = MutableStateFlow<List<Bitmap>>(emptyList())
  val workCells = _workCells.asStateFlow()

  private val _dateList = MutableStateFlow<List<String>>(emptyList())
  private val _scheduleList = MutableStateFlow<List<String>>(emptyList())

  private val _resultMap = MutableStateFlow<List<Pair<String, String>>>(emptyList())

  private fun pickEdgesFor(type: OcrType): List<Int>? = when (type) {
    OcrType.ADAPTIVE -> _edges.value?.adaptive
    OcrType.FROMVALLEY -> _edges.value?.valleys
    OcrType.FROMPEAK -> _edges.value?.peaks
    OcrType.FROMWIDTH -> _edges.value?.width
    OcrType.ENFORCE -> _edges.value?.enforced
  }

  private fun ensureMinCanvas(src: Bitmap, minW: Int = 32, minH: Int = 32): Bitmap {
    if (src.width >= minW && src.height >= minH) return src
    val w = maxOf(minW, src.width)
    val h = maxOf(minH, src.height)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(out)
    c.drawColor(android.graphics.Color.WHITE)
    val dx = ((w - src.width) / 2f)
    val dy = ((h - src.height) / 2f)
    c.drawBitmap(src, dx, dy, null)
    return out
  }

  private suspend fun recognizeCells(bitmaps: List<Bitmap>): List<String> {
    // Sequential; if you want speed, you can use limited concurrency with async.
    val results = mutableListOf<String>()
    for (cell in bitmaps) {
      val safe = ensureMinCanvas(cell, 32, 32)
      val text = recognizeText(safe).text.trim()
      results += if (text.isBlank()) "??" else text
    }
    return results
  }

  private suspend fun runOcrWithEdges(type: OcrType) {
    val header = _headerPreview.value ?: return
    val body = _bodyPreview.value ?: return
    val edges = pickEdgesFor(type) ?: return

    // Split bitmaps by edges
    val dateCells = splitHeadBandByEdges(header, edges)
    val workCells = splitHeadBandByEdges(body, edges)

    // OCR
    val dates = recognizeCells(dateCells)
    val works = recognizeCells(workCells)

    // Publish state (replace, don’t append)
    _dateCells.value = dateCells
    _workCells.value = workCells
    _dateList.value = dates
    _scheduleList.value = works

    // Zip safely to the shorter length
    _resultMap.value = dates.zip(works)

    Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
  }

  fun onAction(action: OcrAction) {
    when (action) {
      is OcrAction.CardChosen -> {
        Log.d("OcrViewModel", "${action.type} selected")
        viewModelScope.launch { runOcrWithEdges(action.type) }
      }
    }
  }

  fun processImage(context: Context) {
    val uri = targetUri ?: return
    viewModelScope.launch {
      val bitmap = loadBitmap(context, uri) ?: return@launch
      val words = recognizeWordsFromUri(context, uri)
      words.forEach {
        Log.v("OcrViewModel", "Word: '${it.text}' @(${it.left},${it.top},${it.right},${it.bottom})")
      }
      val headerBand: RectI? = headerBandFromWords(words, imageWidth = bitmap.width)

      if (headerBand == null) return@launch
      _headerPreview.value = cropToBitmap(bitmap, headerBand)

      if (_headerPreview.value != null) {
        val proj = verticalProjection(_headerPreview.value!!, headerBand)
        val r = roughCharWidth(headerBand)
        val smoothed = smooth(proj, r)
        val minW = minCellWidth(headerBand, r)

        val edgesFromValleys = pickColumnBoundaries(smoothed, headerBand.width, r)
        val edgesFromPeaks = pickColumnBoundariesRobust(proj, headerBand.width, r, false)
        val charW = (headerBand.height * 0.6f).toInt().coerceAtLeast(8)
        val edges = pickColumnBoundariesAdaptive(proj, headerBand.width, charW, 40).map {
          it + headerBand.left
        }
        val edgesEnforce = enforceMinCellWidth(edges, minW)
        val edgesFromWidth = detectEdgesInRow(bitmap, headerBand, RowType.Header)

        _edges.value = ColumnEdges(
          roi = headerBand,
          valleys = edgesFromValleys,
          peaks = edgesFromPeaks,
          adaptive = edges,
          width = edgesFromWidth,
          enforced = edgesEnforce,
        )

        _adaptive.value = cropToBitmap(drawColumnDebug(bitmap, headerBand, edges), headerBand)
        _fromPeak.value =
          cropToBitmap(drawColumnDebug(bitmap, headerBand, edgesFromPeaks), headerBand)
        _fromWidth.value =
          cropToBitmap(drawColumnDebug(bitmap, headerBand, edgesFromWidth), headerBand)
        _fromValley.value =
          cropToBitmap(drawColumnDebug(bitmap, headerBand, edgesFromValleys), headerBand)
        _enforce.value = cropToBitmap(drawColumnDebug(bitmap, headerBand, edgesEnforce), headerBand)
      }

      val bodyBand: RectI =
        bodyBandFromWords(words, headerBand, imageWidth = bitmap.width, imageHeight = bitmap.height)
          ?: return@launch

      _bodyPreview.value = cropToBitmap(bitmap, bodyBand)

      val testWord = recognizeText(_headerPreview.value ?: return@launch)
      Log.d("OcrViewModel", "Test recognized text: $testWord")
    }
  }


  var output by mutableStateOf("")
    private set
  var isLoading by mutableStateOf(false)
    private set
  var error by mutableStateOf<String?>(null)
    private set

  fun analyzeSchedule() {
    Log.d("AI Response", "Analyzing schedule...")
    val prompt = PROMPT_ANALYZE_SCHEDULE.trimIndent()

    val uri = targetUri
    if (uri == null) {
      error = "Image URI is missing. Cannot analyze schedule."
      return
    }
    viewModelScope.launch {
      isLoading = true
      error = null
      try {
        output = repo.askWithImage(
          model = CHATGPT_MODEL,
          prompt = prompt,
          imageUri = uri,
        )
        Log.d("AI Response", "Received response: $output")
        val scheduleList = scheduleParser.parse(output)
        // TODO: pass this data to UI on the next screen.
        Log.d("AI Response", "Parsed schedule: $scheduleList")
      } catch (e: HttpException) {
        val code = e.code()
        val body = e.response()?.errorBody()?.string()
        Log.e("AI Response", "HTTP $code, error=$body")
        error = "API error: HTTP $code${if (!body.isNullOrBlank()) ", $body" else ""}"
      } finally {
        isLoading = false
        Log.d("AI Response", "API call finished")
      }
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

val dateRegex = Regex("""^\d{1,2}/\d{1,2}$""")

data class ColumnEdges(
  val roi: RectI,
  val valleys: List<Int>,
  val peaks: List<Int>,
  val adaptive: List<Int>,
  val width: List<Int>,
  val enforced: List<Int>,
)