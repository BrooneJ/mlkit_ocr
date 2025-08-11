package com.example.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves the given bitmap to a temporary file in the cache directory.
 * Returns the Uri of the saved file, or null if an error occurs.
 */
fun saveTempBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
  return try {
    val temp = File.createTempFile("crop_", ".jpg", context.cacheDir)
    FileOutputStream(temp).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    Uri.fromFile(temp)
  } catch (e: IOException) {
    e.printStackTrace()
    null
  }
}