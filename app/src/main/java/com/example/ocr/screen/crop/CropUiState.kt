package com.example.ocr.screen.crop

import android.net.Uri
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesType

data class CropUiState(
  val sourceUri: Uri? = null,
  val isLoading: Boolean = false,
  val error: Throwable? = null,
  var cropShape: CropShape = CropShape.FreeForm,
  var gridLinesType: GridLinesType = GridLinesType.GRID,
)
