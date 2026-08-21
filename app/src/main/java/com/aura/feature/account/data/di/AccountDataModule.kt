package com.aura.feature.account.data.di

import com.aura.feature.account.data.remote.AccountRemoteDataSource
import com.aura.feature.account.data.remote.ApiAccountRemoteDataSource
import com.aura.feature.account.data.repository.AccountRepositoryImpl
import com.aura.feature.account.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AccountDataModule {

    @Binds
    @Singleton
    fun bindAccountRemoteDataSource(
        impl: ApiAccountRemoteDataSource,
    ): AccountRemoteDataSource

    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
