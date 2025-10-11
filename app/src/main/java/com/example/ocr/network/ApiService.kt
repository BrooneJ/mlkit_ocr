package com.example.ocr.network

import com.example.ocr.network.model.ResponseRequest
import com.example.ocr.network.model.ResponsesResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
  @POST("responses")
  suspend fun createResponse(@Body body: ResponseRequest): ResponsesResponse
}
