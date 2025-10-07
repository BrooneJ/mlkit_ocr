package com.example.ocr.network

import okhttp3.Interceptor
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
  @ApiKey private val apiKey: String
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain) = chain.proceed(
    chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $apiKey")
      .addHeader("Content-Type", "application/json")
      .build()
  )
}
