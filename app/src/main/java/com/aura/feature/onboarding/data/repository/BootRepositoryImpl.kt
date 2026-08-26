package com.aura.feature.onboarding.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.feature.onboarding.data.mapper.toDomain
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.repository.BootRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BootRepository {

    override suspend fun bootstrap(): BootConfig = withContext(ioDispatcher) {
        runCatchingCancellable { remote.bootstrap().toDomain() }.getOrDefault(BootConfig.FALLBACK)
    }
}
