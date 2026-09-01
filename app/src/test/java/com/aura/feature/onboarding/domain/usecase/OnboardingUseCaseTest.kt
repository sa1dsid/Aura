package com.aura.feature.onboarding.domain.usecase

import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
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
    fun `a confirmation code is stripped to its digits before it is sent`() = runTest {
        ConfirmEmailUseCase(auth)("said@ioaura.app", " 482 913 ")

        assertEquals("said@ioaura.app", auth.confirmEmailAddress)
        assertEquals("482913", auth.confirmedCode)
    }

    @Test
    fun `a code shorter than six digits never leaves the device`() = runTest {
        val refused = listOf("48291", "4829a", "abcdef", "")

        for (code in refused) {
            val result = ConfirmEmailUseCase(auth)("said@ioaura.app", code)
            assertEquals(code, EmailVerificationFailure.CODE_REJECTED, result.codeFailure())
        }
        assertNull(auth.confirmedCode)
    }

    @Test
    fun `a code pasted with its tail is cut to six digits rather than refused`() = runTest {
        ConfirmEmailUseCase(auth)("said@ioaura.app", "4829135")

        assertEquals("482913", auth.confirmedCode)
    }

    @Test
    fun `a resend passes the address on untouched`() = runTest {
        ResendEmailCodeUseCase(auth)("said@ioaura.app")

        assertEquals("said@ioaura.app", auth.resendEmail)
    }

    @Test
    fun `a reset asks for an email before anything else`() = runTest {
        assertEquals(
            AuthFailure.EMAIL_REQUIRED,
            RequestPasswordResetUseCase(auth)("  ").failure(),
        )
        assertNull(auth.resetEmail)
    }

    @Test
    fun `a reset checks the email the same way the sign in does`() = runTest {
        assertEquals(
            AuthFailure.EMAIL_INVALID,
            RequestPasswordResetUseCase(auth)("said").failure(),
        )
        assertNull(auth.resetEmail)

        assertTrue(RequestPasswordResetUseCase(auth)("  said@ioaura.app ").isSuccess)
        assertEquals("said@ioaura.app", auth.resetEmail)
    }

    @Test
    fun `an invite code loses its spaces and goes up in case`() = runTest {
        ApplyInviteCodeUseCase(invites)("  sy rex\t482 \n")

        assertEquals("SYREX482", invites.appliedCode)
    }

    @Test
    fun `an invite code loses the punctuation typed into it`() = runTest {
        ApplyInviteCodeUseCase(invites)("syrex-482")

        assertEquals("SYREX482", invites.appliedCode)
    }

    @Test
    fun `an invite code longer than eight characters is cut down to size`() = runTest {
        ApplyInviteCodeUseCase(invites)("syrex482extra")

        assertEquals("SYREX482", invites.appliedCode)
    }

    @Test
    fun `an invite code of the wrong length never reaches the server`() = runTest {
        val result = ApplyInviteCodeUseCase(invites)("abc")

        assertEquals(InviteFailure.UNKNOWN_CODE, result.inviteFailure())
        assertNull(invites.appliedCode)
    }

    @Test
    fun `skipping asks the repository and nothing else`() = runTest {
        SkipInviteUseCase(invites)()

        assertEquals(1, invites.skips)
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
            override suspend fun bootstrap() = BootConfig(nodeCount = 7)
        }

        assertEquals(BootConfig(nodeCount = 7), BootstrapUseCase(boot)())
        assertEquals(StartDestination.HOME, ResolveStartDestinationUseCase(auth)())
    }

    private fun Result<*>.failure(): AuthFailure? =
        (exceptionOrNull() as? AuthException)?.failure

    private fun Result<*>.inviteFailure(): InviteFailure? =
        (exceptionOrNull() as? InviteException)?.failure

    private fun Result<*>.codeFailure(): EmailVerificationFailure? =
        (exceptionOrNull() as? EmailVerificationException)?.failure

    private class RecordingAuthRepository : AuthRepository {
        var signInEmail: String? = null
        var signInPassword: String? = null
        var signUpEmail: String? = null
        var signUpPassword: String? = null
        var confirmEmailAddress: String? = null
        var confirmedCode: String? = null
        var resendEmail: String? = null
        var resetEmail: String? = null

        override suspend fun currentAccount(): Account? = null

        override suspend fun restoreSession(): StartDestination = StartDestination.HOME

        override suspend fun signIn(email: String, password: String): Result<AuthSession> {
            signInEmail = email
            signInPassword = password
            return Result.success(session())
        }

        override suspend fun signUp(email: String, password: String): Result<EmailVerification> {
            signUpEmail = email
            signUpPassword = password
            return Result.success(EmailVerification(email))
        }

        override suspend fun confirmEmail(email: String, code: String): Result<AuthSession> {
            confirmEmailAddress = email
            confirmedCode = code
            return Result.success(session())
        }

        override suspend fun resendEmailCode(email: String): Result<Unit> {
            resendEmail = email
            return Result.success(Unit)
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
            invitePending = false,
        )
    }

    private class RecordingInviteRepository : InviteRepository {
        var attribution: InviteAttribution = InviteAttribution.None
        var appliedCode: String? = null
        var skips = 0
        var rememberedCode: String? = null

        override suspend fun pendingAttribution(): InviteAttribution = attribution

        override suspend fun rememberDeepLinkCode(code: String) {
            rememberedCode = code
        }

        override suspend fun applyCode(code: String): Result<Unit> {
            appliedCode = code
            return Result.success(Unit)
        }

        override suspend fun skipInvite(): Result<Unit> {
            skips++
            return Result.success(Unit)
        }
    }
}
