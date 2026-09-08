package com.aura.feature.onboarding.presentation.verification

import androidx.lifecycle.SavedStateHandle
import com.aura.core.common.TimeSource
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.EMAIL_CODE_RESEND_COOLDOWN
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.usecase.ConfirmEmailUseCase
import com.aura.feature.onboarding.domain.usecase.ResendEmailCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val EMAIL = "said@ioaura.app"

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    private val repository = ProgrammableVerificationRepository()

    @Before
    fun installMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun removeMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun `the code the server just sent blocks a resend for a minute`() = runTest {
        val viewModel = viewModel()

        viewModel.onScreenOpened(EmailVerification.of(EMAIL, 600))

        assertEquals(EMAIL_CODE_RESEND_COOLDOWN, viewModel.uiState.value.resendCooldown)
        assertFalse(viewModel.uiState.value.canResend)

        viewModel.onResendClick()
        assertEquals(0, repository.resends)

        advanceTimeBy(10.seconds)
        assertTrue(viewModel.uiState.value.resendCooldown < EMAIL_CODE_RESEND_COOLDOWN)
        assertFalse(viewModel.uiState.value.canResend)

        advanceTimeBy(EMAIL_CODE_RESEND_COOLDOWN)
        assertEquals(Duration.ZERO, viewModel.uiState.value.resendCooldown)
        assertTrue(viewModel.uiState.value.canResend)
    }

    @Test
    fun `a screen reopened from a refused sign in asks for a code at once`() = runTest {
        val viewModel = viewModel()

        viewModel.onScreenOpened(EmailVerification(EMAIL))

        assertEquals(Duration.ZERO, viewModel.uiState.value.resendCooldown)
        assertTrue(viewModel.uiState.value.canResend)

        viewModel.onResendClick()

        assertEquals(1, repository.resends)
        assertEquals(EMAIL_CODE_RESEND_COOLDOWN, viewModel.uiState.value.resendCooldown)
    }

    @Test
    fun `a resend the server throttles starts the minute as well`() = runTest {
        repository.resendFailure = EmailVerificationFailure.RESEND_TOO_SOON
        val viewModel = viewModel()
        viewModel.onScreenOpened(EmailVerification(EMAIL))

        viewModel.onResendClick()

        assertEquals(
            EmailVerificationFailure.RESEND_TOO_SOON,
            viewModel.uiState.value.failure,
        )
        assertEquals(EMAIL_CODE_RESEND_COOLDOWN, viewModel.uiState.value.resendCooldown)
    }

    @Test
    fun `a dead network leaves the resend button alone`() = runTest {
        repository.resendFailure = EmailVerificationFailure.NETWORK
        val viewModel = viewModel()
        viewModel.onScreenOpened(EmailVerification(EMAIL))

        viewModel.onResendClick()

        assertEquals(EmailVerificationFailure.NETWORK, viewModel.uiState.value.failure)
        assertEquals(Duration.ZERO, viewModel.uiState.value.resendCooldown)
        assertTrue(viewModel.uiState.value.canResend)
    }

    @Test
    fun `the minute restarts on every code the server sends`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenOpened(EmailVerification(EMAIL))

        viewModel.onResendClick()
        advanceTimeBy(30.seconds)
        val halfway = viewModel.uiState.value.resendCooldown

        advanceTimeBy(EMAIL_CODE_RESEND_COOLDOWN)
        viewModel.onResendClick()

        assertTrue(halfway < EMAIL_CODE_RESEND_COOLDOWN)
        assertEquals(2, repository.resends)
        assertEquals(EMAIL_CODE_RESEND_COOLDOWN, viewModel.uiState.value.resendCooldown)
    }

    @Test
    fun `the typed code and the running cooldown survive a recreated screen`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val verification = EmailVerification(EMAIL, codeJustSent = true)
        viewModel(savedStateHandle).apply {
            onScreenOpened(verification)
            onCodeChange("123456")
        }
        advanceTimeBy(20.seconds)

        val restored = viewModel(savedStateHandle)
        restored.onScreenOpened(verification)

        assertEquals("123456", restored.uiState.value.code)
        assertFalse(restored.uiState.value.canResend)
        assertTrue(restored.uiState.value.resendCooldown < EMAIL_CODE_RESEND_COOLDOWN)
    }

    private fun TestScope.viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = EmailVerificationViewModel(
        confirmEmail = ConfirmEmailUseCase(repository),
        resendEmailCode = ResendEmailCodeUseCase(repository),
        timeSource = TimeSource { testScheduler.currentTime },
        savedStateHandle = savedStateHandle,
    )

    private class ProgrammableVerificationRepository : AuthRepository {
        var resends = 0
            private set
        var resendFailure: EmailVerificationFailure? = null

        override suspend fun currentAccount(): Account? = null

        override suspend fun restoreSession(): StartDestination = StartDestination.AUTH

        override suspend fun signIn(email: String, password: String): Result<AuthSession> =
            throw UnsupportedOperationException()

        override suspend fun signUp(email: String, password: String): Result<EmailVerification> =
            throw UnsupportedOperationException()

        override suspend fun confirmEmail(email: String, code: String): Result<AuthSession> =
            throw UnsupportedOperationException()

        override suspend fun resendEmailCode(email: String): Result<Unit> {
            resends++
            return resendFailure
                ?.let { Result.failure(EmailVerificationException(it)) }
                ?: Result.success(Unit)
        }

        override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> =
            throw UnsupportedOperationException()

        override suspend fun requestPasswordReset(email: String): Result<Unit> =
            throw UnsupportedOperationException()
    }
}
