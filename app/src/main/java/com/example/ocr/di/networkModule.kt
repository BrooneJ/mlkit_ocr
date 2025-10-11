package com.example.ocr.di

import com.example.ocr.BuildConfig
import com.example.ocr.network.ApiKey
import com.example.ocr.network.AuthInterceptor
import com.example.ocr.network.OpenAiApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

private const val BASE_URL = "https://api.openai.com/v1/"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppJson

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @ApiKey
  fun provideApiKey(): String = BuildConfig.API_KEY

  @Provides
  @Singleton
  fun provideOkHttp(
    authInterceptor: AuthInterceptor
  ): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(120, TimeUnit.SECONDS)
      .writeTimeout(120, TimeUnit.SECONDS)
      .callTimeout(150, TimeUnit.SECONDS)
      .build()

  @Singleton
  @Provides
  @AppJson
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
  }

  @Provides
  @Singleton
  fun providedOpenAiApi(@AppJson json: Json, okhttp: OkHttpClient): OpenAiApi {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
      .baseUrl(BASE_URL)
      .addConverterFactory(json.asConverterFactory(contentType))
      .client(okhttp)
      .build()
      .create()
  }
}