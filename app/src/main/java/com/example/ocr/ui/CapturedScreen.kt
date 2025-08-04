package com.example.ocr.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ocr.R
import com.example.ocr.cropkit.CropDefaults
import com.example.ocr.cropkit.CropRatio
import com.example.ocr.cropkit.CropShape
import com.example.ocr.cropkit.GridLinesType
import com.example.ocr.cropkit.ImageCropper
import com.example.ocr.cropkit.rememberCropController
import com.example.ocr.ui.theme.OCRTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturedScreen(
  capturedImageUri: Uri,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    if (LocalInspectionMode.current) {
      Image(
        painter = painterResource(R.drawable.img_c02),
        contentDescription = null
      )
      return
    }

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
          title = { Text("Crop") }
        )
      }
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
                  .weight(1f)
                  .padding(24.dp),
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

@Suppress("Deprecation")
private fun Uri.toBitmap(context: Context): Bitmap? {

  return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
    MediaStore.Images.Media.getBitmap(context.contentResolver, this)
  } else {
    val source = ImageDecoder.createSource(context.contentResolver, this)
    ImageDecoder.decodeBitmap(source)
  }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CapturedScreenPreview() {
  OCRTheme {
    CapturedScreen(capturedImageUri = Uri.EMPTY)
  }
}