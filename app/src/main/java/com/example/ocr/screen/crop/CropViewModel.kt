package com.example.ocr.screen.crop

import android.content.Context
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

class CropViewModel : ViewModel() {

  private val _state = MutableStateFlow(CropUiState())
  val state: StateFlow<CropUiState> = _state.asStateFlow()

  fun setSource(context: Context, uri: Uri) {
    if (_state.value.sourceUri == uri) return
    _state.update { it.copy(sourceUri = uri, isLoading = true, decodedBitmap = null, error = null) }
    viewModelScope.launch(Dispatchers.IO) {
      val result = runCatching { loadBitmapFromUri(context, uri) }
      _state.update { currentState ->
        result.fold(
          onSuccess = { bitmap ->
            currentState.copy(isLoading = false, decodedBitmap = bitmap, error = null)
          },
          onFailure = { error ->
            currentState.copy(isLoading = false, decodedBitmap = null, error = error)
          }
        )
      }
    }
  }

  fun setCropShape(shape: CropShape) = _state.update { it.copy(cropShape = shape) }
}