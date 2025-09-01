package com.example.ocr.screen.crop

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.cropkit.CropShape
import com.example.ocr.utils.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CropViewModel() : ViewModel() {

  private val _state = MutableStateFlow(CropUiState())
  val state: StateFlow<CropUiState> = _state.asStateFlow()

  var decodedBitmap: Bitmap? = null
    private set

  fun setSource(context: Context, uri: Uri) {
    if (_state.value.sourceUri == uri) return
    _state.update { it.copy(sourceUri = uri, isLoading = true, error = null) }
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        loadBitmapFromUri(context, uri)
      }.onSuccess { bitmap ->
        decodedBitmap = bitmap
        _state.update { it.copy(isLoading = false) }
      }.onFailure { error ->
        _state.update { it.copy(isLoading = false, error = error) }
      }
    }
  }

  fun setCropShape(shape: CropShape) = _state.update { it.copy(cropShape = shape) }
}