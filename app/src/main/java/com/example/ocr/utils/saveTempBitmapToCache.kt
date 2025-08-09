package com.example.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun saveTempBitmapToCache(context: Context, bitmap: Bitmap): Uri {
  val temp = File.createTempFile("crop_", ".jpg", context.cacheDir)
  FileOutputStream(temp).use { out ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
  }

  return Uri.fromFile(temp)
}