package com.example.ocr.cropkit

import androidx.compose.ui.geometry.Rect
import com.example.ocr.cropkit.internal.DragHandle

data class HandlesRect(
  val topLeft: Rect = Rect.Zero,
  val topRight: Rect = Rect.Zero,
  val bottomLeft: Rect = Rect.Zero,
  val bottomRight: Rect = Rect.Zero,
  val top: Rect = Rect.Zero,
  val bottom: Rect = Rect.Zero,
  val right: Rect = Rect.Zero,
  val left: Rect = Rect.Zero
) {

  fun getCornerHandles(): List<Rect> {
    return listOf(topLeft, topRight, bottomLeft, bottomRight)
  }

  fun getAllHandles(): List<Rect> {
    return listOf(topLeft, topRight, bottomLeft, bottomRight, top, bottom, right, left)
  }

  internal fun getCornerNamedHandles(): List<Pair<Rect, DragHandle>> {
    return listOf(
      topLeft to DragHandle.TopLeft,
      topRight to DragHandle.TopRight,
      bottomLeft to DragHandle.BottomLeft,
      bottomRight to DragHandle.BottomRight
    )
  }

  internal fun getAllNamedHandles(): List<Pair<Rect, DragHandle>> {
    return listOf(
      topLeft to DragHandle.TopLeft,
      topRight to DragHandle.TopRight,
      bottomLeft to DragHandle.BottomLeft,
      bottomRight to DragHandle.BottomRight,
      top to DragHandle.Top,
      bottom to DragHandle.Bottom,
      right to DragHandle.Right,
      left to DragHandle.Left
    )
  }
}
