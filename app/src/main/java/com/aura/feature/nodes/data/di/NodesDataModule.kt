package com.aura.feature.nodes.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.nodes.data.remote.ApiNodesRemoteDataSource
import com.aura.feature.nodes.data.remote.NodesRemoteDataSource
import com.aura.feature.nodes.data.repository.NodesRepositoryImpl
import com.aura.feature.nodes.domain.repository.NodesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NodesDataModule {

    @Binds
    @Singleton
    fun bindNodesRemoteDataSource(impl: ApiNodesRemoteDataSource): NodesRemoteDataSource

    @Binds
    @Singleton
    fun bindNodesRepository(impl: NodesRepositoryImpl): NodesRepository

    @Binds
    @IntoSet
    fun bindNodesSessionCache(impl: NodesRepositoryImpl): SessionCache
}
