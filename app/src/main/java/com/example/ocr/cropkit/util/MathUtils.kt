package com.example.ocr.cropkit.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import kotlin.math.max
import kotlin.math.min

internal object MathUtils {

  fun calculateScaledSize(
    srcWidth: Float,
    srcHeight: Float,
    dstWidth: Float,
    dstHeight: Float,
    contentScale: ContentScale
  ): Size {
    return when (contentScale) {
      ContentScale.Fit -> {
        val widthRatio = dstWidth / srcWidth
        val heightRatio = dstHeight / srcHeight
        val scale = min(widthRatio, heightRatio)
        Size((srcWidth * scale), (srcHeight * scale))
      }

      ContentScale.Crop -> {
        val widthRatio = dstWidth / srcWidth
        val heightRatio = dstHeight / srcHeight
        val scale = max(widthRatio, heightRatio)
        Size((srcWidth * scale), (srcHeight * scale))
      }

      ContentScale.Inside -> {
        if (srcWidth <= dstWidth && srcHeight <= dstHeight) {
          Size(srcWidth, srcHeight)
        } else {
          val widthRatio = dstWidth / srcWidth
          val heightRatio = dstHeight / srcHeight
          val scale = min(widthRatio, heightRatio)
          Size((srcWidth * scale), (srcHeight * scale))
        }
      }

      else -> Size(dstWidth, dstHeight) // Default case
    }
  }
}
