package com.aura.feature.terminal.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.terminal.data.remote.ApiTerminalRemoteDataSource
import com.aura.feature.terminal.data.remote.TerminalRemoteDataSource
import com.aura.feature.terminal.data.repository.TerminalRepositoryImpl
import com.aura.feature.terminal.domain.repository.TerminalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TerminalDataModule {

    @Binds
    @Singleton
    fun bindTerminalRemoteDataSource(impl: ApiTerminalRemoteDataSource): TerminalRemoteDataSource

    @Binds
    @Singleton
    fun bindTerminalRepository(impl: TerminalRepositoryImpl): TerminalRepository

    @Binds
    @IntoSet
    fun bindTerminalSessionCache(impl: TerminalRepositoryImpl): SessionCache
}
