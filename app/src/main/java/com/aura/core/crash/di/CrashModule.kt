package com.aura.core.crash.di

import android.content.Context
import com.aura.core.crash.CrashReporter
import com.aura.core.crash.FirebaseCrashReporter
import com.aura.core.crash.NoCrashReporter
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CrashModule {

    @Provides
    @Singleton
    fun provideCrashReporter(@ApplicationContext context: Context): CrashReporter =
        if (FirebaseApp.getApps(context).isEmpty()) {
            NoCrashReporter
        } else {
            FirebaseCrashReporter(FirebaseCrashlytics.getInstance())
        }
}
