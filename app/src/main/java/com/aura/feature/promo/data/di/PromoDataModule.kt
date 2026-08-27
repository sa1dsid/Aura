package com.aura.feature.promo.data.di

import com.aura.core.session.SessionCache
import com.aura.feature.promo.data.remote.ApiPromoCodesRemoteDataSource
import com.aura.feature.promo.data.remote.PromoCodesRemoteDataSource
import com.aura.feature.promo.data.repository.PromoCodesRepositoryImpl
import com.aura.feature.promo.domain.repository.PromoCodesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PromoDataModule {

    @Binds
    @Singleton
    fun bindPromoCodesRemoteDataSource(
        impl: ApiPromoCodesRemoteDataSource,
    ): PromoCodesRemoteDataSource

    @Binds
    @Singleton
    fun bindPromoCodesRepository(impl: PromoCodesRepositoryImpl): PromoCodesRepository

    @Binds
    @IntoSet
    fun bindPromoCodesSessionCache(impl: PromoCodesRepositoryImpl): SessionCache
}
