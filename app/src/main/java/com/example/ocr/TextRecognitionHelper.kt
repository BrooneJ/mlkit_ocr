package com.example.ocr

import android.content.Context
import android.net.Uri
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