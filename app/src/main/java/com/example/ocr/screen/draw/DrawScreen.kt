@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr.screen.draw

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.example.ocr.screen.draw.components.CustomAlertDialog
import com.example.ocr.utils.loadBitmapFromUri
import com.example.ocr.utils.saveTempBitmapToCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class Tool { Mosaic, White, Eraser }

data class Stroke(
  val tool: Tool,
  val path: Path,
  val width: Float,
  val alpha: Float = 1f,
)

@Composable
fun DrawScreen(
  paths: List<PathData>,
  currentPath: PathData?,
  thickness: Float,
  onAction: (DrawingAction) -> Unit,
  capturedImageUri: Uri,
  maxDecodeSizePx: Int = 2048,
  onExported: (Uri) -> Unit,
  onBack: () -> Unit
) {
  val openAlertDialog = remember { mutableStateOf(false) }
  BackHandler(enabled = !openAlertDialog.value) {
    openAlertDialog.value = true
  }

  if (openAlertDialog.value) {
    CustomAlertDialog(
      onDismissRequest = {
        openAlertDialog.value = false
      },
      onConfirm = {
        openAlertDialog.value = false
        onBack()
        onAction(DrawingAction.OnClearCanvas)
      },
      dialogTitle = "Discard Changes?",
      dialogMessage = "Are you sure you want to discard your changes and go back?"
    )
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var canvasSize by remember { mutableStateOf(IntSize.Zero) }

  LaunchedEffect(capturedImageUri) {
    try {
      bitmap = loadBitmapFromUri(context, capturedImageUri, maxDecodeSizePx)
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (t: Throwable) {
      // Handle error loading bitmap
      t.printStackTrace()
    }
  }

  if (bitmap == null) {
    // Show loading or error state
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  Scaffold(
    Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Draw on Image") },
        actions = {
          IconButton(
            onClick = {
              scope.launch {
                val outBmp = renderDisplayComposite(
                  baseBitmap = bitmap!!,
                  paths = paths,
                  currentPath = currentPath,
                  outWidth = canvasSize.width,
                  outHeight = canvasSize.height,
                  cropToImageRect = true
                )

                val uri = saveTempBitmapToCache(
                  context = context,
                  bitmap = outBmp,
                )
                if (uri == null) {
                  // Handle error saving bitmap
                  return@launch
                }
                onExported(uri)
                onAction(DrawingAction.OnClearCanvas)
              }
            }
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
          }
        }
      )
    }) {
    Surface(
      modifier = Modifier.padding(it)
    ) {
      Column(
        Modifier
          .fillMaxSize()
      ) {
        Canvas(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .onSizeChanged { size ->
              canvasSize = size
            }
            .clipToBounds()
            .pointerInput(true) {
              detectDragGestures(
                onDragStart = {
                  onAction(DrawingAction.OnNewPathStart)
                },
                onDragEnd = {
                  onAction(DrawingAction.OnPathEnd)
                },
                onDrag = { change, _ ->
                  onAction(
                    DrawingAction.OnDraw(change.position)
                  )
                },
                onDragCancel = {
                  onAction(DrawingAction.OnPathEnd)
                }
              )
            }
        ) {
          val canvasWidth = size.width
          val canvasHeight = size.height
          val bitmapWidth = bitmap!!.width.toFloat()
          val bitmapHeight = bitmap!!.height.toFloat()

          val scale = minOf(
            canvasWidth / bitmapWidth,
            canvasHeight / bitmapHeight
          )
          val dstWidth = bitmapWidth * scale
          val dstHeight = bitmapHeight * scale
          val offsetX = (canvasWidth - dstWidth) / 2
          val offsetY = (canvasHeight - dstHeight) / 2

          drawImage(
            image = bitmap!!.asImageBitmap(),
            srcSize = IntSize(bitmap!!.width, bitmap!!.height),
            dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt())
          )

          paths.fastForEach { pathData ->
            drawPath(
              path = pathData.path,
              color = pathData.color,
              thickness = pathData.thickness
            )
          }
          currentPath?.let {
            drawPath(
              path = it.path,
              color = it.color,
              thickness = it.thickness
            )
          }
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val thicks = listOf(30f, 50f, 70f)
          thicks.forEach { thick ->
            val isSelected = thickness == thick
            Box(
              modifier = Modifier
                .size((thick / 2).dp) // UI 용 고정 크기 권장
                .clip(CircleShape)
                .background(Color.White)
                .border(if (isSelected) 2.dp else 0.dp, Color.DarkGray, CircleShape)
                .clickable { onAction(DrawingAction.OnSelectThickness(thick)) }
            )
          }
        }
      }
    }
  }

// May be used for mosaic or other drawing tools in the future

  /*
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
  */
}

private fun DrawScope.drawPath(
  path: List<Offset>,
  color: Color,
  thickness: Float = 10f
) {
  val smoothedPath = Path().apply {
    if (path.isNotEmpty()) {
      moveTo(path.first().x, path.first().y)

      val smoothness = 5
      for (i in 1..path.lastIndex) {
        val from = path[i - 1]
        val to = path[i]
        val dx = abs(from.x - to.x)
        val dy = abs(from.y - to.y)
        if (dx >= smoothness || dy >= smoothness) {
          quadraticTo(
            x1 = (from.x + to.x) / 2f,
            y1 = (from.y + to.y) / 2f,
            x2 = to.x,
            y2 = to.y
          )
        }
      }
    }
  }
  drawPath(
    path = smoothedPath,
    color = color,
    style = Stroke(
      width = thickness,
      cap = StrokeCap.Round,
      join = StrokeJoin.Round
    )
  )
}

suspend fun renderDisplayComposite(
  baseBitmap: Bitmap,
  paths: List<PathData>,
  currentPath: PathData?,
  outWidth: Int,
  outHeight: Int,
  cropToImageRect: Boolean = false   // ★ 추가
): Bitmap {
  val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
  val androidCanvas = android.graphics.Canvas(out)
  val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
  val drawScope = CanvasDrawScope()
  val imageBitmap = baseBitmap.asImageBitmap()

  var dstLeft = 0
  var dstTop = 0
  var dstW = outWidth
  var dstH = outHeight

  drawScope.draw(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    canvas = composeCanvas,
    size = Size(outWidth.toFloat(), outHeight.toFloat())
  ) {
    val bw = imageBitmap.width.toFloat()
    val bh = imageBitmap.height.toFloat()

    // Fit
    val scale = minOf(size.width / bw, size.height / bh)
    val dw = (bw * scale)
    val dh = (bh * scale)
    val ox = (size.width - dw) / 2f
    val oy = (size.height - dh) / 2f

    dstLeft = ox.toInt().coerceAtLeast(0)
    dstTop = oy.toInt().coerceAtLeast(0)
    dstW = dw.toInt().coerceAtMost(outWidth - dstLeft)
    dstH = dh.toInt().coerceAtMost(outHeight - dstTop)

    drawImage(
      image = imageBitmap,
      srcSize = IntSize(imageBitmap.width, imageBitmap.height),
      dstOffset = IntOffset(dstLeft, dstTop),
      dstSize = IntSize(dstW, dstH)
    )

    paths.forEach { drawPath(it.path, it.color, it.thickness) }
    currentPath?.let { drawPath(it.path, it.color, it.thickness) }
  }

  if (!cropToImageRect) return out

  return Bitmap.createBitmap(out, dstLeft, dstTop, dstW, dstH)
}

// May be used for future segmented buttons
/*
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
}*/
