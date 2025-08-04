package com.example.ocr.cropkit.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

interface CropStateChangeActions {
  data class DragStart(val offSet: Offset) : CropStateChangeActions
  data object DragEnd : CropStateChangeActions
  data class DragBy(val offset: Offset) : CropStateChangeActions
  data class CanvasSizeChanged(val size: Size) : CropStateChangeActions
}