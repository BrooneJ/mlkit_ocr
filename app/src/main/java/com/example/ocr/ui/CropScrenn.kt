@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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

  EdgeExclusionLayer(
    leftDp = 48.dp,
    rightDp = 48.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
    ) {

      var image: Bitmap? by remember { mutableStateOf(null) }
      var cropShape: CropShape by remember { mutableStateOf(CropShape.Original) }
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
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {

              if (cropController != null) {

                ImageCropper(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                  cropController = cropController
                )

                Spacer(Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {

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
}

@Composable
fun EdgeExclusionLayer(
  leftDp: Dp = 48.dp,
  rightDp: Dp = 48.dp,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  val density = LocalDensity.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned {
        val h = view.height
        val w = view.width
        val leftPx = with(density) { leftDp.toPx().toInt() }
        val rightPx = with(density) { rightDp.toPx().toInt() }

        val rects = buildList {
          if (leftPx > 0) add(android.graphics.Rect(0, 0, leftPx, h))
          if (rightPx > 0) add(android.graphics.Rect(w - rightPx, 0, w, h))
        }
        Log.d("Exclusion", "left=$leftPx right=$rightPx w=$w h=$h")

        view.setSystemGestureExclusionRects(rects)
      }
  ) {

    DisposableEffect(Unit) {
      onDispose { view.setSystemGestureExclusionRects(emptyList()) }
    }
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
