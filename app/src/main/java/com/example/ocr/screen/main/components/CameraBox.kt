package com.example.ocr.screen.main.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ocr.TextRecognitionHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraBox(onTextRecognized: (String) -> Unit, onCaptured: (Uri) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val previewView = remember {
    PreviewView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }
  }
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance((context)) }
  val imageCapture = remember { ImageCapture.Builder().build() }
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  // Bind camera
  LaunchedEffect(Unit) {
    cameraProviderFuture.addListener(
      {
        try {
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
          }
          val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
          )
        } catch (e: Exception) {
          Log.e("CameraBox", "Use case binding failed", e)
        }
      }, ContextCompat.getMainExecutor(context)
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .border(2.dp, Color.Gray, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .aspectRatio(1f)
      .background(Color.Black)
  ) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize()
    )
    Button(
      onClick = {
        context.cacheDir.listFiles()?.forEach { file ->
          if (file.name.startsWith("capture_image_") && file.extension == "jpg") {
            file.delete() // Clean up old images
          }
        }
        val outputFile = File(
          context.cacheDir,
          "capture_image_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(
          outputOptions,
          cameraExecutor,
          object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
              val savedUri = outputFileResults.savedUri ?: outputFile.toUri()
              coroutineScope.launch {
                onCaptured(savedUri)
              }
              TextRecognitionHelper.recognizeTextFromUri(context, savedUri) {
                onTextRecognized(it)
              }
            }

            override fun onError(exception: ImageCaptureException) {
              Log.e("CameraPreview", "Capture failed: ${exception.message}", exception)
            }
          }
        )
      },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(12.dp)
    ) {
      Text("Capture")
    }
  }
}