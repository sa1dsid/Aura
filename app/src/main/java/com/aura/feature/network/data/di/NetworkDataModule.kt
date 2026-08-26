package com.aura.feature.network.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.network.data.diagnostics.HttpThroughputProbe
import com.aura.feature.network.data.diagnostics.SocketPingProbe
import com.aura.feature.network.data.diagnostics.SpeedTestEngine
import com.aura.feature.network.data.diagnostics.ThroughputProbe
import com.aura.feature.network.data.diagnostics.PingProbe
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.remote.ApiNetworkRemoteDataSource
import com.aura.feature.network.data.remote.LinkConditionsSource
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.data.repository.NetworkRepositoryImpl
import com.aura.feature.network.data.repository.PingHistoryRepositoryImpl
import com.aura.feature.network.domain.repository.NetworkRepository
import com.aura.feature.network.domain.repository.PingHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkDataModule {

    @Binds
    @Singleton
    fun bindNetworkRemoteDataSource(impl: ApiNetworkRemoteDataSource): NetworkRemoteDataSource

    @Binds
    @Singleton
    fun bindPingProbe(impl: SocketPingProbe): PingProbe

    @Binds
    @Singleton
    fun bindThroughputProbe(impl: HttpThroughputProbe): ThroughputProbe

    @Binds
    @Singleton
    fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepository

    @Binds
    @Singleton
    fun bindPingHistoryRepository(impl: PingHistoryRepositoryImpl): PingHistoryRepository

    @Binds
    @IntoSet
    fun bindNetworkSessionCache(impl: NetworkRepositoryImpl): SessionCache

    @Binds
    @IntoSet
    fun bindNetworkLogSessionCache(impl: NetworkLocalStore): SessionCache

    @Binds
    @IntoSet
    fun bindSpeedTestSessionCache(impl: SpeedTestEngine): SessionCache

    @Binds
    @IntoSet
    fun bindLinkConditionsSessionCache(impl: LinkConditionsSource): SessionCache
}
