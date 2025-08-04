package com.example.ocr.cropkit

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberCropController(
  bitmap: Bitmap,
  cropOptions: CropOptions = CropDefaults.cropOptions(),
  cropColors: CropColors = CropDefaults.cropColors()
): CropController = remember(bitmap, cropOptions, cropColors) {
  CropController(bitmap, cropOptions, cropColors)
}