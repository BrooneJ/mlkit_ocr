package com.example.ocr.screen.crop.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Side { Left, Right }
enum class Slot { Top, Middle, Bottom }

@Composable
fun EdgeExclusionLayer(
  modifier: Modifier = Modifier,
  targetBounds: Rect? = null,
  content: @Composable () -> Unit,
) {
  if (targetBounds == null) {
    content()
    return
  }
  val density = LocalDensity.current

  val makeRect: (LayoutCoordinates, Side, Slot) -> Rect = { coords, side, slot ->
    sliceRectLocal(
      coords = coords,
      targetBounds = targetBounds,
      density = density,
      side = side,
      slot = slot
    )
  }

  val exclusionModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      Modifier
        // The left side 3 slices
        .systemGestureExclusion { coords -> makeRect(coords, Side.Left, Slot.Top) }
        .systemGestureExclusion { coords -> makeRect(coords, Side.Left, Slot.Middle) }
        .systemGestureExclusion { coords -> makeRect(coords, Side.Left, Slot.Bottom) }
        // The right side 3 slices
        .systemGestureExclusion { coords -> makeRect(coords, Side.Right, Slot.Top) }
        .systemGestureExclusion { coords -> makeRect(coords, Side.Right, Slot.Middle) }
        .systemGestureExclusion { coords -> makeRect(coords, Side.Right, Slot.Bottom) }
    else Modifier

  Box(
    modifier
      .fillMaxWidth()
      .then(exclusionModifier)
  ) {
    content()
  }
}

// The function creates 6 slices of Rects for the left and right sides of the cropper
private fun sliceRectLocal(
  coords: LayoutCoordinates,
  targetBounds: Rect,
  density: Density,
  leftDp: Dp = 48.dp,
  rightDp: Dp = 48.dp,
  side: Side, // "left" or "right"
  slot: Slot, // "top", "middle", or "bottom"
): Rect {

  val widthLocal = coords.size.width.toFloat()
  val heightLocal = coords.size.height.toFloat()

  // Take the top of this node (Box) in root coordinates and adjust it to local coordinates
  val nodeTopInRoot = coords.boundsInRoot().top

  // Adjust the target bounds to local coordinates
  val topLocal = (targetBounds.top - nodeTopInRoot).coerceIn(0f, heightLocal)
  val bottomLocal = (targetBounds.bottom - nodeTopInRoot).coerceIn(0f, heightLocal)
  val targetH = (bottomLocal - topLocal).coerceAtLeast(0f)

  // Convert Dp to pixels using the current density
  val leftPx = with(density) { leftDp.toPx() }
  val rightPx = with(density) { rightDp.toPx() }
  val maxHPx = with(density) { 200.dp.toPx() }

  val sliceH = (minOf(maxHPx, targetH)) / 3f
  if (sliceH <= 0f) return Rect.Zero

  val yTop = when (slot) {
    Slot.Top -> topLocal - sliceH / 2f
    Slot.Middle -> topLocal + (targetH - sliceH) / 2f
    else -> bottomLocal - sliceH / 2f
  }.coerceIn(0f, (heightLocal - sliceH).coerceAtLeast(0f))

  return if (side == Side.Left) {
    Rect(
      left = 0f,
      top = yTop,
      right = leftPx.coerceAtMost(widthLocal),
      bottom = (yTop + sliceH).coerceAtMost(heightLocal)
    )
  } else {
    Rect(
      left = (widthLocal - rightPx).coerceAtLeast(0f),
      top = yTop,
      right = widthLocal,
      bottom = (yTop + sliceH).coerceAtMost(heightLocal)
    )
  }
}
