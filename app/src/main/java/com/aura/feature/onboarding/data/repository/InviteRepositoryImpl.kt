package com.aura.feature.onboarding.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.feature.onboarding.data.attribution.InviteAttributionStore
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.repository.InviteRepository
import kotlinx.coroutines.CancellationException
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

    override suspend fun applyCode(accountId: String, code: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                remote.applyInviteCode(accountId, code)
                attributionStore.consume()
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error.asInviteFailure())
            }
        }

    override suspend fun skipInvite(accountId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                remote.skipInvite(accountId)
                attributionStore.consume()
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error.asInviteFailure())
            }
        }

    private fun Throwable.asInviteFailure(): Throwable =
        this as? InviteException ?: InviteException(InviteFailure.NETWORK)
}
