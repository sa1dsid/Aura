package com.aura.feature.news.data.di

import com.aura.feature.news.data.remote.MockNewsRemoteDataSource
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
    fun bindNewsRemoteDataSource(impl: MockNewsRemoteDataSource): NewsRemoteDataSource

    @Binds
    @Singleton
    fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository
}
