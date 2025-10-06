package com.example.ocr.screen.ocr.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

fun uriToDataUri(context: Context, uri: Uri, maxWidth: Int = 1280, jpegQuality: Int = 90): String {
  val resolver: ContentResolver = context.contentResolver
  val mime = resolver.getType(uri) ?: "image/*"

  val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
    val source = ImageDecoder.createSource(resolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
      decoder.isMutableRequired = false
    }
  } else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media.getBitmap(resolver, uri)
  }

  val scale = if (bitmap.width > maxWidth) {
    val ratio = maxWidth.toFloat() / bitmap.width
    val newW = maxWidth
    val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(bitmap, newW, newH, true)
  } else {
    bitmap
  }

  val baos = ByteArrayOutputStream()
  val usePng = mime.equals("image/png", ignoreCase = true)
  if (usePng) {
    scale.compress(Bitmap.CompressFormat.PNG, 100, baos)
  } else {
    scale.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
  }
  val bytes = baos.toByteArray()

  val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
  val finalMime = if (usePng) "image/png" else "image/jpeg"
  return "data:$finalMime;base64,$base64"
}