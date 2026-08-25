package com.aura.feature.onboarding.domain.usecase

import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ACCOUNT_ID = "39"

class OnboardingUseCaseTest {

    private val auth = RecordingAuthRepository()

    private val invites = RecordingInviteRepository()

    @Test
    fun `the shapes the email check lets through`() = runTest {
        val accepted = listOf(
            "a@b.dev",
            "said@ioaura.app",
            "said@sub.domain.dev",
            "said.ahmedov@ioaura.app",
            "said+aura@ioaura.app",
            "SAID@IOAURA.APP",
        )

        for (email in accepted) {
            SignInUseCase(auth)(email, "Password123")
            assertEquals(email, email.trim(), auth.signInEmail)
        }
    }

    @Test
    fun `the shapes the email check turns away`() = runTest {
        val refused = listOf(
            "said",
            "said@",
            "@ioaura.app",
            "said@ioaura",
            "said@@ioaura.app",
            "said ahmedov@ioaura.app",
            "said@io aura.app",
            "said@.app",
            "",
            "   ",
        )

        for (email in refused) {
            val result = SignInUseCase(auth)(email, "Password123")
            assertEquals(email, AuthFailure.EMAIL_INVALID, result.failure())
        }
        assertNull(auth.signInEmail)
    }

    @Test
    fun `an email is trimmed but never lower cased`() = runTest {
        SignUpUseCase(auth)("  SAID@ioaura.app  ", "Password123")

        assertEquals("SAID@ioaura.app", auth.signUpEmail)
    }

    @Test
    fun `a password is passed on exactly as typed`() = runTest {
        SignUpUseCase(auth)("said@ioaura.app", "  Password123  ")

        assertEquals("  Password123  ", auth.signUpPassword)
    }

    @Test
    fun `the password length check belongs to sign up alone`() = runTest {
        assertEquals(
            AuthFailure.PASSWORD_TOO_SHORT,
            SignUpUseCase(auth)("said@ioaura.app", "1234567").failure(),
        )

        SignInUseCase(auth)("said@ioaura.app", "1234567")

        assertEquals("1234567", auth.signInPassword)
    }

    @Test
    fun `a reset asks for an email but not for a valid one`() = runTest {
        assertEquals(
            AuthFailure.EMAIL_REQUIRED,
            RequestPasswordResetUseCase(auth)("  ").failure(),
        )

        assertTrue(RequestPasswordResetUseCase(auth)("said").isSuccess)
        assertEquals("said", auth.resetEmail)
    }

    @Test
    fun `an invite code loses its spaces and goes up in case`() = runTest {
        ApplyInviteCodeUseCase(invites)(ACCOUNT_ID, "  sy rex\t482 \n")

        assertEquals("SYREX482", invites.appliedCode)
    }

    @Test
    fun `an invite code keeps the punctuation typed into it`() = runTest {
        ApplyInviteCodeUseCase(invites)(ACCOUNT_ID, "syrex-482")

        assertEquals("SYREX-482", invites.appliedCode)
    }

    @Test
    fun `an invite code of the wrong length still reaches the server`() = runTest {
        ApplyInviteCodeUseCase(invites)(ACCOUNT_ID, "abc")

        assertEquals("ABC", invites.appliedCode)
    }

    @Test
    fun `skipping names the account and nothing else`() = runTest {
        SkipInviteUseCase(invites)(ACCOUNT_ID)

        assertEquals(ACCOUNT_ID, invites.skippedAccount)
    }

    @Test
    fun `the pending attribution is read straight from the repository`() = runTest {
        invites.attribution = InviteAttribution.FromLink("SYREX482")

        assertEquals(
            InviteAttribution.FromLink("SYREX482"),
            ObserveInviteAttributionUseCase(invites)(),
        )
    }

    @Test
    fun `booting and resolving are plain pass through`() = runTest {
        val boot = object : BootRepository {
            override suspend fun bootstrap() = BootConfig(7, listOf("Tallinn"))
        }

        assertEquals(BootConfig(7, listOf("Tallinn")), BootstrapUseCase(boot)())
        assertEquals(StartDestination.HOME, ResolveStartDestinationUseCase(auth)())
    }

    private fun Result<*>.failure(): AuthFailure? =
        (exceptionOrNull() as? AuthException)?.failure

    private class RecordingAuthRepository : AuthRepository {
        var signInEmail: String? = null
        var signInPassword: String? = null
        var signUpEmail: String? = null
        var signUpPassword: String? = null
        var resetEmail: String? = null

        override suspend fun currentAccount(): Account? = null

        override suspend fun restoreSession(): StartDestination = StartDestination.HOME

        override suspend fun signIn(email: String, password: String): Result<AuthSession> {
            signInEmail = email
            signInPassword = password
            return Result.success(session())
        }

        override suspend fun signUp(email: String, password: String): Result<AuthSession> {
            signUpEmail = email
            signUpPassword = password
            return Result.success(session())
        }

        override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> =
            Result.success(session())

        override suspend fun requestPasswordReset(email: String): Result<Unit> {
            resetEmail = email
            return Result.success(Unit)
        }

        private fun session() = AuthSession(
            account = Account(
                id = ACCOUNT_ID,
                email = "said@ioaura.app",
                handle = "said",
                inviteLink = "https://ioaura.app/i/SYREX482",
                authProvider = AuthProvider.EMAIL,
            ),
            accountCreated = false,
            invitePending = false,
        )
    }

    private class RecordingInviteRepository : InviteRepository {
        var attribution: InviteAttribution = InviteAttribution.None
        var appliedCode: String? = null
        var skippedAccount: String? = null
        var rememberedCode: String? = null

        override suspend fun pendingAttribution(): InviteAttribution = attribution

        override suspend fun rememberDeepLinkCode(code: String) {
            rememberedCode = code
        }

        override suspend fun applyCode(accountId: String, code: String): Result<Unit> {
            appliedCode = code
            return Result.success(Unit)
        }

        override suspend fun skipInvite(accountId: String): Result<Unit> {
            skippedAccount = accountId
            return Result.success(Unit)
        }
    }
}
