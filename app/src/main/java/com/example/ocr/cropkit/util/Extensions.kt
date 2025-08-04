package com.example.ocr.cropkit.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal object Extensions {

  fun Offset.isInsideRect(rect: Rect): Boolean {
    return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
  }
}