package com.example.ocr.cropkit

import android.graphics.Bitmap
import com.example.ocr.cropkit.internal.CropStateChangeActions
import com.example.ocr.cropkit.internal.CropStateManager

class CropController(
  bitmap: Bitmap,
  val cropOptions: CropOptions,
  val cropColors: CropColors
) {

  private val stateManager: CropStateManager = CropStateManager(
    bitmap = bitmap,
    cropShape = cropOptions.cropShape,
    contentScale = cropOptions.contentScale,
    gridLinesVisibility = cropOptions.gridLinesVisibility,
    handleRadius = cropOptions.handleRadius,
    touchPadding = cropOptions.touchPadding
  )

  internal val state = stateManager.state

  fun crop(): Bitmap = stateManager.crop()

  fun rotateClockwise() = stateManager.rotateClockwise()
  fun rotateAntiClockwise() = stateManager.rotateAntiClockwise()
  fun flipHorizontally() = stateManager.flipHorizontally()
  fun flipVertically() = stateManager.flipVertically()

  internal fun onStateChange(action: CropStateChangeActions) {
    when (action) {
      is CropStateChangeActions.DragStart -> stateManager.onDragStart(action.offSet)
      is CropStateChangeActions.DragEnd -> stateManager.onDragEnd()
      is CropStateChangeActions.DragBy -> stateManager.onDrag(action.offset)
      is CropStateChangeActions.CanvasSizeChanged -> stateManager.updateCanvasSize(action.size)
    }
  }
}