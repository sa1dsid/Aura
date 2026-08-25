package com.aura.core.session.di

import com.aura.core.geo.UserLocationSource
import com.aura.core.session.SessionCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {

    @Binds
    @IntoSet
    fun bindUserLocationSessionCache(impl: UserLocationSource): SessionCache
}
