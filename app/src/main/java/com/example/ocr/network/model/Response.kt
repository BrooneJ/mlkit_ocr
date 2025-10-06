package com.example.ocr.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class ResponseRequest(
  val model: String,
  val input: List<ResponseInput>
)

@Serializable
data class ResponseInput(
  val role: String,
  val content: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface ContentPart

@Serializable
@SerialName("input_text")
data class InputText(
  val text: String
) : ContentPart

@Serializable
data class ImageUrl(
  val url: String
)

@Serializable
@SerialName("input_image")
data class InputImage(
  @SerialName("image_url") val imageUrl: ImageUrl
) : ContentPart

@Serializable
data class ResponsesResponse(
  val id: String? = null,
  @SerialName("output_text") val outputText: String? = null,
  val created: Long? = null
)