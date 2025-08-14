package com.example.ocr.screen.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawingState(
  val selectedColor: Color = Color.Black,
  val thickness: Float = 20f,
  val currentPath: PathData? = null,
  val paths: List<PathData> = emptyList()
)

data class PathData(
  val id: String,
  val color: Color,
  val thickness: Float,
  val path: List<Offset>
)
