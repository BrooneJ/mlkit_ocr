package com.example.ocr.cropkit

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.cropkit.internal.CropStateChangeActions

@Composable
fun rememberCropController(
  bitmap: Bitmap,
  cropOptions: CropOptions = CropDefaults.cropOptions(),
  cropColors: CropColors = CropDefaults.cropColors()
): CropController = remember(bitmap, cropOptions, cropColors) {
  CropController(bitmap, cropOptions, cropColors)
}

@Composable
fun ImageCropper(
  modifier: Modifier = Modifier,
  cropController: CropController
) {

  val state = cropController.state.collectAsStateWithLifecycle().value
  val cropOptions = cropController.cropOptions
  val cropColors = cropController.cropColors

  val overlay = animateColorAsState(
    targetValue = if (state.isDragging) {
      cropColors.overlayActive
    } else {
      cropColors.overlay
    },
    label = "overlay"
  )

  key(cropController) {
    Box(modifier = modifier) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { offset ->
                cropController.onStateChange(CropStateChangeActions.DragStart(offset))
              },
              onDragEnd = {
                cropController.onStateChange(CropStateChangeActions.DragEnd)
              },
              onDrag = { change, dragAmount ->
                change.consume()
                cropController.onStateChange(CropStateChangeActions.DragBy(dragAmount))
              }
            )
          }
          .onSizeChanged { size ->
            cropController.onStateChange(CropStateChangeActions.CanvasSizeChanged(size.toSize()))
          }
      ) {

        state.imageBitmap?.let {
          drawImage(
            image = it,
            topLeft = state.imageRect.topLeft
          )

          clipPath(
            path = Path().apply {
              when (cropOptions.gridLinesType) {
                GridLinesType.CIRCLE, GridLinesType.GRID_AND_CIRCLE -> addOval(state.cropRect)
                else -> addRect(state.cropRect)
              }
            },
            clipOp = ClipOp.Difference
          ) {
            drawRect(
              color = overlay.value,
              topLeft = state.imageRect.topLeft,
              size = state.imageRect.size
            )
          }
        }

        val cropRect = state.cropRect

        drawRect(
          color = cropColors.cropRectangle,
          topLeft = cropRect.topLeft,
          size = cropRect.size,
          style = Stroke(width = 2.dp.toPx())
        )

        if (state.gridlinesActive) {

          if (cropOptions.gridLinesType in listOf(
              GridLinesType.GRID,
              GridLinesType.GRID_AND_CIRCLE
            )
          ) {
            val thirdWidth = cropRect.width / 3
            val thirdHeight = cropRect.height / 3

            for (i in 1..2) {
              drawLine(
                color = cropColors.gridlines,
                start = Offset(cropRect.left + thirdWidth * i, cropRect.top),
                end = Offset(cropRect.left + thirdWidth * i, cropRect.bottom),
                strokeWidth = 1.dp.toPx()
              )
            }

            for (i in 1..2) {
              drawLine(
                color = cropColors.gridlines,
                start = Offset(cropRect.left, cropRect.top + thirdHeight * i),
                end = Offset(cropRect.right, cropRect.top + thirdHeight * i),
                strokeWidth = 1.dp.toPx()
              )
            }
          }

          if (cropOptions.gridLinesType in listOf(
              GridLinesType.CIRCLE,
              GridLinesType.GRID_AND_CIRCLE
            )
          ) {
            drawOval(
              color = cropColors.gridlines,
              topLeft = cropRect.topLeft,
              size = cropRect.size,
              style = Stroke(width = 1.dp.toPx())
            )
          }

          if (cropOptions.gridLinesType == GridLinesType.CROSSHAIR) {
            drawLine(
              color = cropColors.gridlines,
              start = Offset(cropRect.left + cropRect.width / 2, cropRect.top),
              end = Offset(cropRect.left + cropRect.width / 2, cropRect.bottom),
              strokeWidth = 1.dp.toPx()
            )

            drawLine(
              color = cropColors.gridlines,
              start = Offset(cropRect.left, cropRect.top + cropRect.height / 2),
              end = Offset(cropRect.right, cropRect.top + cropRect.height / 2),
              strokeWidth = 1.dp.toPx()
            )
          }
        }

        val handles = if (cropOptions.cropShape is CropShape.FreeForm) {
          state.handles.getAllHandles()
        } else {
          state.handles.getCornerHandles()
        }

        handles.forEach { handle ->
          drawOval(
            color = cropColors.handle,
            topLeft = handle.topLeft,
            size = handle.size,
            style = Fill
          )
        }
      }
    }
  }
}