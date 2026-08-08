package com.aura.feature.home.data.di

import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.data.remote.MeshRemoteDataSource
import com.aura.feature.home.data.remote.MockHomeRemoteDataSource
import com.aura.feature.home.data.remote.MockMeshRemoteDataSource
import com.aura.feature.home.data.repository.HomeRepositoryImpl
import com.aura.feature.home.data.repository.MeshRepositoryImpl
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface HomeDataModule {

    @Binds
    @Singleton
    fun bindMeshRemoteDataSource(impl: MockMeshRemoteDataSource): MeshRemoteDataSource

    @Binds
    @Singleton
    fun bindHomeRemoteDataSource(impl: MockHomeRemoteDataSource): HomeRemoteDataSource

    @Binds
    @Singleton
    fun bindMeshRepository(impl: MeshRepositoryImpl): MeshRepository

    @Binds
    @Singleton
    fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
