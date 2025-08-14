package com.example.ocr.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ocr.utils.loadBitmapFromUri
import com.example.ocr.utils.makeMosaicBitmap
import com.example.ocr.utils.renderToBitmap
import com.example.ocr.utils.saveBitmapToCacheUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tool { Mosaic, White, Eraser }

data class Stroke(
  val tool: Tool,
  val path: Path,
  val width: Float,
  val alpha: Float = 1f,
)

@Composable
fun DrawScreen(
  capturedImageUri: Uri,
  maxDecodeSizePx: Int = 2048,
  onExported: (Uri) -> Unit,
  mosaicFactor: Int = 16,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var mosaicedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  var tool by remember { mutableStateOf(Tool.Mosaic) }
  var strokeWidth by remember { mutableStateOf(24.dp) }
  val strokes = remember { mutableStateListOf<Stroke>() }
  val redoStack = remember { ArrayDeque<Stroke>() }
  var currentPath by remember { mutableStateOf<Path?>(null) }

  LaunchedEffect(capturedImageUri) {
    try {
      error = null
      baseBitmap = loadBitmapFromUri(context, capturedImageUri, maxDecodeSizePx)
      mosaicedBitmap = baseBitmap?.let { makeMosaicBitmap(it, mosaicFactor) }
    } catch (t: Throwable) {
      error = t.message
    }
  }

  if (error != null) {
    Log.d("DrawScreen", "Error loading image: $error")
    Text(
      "Failed to load image: $error",
      color = MaterialTheme.colorScheme.error,
      modifier = Modifier.padding(16.dp)
    )
    return
  }

  if (baseBitmap == null || mosaicedBitmap == null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val density = LocalDensity.current
  val strokeWidthPx = with(density) { strokeWidth.toPx() }

  val pointerModifier = Modifier.pointerInput(tool, strokeWidthPx) {
    detectDragGestures(
      onDragStart = { pos ->
        redoStack.clear()
        currentPath = Path().apply { moveTo(pos.x, pos.y) }
        strokes += Stroke(tool, currentPath!!, strokeWidthPx)
      },
      onDrag = { change, _ -> currentPath?.lineTo(change.position.x, change.position.y) },
      onDragEnd = { currentPath = null },
      onDragCancel = { currentPath = null }
    )
  }

  Scaffold(Modifier.fillMaxSize()) {
    Surface(
      modifier = Modifier.padding(it)
    ) {
      Column {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Seg(text = "Mosaic", selected = tool == Tool.Mosaic) { tool = Tool.Mosaic }
          Seg(text = "White", selected = tool == Tool.White) { tool = Tool.White }
          Seg(text = "Eraser", selected = tool == Tool.Eraser) { tool = Tool.Eraser }
          Seg(text = "Undo", enabled = strokes.isNotEmpty()) {
            if (strokes.isNotEmpty()) {
              val last = strokes.removeAt(strokes.lastIndex)
              redoStack.addLast(last)
            }
          }
          Seg(text = "Redo", enabled = redoStack.isNotEmpty()) {
            if (redoStack.isNotEmpty()) redoStack.removeLastOrNull()?.let { strokes.add(it) }
          }
          Seg(text = "Export") {
            scope.launch {
              val out = withContext(Dispatchers.Default) {
                renderToBitmap(baseBitmap!!, mosaicedBitmap!!, strokes)
              }
              val uri = withContext(Dispatchers.IO) {
                saveBitmapToCacheUri(context, out, "redacted_", ".jpg")
              }
              onExported(uri)
            }
          }
        }

        val b = baseBitmap!!
        Box(
          Modifier
            .fillMaxWidth()
            .aspectRatio(b.width / b.height.toFloat())
            .then(pointerModifier)
        ) {
          Canvas(Modifier.matchParentSize()) {
            drawImage(b.asImageBitmap())

            strokes.forEach { strokes ->
              when (strokes.tool) {
                Tool.White -> {
                  drawPath(
                    path = strokes.path,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                      width = strokes.width,
                      cap = StrokeCap.Round,
                      join = StrokeJoin.Round
                    ),
                    alpha = strokes.alpha
                  )
                }

                Tool.Mosaic -> {
                  clipPath(strokes.path) {
                    drawImage(
                      mosaicedBitmap!!.asImageBitmap(),
                      dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                  }
                }

                Tool.Eraser -> {
                  clipPath(strokes.path) {
                    drawImage(
                      b.asImageBitmap(),
                      dstSize = IntSize(size.width.toInt(), size.height.toInt())
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
private fun Seg(
  text: String,
  selected: Boolean = false,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  val colors =
    if (selected) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.outlinedButtonColors()
  FilledTonalButton(onClick = onClick, enabled = enabled, colors = colors) {
    Text(text)
  }
}