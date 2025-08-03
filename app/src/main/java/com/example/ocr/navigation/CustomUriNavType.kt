package com.example.ocr.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json

object CustomUriNavType {
  
  val UriType = object : NavType<Uri>(
    isNullableAllowed = false
  ) {
    override fun get(bundle: Bundle, key: String): Uri? {
      return Json.decodeFromString(bundle.getString(key) ?: return null)
    }

    override fun parseValue(value: String): Uri {
      return Json.decodeFromString(Uri.decode(value))
    }

    override fun serializeAsValue(value: Uri): String {
      return Uri.encode(Json.encodeToString(value))
    }

    override fun put(bundle: Bundle, key: String, value: Uri) {
      bundle.putString(key, Json.encodeToString(value))
    }
  }
}