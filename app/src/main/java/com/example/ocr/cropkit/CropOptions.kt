package com.example.ocr.cropkit

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

data class CropOptions(
  val cropShape: CropShape,
  val contentScale: ContentScale,
  val gridLinesVisibility: GridLinesVisibility,
  val gridLinesType: GridLinesType,
  val handleRadius: Dp,
  val touchPadding: Dp
)
