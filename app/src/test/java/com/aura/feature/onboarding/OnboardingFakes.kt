package com.aura.feature.onboarding

import com.aura.core.auth.TokenStore
import com.aura.core.push.PushTokenRepository
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal fun testAccount(id: String) = Account(
    id = id,
    email = "user$id@example.com",
    handle = "user$id",
    inviteLink = "https://ioaura.app/i/$id",
    authProvider = AuthProvider.EMAIL,
)

internal fun testAccountDto(
    id: String = "39",
    inviteCode: String = "SYREX482",
    authProvider: String = "EMAIL",
) = AccountDto(
    id = id,
    email = "said@ioaura.app",
    handle = "said",
    inviteCode = inviteCode,
    inviteLink = "https://ioaura.app/i/$inviteCode",
    authProvider = authProvider,
)

internal fun testSessionDto(
    accountCreated: Boolean = false,
    invitePending: Boolean = false,
) = AuthSessionDto(
    account = testAccountDto(),
    accountCreated = accountCreated,
    invitePending = invitePending,
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

internal class FakeOnboardingRemoteDataSource : OnboardingRemoteDataSource {

    var session = testSessionDto()
    var flags = OnboardingFlagsDto(
        inviteScreenPassed = false,
        bonusPopupShown = false,
        reservedBonusIon = 3_000L,
    )
    var bootConfig = BootConfigDto(nodeCount = 4_210, hotCities = emptyList())

    var restoreError: Throwable? = null
    var signInError: Throwable? = null
    var signUpError: Throwable? = null
    var resetError: Throwable? = null
    var applyError: Throwable? = null
    var skipError: Throwable? = null
    var flagsError: Throwable? = null
    var bootstrapError: Throwable? = null
    var markBonusPopupShownError: Throwable? = null

    val googleErrors = ArrayDeque<Throwable>()

    var restoreCalls = 0
    var googleCalls = 0
    var signInCalls = 0
    var signUpCalls = 0
    var markBonusPopupShownCalls = 0
    val sentIdTokens = mutableListOf<String>()
    val appliedCodes = mutableListOf<String>()
    val skippedAccounts = mutableListOf<String>()
    val resetEmails = mutableListOf<String>()

    override suspend fun bootstrap(): BootConfigDto {
        bootstrapError?.let { throw it }
        return bootConfig
    }

    override suspend fun signIn(email: String, password: String): AuthSessionDto {
        signInCalls++
        signInError?.let { throw it }
        return session
    }

    override suspend fun signUp(email: String, password: String): AuthSessionDto {
        signUpCalls++
        signUpError?.let { throw it }
        return session
    }

    override suspend fun signInWithGoogle(idToken: String): AuthSessionDto {
        googleCalls++
        sentIdTokens += idToken
        googleErrors.removeFirstOrNull()?.let { throw it }
        return session
    }

    override suspend fun restore(): AuthSessionDto {
        restoreCalls++
        restoreError?.let { throw it }
        return session
    }

    override suspend fun requestPasswordReset(email: String) {
        resetEmails += email
        resetError?.let { throw it }
    }

    override suspend fun flags(accountId: String): OnboardingFlagsDto {
        flagsError?.let { throw it }
        return flags
    }

    override suspend fun applyInviteCode(accountId: String, code: String) {
        appliedCodes += code
        applyError?.let { throw it }
    }

    override suspend fun skipInvite(accountId: String) {
        skippedAccounts += accountId
        skipError?.let { throw it }
    }

    override suspend fun markBonusPopupShown(accountId: String) {
        markBonusPopupShownCalls++
        markBonusPopupShownError?.let { throw it }
    }
}

internal class FakeTokenStore(initial: String? = null) {

    var token: String? = initial
        private set

    var expiresIn: Int? = null
        private set

    var clears = 0
        private set

    val store: TokenStore = mockk()

    init {
        coEvery { store.token() } answers { token }
        every { store.blockingToken() } answers { token }
        coEvery { store.save(any(), any()) } answers {
            token = firstArg()
            expiresIn = secondArg()
        }
        coEvery { store.clear() } answers {
            token = null
            clears++
        }
    }
}

internal class FakePushTokenRepository {

    var refreshes = 0
        private set

    val repository: PushTokenRepository = mockk()

    init {
        coEvery { repository.refresh() } answers { refreshes++ }
    }
}
