package com.example.ocr.cropkit

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CropDefaults {

  fun cropOptions(
    cropShape: CropShape = CropShape.Original,
    contentScale: ContentScale = ContentScale.Fit,
    gridLinesVisibility: GridLinesVisibility = GridLinesVisibility.ON_TOUCH,
    gridLinesType: GridLinesType = GridLinesType.GRID,
    handleRadius: Dp = 8.dp,
    touchPadding: Dp = 10.dp
  ) = CropOptions(
    cropShape = cropShape,
    contentScale = contentScale,
    gridLinesVisibility = gridLinesVisibility,
    gridLinesType = gridLinesType,
    handleRadius = handleRadius,
    touchPadding = touchPadding
  )
}