package com.aura.core.push.di

import com.aura.core.push.PushTokenProvider
import com.aura.core.push.UnavailablePushTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PushModule {

    @Binds
    @Singleton
    fun bindPushTokenProvider(impl: UnavailablePushTokenProvider): PushTokenProvider
}
