package com.example.ocr.cropkit.internal

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.scale
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesVisibility
import com.example.ocr.cropkit.util.GestureUtils
import com.example.ocr.cropkit.util.MathUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CropStateManager(
  bitmap: Bitmap,
  private val cropShape: CropShape,
  private val contentScale: ContentScale,
  private val gridLinesVisibility: GridLinesVisibility,
  private val handleRadius: Dp,
  private val touchPadding: Dp
) {

  private val _state = MutableStateFlow(CropState(bitmap))
  val state = _state.asStateFlow()
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  private var dragMode: DragMode = DragMode.None
  private val density get() = Resources.getSystem().displayMetrics.density
  private val handleRadiusPx: Float get() = handleRadius.value * density

  init {
    reset(bitmap)
  }

  fun updateCanvasSize(canvasSize: Size) {
    setState(canvasSize, state.value.bitmap)
  }

  fun crop(): Bitmap {
    val state = state.value
    val bitmap = state.bitmap
    val imageRect = state.imageRect
    val cropRect = state.cropRect

    val scaleX = bitmap.width / imageRect.width
    val scaleY = bitmap.height / imageRect.height

    val cropX = ((cropRect.left - imageRect.left) * scaleX).toInt()
    val cropY = ((cropRect.top - imageRect.top) * scaleY).toInt()
    val cropWidth = (cropRect.width * scaleX).toInt()
    val cropHeight = (cropRect.height * scaleY).toInt()

    val x = cropX.coerceIn(0, bitmap.width)
    val y = cropY.coerceIn(0, bitmap.height)
    val width = cropWidth.coerceIn(0, bitmap.width - x)
    val height = cropHeight.coerceIn(0, bitmap.height - y)

    return Bitmap.createBitmap(
      bitmap,
      x, y,
      width, height
    )
  }

  fun reset(bitmap: Bitmap) {
    coroutineScope.launch {
      setState(
        state.value.canvasSize,
        bitmap
      )
    }
  }

  private fun setState(
    canvasSize: Size,
    bitmap: Bitmap
  ) {

    if (canvasSize == Size.Zero) {
      return
    }

    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()

    val scaledSize = MathUtils.calculateScaledSize(
      srcWidth = imageWidth,
      srcHeight = imageHeight,
      dstWidth = canvasSize.width,
      dstHeight = canvasSize.height,
      contentScale = contentScale
    )

    val newBitmap = bitmap.scale(scaledSize.width.toInt(), scaledSize.height.toInt())

    val offsetX = (canvasSize.width - scaledSize.width) / 2f
    val offsetY = (canvasSize.height - scaledSize.height) / 2f

    val aspectRatio = when (cropShape) {
      is CropShape.FreeForm -> null
      is CropShape.AspectRatio -> cropShape.ratio
      is CropShape.Original -> imageWidth / imageHeight
    }

    val cropSize: Size
    val cropOffset: Offset

    if (aspectRatio != null) {
      val availableWidth = scaledSize.width
      val availableHeight = scaledSize.height

      var cropWidth = availableWidth
      var cropHeight = cropWidth / aspectRatio

      if (cropHeight > availableHeight) {
        cropHeight = availableHeight
        cropWidth = cropHeight * aspectRatio
      }

      cropSize = Size(cropWidth, cropHeight)
      cropOffset = Offset(
        offsetX + (availableWidth - cropWidth) / 2,
        offsetY + (availableHeight - cropHeight) / 2
      )
    } else {
      cropSize = Size(scaledSize.width, scaledSize.height)
      cropOffset = Offset(offsetX, offsetY)
    }
    val cropRect = Rect(cropOffset, cropSize)

    _state.update {
      it.copy(
        bitmap = newBitmap,
        imageBitmap = newBitmap.asImageBitmap(),
        cropRect = cropRect,
        imageRect = Rect(
          Offset(offsetX, offsetY),
          Size(scaledSize.width, scaledSize.height)
        ),
        handles = GestureUtils.getNewHandleMeasures(cropRect, handleRadiusPx),
        canvasSize = canvasSize,
        gridlinesActive = gridLinesVisibility == GridLinesVisibility.ALWAYS,
        aspectRatio = when (cropShape) {
          is CropShape.AspectRatio -> cropShape.ratio
          CropShape.FreeForm -> 0f
          CropShape.Original -> imageWidth / imageHeight
        }
      )
    }
  }
}