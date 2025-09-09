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
import com.example.ocr.screen.ocr.utils.bodyBandFromWords
import com.example.ocr.screen.ocr.utils.buildRowBands
import com.example.ocr.screen.ocr.utils.detectEdgesInRow
import com.example.ocr.screen.ocr.utils.extractScheduleJson
import com.example.ocr.screen.ocr.utils.headerBandFromWords
import com.example.ocr.screen.ocr.utils.isHeaderWord
import com.example.ocr.screen.ocr.utils.readHeaderDates
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
      Log.d("HeaderBand", "Detected header band: $headerBand")

      if (headerBand == null) return@launch
      val test = detectEdgesInRow(bitmap, headerBand)
      Log.d("Text", "Detected text edges in header: $test")
      val test2 = readHeaderDates(bitmap, headerBand, 2025, 9)
      Log.d("Text2", "Detected header dates: $test2")

      val bodyWords = words.filter { !isHeaderWord(it) && it.bottom > headerBand.bottom }
      Log.d("BodyWords", "Detected $bodyWords body words")

      val test3 = bodyBandFromWords(bodyWords, headerBand, bitmap.width, bitmap.height)
      Log.d("Text3", "Detected body band: $test3")


      val rowBands = buildRowBands(bodyWords, imageWidth = bitmap.width)
      Log.d("RowBands", "Detected $rowBands row bands")
      val targetBand: RectI = rowBands.maxByOrNull { it.rect.height }?.rect ?: run {
        RectI(0, headerBand!!.bottom, bitmap.width, bitmap.height)
      }
      Log.d("TargetBand", "Using target band: $targetBand")
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

val dateRegex = Regex("""^\d{1,2}/\d{1,2}$""")
val weekdayRegex = Regex("""^[\(\（].+[\)\）]$""")