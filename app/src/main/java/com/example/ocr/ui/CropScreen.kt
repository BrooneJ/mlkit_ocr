@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.R
import com.example.ocr.cropkit.CropDefaults
import com.example.ocr.cropkit.CropRatio
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesType
import com.example.ocr.cropkit.ImageCropper
import com.example.ocr.cropkit.rememberCropController
import com.example.ocr.utils.saveTempBitmapToCache

@Composable
fun CropScreen(
  capturedImageUri: Uri,
  onCropComplete: (Uri) -> Unit = {}
) {

// Uncomment the following lines if you want to use Immersive mode

//  val activity = LocalActivity.current as ComponentActivity
//  val lifecycleOwner = LocalLifecycleOwner.current
//
//  DisposableEffect(lifecycleOwner) {
//    val window = activity.window
//    val controller = window.insetsController
//
//    controller?.hide(WindowInsets.Type.systemBars())
//    controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//
//    val observer = LifecycleEventObserver { _, event ->
//      if (event == Lifecycle.Event.ON_DESTROY || event == Lifecycle.Event.ON_STOP) {
//        controller?.show(WindowInsets.Type.systemBars())
//      }
//    }
//    lifecycleOwner.lifecycle.addObserver(observer)
//
//    onDispose {
//      controller?.show(WindowInsets.Type.systemBars())
//      lifecycleOwner.lifecycle.removeObserver(observer)
//    }
//  }
  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {

    var image: Bitmap? by remember { mutableStateOf(null) }
    var cropShape: CropShape by remember { mutableStateOf(CropShape.FreeForm) }
    var gridLinesType by remember { mutableStateOf(GridLinesType.GRID) }
    val cropController = image?.let {
      rememberCropController(
        bitmap = it,
        cropOptions = CropDefaults.cropOptions(
          cropShape = cropShape,
          gridLinesType = gridLinesType
        )
      )
    }

    val context = LocalContext.current
    image = remember(capturedImageUri) {
      capturedImageUri.toBitmap(context)
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = { Text("Crop") },
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
            ) {

              SegmentedButton(
                selected = cropShape == CropShape.FreeForm,
                onClick = { cropShape = CropShape.FreeForm },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 0,
                  count = 4
                )
              ) {
                Text("Free-Form")
              }

              SegmentedButton(
                selected = cropShape == CropShape.Original,
                onClick = { cropShape = CropShape.Original },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 1,
                  count = 4
                )
              ) {
                Text("Original")
              }

              SegmentedButton(
                selected = cropShape is CropShape.AspectRatio && gridLinesType == GridLinesType.CROSSHAIR,
                onClick = {
                  cropShape = CropShape.AspectRatio(CropRatio.SQUARE)
                  gridLinesType = GridLinesType.CROSSHAIR
                },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 2,
                  count = 4
                )
              ) {
                Text("Square")
              }

              SegmentedButton(
                selected = cropShape is CropShape.AspectRatio && gridLinesType == GridLinesType.GRID_AND_CIRCLE,
                onClick = {
                  cropShape = CropShape.AspectRatio(CropRatio.SQUARE)
                  gridLinesType = GridLinesType.GRID_AND_CIRCLE
                },
                shape = SegmentedButtonDefaults.itemShape(
                  index = 3,
                  count = 4
                )
              ) {
                Text("Circle")
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

@Composable
fun EdgeExclusionLayer(
  modifier: Modifier = Modifier,
  leftDp: Dp = 48.dp,
  rightDp: Dp = 48.dp,
  targetBounds: Rect? = null,
  debugOverlay: Boolean = true,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  val density = LocalDensity.current
  var lastRects by remember { mutableStateOf(emptyList<android.graphics.Rect>()) }

  // save the position of the layer in root coordinates
  var layerOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        layerOffset = coords.positionInRoot()
      }
  ) {
    DisposableEffect(targetBounds) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetBounds != null) {
        val width = view.width
        val height = view.height
        val leftExclusionPx = with(density) { leftDp.roundToPx() }
        val rightExclusionPx = with(density) { rightDp.roundToPx() }
        // For the exclusion rects, we can only use a maximum height of 200dp
        val maxH = with(density) { 200.dp.roundToPx() }

        val top = targetBounds.top.toInt().coerceIn(0, height)
        val bottom = targetBounds.bottom.toInt().coerceIn(0, height)
        val targetHeight = (bottom - top).coerceAtLeast(0)

        // Divide the target height into 3 slices because we want to create 3 exclusion rects on each side
        val slice = minOf(maxH, targetHeight) / 3
        val topPositionOfImage = top - slice / 2
        val middlePositionOfImage = top + (targetHeight - slice) / 2
        val bottomPositionOfImage = bottom - slice / 2

        val rects = buildList {
          // Slice of the left side
          add(
            android.graphics.Rect(
              0,
              topPositionOfImage,
              leftExclusionPx,
              topPositionOfImage + slice
            )
          )
          add(
            android.graphics.Rect(
              0,
              middlePositionOfImage,
              leftExclusionPx,
              middlePositionOfImage + slice
            )
          )
          add(
            android.graphics.Rect(
              0,
              bottomPositionOfImage,
              leftExclusionPx,
              bottomPositionOfImage + slice
            )
          )
          // Slice of the right side
          add(
            android.graphics.Rect(
              width - rightExclusionPx,
              topPositionOfImage,
              width,
              topPositionOfImage + slice
            )
          )
          add(
            android.graphics.Rect(
              width - rightExclusionPx,
              middlePositionOfImage,
              width,
              middlePositionOfImage + slice
            )
          )
          add(
            android.graphics.Rect(
              width - rightExclusionPx,
              bottomPositionOfImage,
              width,
              bottomPositionOfImage + slice
            )
          )
        }

        view.setSystemGestureExclusionRects(rects)
        lastRects = rects
      }

      onDispose {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          view.setSystemGestureExclusionRects(emptyList())
        }
        lastRects = emptyList()
      }
    }

    // Draw the exclusion rects as a debug overlay
    ExclusionDebugOverlay(
      rects = lastRects,
      layerOffset = layerOffset,
      enabled = debugOverlay
    )

    content()
  }
}

@Suppress("Deprecation")
private fun Uri.toBitmap(context: Context): Bitmap? {

  return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
    MediaStore.Images.Media.getBitmap(context.contentResolver, this)
  } else {
    val source = ImageDecoder.createSource(context.contentResolver, this)
    ImageDecoder.decodeBitmap(source)
  }
}

@Composable
private fun ExclusionDebugOverlay(
  rects: List<android.graphics.Rect>,
  layerOffset: Offset,
  enabled: Boolean = true
) {
  if (!enabled) return
  androidx.compose.foundation.Canvas(
    modifier = Modifier
      .fillMaxSize()
      .zIndex(999f)
  ) {
    val ox = layerOffset.x
    val oy = layerOffset.y
    rects.forEach { r ->
      val left = r.left.toFloat() - ox
      val top = r.top.toFloat() - oy
      val width = (r.right - r.left).toFloat()
      val height = (r.bottom - r.top).toFloat()

      drawRect(
        color = androidx.compose.ui.graphics.Color(1f, 0f, 0f, 0.25f),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height)
      )
      drawRect(
        color = androidx.compose.ui.graphics.Color(1f, 0f, 0f, 0.9f),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
      )
    }
  }
}
