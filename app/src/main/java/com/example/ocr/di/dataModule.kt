package com.example.ocr.di

import com.example.ocr.network.ApiRepository
import com.example.ocr.network.ApiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface dataModule {

  @Binds
  fun bindApiRepository(impl: ApiRepositoryImpl): ApiRepository
}