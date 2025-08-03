package com.example.ocr.navigation

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute

@Serializable
data class TakenPictureRoute(
  val encodedUri: String
) {
  val uri: Uri
    get() = Uri.parse(Uri.decode(encodedUri))

  companion object {
    fun create(uri: Uri): TakenPictureRoute {
      return TakenPictureRoute(encodedUri = Uri.encode(uri.toString()))
    }
  }
}
