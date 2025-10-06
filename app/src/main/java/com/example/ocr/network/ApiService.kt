package com.example.ocr.network

import com.example.ocr.network.model.ResponseRequest
import com.example.ocr.network.model.ResponsesResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST

private const val BASE_URL = "https://api.openai.com/v1/"

interface OpenAiApi {
  @POST("responses")
  suspend fun createResponse(@Body body: ResponseRequest): ResponsesResponse
}

private val json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = false
}

fun providedOpenAiApi(okhttp: OkHttpClient): OpenAiApi {
  val contentType = "application/json".toMediaType()
  return Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(json.asConverterFactory(contentType))
    .client(okhttp)
    .build()
    .create()
}
