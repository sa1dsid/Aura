package com.aura.feature.account.data.repository

import com.aura.core.auth.TokenStore
import com.aura.core.common.IoDispatcher
import com.aura.feature.account.data.mapper.toDomain
import com.aura.feature.account.data.mapper.toProfile
import com.aura.feature.account.data.remote.AccountRemoteDataSource
import com.aura.feature.account.domain.model.AccountProfile
import com.aura.feature.account.domain.model.LegalLinks
import com.aura.feature.account.domain.repository.AccountRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val remote: AccountRemoteDataSource,
    private val sessionStore: SessionStore,
    private val tokenStore: TokenStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    override val profile: Flow<AccountProfile?> =
        sessionStore.account.map { account -> account?.toProfile() }

    override suspend fun pushNotifications(): Result<Boolean> =
        request { accountId -> remote.settings(accountId).pushNotifications }

    override suspend fun setPushNotifications(enabled: Boolean): Result<Unit> =
        request { accountId -> remote.updatePushNotifications(accountId, enabled) }

    override suspend fun legalLinks(): LegalLinks = withContext(ioDispatcher) {
        try {
            remote.legalLinks().toDomain()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            LegalLinks.EMPTY
        }
    }

    override suspend fun logOut() {
        sessionStore.close()
        tokenStore.clear()
    }

    override suspend fun deleteAccount(): Result<Unit> =
        request { accountId -> remote.deleteAccount(accountId) }
            .onSuccess { logOut() }

    private suspend fun <T> request(call: suspend (String) -> T): Result<T> =
        withContext(ioDispatcher) {
            val accountId = sessionStore.account.value?.id
                ?: return@withContext Result.failure(AuthException(AuthFailure.ACCOUNT_NOT_FOUND))
            try {
                Result.success(call(accountId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }
}
