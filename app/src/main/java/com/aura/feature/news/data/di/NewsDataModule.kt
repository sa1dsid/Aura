package com.aura.feature.news.data.di

import com.aura.feature.news.data.remote.ApiNewsRemoteDataSource
import com.aura.feature.news.data.remote.NewsRemoteDataSource
import com.aura.feature.news.data.repository.NewsRepositoryImpl
import com.aura.feature.news.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NewsDataModule {

    @Binds
    @Singleton
    fun bindNewsRemoteDataSource(impl: ApiNewsRemoteDataSource): NewsRemoteDataSource

    @Binds
    @Singleton
    fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository
}
