package com.example.ocr.ui

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ocr.CameraBox
import com.example.ocr.TextRecognitionHelper

@Composable
fun MainScreen() {
  var recognizedText by remember { mutableStateOf("Recognized text will appear here.") }
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  val context = LocalContext.current
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    selectedImageUri = uri
    uri?.let {
      TextRecognitionHelper.recognizeTextFromUri(context, uri) { result ->
        recognizedText = result
      }
    }
  }
  val cameraPermissionState = remember { mutableStateOf(false) }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      cameraPermissionState.value = isGranted
    }
  )

  LaunchedEffect(Unit) {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    if (hasPermission) {
      cameraPermissionState.value = true
    } else {
      permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
  }

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(8.dp)
    ) {
      if (cameraPermissionState.value) {
        CameraBox {
          recognizedText = it
        }
      } else {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text("Camera permission is required to use this feature.")
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { galleryLauncher.launch("image/*") },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
      ) {
        Text("Select Image from Gallery")
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = recognizedText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
      )
    }
  }
}