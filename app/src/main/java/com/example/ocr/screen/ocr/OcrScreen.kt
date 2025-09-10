@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.ocr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OcrScreen(
  viewModel: OcrViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(viewModel) {
    viewModel.processImage(context)
  }

  val text by viewModel.text.collectAsStateWithLifecycle()
  val headerBitmap by viewModel.headerPreview.collectAsStateWithLifecycle()

  if (text == null) {
    Text("No Text")
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(text = "OCR Screen") },
        navigationIcon = {
          IconButton(onClick = { onBack() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
      )
    }
  ) { paddingValues ->
    Surface(modifier = Modifier.padding(paddingValues)) {
      Column {
        Text(text = "This is the OCR screen")
        Text(text ?: "Processing...")
        Text(text = "Header Preview:")
        if (headerBitmap != null) {
          Image(
            bitmap = headerBitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp)
          )
        } else {
          Text(text = "No header preview available", modifier = Modifier.padding(12.dp))
        }
      }
    }
  }
}