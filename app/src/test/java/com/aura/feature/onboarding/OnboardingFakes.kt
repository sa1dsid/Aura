package com.aura.feature.onboarding

import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository

internal fun testAccount(id: String) = Account(
    id = id,
    email = "user$id@example.com",
    handle = "user$id",
    inviteLink = "https://ioaura.app/i/$id",
    authProvider = AuthProvider.EMAIL,
)

internal class FakeAuthRepository(var account: Account? = testAccount("1")) : AuthRepository {

    override suspend fun currentAccount(): Account? = account

    override suspend fun restoreSession(): StartDestination = throw UnsupportedOperationException()

    override suspend fun signIn(email: String, password: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun signUp(email: String, password: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun requestPasswordReset(email: String): Result<Unit> =
        throw UnsupportedOperationException()
}

internal class FakeOnboardingFlagsRepository : OnboardingFlagsRepository {
    var flags = OnboardingFlags.FALLBACK
    val markedAccounts = mutableListOf<String>()

    override suspend fun flags(accountId: String): OnboardingFlags = flags

    override suspend fun markBonusPopupShown(accountId: String) {
        markedAccounts += accountId
    }
}

internal class FakeInviteRepository : InviteRepository {
    var attribution: InviteAttribution = InviteAttribution.None
    var result: Result<Unit> = Result.success(Unit)
    val skippedAccounts = mutableListOf<String>()
    val appliedCodes = mutableListOf<String>()

    override suspend fun pendingAttribution(): InviteAttribution = attribution

    override suspend fun rememberDeepLinkCode(code: String) = Unit

    override suspend fun applyCode(accountId: String, code: String): Result<Unit> {
        appliedCodes += code
        return result
    }

    override suspend fun skipInvite(accountId: String): Result<Unit> {
        skippedAccounts += accountId
        return result
    }
}
