@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OcrScreen(
  viewModel: OcrViewModel,
  onBack: () -> Unit
) {

  val text by viewModel.text.collectAsStateWithLifecycle()

  if (text == null) {
    viewModel.processImage(LocalContext.current)
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
    val context = LocalContext.current
    Surface(modifier = Modifier.padding(paddingValues)) {
      Column {
        Text(text = "This is the OCR screen")
        Text(text ?: "Processing...")
      }
    }
  }
}