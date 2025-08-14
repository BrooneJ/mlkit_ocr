package com.example.ocr.screen.captured

import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.ocr.R
import com.example.ocr.ui.theme.OCRTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturedScreen(
  capturedImageUri: Uri,
  onCrop: (Uri) -> Unit = {},
  onDraw: (Uri) -> Unit = {}
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

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = {
            Text(text = "Preview Captured Image")
          }
        )
      }
    ) { padding ->

      Surface(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(Modifier.height(24.dp))

          AsyncImage(
            model = capturedImageUri,
            contentDescription = null,
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            IconButton(
              onClick = {
                onCrop(capturedImageUri)
              }
            ) {
              Icon(
                painter = painterResource(id = R.drawable.crop),
                contentDescription = "Crop",
              )
            }

            IconButton(
              onClick = {
                onDraw(capturedImageUri)
              }
            ) {
              Icon(
                painter = painterResource(id = R.drawable.pencil),
                contentDescription = "Edit",
              )
            }
          }
        }
      }
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