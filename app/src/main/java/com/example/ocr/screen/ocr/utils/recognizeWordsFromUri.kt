package com.example.ocr.screen.ocr.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

data class OcrWord(
  val text: String,
  val left: Int, val top: Int, val right: Int, val bottom: Int,
) {
  val cx get() = (left + right) / 2f
  val cy get() = (top + bottom) / 2f
  val w = (right - left).toFloat()
  val h = (bottom - top).toFloat()
}

suspend fun recognizeWordsFromUri(context: Context, uri: Uri): List<OcrWord> =
  suspendCancellableCoroutine { continuation ->
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    recognizer.process(image)
      .addOnSuccessListener { text ->
        val words = mutableListOf<OcrWord>()
        text.textBlocks.forEach { block ->
          block.lines.forEach { line ->
            line.elements.forEach { el ->
              el.boundingBox?.let { rect ->
                if (el.text == "|") return@let
                words += OcrWord(
                  text = el.text.replace(Regex("^\\||\\|$"), "").replace("日", ""),
                  left = rect.left, top = rect.top,
                  right = rect.right, bottom = rect.bottom,
                )
              }
            }
          }
        }
        continuation.resume(words) {}
      }
      .addOnFailureListener { error -> continuation.resumeWithException(error) }
  }