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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ocr.common.components.BackNavDialog
import com.example.ocr.utils.loadBitmapFromUri
import com.example.ocr.utils.saveTempBitmapToCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

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
  val openBackNavDialog = remember { mutableStateOf(false) }
  BackHandler(enabled = !openBackNavDialog.value) {
    openBackNavDialog.value = true
  }

  if (openBackNavDialog.value) {
    BackNavDialog(
      onDismissRequest = {
        openBackNavDialog.value = false
      },
      onConfirm = {
        openBackNavDialog.value = false
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
        navigationIcon = {
          IconButton(onClick = { openBackNavDialog.value = true }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
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
      val safeGestureInsets = WindowInsets.safeGestures.asPaddingValues()

      Column(
        Modifier
          .fillMaxSize()
      ) {
        Canvas(
          modifier = Modifier
            .padding(safeGestureInsets)
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
                .size((thick / 2).dp) // Recommended to use fixed size for UI
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

fun renderDisplayComposite(
  baseBitmap: Bitmap,
  paths: List<PathData>,
  currentPath: PathData?,
  outWidth: Int,
  outHeight: Int,
  cropToImageRect: Boolean = false   // Added
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
