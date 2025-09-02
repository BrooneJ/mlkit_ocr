package com.example.ocr.screen.ocr

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.example.ocr.TextRecognitionHelper
import com.example.ocr.navigation.OcrRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OcrViewModel(
  private val handle: SavedStateHandle,
) : ViewModel() {
  val args: OcrRoute = handle.toRoute<OcrRoute>()
  val targetUri: Uri? = args.uri

  private val _text = MutableStateFlow<String?>(null)
  val text = _text.asStateFlow()

  fun processImage(context: Context) {
    val uri = targetUri ?: return
    TextRecognitionHelper.recognizeTextFromUri(context, uri) { result ->
      _text.value = result
    }
  }
}