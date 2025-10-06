package com.example.ocr.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val apiKey: String) : Interceptor {
  override fun intercept(chain: Interceptor.Chain) = chain.proceed(
    chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $apiKey")
      .addHeader("Content-Type", "application/json")
      .build()
  )
}

fun provideOkHttp(apiKey: String): OkHttpClient {
  return OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(apiKey))
    .connectTimeout(15, TimeUnit.SECONDS)
    .build()
}