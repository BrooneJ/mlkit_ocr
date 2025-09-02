package com.example.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

object TextRecognitionHelper {
  fun recognizeTextFromUri(
    context: Context,
    uri: Uri,
    onResult: (String) -> Unit,
  ) {
    val inputImage = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    recognizer.process(inputImage)
      .addOnSuccessListener { visionText ->
        Log.d("TextRecognition", "Recognized text: ${visionText.text}")
        for (block in visionText.textBlocks) {
          val blockText = block.text
          Log.d("TextRecognition", "Block text: $blockText")
          for (line in block.lines) {
            val lineText = line.text
            Log.d("TextRecognition", "\tLine text: $lineText")
            for (element in line.elements) {
              val elementText = element.text
              Log.d("TextRecognition", "\t\tElement text: $elementText")
            }
          }
        }
        // Process the recognized text
        onResult(visionText.text)
      }
      .addOnFailureListener { e ->
        // Handle any errors
        e.printStackTrace()
        onResult("Error recognizing text: ${e.message}")
      }
  }
}