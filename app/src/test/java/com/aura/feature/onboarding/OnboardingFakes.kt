package com.aura.feature.onboarding

import com.aura.core.auth.TokenStore
import com.aura.core.push.PushTokenRepository
import com.aura.feature.onboarding.data.attribution.InviteAttributionState
import com.aura.feature.onboarding.data.attribution.InviteAttributionStorage
import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.EmailVerificationDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.EmailVerification
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
    inviteLink: String = "https://ioaura.app/i/SYREX482",
    authProvider: String = "EMAIL",
) = AccountDto(
    id = id,
    email = "said@ioaura.app",
    handle = "said",
    inviteLink = inviteLink,
    authProvider = authProvider,
)

internal fun testSessionDto(invitePending: Boolean = false) = AuthSessionDto(
    account = testAccountDto(),
    invitePending = invitePending,
)

internal fun testPendingDto(
    email: String = "said@ioaura.app",
    expiresInSeconds: Int = 600,
) = EmailVerificationDto(email = email, expiresInSeconds = expiresInSeconds)

internal class FakeAuthRepository(var account: Account? = testAccount("1")) : AuthRepository {

    override suspend fun currentAccount(): Account? = account

    override suspend fun restoreSession(): StartDestination = throw UnsupportedOperationException()

    override suspend fun signIn(email: String, password: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun signUp(email: String, password: String): Result<EmailVerification> =
        throw UnsupportedOperationException()

    override suspend fun confirmEmail(email: String, code: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun resendEmailCode(email: String): Result<Unit> =
        throw UnsupportedOperationException()

    override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> =
        throw UnsupportedOperationException()

    override suspend fun requestPasswordReset(email: String): Result<Unit> =
        throw UnsupportedOperationException()
}

internal class FakeOnboardingFlagsRepository : OnboardingFlagsRepository {
    var flags = OnboardingFlags.FALLBACK
    var bonusPopupMarks = 0
        private set

    override suspend fun flags(): OnboardingFlags = flags

    override suspend fun markBonusPopupShown() {
        bonusPopupMarks++
    }
}

internal class FakeInviteRepository : InviteRepository {
    var attribution: InviteAttribution = InviteAttribution.None
    var result: Result<Unit> = Result.success(Unit)
    var skips = 0
        private set
    val appliedCodes = mutableListOf<String>()

    override suspend fun pendingAttribution(): InviteAttribution = attribution

    override suspend fun rememberDeepLinkCode(code: String) = Unit

    override suspend fun applyCode(code: String): Result<Unit> {
        appliedCodes += code
        return result
    }

    override suspend fun skipInvite(): Result<Unit> {
        skips++
        return result
    }
}

internal class FakeInviteAttributionStorage(
    private var state: InviteAttributionState = InviteAttributionState(),
) : InviteAttributionStorage {

    var writes = 0
        private set

    override suspend fun read(): InviteAttributionState = state

    override suspend fun write(state: InviteAttributionState) {
        this.state = state
        writes++
    }
}

internal class FakeOnboardingRemoteDataSource : OnboardingRemoteDataSource {

    var session = testSessionDto()
    var pending = testPendingDto()
    var flags = OnboardingFlagsDto(bonusPopupShown = false, reservedBonusIon = 3_000L)
    var bootConfig = BootConfigDto(nodeCount = 4_210)

    var restoreError: Throwable? = null
    var signInError: Throwable? = null
    var signUpError: Throwable? = null
    var confirmError: Throwable? = null
    var resendError: Throwable? = null
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
    var skipCalls = 0
    var markBonusPopupShownCalls = 0
    val sentIdTokens = mutableListOf<String>()
    val appliedCodes = mutableListOf<String>()
    val confirmedCodes = mutableListOf<String>()
    val resendEmails = mutableListOf<String>()
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

    override suspend fun signUp(email: String, password: String): EmailVerificationDto {
        signUpCalls++
        signUpError?.let { throw it }
        return pending
    }

    override suspend fun confirmEmail(email: String, code: String): AuthSessionDto {
        confirmedCodes += code
        confirmError?.let { throw it }
        return session
    }

    override suspend fun resendEmailCode(email: String) {
        resendEmails += email
        resendError?.let { throw it }
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

    override suspend fun flags(): OnboardingFlagsDto {
        flagsError?.let { throw it }
        return flags
    }

    override suspend fun applyInviteCode(code: String) {
        appliedCodes += code
        applyError?.let { throw it }
    }

    override suspend fun skipInvite() {
        skipCalls++
        skipError?.let { throw it }
    }

    override suspend fun markBonusPopupShown() {
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
