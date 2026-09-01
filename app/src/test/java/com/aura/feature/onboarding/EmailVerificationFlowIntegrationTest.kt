package com.aura.feature.onboarding

import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.presentation.verification.EmailVerificationEvent
import com.aura.feature.onboarding.presentation.verification.EmailVerificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EMAIL = "smoke@auratest.dev"

private const val CODE = "482913"

class EmailVerificationFlowIntegrationTest : OnboardingTestCase() {

    @Test
    fun `a confirmed code opens the session and walks to the invite screen`() =
        onboarding { stack ->
            stack.server.next(
                Paths.EMAIL_CONFIRM,
                body = Server.token(isNewAccount = true, expiresIn = 604_800),
            )
            val viewModel = stack.openedViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.onCodeChange(CODE)
            viewModel.onConfirmClick()
            awaitEvent(events)

            assertEquals(listOf(EmailVerificationEvent.Confirmed(invitePending = true)), events)
            assertEquals(
                """{"email":"$EMAIL","code":"$CODE"}""",
                stack.server.bodyOf(Paths.EMAIL_CONFIRM),
            )
            assertEquals("header.payload.signature", stack.savedToken)
            assertEquals(604_800, stack.storedExpiresIn)
            assertEquals("smoke", stack.sessionStore.account.value?.handle)
            assertEquals(1, stack.pushRefreshes)
        }

    @Test
    fun `a settled account skips the invite screen`() = onboarding { stack ->
        stack.server.next(
            Paths.EMAIL_CONFIRM,
            body = Server.token(user = Server.user(inviteDecision = Server.APPLIED)),
        )
        val viewModel = stack.openedViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onCodeChange(CODE)
        viewModel.onConfirmClick()
        awaitEvent(events)

        assertEquals(listOf(EmailVerificationEvent.Confirmed(invitePending = false)), events)
    }

    @Test
    fun `a rejected code keeps the screen and opens no session`() = onboarding { stack ->
        stack.server.next(
            Paths.EMAIL_CONFIRM,
            code = 400,
            body = Server.detail("Invalid or expired confirmation code"),
        )
        val viewModel = stack.openedViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onCodeChange(CODE)
        viewModel.onConfirmClick()
        awaitUntil("the rejection") { viewModel.uiState.value.failure != null }

        assertEquals(EmailVerificationFailure.CODE_REJECTED, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(emptyList<EmailVerificationEvent>(), events)
        assertNull(stack.savedToken)
        assertNull(stack.sessionStore.account.value)
    }

    @Test
    fun `a short code never leaves the device`() = onboarding { stack ->
        val viewModel = stack.openedViewModel()

        viewModel.onCodeChange("4829")
        viewModel.onConfirmClick()

        assertEquals(0, stack.server.hits(Paths.EMAIL_CONFIRM))
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `letters pasted with the code are dropped and the digits kept`() = onboarding { stack ->
        val viewModel = stack.openedViewModel()

        viewModel.onPaste("Your code is 482 913")

        assertEquals(CODE, viewModel.uiState.value.code)
        assertEquals(0, stack.server.hits(Paths.EMAIL_CONFIRM))
    }

    @Test
    fun `resending asks the server again and clears the typed code`() = onboarding { stack ->
        stack.server.next(Paths.EMAIL_RESEND, body = Server.message())
        val viewModel = stack.openedViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onCodeChange(CODE)
        viewModel.onResendClick()
        awaitEvent(events)

        assertEquals(listOf(EmailVerificationEvent.CodeSent), events)
        assertEquals("""{"email":"$EMAIL"}""", stack.server.bodyOf(Paths.EMAIL_RESEND))
        assertTrue(viewModel.uiState.value.code.isEmpty())
    }

    @Test
    fun `resending too early is told apart from a dead network`() = onboarding { stack ->
        stack.server.next(
            Paths.EMAIL_RESEND,
            code = 429,
            body = Server.detail("Please wait before requesting another code"),
        )
        val viewModel = stack.openedViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onResendClick()
        awaitUntil("the cooldown") { viewModel.uiState.value.failure != null }

        assertEquals(EmailVerificationFailure.RESEND_TOO_SOON, viewModel.uiState.value.failure)
        assertEquals(emptyList<EmailVerificationEvent>(), events)
    }

    @Test
    fun `the screen keeps the address the server confirmed the code was sent to`() =
        onboarding { stack ->
            val viewModel = stack.emailVerificationViewModel()

            viewModel.onScreenOpened(EmailVerification(EMAIL))
            viewModel.onCodeChange(CODE)
            viewModel.onScreenOpened(EmailVerification(EMAIL))

            assertEquals(CODE, viewModel.uiState.value.code)
            assertEquals(EMAIL, viewModel.uiState.value.verification.email)
        }
}

private fun OnboardingStack.openedViewModel(): EmailVerificationViewModel =
    emailVerificationViewModel().apply { onScreenOpened(EmailVerification(EMAIL)) }
