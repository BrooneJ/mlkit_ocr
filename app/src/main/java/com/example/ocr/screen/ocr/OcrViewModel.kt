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
import com.example.ocr.screen.ocr.utils.RectI
import com.example.ocr.screen.ocr.utils.RowType
import com.example.ocr.screen.ocr.utils.bodyBandFromWords
import com.example.ocr.screen.ocr.utils.cropToBitmap
import com.example.ocr.screen.ocr.utils.detectEdgesInRow
import com.example.ocr.screen.ocr.utils.drawColumnDebug
import com.example.ocr.screen.ocr.utils.enforceMinCellWidth
import com.example.ocr.screen.ocr.utils.headerBandFromWords
import com.example.ocr.screen.ocr.utils.minCellWidth
import com.example.ocr.screen.ocr.utils.pickColumnBoundaries
import com.example.ocr.screen.ocr.utils.pickColumnBoundariesRobust
import com.example.ocr.screen.ocr.utils.recognizeText
import com.example.ocr.screen.ocr.utils.recognizeWordsFromUri
import com.example.ocr.screen.ocr.utils.roughCharWidth
import com.example.ocr.screen.ocr.utils.smooth
import com.example.ocr.screen.ocr.utils.splitHeadBandByEdges
import com.example.ocr.screen.ocr.utils.verticalProjection
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

  fun onAction(action: OcrAction) {
    when (action) {
      is OcrAction.CardChosen -> {
        when (action.type) {
          OcrType.ADAPTIVE -> {
            if (_headerPreview.value == null) return
            if (_bodyPreview.value == null) return
            if (_edges.value?.adaptive.isNullOrEmpty()) return
            _dateCells.value = splitHeadBandByEdges(_headerPreview.value!!, _edges.value!!.adaptive)
            _workCells.value = splitHeadBandByEdges(_bodyPreview.value!!, _edges.value!!.adaptive)
            viewModelScope.launch {
              _dateCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _dateList.value = _dateList.value + "??"
                  } else {
                    _dateList.value = _dateList.value + result.text
                  }
                }
              }

              _workCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _scheduleList.value = _scheduleList.value + "??"
                  } else {
                    _scheduleList.value = _scheduleList.value + result.text
                  }
                }
              }

              _resultMap.value = _dateList.value.zip(_scheduleList.value)
              Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
            }
          }

          OcrType.FROMVALLEY -> {
            if (_headerPreview.value == null) return
            if (_edges.value?.valleys.isNullOrEmpty()) return
            _dateCells.value = splitHeadBandByEdges(_headerPreview.value!!, _edges.value!!.valleys)
            _workCells.value = splitHeadBandByEdges(_bodyPreview.value!!, _edges.value!!.valleys)
            viewModelScope.launch {
              _dateCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _dateList.value = _dateList.value + "??"
                  } else {
                    _dateList.value = _dateList.value + result.text
                  }
                }
              }

              _workCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _scheduleList.value = _scheduleList.value + "??"
                  } else {
                    _scheduleList.value = _scheduleList.value + result.text
                  }
                }
              }

              _resultMap.value = _dateList.value.zip(_scheduleList.value)
              Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
            }
          }

          OcrType.FROMPEAK -> {
            if (_headerPreview.value == null) return
            if (_edges.value?.peaks.isNullOrEmpty()) return
            _dateCells.value = splitHeadBandByEdges(_headerPreview.value!!, _edges.value!!.peaks)
            _workCells.value = splitHeadBandByEdges(_bodyPreview.value!!, _edges.value!!.peaks)
            viewModelScope.launch {
              _dateCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _dateList.value = _dateList.value + "??"
                  } else {
                    _dateList.value = _dateList.value + result.text
                  }
                }
              }

              _workCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _scheduleList.value = _scheduleList.value + "??"
                  } else {
                    _scheduleList.value = _scheduleList.value + result.text
                  }
                }
              }

              _resultMap.value = _dateList.value.zip(_scheduleList.value)
              Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
            }
          }

          OcrType.FROMWIDTH -> {
            if (_headerPreview.value == null) return
            if (_edges.value?.width.isNullOrEmpty()) return
            _dateCells.value = splitHeadBandByEdges(_headerPreview.value!!, _edges.value!!.width)
            _workCells.value = splitHeadBandByEdges(_bodyPreview.value!!, _edges.value!!.width)
            viewModelScope.launch {
              _dateCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _dateList.value = _dateList.value + "??"
                  } else {
                    _dateList.value = _dateList.value + result.text
                  }
                }
              }

              _workCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _scheduleList.value = _scheduleList.value + "??"
                  } else {
                    _scheduleList.value = _scheduleList.value + result.text
                  }
                }
              }

              _resultMap.value = _dateList.value.zip(_scheduleList.value)
              Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
            }
          }

          OcrType.ENFORCE -> {
            if (_headerPreview.value == null) return
            if (_edges.value?.enforced.isNullOrEmpty()) return
            _dateCells.value = splitHeadBandByEdges(_headerPreview.value!!, _edges.value!!.enforced)
            _workCells.value = splitHeadBandByEdges(_bodyPreview.value!!, _edges.value!!.enforced)
            viewModelScope.launch {
              _dateCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _dateList.value = _dateList.value + "??"
                  } else {
                    _dateList.value = _dateList.value + result.text
                  }
                }
              }

              _workCells.value.forEach {
                if (it.width < 32 || it.height < 32) {
                  return@forEach
                } else {
                  val result = recognizeText(it)
                  if (result.text == "") {
                    _scheduleList.value = _scheduleList.value + "??"
                  } else {
                    _scheduleList.value = _scheduleList.value + result.text
                  }
                }
              }

              _resultMap.value = _dateList.value.zip(_scheduleList.value)
              Log.d("OcrViewModel", "Schedule map: ${_resultMap.value}")
            }
          }
        }
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