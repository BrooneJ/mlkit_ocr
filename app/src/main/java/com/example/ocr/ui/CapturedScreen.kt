package com.example.ocr.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CapturedScreen(
  capturedImageUri: Uri,
) {
  Log.d("CapturedScreen", "Captured image URI: $capturedImageUri")
  Box(modifier = Modifier.fillMaxSize()) {
    Text("Captured Image URI: $capturedImageUri")
  }
  // This screen will display the captured image and recognized text.
  // You can implement the UI here using Compose.

  // For example:
  // Box(modifier = Modifier.fillMaxSize()) {
  //     AsyncImage(model = capturedImageUri, contentDescription = null)
  //     Text(text = recognizedText, style = MaterialTheme.typography.body1)
  //     Button(onClick = onBack) {
  //         Text("Back")
  //     }
  // }
}