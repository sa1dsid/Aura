package com.aura.feature.onboarding.data.repository

import com.aura.core.api.toInviteFailure
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.feature.onboarding.data.attribution.InviteAttributionStore
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.repository.InviteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    private val attributionStore: InviteAttributionStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InviteRepository {

    override suspend fun pendingAttribution(): InviteAttribution =
        withContext(ioDispatcher) { attributionStore.pending() }

    override suspend fun rememberDeepLinkCode(code: String) {
        withContext(ioDispatcher) { attributionStore.rememberDeepLink(code) }
    }

    override suspend fun applyCode(code: String): Result<Unit> =
        settle { remote.applyInviteCode(code) }

    override suspend fun skipInvite(): Result<Unit> = settle { remote.skipInvite() }

    private suspend fun settle(decision: suspend () -> Unit): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingCancellable { decision() }.fold(
                onSuccess = {
                    attributionStore.consume()
                    Result.success(Unit)
                },
                onFailure = { error -> Result.failure(error.toInviteFailure()) },
            )
        }
}
