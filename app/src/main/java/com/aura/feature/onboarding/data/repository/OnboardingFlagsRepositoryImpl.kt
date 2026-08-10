package com.aura.feature.onboarding.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.feature.onboarding.data.mapper.toDomain
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingFlagsRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OnboardingFlagsRepository {

    override suspend fun flags(accountId: String): OnboardingFlags =
        withContext(ioDispatcher) { remote.flags(accountId).toDomain() }

    override suspend fun markBonusPopupShown(accountId: String) {
        withContext(ioDispatcher) { remote.markBonusPopupShown(accountId) }
    }
}

@Singleton
class BootRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BootRepository {

    override suspend fun bootstrap(): BootConfig = withContext(ioDispatcher) {
        try {
            remote.bootstrap().toDomain()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            BootConfig(nodeCount = BootConfig.DEFAULT_NODE_COUNT, hotCities = emptyList())
        }
    }
}
