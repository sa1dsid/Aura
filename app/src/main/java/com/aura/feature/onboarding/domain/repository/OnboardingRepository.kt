package com.aura.feature.onboarding.domain.repository

import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.model.StartDestination

interface AuthRepository {
    suspend fun currentAccount(): Account?

    suspend fun restoreSession(): StartDestination

    suspend fun signIn(email: String, password: String): Result<AuthSession>

    suspend fun signUp(email: String, password: String): Result<AuthSession>

    suspend fun continueWithGoogle(idToken: String): Result<AuthSession>

    suspend fun requestPasswordReset(email: String): Result<Unit>
}

interface InviteRepository {
    suspend fun pendingAttribution(): InviteAttribution

    suspend fun rememberDeepLinkCode(code: String)

    suspend fun applyCode(accountId: String, code: String): Result<Unit>

    suspend fun skipInvite(accountId: String): Result<Unit>
}

interface OnboardingFlagsRepository {
    suspend fun flags(accountId: String): OnboardingFlags

    suspend fun markBonusPopupShown(accountId: String)
}

interface BootRepository {
    suspend fun bootstrap(): BootConfig
}
