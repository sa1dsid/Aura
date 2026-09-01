package com.aura.feature.onboarding

import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.presentation.auth.AuthEvent
import com.aura.feature.onboarding.presentation.auth.AuthField
import com.aura.feature.onboarding.presentation.auth.AuthToast
import com.aura.feature.onboarding.presentation.auth.AuthViewModel
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

private const val EMAIL = "smoke@auratest.dev"

private const val PASSWORD = "Password123"

class AuthFlowIntegrationTest : OnboardingTestCase() {

    @Test
    fun `signing up walks to the code screen and opens no session yet`() = onboarding { stack ->
        stack.server.next(
            Paths.REGISTER,
            code = 201,
            body = Server.verificationPending(email = EMAIL, expiresIn = 600),
        )
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_UP)
        awaitEvent(events)

        assertEquals(
            listOf(AuthEvent.OpenEmailVerification(EmailVerification.of(EMAIL, 600))),
            events,
        )
        assertEquals(10.minutes, verificationOf(events).codeLifetime)
        assertTrue(verificationOf(events).codeJustSent)
        assertEquals(
            """{"email":"$EMAIL","password":"$PASSWORD"}""",
            stack.server.bodyOf(Paths.REGISTER),
        )
        assertNull(stack.savedToken)
        assertNull(stack.sessionStore.account.value)
        assertEquals(0, stack.pushRefreshes)
    }

    @Test
    fun `signing in on an unconfirmed account reopens the code screen instead of an error`() =
        onboarding { stack ->
            stack.server.next(
                Paths.LOGIN,
                code = 403,
                body = Server.detail("Email verification required"),
            )
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.submitAs(AuthMode.SIGN_IN)
            awaitEvent(events)

            assertEquals(
                listOf(AuthEvent.OpenEmailVerification(EmailVerification(EMAIL))),
                events,
            )
            assertFalse(verificationOf(events).codeJustSent)
            assertNull(viewModel.uiState.value.invalidField)
            assertNull(stack.savedToken)
        }

    @Test
    fun `signing in on a settled account goes straight home`() = onboarding { stack ->
        stack.server.next(
            Paths.LOGIN,
            body = Server.token(user = Server.user(inviteDecision = Server.APPLIED)),
        )
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_IN)
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.OpenHome), events)
        assertEquals(Paths.LOGIN, stack.server.paths().last())
    }

    @Test
    fun `signing in still detours through the invite screen while the server says pending`() =
        onboarding { stack ->
            stack.server.next(
                Paths.LOGIN,
                body = Server.token(
                    isNewAccount = false,
                    user = Server.user(inviteDecision = Server.PENDING),
                ),
            )
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.submitAs(AuthMode.SIGN_IN)
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.OpenInvite), events)
        }

    @Test
    fun `a wrong password marks the password field and never opens a session`() =
        onboarding { stack ->
            stack.server.next(
                Paths.LOGIN,
                code = 401,
                body = Server.detail("Wrong email or password"),
            )
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.submitAs(AuthMode.SIGN_IN)
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.ShowToast(AuthToast.WRONG_CREDENTIALS)), events)
            assertEquals(AuthField.PASSWORD, viewModel.uiState.value.invalidField)
            assertFalse(viewModel.uiState.value.submitting)
            assertNull(stack.sessionStore.account.value)
            assertNull(stack.savedToken)
            assertEquals(0, stack.pushRefreshes)
        }

    @Test
    fun `a taken email marks the email field`() = onboarding { stack ->
        stack.server.next(Paths.REGISTER, code = 409, body = Server.detail("Account already exists"))
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_UP)
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.ACCOUNT_EXISTS)), events)
        assertEquals(AuthField.EMAIL, viewModel.uiState.value.invalidField)
    }

    @Test
    fun `a password the server calls too long is told apart from a short one`() =
        onboarding { stack ->
            stack.server.next(
                Paths.REGISTER,
                code = 422,
                body = Server.fieldError(field = "password", type = "string_too_long"),
            )
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.submitAs(AuthMode.SIGN_UP)
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.ShowToast(AuthToast.PASSWORD_TOO_LONG)), events)
            assertEquals(AuthField.PASSWORD, viewModel.uiState.value.invalidField)
        }

    @Test
    fun `an email the server rejects marks the email field`() = onboarding { stack ->
        stack.server.next(
            Paths.REGISTER,
            code = 422,
            body = Server.fieldError(field = "email", type = "value_error"),
        )
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_UP)
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.EMAIL_INVALID)), events)
        assertEquals(AuthField.EMAIL, viewModel.uiState.value.invalidField)
    }

    @Test
    fun `a short password never leaves the device`() = onboarding { stack ->
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_UP, password = "1234567")
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.PASSWORD_TOO_SHORT)), events)
        assertEquals(0, stack.server.hits(Paths.REGISTER))
    }

    @Test
    fun `a malformed email never leaves the device`() = onboarding { stack ->
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_IN, email = "said")
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.EMAIL_INVALID)), events)
        assertEquals(0, stack.server.hits(Paths.LOGIN))
    }

    @Test
    fun `google sends the id token and opens the invite screen for a fresh account`() =
        onboarding { stack ->
            stack.server.next(Paths.GOOGLE, body = Server.token(isNewAccount = true))
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.onGoogleClick(stack.activityContext)
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.OpenInvite), events)
            assertEquals(
                """{"id_token":"${OnboardingStack.GOOGLE_ID_TOKEN}"}""",
                stack.server.bodyOf(Paths.GOOGLE),
            )
        }

    @Test
    fun `a google token the server will not verify is not reported as a wrong password`() =
        onboarding { stack ->
            stack.server.next(
                Paths.GOOGLE,
                code = 401,
                body = Server.detail("Invalid Google token"),
            )
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.onGoogleClick(stack.activityContext)
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.ShowToast(AuthToast.GOOGLE_UNAVAILABLE)), events)
            assertNull(viewModel.uiState.value.invalidField)
        }

    @Test
    fun `a google outage on the server side reads as google unavailable`() = onboarding { stack ->
        stack.server.next(
            Paths.GOOGLE,
            code = 503,
            body = Server.detail("Google verification unavailable"),
        )
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onGoogleClick(stack.activityContext)
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.GOOGLE_UNAVAILABLE)), events)
    }

    @Test
    fun `closing the google sheet says nothing and unlocks the form`() = onboarding { stack ->
        coEvery { stack.googleSignInClient.idToken(any()) } throws
            AuthException(AuthFailure.GOOGLE_CANCELLED)
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onGoogleClick(stack.activityContext)
        awaitUntil("the form to unlock") { !viewModel.uiState.value.submitting }

        assertTrue(events.isEmpty())
        assertNull(viewModel.uiState.value.invalidField)
        assertEquals(0, stack.server.hits(Paths.GOOGLE))
    }

    @Test
    fun `google survives a connection dropped on the first attempt`() = onboarding { stack ->
        stack.server.nextDropsConnection(Paths.GOOGLE)
        stack.server.next(Paths.GOOGLE, body = Server.token(isNewAccount = true))
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onGoogleClick(stack.activityContext)
        awaitEvent(events)

        assertEquals(2, stack.server.hits(Paths.GOOGLE))
        assertEquals(listOf(AuthEvent.OpenInvite), events)
    }

    @Test
    fun `email sign in is not retried on a dropped connection`() = onboarding { stack ->
        stack.server.nextDropsConnection(Paths.LOGIN)
        stack.server.next(Paths.LOGIN, body = Server.token())
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_IN)
        awaitEvent(events)

        assertEquals(1, stack.server.hits(Paths.LOGIN))
        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.NO_CONNECTION)), events)
    }

    @Test
    fun `a password reset posts the trimmed email and confirms the link was sent`() =
        onboarding { stack ->
            stack.server.next(Paths.PASSWORD_RESET, body = """{"message":"Reset link sent"}""")
            val viewModel = stack.authViewModel()
            val events = stack.eventsOf(viewModel.events)

            viewModel.onEmailChange("  $EMAIL  ")
            viewModel.onForgotPasswordClick()
            awaitEvent(events)

            assertEquals(listOf(AuthEvent.ShowToast(AuthToast.RESET_LINK_SENT)), events)
            assertEquals("""{"email":"$EMAIL"}""", stack.server.bodyOf(Paths.PASSWORD_RESET))
        }

    @Test
    fun `a password reset without an email never leaves the device`() = onboarding { stack ->
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onForgotPasswordClick()
        awaitEvent(events)

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.EMAIL_REQUIRED)), events)
        assertEquals(AuthField.EMAIL, viewModel.uiState.value.invalidField)
        assertEquals(0, stack.server.hits(Paths.PASSWORD_RESET))
    }

    @Test
    fun `a second tap on submit does not fire a second request`() = onboarding { stack ->
        stack.server.next(Paths.LOGIN, code = 401, body = Server.detail("Wrong email or password"))
        stack.server.next(Paths.LOGIN, body = Server.token())
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.submitAs(AuthMode.SIGN_IN)
        viewModel.onSubmit()
        awaitEvent(events)

        assertEquals(1, stack.server.hits(Paths.LOGIN))
    }

    @Test
    fun `typing in a field clears only the mark that field carries`() = onboarding { stack ->
        stack.server.next(Paths.LOGIN, code = 401, body = Server.detail("Wrong email or password"))
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.submitAs(AuthMode.SIGN_IN)
        awaitEvent(events)

        viewModel.onEmailChange("other@auratest.dev")

        assertEquals(AuthField.PASSWORD, viewModel.uiState.value.invalidField)

        viewModel.onPasswordChange("OtherPassword1")

        assertNull(viewModel.uiState.value.invalidField)
    }

    @Test
    fun `switching between sign in and sign up clears the marked field`() = onboarding { stack ->
        stack.server.next(Paths.LOGIN, code = 401, body = Server.detail("Wrong email or password"))
        val viewModel = stack.authViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.submitAs(AuthMode.SIGN_IN)
        awaitEvent(events)

        viewModel.onModeChange(AuthMode.SIGN_UP)

        assertNull(viewModel.uiState.value.invalidField)
        assertEquals(EMAIL, viewModel.uiState.value.email)
        assertEquals(PASSWORD, viewModel.uiState.value.password)
    }

    private fun verificationOf(events: List<AuthEvent>): EmailVerification =
        events.filterIsInstance<AuthEvent.OpenEmailVerification>().single().verification

    private fun AuthViewModel.submitAs(
        mode: AuthMode,
        email: String = EMAIL,
        password: String = PASSWORD,
    ) {
        onModeChange(mode)
        onEmailChange(email)
        onPasswordChange(password)
        onSubmit()
    }
}
