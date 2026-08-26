package com.aura.feature.onboarding.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.feature.onboarding.data.mapper.toDomain
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingFlagsRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OnboardingFlagsRepository {

    override suspend fun flags(): OnboardingFlags = withContext(ioDispatcher) {
        runCatchingCancellable { remote.flags().toDomain() }.getOrDefault(OnboardingFlags.FALLBACK)
    }

    override suspend fun markBonusPopupShown() {
        withContext(ioDispatcher) { runCatchingCancellable { remote.markBonusPopupShown() } }
    }
}
