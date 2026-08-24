package com.aura.core.di

import com.aura.core.common.SystemTimeSource
import com.aura.core.common.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TimeModule {

    @Binds
    @Singleton
    fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
