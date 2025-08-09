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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.R
import com.example.ocr.cropkit.CropDefaults
import com.example.ocr.cropkit.CropRatio
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesType
import com.example.ocr.cropkit.ImageCropper
import com.example.ocr.cropkit.rememberCropController

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
                  // TODO: refactor this code
                  val croppedUri = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    it,
                    "Cropped Image",
                    "Cropped using OCR App"
                  ).toUri()
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

          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {

            if (cropController != null) {
              val cropState = cropController?.state?.collectAsStateWithLifecycle()?.value
              var cropperBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

              EdgeExclusionLayer(
                modifier = Modifier
                  .weight(1f),
                leftDp = 48.dp,
                rightDp = 48.dp,
                // 3) 루트 기준 이미지 사각형을 넘겨줌
                targetBounds = remember(cropperBoundsInRoot, cropState?.imageRect) {
                  val b = cropperBoundsInRoot
                  val img = cropState?.imageRect
                  if (b != null && img != null) {
                    // Compose Rect은 Float px 단위. 루트 좌표로 translate
                    Rect(
                      left = b.left + img.left,
                      top = b.top + img.top,
                      right = b.left + img.right,
                      bottom = b.top + img.bottom
                    )
                  } else null
                }
              ) {
                ImageCropper(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // 2) ImageCropper(=Canvas) 의 루트 기준 bounds
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
}

@Composable
fun EdgeExclusionLayer(
  modifier: Modifier = Modifier,
  leftDp: Dp = 48.dp,
  rightDp: Dp = 48.dp,
  targetBounds: androidx.compose.ui.geometry.Rect? = null,
  debugOverlay: Boolean = true,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  val density = LocalDensity.current
  var lastRects by remember { mutableStateOf(emptyList<android.graphics.Rect>()) }

  // 👇 이 박스가 루트 기준 어디에 있는지 저장
  var layerOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

  Box(
    modifier
      .fillMaxWidth() // 필요에 맞게
      .onGloballyPositioned { coords ->
        layerOffset = coords.positionInRoot() // 루트기준 (x,y)
      }
  ) {
    DisposableEffect(targetBounds) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetBounds != null) {
        val w = view.width
        val h = view.height
        val leftPx = with(density) { leftDp.roundToPx() }
        val rightPx = with(density) { rightDp.roundToPx() }
        val maxH = with(density) { 200.dp.roundToPx() } // 엣지당 합집합 200dp

        val t = targetBounds.top.toInt().coerceIn(0, h)
        val b = targetBounds.bottom.toInt().coerceIn(0, h)
        val targetHeight = (b - t).coerceAtLeast(0)

        // 200dp 안에서 3분할 예시 (겹치지 않게)
        val slice = minOf(maxH, targetHeight) / 3
        val top1 = t - slice / 2
        val top2 = t + (targetHeight - slice) / 2   // ✅ t 보정
        val top3 = b - slice / 2

        val rects = buildList {
          // 왼쪽 3슬라이스
          add(android.graphics.Rect(0, top1, leftPx, top1 + slice))
          add(android.graphics.Rect(0, top2, leftPx, top2 + slice))
          add(android.graphics.Rect(0, top3, leftPx, top3 + slice))
          // 오른쪽 3슬라이스
          add(android.graphics.Rect(w - rightPx, top1, w, top1 + slice))
          add(android.graphics.Rect(w - rightPx, top2, w, top2 + slice))
          add(android.graphics.Rect(w - rightPx, top3, w, top3 + slice))
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

    // 👇 루트 좌표로 만든 rects를, 현재 레이어 로컬 좌표로 그리기 위해 offset을 빼줍니다.
    ExclusionDebugOverlay(
      rects = lastRects,
      layerOffset = layerOffset, // ★ 추가
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
  layerOffset: androidx.compose.ui.geometry.Offset, // ★ 추가
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
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height)
      )
      drawRect(
        color = androidx.compose.ui.graphics.Color(1f, 0f, 0f, 0.9f),
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
      )
    }
  }
}