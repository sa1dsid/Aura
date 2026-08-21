package com.aura.feature.onboarding.data.di

import com.aura.feature.onboarding.data.attribution.InstallReferrerSource
import com.aura.feature.onboarding.data.attribution.PlayInstallReferrerSource
import com.aura.feature.onboarding.data.remote.ApiOnboardingRemoteDataSource
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.data.repository.AuthRepositoryImpl
import com.aura.feature.onboarding.data.repository.BootRepositoryImpl
import com.aura.feature.onboarding.data.repository.InviteRepositoryImpl
import com.aura.feature.onboarding.data.repository.OnboardingFlagsRepositoryImpl
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface OnboardingDataModule {

    @Binds
    @Singleton
    fun bindInstallReferrerSource(impl: PlayInstallReferrerSource): InstallReferrerSource

    @Binds
    @Singleton
    fun bindOnboardingRemoteDataSource(
        impl: ApiOnboardingRemoteDataSource,
    ): OnboardingRemoteDataSource

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindInviteRepository(impl: InviteRepositoryImpl): InviteRepository

    @Binds
    @Singleton
    fun bindOnboardingFlagsRepository(
        impl: OnboardingFlagsRepositoryImpl,
    ): OnboardingFlagsRepository

    @Binds
    @Singleton
    fun bindBootRepository(impl: BootRepositoryImpl): BootRepository
}
