package com.aura.core.push.di

import com.aura.core.push.FirebasePushTokenProvider
import com.aura.core.push.PushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PushModule {

    @Binds
    @Singleton
    fun bindPushTokenProvider(impl: FirebasePushTokenProvider): PushTokenProvider

    companion object {

        @Provides
        @Singleton
        fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    }
}
