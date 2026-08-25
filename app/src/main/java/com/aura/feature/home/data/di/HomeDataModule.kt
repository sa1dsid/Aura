package com.aura.feature.home.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.home.data.local.DataStoreTapSessionStore
import com.aura.feature.home.data.local.TapSessionStore
import com.aura.feature.home.data.remote.ApiHomeRemoteDataSource
import com.aura.feature.home.data.remote.ApiMeshRemoteDataSource
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.data.remote.MeshRemoteDataSource
import com.aura.feature.home.data.repository.HomeRepositoryImpl
import com.aura.feature.home.data.repository.MeshRepositoryImpl
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface HomeDataModule {

    @Binds
    @Singleton
    fun bindMeshRemoteDataSource(impl: ApiMeshRemoteDataSource): MeshRemoteDataSource

    @Binds
    @Singleton
    fun bindHomeRemoteDataSource(impl: ApiHomeRemoteDataSource): HomeRemoteDataSource

    @Binds
    @Singleton
    fun bindMeshRepository(impl: MeshRepositoryImpl): MeshRepository

    @Binds
    @Singleton
    fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    @Binds
    @Singleton
    fun bindTapSessionStore(impl: DataStoreTapSessionStore): TapSessionStore

    @Binds
    @IntoSet
    fun bindHomeSessionCache(impl: HomeRepositoryImpl): SessionCache

    @Binds
    @IntoSet
    fun bindMeshSessionCache(impl: MeshRepositoryImpl): SessionCache

    @Binds
    @IntoSet
    fun bindTestSessionCache(impl: TestSessionEngine): SessionCache
}
