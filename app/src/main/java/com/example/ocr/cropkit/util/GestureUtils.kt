package com.example.ocr.cropkit.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.ocr.cropkit.HandlesRect
import com.example.ocr.cropkit.internal.DragHandle

internal object GestureUtils {

  fun getNewRectMeasures(
    activeHandle: DragHandle,
    dragAmount: Offset,
    imageRect: Rect,
    cropRect: Rect,
    minCropSize: Float,
  ): Rect? {
    return when (activeHandle) {
      DragHandle.TopLeft -> {
        val newOffset = cropRect.topLeft + dragAmount
        // minCropSize <= cropRect.right - newOffset.x
        if (newOffset.x !in imageRect.left..cropRect.right - minCropSize
          || newOffset.y !in imageRect.top..cropRect.bottom - minCropSize
        ) return null
        Rect(
          left = newOffset.x,
          top = newOffset.y,
          right = cropRect.right,
          bottom = cropRect.bottom
        )
      }

      DragHandle.TopRight -> {
        val newOffset = cropRect.topRight + dragAmount
        if (newOffset.x !in cropRect.left + minCropSize..imageRect.right
          || newOffset.y !in imageRect.top..cropRect.bottom - minCropSize
        ) return null
        Rect(
          left = cropRect.left,
          top = newOffset.y,
          right = newOffset.x,
          bottom = cropRect.bottom
        )
      }

      DragHandle.BottomLeft -> {
        val newOffset = cropRect.bottomLeft + dragAmount
        if (newOffset.x !in imageRect.left..cropRect.right - minCropSize
          || newOffset.y !in imageRect.top + minCropSize..imageRect.bottom
        ) return null
        Rect(
          left = newOffset.x,
          top = cropRect.top,
          right = cropRect.right,
          bottom = newOffset.y
        )
      }

      DragHandle.BottomRight -> {
        val newOffset = cropRect.bottomRight + dragAmount
        if (newOffset.x !in cropRect.left + minCropSize..imageRect.right
          || newOffset.y !in cropRect.top + minCropSize..imageRect.bottom
        ) return null
        Rect(
          left = cropRect.left,
          top = cropRect.top,
          right = newOffset.x,
          bottom = newOffset.y
        )
      }

      DragHandle.Left -> {
        val newOffset = cropRect.bottomLeft + dragAmount
        val newLeft = newOffset.x.coerceIn(
          imageRect.left,
          cropRect.right - minCropSize
        )
        Rect(
          left = newLeft,
          top = cropRect.top,
          right = cropRect.right,
          bottom = cropRect.bottom
        )
      }

      DragHandle.Top -> {
        val newOffset = cropRect.topLeft + dragAmount
        val newTop = newOffset.y.coerceIn(
          imageRect.top,
          cropRect.bottom - minCropSize
        )
        Rect(
          left = cropRect.left,
          top = newTop,
          right = cropRect.right,
          bottom = cropRect.bottom
        )
      }

      DragHandle.Right -> {
        val newOffset = cropRect.topRight + dragAmount
        val newRight = newOffset.x.coerceIn(
          cropRect.left + minCropSize,
          imageRect.right
        )
        Rect(
          left = cropRect.left,
          top = cropRect.top,
          right = newRight,
          bottom = cropRect.bottom
        )
      }

      DragHandle.Bottom -> {
        val newOffset = cropRect.bottomLeft + dragAmount
        val newBottom = newOffset.y.coerceIn(
          cropRect.top + minCropSize,
          imageRect.bottom
        )
        Rect(
          left = cropRect.left,
          top = cropRect.top,
          right = cropRect.right,
          bottom = newBottom
        )
      }
    }
  }

  fun getNewHandleMeasures(
    cropRect: Rect,
    handleRadius: Float,
  ): HandlesRect {

    val topLeftOffset = cropRect.topLeft - Offset(handleRadius, handleRadius)
    val topLeftRect = Rect(
      left = topLeftOffset.x,
      top = topLeftOffset.y,
      right = topLeftOffset.x + handleRadius * 2,
      bottom = topLeftOffset.y + handleRadius * 2
    )

    val topRightOffset = cropRect.topRight - Offset(-handleRadius, handleRadius)
    val topRightRect = Rect(
      left = topRightOffset.x,
      top = topRightOffset.y,
      right = topRightOffset.x + handleRadius * 2,
      bottom = topRightOffset.y + handleRadius * 2
    )

    val bottomLeftOffset = cropRect.bottomLeft - Offset(handleRadius, -handleRadius)
    val bottomLeftRect = Rect(
      left = bottomLeftOffset.x,
      top = bottomLeftOffset.y,
      right = bottomLeftOffset.x + handleRadius * 2,
      bottom = bottomLeftOffset.y + handleRadius * 2
    )

    val bottomRightOffset = cropRect.bottomRight - Offset(-handleRadius, -handleRadius)
    val bottomRightRect = Rect(
      left = bottomRightOffset.x,
      top = bottomRightOffset.y,
      right = bottomRightOffset.x + handleRadius * 2,
      bottom = bottomRightOffset.y + handleRadius * 2
    )

    val halfWidth = (cropRect.width) / 2
    val halfHeight = (cropRect.height) / 2

    val topOffset = cropRect.topLeft + Offset(halfWidth, 0f) - Offset(handleRadius, handleRadius)
    val topRect = Rect(
      left = topOffset.x,
      top = topOffset.y,
      right = topOffset.x + handleRadius * 2,
      bottom = topOffset.y + handleRadius * 2
    )

    val bottomOffset =
      cropRect.bottomLeft + Offset(halfWidth, 0f) - Offset(handleRadius, -handleRadius)
    val bottomRect = Rect(
      left = bottomOffset.x,
      top = bottomOffset.y,
      right = bottomOffset.x + handleRadius * 2,
      bottom = bottomOffset.y + handleRadius * 2
    )

    val leftOffset = cropRect.topLeft + Offset(0f, halfHeight) - Offset(handleRadius, handleRadius)
    val leftRect = Rect(
      left = leftOffset.x,
      top = leftOffset.y,
      right = leftOffset.x + handleRadius * 2,
      bottom = leftOffset.y + handleRadius * 2
    )

    val rightOffset =
      cropRect.topRight + Offset(0f, halfHeight) - Offset(-handleRadius, handleRadius)
    val rightRect = Rect(
      left = rightOffset.x,
      top = rightOffset.y,
      right = rightOffset.x + handleRadius * 2,
      bottom = rightOffset.y + handleRadius * 2
    )

    return HandlesRect(
      topLeft = topLeftRect,
      topRight = topRightRect,
      bottomLeft = bottomLeftRect,
      bottomRight = bottomRightRect,
      top = topRect,
      bottom = bottomRect,
      right = rightRect,
      left = leftRect
    )
  }
}