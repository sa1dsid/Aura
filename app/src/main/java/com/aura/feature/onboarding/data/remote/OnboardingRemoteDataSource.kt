package com.aura.feature.onboarding.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.EmailCredentialsDto
import com.aura.core.api.dto.GoogleSignInRequestDto
import com.aura.core.api.dto.InviteApplyDto
import com.aura.core.api.dto.PasswordResetRequestDto
import com.aura.core.api.dto.TokenResponseDto
import com.aura.core.api.dto.UserDto
import com.aura.core.auth.TokenStore
import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val INVITE_DECISION_PENDING = "pending"

private const val AUTH_METHOD_GOOGLE = "google"

interface OnboardingRemoteDataSource {
    suspend fun bootstrap(): BootConfigDto

    suspend fun signIn(email: String, password: String): AuthSessionDto

    suspend fun signUp(email: String, password: String): AuthSessionDto

    suspend fun signInWithGoogle(idToken: String): AuthSessionDto

    suspend fun restore(): AuthSessionDto

    suspend fun requestPasswordReset(email: String)

    suspend fun flags(accountId: String): OnboardingFlagsDto

    suspend fun applyInviteCode(accountId: String, code: String)

    suspend fun skipInvite(accountId: String)

    suspend fun markBonusPopupShown(accountId: String)
}

@Singleton
class ApiOnboardingRemoteDataSource @Inject constructor(
    private val api: AuraApi,
    private val tokenStore: TokenStore,
) : OnboardingRemoteDataSource {

    override suspend fun bootstrap(): BootConfigDto =
        BootConfigDto(nodeCount = null, hotCities = emptyList())

    override suspend fun signIn(email: String, password: String): AuthSessionDto =
        api.login(EmailCredentialsDto(email = email, password = password)).toSession()

    override suspend fun signUp(email: String, password: String): AuthSessionDto =
        api.register(EmailCredentialsDto(email = email, password = password)).toSession()

    override suspend fun signInWithGoogle(idToken: String): AuthSessionDto =
        api.googleSignIn(GoogleSignInRequestDto(idToken = idToken)).toSession()

    override suspend fun restore(): AuthSessionDto {
        val user = api.currentUser()
        return AuthSessionDto(
            account = user.toAccount(inviteLink = personalUrl()),
            accountCreated = false,
            invitePending = user.inviteDecision == INVITE_DECISION_PENDING,
        )
    }

    override suspend fun requestPasswordReset(email: String) {
        api.requestPasswordReset(PasswordResetRequestDto(email = email))
    }

    override suspend fun flags(accountId: String): OnboardingFlagsDto {
        val user = api.currentUser()
        return OnboardingFlagsDto(
            inviteScreenPassed = user.inviteDecision != INVITE_DECISION_PENDING,
            bonusPopupShown = user.giftPopupSeen,
            reservedBonusIon = user.bonusReservedIon,
        )
    }

    override suspend fun applyInviteCode(accountId: String, code: String) {
        api.applyInvite(InviteApplyDto(code = code))
    }

    override suspend fun skipInvite(accountId: String) {
        api.skipInvite()
    }

    override suspend fun markBonusPopupShown(accountId: String) {
        api.markGiftPopupSeen()
    }

    private suspend fun TokenResponseDto.toSession(): AuthSessionDto {
        tokenStore.save(token = accessToken, expiresInSeconds = expiresIn)

        return AuthSessionDto(
            account = user.toAccount(inviteLink = personalUrl()),
            accountCreated = isNewAccount,
            invitePending = user.inviteDecision == INVITE_DECISION_PENDING,
        )
    }

    private suspend fun personalUrl(): String? = try {
        api.inviteState().personalUrl
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        null
    }

    private fun UserDto.toAccount(inviteLink: String?) = AccountDto(
        id = id.toString(),
        email = email,
        handle = displayName,
        inviteCode = promoCode,
        inviteLink = inviteLink ?: fallbackInviteLink(promoCode),
        authProvider = if (authMethods.contains(AUTH_METHOD_GOOGLE)) "GOOGLE" else "EMAIL",
    )

    private fun fallbackInviteLink(code: String) = "https://ioaura.app/i/$code"
}
