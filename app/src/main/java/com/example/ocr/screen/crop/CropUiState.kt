package com.example.ocr.screen.crop

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesType

@Immutable
data class CropUiState(
  val sourceUri: Uri? = null,
  var decodedBitmap: Bitmap? = null,
  val isLoading: Boolean = false,
  val error: Throwable? = null,
  val cropShape: CropShape = CropShape.FreeForm,
  val gridLinesType: GridLinesType = GridLinesType.GRID,
)
