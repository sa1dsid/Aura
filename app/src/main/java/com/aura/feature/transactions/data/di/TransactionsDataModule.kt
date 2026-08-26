package com.aura.feature.transactions.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.transactions.data.remote.ApiTransactionsRemoteDataSource
import com.aura.feature.transactions.data.remote.TransactionsRemoteDataSource
import com.aura.feature.transactions.data.repository.TransactionsRepositoryImpl
import com.aura.feature.transactions.domain.repository.TransactionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TransactionsDataModule {

    @Binds
    @Singleton
    fun bindTransactionsRemoteDataSource(
        impl: ApiTransactionsRemoteDataSource,
    ): TransactionsRemoteDataSource

    @Binds
    @Singleton
    fun bindTransactionsRepository(impl: TransactionsRepositoryImpl): TransactionsRepository

    @Binds
    @IntoSet
    fun bindTransactionsSessionCache(impl: TransactionsRepositoryImpl): SessionCache
}
