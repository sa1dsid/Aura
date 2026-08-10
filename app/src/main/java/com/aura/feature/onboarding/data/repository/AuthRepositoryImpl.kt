package com.aura.feature.onboarding.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.data.mapper.toDomain
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    private val sessionStore: SessionStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun currentAccount(): Account? = sessionStore.account.value

    override suspend fun signIn(email: String, password: String): Result<AuthSession> =
        authenticate { remote.signIn(email, password).toDomain() }

    override suspend fun signUp(email: String, password: String): Result<AuthSession> =
        authenticate { remote.signUp(email, password).toDomain() }

    override suspend fun continueWithGoogle(): Result<AuthSession> =
        authenticate { remote.signInWithGoogle().toDomain() }

    override suspend fun requestPasswordReset(email: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                Result.success(remote.requestPasswordReset(email))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error.asAuthFailure())
            }
        }

    private suspend fun authenticate(request: suspend () -> AuthSession): Result<AuthSession> =
        withContext(ioDispatcher) {
            try {
                val session = request()
                sessionStore.open(session.account)
                Result.success(session)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error.asAuthFailure())
            }
        }

    private fun Throwable.asAuthFailure(): Throwable =
        this as? AuthException ?: AuthException(AuthFailure.NETWORK)
}
