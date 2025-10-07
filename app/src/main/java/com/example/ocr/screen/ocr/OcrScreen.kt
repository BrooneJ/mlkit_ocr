@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.ocr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.screen.ocr.components.SelectableCard

@Composable
fun OcrScreen(
  viewModel: OcrViewModel = hiltViewModel(),
  onBack: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(viewModel) {
    viewModel.processImage(context)
    viewModel.analyzeSchedule(context)
  }

  val scrollState = rememberScrollState()


  val text by viewModel.text.collectAsStateWithLifecycle()
  val headerBitmap by viewModel.headerPreview.collectAsStateWithLifecycle()
  val bodyBitmap by viewModel.bodyPreview.collectAsStateWithLifecycle()
  val adaptive by viewModel.adaptive.collectAsStateWithLifecycle()
  val fromPeak by viewModel.fromPeak.collectAsStateWithLifecycle()
  val fromWidth by viewModel.fromWidth.collectAsStateWithLifecycle()
  val fromValley by viewModel.fromValley.collectAsStateWithLifecycle()
  val enforce by viewModel.enforce.collectAsStateWithLifecycle()

  val dateCells by viewModel.dateCells.collectAsStateWithLifecycle()
  val workCells by viewModel.workCells.collectAsStateWithLifecycle()

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
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(16.dp)

      ) {
        Text(text = "Select appropriate card:")

        if (headerBitmap != null) {
          Image(headerBitmap!!.asImageBitmap(), contentDescription = null)
        }
        if (bodyBitmap != null) {
          Image(bodyBitmap!!.asImageBitmap(), contentDescription = null)
        }

        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = adaptive,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.ADAPTIVE)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = fromPeak,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMPEAK)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = fromWidth,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMWIDTH)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = fromValley,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.FROMVALLEY)
            )
          }
        )
        Spacer(modifier = Modifier.padding(15.dp))
        SelectableCard(
          bitmap = enforce,
          onClick = {
            viewModel.onAction(
              OcrAction.CardChosen(OcrType.ENFORCE)
            )
          }
        )

        Spacer(modifier = Modifier.padding(15.dp))
        Text(text = "Detected date cells:")
        Spacer(modifier = Modifier.padding(8.dp))
        dateCells.forEach { cellBitmap ->
          Image(cellBitmap.asImageBitmap(), contentDescription = null)
          Spacer(modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.padding(15.dp))
        workCells.forEach { cellBitmap ->
          Image(cellBitmap.asImageBitmap(), contentDescription = null)
          Spacer(modifier = Modifier.padding(8.dp))
        }
      }
    }
  }
}