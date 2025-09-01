@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.crop

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ocr.R
import com.example.ocr.common.components.BackNavDialog
import com.example.ocr.cropkit.CropDefaults
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.ImageCropper
import com.example.ocr.cropkit.rememberCropController
import com.example.ocr.utils.saveTempBitmapToCache

@Composable
fun CropScreen(
  capturedImageUri: Uri,
  onCropComplete: (Uri?) -> Unit = {},
  viewModel: CropViewModel = viewModel(),
  onBack: () -> Unit
) {
  val openBackNavDialog = remember { mutableStateOf(false) }
  BackHandler(enabled = !openBackNavDialog.value) {
    if (!openBackNavDialog.value) {
      openBackNavDialog.value = true
    }
  }

  if (openBackNavDialog.value) {
    BackNavDialog(
      onDismissRequest = { openBackNavDialog.value = false },
      onConfirm = {
        openBackNavDialog.value = false
        onBack()
      },
      dialogTitle = "Discard Changes",
      dialogMessage = "Are you sure you want to discard the changes and go back?"
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val image = viewModel.decodedBitmap

    val cropController = image?.let {
      rememberCropController(
        bitmap = it,
        cropOptions = CropDefaults.cropOptions(
          cropShape = uiState.cropShape,
          gridLinesType = uiState.gridLinesType
        )
      )
    }

    val context = LocalContext.current

    LaunchedEffect(capturedImageUri) {
      viewModel.setSource(context, capturedImageUri)
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = { Text("Crop") },
          navigationIcon = {
            IconButton(onClick = { openBackNavDialog.value = true }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
              )
            }
          },
          actions = {
            IconButton(
              onClick = {
                cropController?.crop()?.let {
                  val croppedUri = saveTempBitmapToCache(context, it)
                  onCropComplete(croppedUri)
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Crop and Save",
              )
            }
          }
        )
      },
    ) { innerPadding ->
      val safeGestureInsets = WindowInsets.safeGestures.asPaddingValues()

      Surface(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {

          if (cropController != null) {
            val cropState = cropController.state.collectAsStateWithLifecycle().value
            var cropperBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
            // This Rect will be used to calculate the image rectangle in the root layout coordinates
            val targetBounds = remember(cropperBoundsInRoot, cropState.imageRect) {
              val bounds = cropperBoundsInRoot
              val img = cropState.imageRect
              if (bounds != null) {
                Rect(
                  left = bounds.left + img.left,
                  top = bounds.top + img.top,
                  right = bounds.left + img.right,
                  bottom = bounds.top + img.bottom
                )
              } else null
            }

            EdgeExclusionLayer(
              modifier = Modifier
                .weight(1f),
              // 3) Delivers the image rectangle in root coordinates
              targetBounds = targetBounds
            ) {
              ImageCropper(
                modifier = Modifier
                  .padding(safeGestureInsets)
                  .fillMaxWidth()
                  .weight(1f)
                  // 2) This is the bounds of the cropper in the root layout
                  .onGloballyPositioned { coords ->
                    cropperBoundsInRoot = coords.boundsInRoot()
                  },
                cropController = cropController
              )
            }

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            ) {

              SegmentedButton(
                selected = uiState.cropShape == CropShape.FreeForm,
                onClick = { viewModel.setCropShape(CropShape.FreeForm) },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 0,
                  count = 2
                )
              ) {
                Text("Free-Form")
              }

              SegmentedButton(
                selected = uiState.cropShape == CropShape.Original,
                onClick = { viewModel.setCropShape(CropShape.Original) },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 1,
                  count = 2
                )
              ) {
                Text("Original")
              }
            }

            Spacer(Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {

              IconButton(
                onClick = {
                  cropController.rotateAntiClockwise()
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_rotate_acw),
                  contentDescription = "Rotate Anti-Clockwise",
                )
              }

              IconButton(
                onClick = {
                  cropController.rotateClockwise()
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_rotate_cw),
                  contentDescription = "Rotate Clockwise",
                )
              }

              IconButton(
                onClick = {
                  cropController.flipVertically()
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_flip_vert),
                  contentDescription = "Flip Vertically",
                )
              }

              IconButton(
                onClick = {
                  cropController.flipHorizontally()
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_flip_horiz),
                  contentDescription = "Flip Horizontally",
                )
              }
            }
          }
        }
      }

    }
  }
}

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
