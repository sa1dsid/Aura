package com.aura.feature.onboarding.domain.usecase

import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    private val repository = RecordingAuthRepository()

    @Test
    fun `sign in rejects malformed email before reaching the backend`() = runTest {
        val result = SignInUseCase(repository)("said", "12345678")

        assertEquals(AuthFailure.EMAIL_INVALID, result.failure())
        assertNull(repository.signInEmail)
    }

    @Test
    fun `sign in sends a short password to the backend`() = runTest {
        SignInUseCase(repository)("said@ioaura.app", "123")

        assertEquals("said@ioaura.app", repository.signInEmail)
        assertEquals("123", repository.signInPassword)
    }

    @Test
    fun `sign in sends an empty password to the backend`() = runTest {
        SignInUseCase(repository)("said@ioaura.app", "")

        assertEquals("said@ioaura.app", repository.signInEmail)
        assertEquals("", repository.signInPassword)
    }

    @Test
    fun `sign in surfaces wrong password as a backend failure`() = runTest {
        repository.failWith = AuthFailure.WRONG_PASSWORD

        val result = SignInUseCase(repository)("said@ioaura.app", "12345678")

        assertEquals(AuthFailure.WRONG_PASSWORD, result.failure())
    }

    @Test
    fun `sign up rejects a password shorter than eight characters`() = runTest {
        val result = SignUpUseCase(repository)("said@ioaura.app", "1234567")

        assertEquals(AuthFailure.PASSWORD_TOO_SHORT, result.failure())
        assertNull(repository.signUpEmail)
    }

    @Test
    fun `sign up accepts a password of exactly eight characters`() = runTest {
        SignUpUseCase(repository)("said@ioaura.app", "12345678")

        assertEquals("said@ioaura.app", repository.signUpEmail)
    }

    @Test
    fun `sign up reports the email problem before the password one`() = runTest {
        val result = SignUpUseCase(repository)("said", "123")

        assertEquals(AuthFailure.EMAIL_INVALID, result.failure())
    }

    @Test
    fun `credentials are trimmed before they leave the use case`() = runTest {
        SignUpUseCase(repository)("  said@ioaura.app  ", "12345678")

        assertEquals("said@ioaura.app", repository.signUpEmail)
    }

    @Test
    fun `password reset asks for an email when the field is blank`() = runTest {
        val result = RequestPasswordResetUseCase(repository)("   ")

        assertEquals(AuthFailure.EMAIL_REQUIRED, result.failure())
        assertNull(repository.resetEmail)
    }

    @Test
    fun `password reset does not check the email format`() = runTest {
        val result = RequestPasswordResetUseCase(repository)("said")

        assertTrue(result.isSuccess)
        assertEquals("said", repository.resetEmail)
    }

    private fun Result<*>.failure(): AuthFailure? =
        (exceptionOrNull() as? AuthException)?.failure

    private class RecordingAuthRepository : AuthRepository {
        var signInEmail: String? = null
        var signInPassword: String? = null
        var signUpEmail: String? = null
        var resetEmail: String? = null
        var failWith: AuthFailure? = null

        override suspend fun currentAccount(): Account? = null

        override suspend fun signIn(email: String, password: String): Result<AuthSession> {
            signInEmail = email
            signInPassword = password
            return session(accountCreated = false)
        }

        override suspend fun signUp(email: String, password: String): Result<AuthSession> {
            signUpEmail = email
            return session(accountCreated = true)
        }

        override suspend fun continueWithGoogle(): Result<AuthSession> =
            session(accountCreated = true)

        override suspend fun requestPasswordReset(email: String): Result<Unit> {
            resetEmail = email
            return failWith?.let { Result.failure(AuthException(it)) } ?: Result.success(Unit)
        }

        private fun session(accountCreated: Boolean): Result<AuthSession> =
            failWith?.let { Result.failure(AuthException(it)) }
                ?: Result.success(
                    AuthSession(
                        account = Account(
                            id = "acc_1",
                            email = "said@ioaura.app",
                            handle = "said",
                            inviteLink = "https://ioaura.app/i/SAID001",
                            authProvider = AuthProvider.EMAIL,
                        ),
                        accountCreated = accountCreated,
                    )
                )
    }
}
