package com.example.ocr.cropkit

import androidx.compose.ui.graphics.Color
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
    touchPadding: Dp = 16.dp
  ) = CropOptions(
    cropShape = cropShape,
    contentScale = contentScale,
    gridLinesVisibility = gridLinesVisibility,
    gridLinesType = gridLinesType,
    handleRadius = handleRadius,
    touchPadding = touchPadding
  )

  fun cropColors(
    overlay: Color = Color.Black.copy(0.75f),
    overlayActive: Color = Color.Black.copy(0.5f),
    gridlines: Color = Color.White.copy(0.5f),
    cropRectangle: Color = Color.White.copy(0.5f),
    handle: Color = Color.White
  ) = CropColors(
    overlay = overlay,
    overlayActive = overlayActive,
    gridlines = gridlines,
    cropRectangle = cropRectangle,
    handle = handle
  )
}