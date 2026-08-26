package com.aura.feature.onboarding.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.EmailCredentialsDto
import com.aura.core.api.dto.GoogleSignInRequestDto
import com.aura.core.api.dto.InviteApplyDto
import com.aura.core.api.dto.PasswordResetRequestDto
import com.aura.core.api.dto.TokenResponseDto
import com.aura.core.api.dto.UserDto
import com.aura.core.auth.TokenStore
import com.aura.core.common.runCatchingCancellable
import com.aura.core.config.AppConfigRepository
import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import javax.inject.Inject
import javax.inject.Singleton

private const val INVITE_DECISION_PENDING = "pending"

private const val AUTH_METHOD_GOOGLE = "google"

private const val INVITE_LINK_PREFIX = "https://ioaura.app/i/"

interface OnboardingRemoteDataSource {
    suspend fun bootstrap(): BootConfigDto

    suspend fun signIn(email: String, password: String): AuthSessionDto

    suspend fun signUp(email: String, password: String): AuthSessionDto

    suspend fun signInWithGoogle(idToken: String): AuthSessionDto

    suspend fun restore(): AuthSessionDto

    suspend fun requestPasswordReset(email: String)

    suspend fun flags(): OnboardingFlagsDto

    suspend fun applyInviteCode(code: String)

    suspend fun skipInvite()

    suspend fun markBonusPopupShown()
}

@Singleton
class ApiOnboardingRemoteDataSource @Inject constructor(
    private val api: AuraApi,
    private val tokenStore: TokenStore,
    private val appConfigRepository: AppConfigRepository,
) : OnboardingRemoteDataSource {

    override suspend fun bootstrap(): BootConfigDto {
        appConfigRepository.refresh()
        val nodesOnline = runCatchingCancellable { api.mesh().nodesOnline }.getOrNull()

        return BootConfigDto(nodeCount = nodesOnline?.takeIf { it > 0 })
    }

    override suspend fun signIn(email: String, password: String): AuthSessionDto =
        api.login(EmailCredentialsDto(email = email, password = password)).toSession()

    override suspend fun signUp(email: String, password: String): AuthSessionDto =
        api.register(EmailCredentialsDto(email = email, password = password)).toSession()

    override suspend fun signInWithGoogle(idToken: String): AuthSessionDto =
        api.googleSignIn(GoogleSignInRequestDto(idToken = idToken)).toSession()

    override suspend fun restore(): AuthSessionDto = api.currentUser().toSession()

    override suspend fun requestPasswordReset(email: String) {
        api.requestPasswordReset(PasswordResetRequestDto(email = email))
    }

    override suspend fun flags(): OnboardingFlagsDto {
        val user = api.currentUser()
        return OnboardingFlagsDto(
            bonusPopupShown = user.giftPopupSeen,
            reservedBonusIon = user.bonusReservedIon,
        )
    }

    override suspend fun applyInviteCode(code: String) {
        api.applyInvite(InviteApplyDto(code = code))
    }

    override suspend fun skipInvite() {
        api.skipInvite()
    }

    override suspend fun markBonusPopupShown() {
        api.markGiftPopupSeen()
    }

    private suspend fun TokenResponseDto.toSession(): AuthSessionDto {
        tokenStore.save(token = accessToken, expiresInSeconds = expiresIn)
        return user.toSession()
    }

    private fun UserDto.toSession() = AuthSessionDto(
        account = toAccount(),
        invitePending = inviteDecision == INVITE_DECISION_PENDING,
    )

    private fun UserDto.toAccount() = AccountDto(
        id = id.toString(),
        email = email,
        handle = displayName,
        inviteLink = INVITE_LINK_PREFIX + promoCode,
        authProvider = if (authMethods.contains(AUTH_METHOD_GOOGLE)) "GOOGLE" else "EMAIL",
    )
}
