package com.example.ocr.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.example.ocr.R
import com.example.ocr.ui.theme.OCRTheme

@Composable
fun CapturedScreen(
  capturedImageUri: Uri,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    if (LocalInspectionMode.current) {
      Image(
        painter = painterResource(R.drawable.img_c02),
        contentDescription = null
      )
    } else {
      AsyncImage(
        model = capturedImageUri,
        contentDescription = null,
      )
    }
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