package com.aura.feature.onboarding.data.remote

import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface OnboardingRemoteDataSource {
    suspend fun bootstrap(): BootConfigDto

    suspend fun signIn(email: String, password: String): AuthSessionDto

    suspend fun signUp(email: String, password: String): AuthSessionDto

    suspend fun signInWithGoogle(): AuthSessionDto

    suspend fun requestPasswordReset(email: String)

    suspend fun flags(accountId: String): OnboardingFlagsDto

    suspend fun applyInviteCode(accountId: String, code: String)

    suspend fun skipInvite(accountId: String)

    suspend fun markBonusPopupShown(accountId: String)
}

@Singleton
class MockOnboardingRemoteDataSource @Inject constructor(
    private val backend: OnboardingBackend,
) : OnboardingRemoteDataSource {

    override suspend fun bootstrap(): BootConfigDto {
        delay(BOOTSTRAP_DELAY_MILLIS)
        return BootConfigDto(
            nodeCount = 4_210,
            hotCities = listOf("Amsterdam", "Frankfurt", "Singapore", "São Paulo"),
        )
    }

    override suspend fun signIn(email: String, password: String): AuthSessionDto {
        delay(NETWORK_DELAY_MILLIS)
        return AuthSessionDto(account = backend.signIn(email, password), accountCreated = false)
    }

    override suspend fun signUp(email: String, password: String): AuthSessionDto {
        delay(NETWORK_DELAY_MILLIS)
        return AuthSessionDto(account = backend.signUp(email, password), accountCreated = true)
    }

    override suspend fun signInWithGoogle(): AuthSessionDto {
        delay(NETWORK_DELAY_MILLIS)
        val (account, created) = backend.signInWithGoogle(GOOGLE_PROFILE_EMAIL)
        return AuthSessionDto(account = account, accountCreated = created)
    }

    override suspend fun requestPasswordReset(email: String) {
        delay(NETWORK_DELAY_MILLIS)
        backend.isRegistered(email)
    }

    override suspend fun flags(accountId: String): OnboardingFlagsDto {
        delay(FLAG_DELAY_MILLIS)
        return backend.flags(accountId)
    }

    override suspend fun applyInviteCode(accountId: String, code: String) {
        delay(NETWORK_DELAY_MILLIS)
        backend.applyInviteCode(accountId, code)
    }

    override suspend fun skipInvite(accountId: String) {
        delay(FLAG_DELAY_MILLIS)
        backend.skipInvite(accountId)
    }

    override suspend fun markBonusPopupShown(accountId: String) {
        delay(FLAG_DELAY_MILLIS)
        backend.markBonusPopupShown(accountId)
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 600L
        const val BOOTSTRAP_DELAY_MILLIS = 900L
        const val FLAG_DELAY_MILLIS = 200L
        const val GOOGLE_PROFILE_EMAIL = "said.ahmedov@gmail.com"
    }
}
