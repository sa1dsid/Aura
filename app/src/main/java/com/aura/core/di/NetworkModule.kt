package com.aura.core.di

import com.aura.core.network.AndroidNetworkMonitor
import com.aura.core.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkModule {

    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
