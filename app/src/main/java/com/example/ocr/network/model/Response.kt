@file:OptIn(ExperimentalSerializationApi::class)

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
  val content: List<ContentPart>
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
@SerialName("input_image")
data class InputImage(
  @SerialName("image_url") val imageUrl: String
) : ContentPart

@Serializable
data class ResponsesResponse(
  val id: String? = null,
  val output: List<OutputItem>? = null,
  @SerialName("created_at") val created: Long? = null
)

@Serializable
@JsonClassDiscriminator("type")
sealed interface OutputItem

@Serializable
@SerialName("message")
data class OutputMessage(
  val id: String? = null,
  val status: String? = null,
  val role: String? = null,
  val content: List<OutputContent> = emptyList()
) : OutputItem

@Serializable
@JsonClassDiscriminator("type")
sealed interface OutputContent

@Serializable
@SerialName("output_text")
data class OutputText(
  val text: String,
) : OutputContent