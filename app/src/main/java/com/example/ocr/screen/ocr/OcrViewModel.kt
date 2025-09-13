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
import com.example.ocr.screen.ocr.utils.cropToBitmap
import com.example.ocr.screen.ocr.utils.drawColumnDebug
import com.example.ocr.screen.ocr.utils.headerBandFromWords
import com.example.ocr.screen.ocr.utils.pickColumnBoundaries
import com.example.ocr.screen.ocr.utils.pickColumnBoundariesRobust
import com.example.ocr.screen.ocr.utils.recognizeText
import com.example.ocr.screen.ocr.utils.recognizeWordsFromUri
import com.example.ocr.screen.ocr.utils.roughCharWidth
import com.example.ocr.screen.ocr.utils.smooth
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

  private val _test = MutableStateFlow<Bitmap?>(null)
  val test = _test.asStateFlow()

  fun processImage(context: Context) {
    val uri = targetUri ?: return
    viewModelScope.launch {
      val bitmap = loadBitmap(context, uri) ?: return@launch
      val words = recognizeWordsFromUri(context, uri)
      words.forEach {
        Log.v("OcrViewModel", "Word: '${it.text}' @(${it.left},${it.top},${it.right},${it.bottom})")
      }
      val headerBand: RectI? = headerBandFromWords(words, imageWidth = bitmap.width)
      Log.d("HeaderBand", "Detected header band: $headerBand")

      if (headerBand == null) return@launch
      _headerPreview.value = cropToBitmap(bitmap, headerBand)

      if (_headerPreview.value != null) {
        val proj = verticalProjection(_headerPreview.value!!, headerBand)
        Log.d("VerticalProjection", "Vertical projection: $proj")
        val r = roughCharWidth(headerBand)
        Log.d("RoughCharWidth", "Rough char width: $r")
        val smoothed = smooth(proj, r)
        Log.d("ProjectionSmoothing", "Smoothed projection: $smoothed")

        val edgesFromValleys = pickColumnBoundaries(smoothed, headerBand.width, r)
        Log.d("ColumnBoundaries", "Detected column boundaries: $edgesFromValleys")
        val edgesFromPeaks = pickColumnBoundariesRobust(proj, headerBand.width, r, false)
        Log.d("ColumnBoundaries", "Detected column boundaries (robust): $edgesFromPeaks")
        val edges = (if (edgesFromPeaks.size >= 3) edgesFromPeaks else edgesFromValleys).map {
          headerBand.left + it
        }
        _test.value = drawColumnDebug(bitmap, headerBand, edges)
      }

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
val weekdayRegex = Regex("""^[\(\（].+[\)\）]$""")