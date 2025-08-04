package com.example.ocr.cropkit.internal

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import com.example.ocr.cropkit.HandlesRect

internal data class CropState(
  val bitmap: Bitmap,
  val imageBitmap: ImageBitmap? = null,
  val cropRect: Rect = Rect.Zero,
  val imageRect: Rect = Rect.Zero,
  val handles: HandlesRect = HandlesRect(),
  val canvasSize: Size = Size.Zero,
  val isDragging: Boolean = false,
  val gridlinesActive: Boolean = false,
  val aspectRatio: Float = 0f
)