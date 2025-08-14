package com.example.ocr.screen.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class DrawingAction {
  data object OnNewPathStart : DrawingAction()
  data class OnDraw(val offset: Offset) : DrawingAction()
  data object OnPathEnd : DrawingAction()
  data class OnSelectColor(val color: Color) : DrawingAction()
  data object OnClearCanvas : DrawingAction()
}