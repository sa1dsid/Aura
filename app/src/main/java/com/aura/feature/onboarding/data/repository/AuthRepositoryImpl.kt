package com.aura.feature.onboarding.data.repository

import com.aura.core.api.toAuthFailure
import com.aura.core.auth.TokenStore
import com.aura.core.common.ApplicationScope
import com.aura.core.common.IoDispatcher
import com.aura.core.push.PushTokenRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.data.mapper.toDomain
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val UNAUTHORIZED = 401

private const val RETRY_DELAY_MILLIS = 400L

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: OnboardingRemoteDataSource,
    private val sessionStore: SessionStore,
    private val tokenStore: TokenStore,
    private val pushTokenRepository: PushTokenRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun currentAccount(): Account? = sessionStore.account.value

    override suspend fun restoreSession(): StartDestination = withContext(ioDispatcher) {
        if (tokenStore.token() == null) return@withContext StartDestination.AUTH

        try {
            val session = remote.restore().toDomain()
            openSession(session)
            if (session.invitePending) StartDestination.INVITE else StartDestination.HOME
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: HttpException) {
            if (error.code() != UNAUTHORIZED) return@withContext StartDestination.HOME
            tokenStore.clear()
            StartDestination.AUTH
        } catch (error: Throwable) {
            StartDestination.HOME
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthSession> =
        authenticate { remote.signIn(email, password).toDomain() }

    override suspend fun signUp(email: String, password: String): Result<AuthSession> =
        authenticate { remote.signUp(email, password).toDomain() }

    override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> =
        authenticate(googleSignIn = true) {
            retryOnceOnBrokenConnection { remote.signInWithGoogle(idToken).toDomain() }
        }

    override suspend fun requestPasswordReset(email: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                Result.success(remote.requestPasswordReset(email))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error.toAuthFailure())
            }
        }

    private fun openSession(session: AuthSession) {
        sessionStore.open(session.account)
        applicationScope.launch { pushTokenRepository.refresh() }
    }

    private suspend fun authenticate(
        googleSignIn: Boolean = false,
        request: suspend () -> AuthSession,
    ): Result<AuthSession> = withContext(ioDispatcher) {
        try {
            val session = request()
            openSession(session)
            Result.success(session)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error.toAuthFailure(googleSignIn))
        }
    }

    private suspend fun <T> retryOnceOnBrokenConnection(request: suspend () -> T): T = try {
        request()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (timeout: SocketTimeoutException) {
        throw timeout
    } catch (broken: IOException) {
        delay(RETRY_DELAY_MILLIS)
        request()
    }
}
