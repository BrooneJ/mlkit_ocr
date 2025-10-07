package com.example.ocr.network

import android.content.Context
import android.net.Uri
import com.example.ocr.network.model.ImageUrl
import com.example.ocr.network.model.InputImage
import com.example.ocr.network.model.InputText
import com.example.ocr.network.model.ResponseInput
import com.example.ocr.network.model.ResponseRequest
import com.example.ocr.screen.ocr.utils.uriToDataUri
import javax.inject.Inject

interface ApiRepository {
  suspend fun askWithImage(
    model: String,
    prompt: String,
    imageUri: Uri,
  ): String
}

class ApiRepositoryImpl @Inject constructor(
  private val api: OpenAiApi,
  private val appContext: Context
) : ApiRepository {
  override suspend fun askWithImage(
    model: String,
    prompt: String,
    imageUri: Uri,
  ): String {
    val dataUri = uriToDataUri(appContext, imageUri)

    val req = ResponseRequest(
      model = model,
      input = listOf(
        ResponseInput(
          role = "user",
          content = listOf(
            InputText(
              text = prompt
            ),
            InputImage(
              imageUrl = ImageUrl(url = dataUri)
            )
          )
        )
      )
    )

    val res = api.createResponse(req)
    return res.outputText ?: "No response"
  }
}