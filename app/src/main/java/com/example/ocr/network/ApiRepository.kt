package com.example.ocr.network

import android.content.Context
import android.net.Uri
import com.example.ocr.network.model.InputImage
import com.example.ocr.network.model.InputText
import com.example.ocr.network.model.OutputMessage
import com.example.ocr.network.model.OutputText
import com.example.ocr.network.model.ResponseInput
import com.example.ocr.network.model.ResponseRequest
import com.example.ocr.screen.ocr.utils.uriToDataUri
import dagger.hilt.android.qualifiers.ApplicationContext
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
  @ApplicationContext private val appContext: Context,
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
              imageUrl = dataUri
            )
          )
        )
      )
    )

    val res = api.createResponse(req)
    if (res.output == null) return "No response"
    val result = res.output.let { list ->
      val message = list.filterIsInstance<OutputMessage>().firstOrNull()
      val texts = message?.content?.filterIsInstance<OutputText>()?.map { it.text }
      texts?.joinToString("\n") ?: "No response"
    }
    return result
  }
}