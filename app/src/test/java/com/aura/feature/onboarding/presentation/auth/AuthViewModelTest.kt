package com.aura.feature.onboarding.presentation.auth

import androidx.lifecycle.ViewModelStore
import com.aura.core.auth.GoogleSignInClient
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.usecase.ContinueWithGoogleUseCase
import com.aura.feature.onboarding.domain.usecase.RequestPasswordResetUseCase
import com.aura.feature.onboarding.domain.usecase.SignInUseCase
import com.aura.feature.onboarding.domain.usecase.SignUpUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val EMAIL = "said@ioaura.app"

private const val PASSWORD = "Password123"

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val repository = ProgrammableAuthRepository()

    private val googleSignInClient: GoogleSignInClient = mockk()

    private val viewModelStore = ViewModelStore()

    private var created = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { googleSignInClient.warmUp() } answers { }
        coEvery { googleSignInClient.idToken(any()) } returns "google.id.token"
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `the google sheet is warmed up as soon as the screen exists`() = runTest {
        viewModel()

        coVerify(exactly = 1) { googleSignInClient.warmUp() }
    }

    @Test
    fun `every way the backend can refuse lands on a field and a toast`() = runTest {
        val expectations = mapOf(
            AuthFailure.EMAIL_REQUIRED to (AuthField.EMAIL to AuthToast.EMAIL_REQUIRED),
            AuthFailure.EMAIL_INVALID to (AuthField.EMAIL to AuthToast.EMAIL_INVALID),
            AuthFailure.EMAIL_ALREADY_REGISTERED to (AuthField.EMAIL to AuthToast.ACCOUNT_EXISTS),
            AuthFailure.ACCOUNT_NOT_FOUND to (AuthField.EMAIL to AuthToast.NO_ACCOUNT),
            AuthFailure.PASSWORD_TOO_SHORT to
                (AuthField.PASSWORD to AuthToast.PASSWORD_TOO_SHORT),
            AuthFailure.PASSWORD_TOO_LONG to (AuthField.PASSWORD to AuthToast.PASSWORD_TOO_LONG),
            AuthFailure.WRONG_PASSWORD to (AuthField.PASSWORD to AuthToast.WRONG_CREDENTIALS),
            AuthFailure.NETWORK to (null to AuthToast.NO_CONNECTION),
            AuthFailure.GOOGLE_UNAVAILABLE to (null to AuthToast.GOOGLE_UNAVAILABLE),
            AuthFailure.GOOGLE_CANCELLED to (null to null),
        )

        assertEquals(AuthFailure.entries.size, expectations.size)

        for ((failure, expected) in expectations) {
            val (field, toast) = expected
            repository.failWith = failure
            val viewModel = viewModel()
            val events = events(viewModel)

            viewModel.fill()
            viewModel.onSubmit()

            assertEquals(failure.name, field, viewModel.uiState.value.invalidField)
            assertEquals(failure.name, listOfNotNull(toast?.let(AuthEvent::ShowToast)), events)
            assertFalse(failure.name, viewModel.uiState.value.submitting)
        }
    }

    @Test
    fun `a failure that is not ours reads as a dead network`() = runTest {
        repository.failWithRaw = IllegalStateException("boom")
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.fill()
        viewModel.onSubmit()

        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.NO_CONNECTION)), events)
        assertNull(viewModel.uiState.value.invalidField)
    }

    @Test
    fun `sign in and sign up reach different endpoints`() = runTest {
        val viewModel = viewModel()

        viewModel.fill()
        viewModel.onSubmit()
        viewModel.onModeChange(AuthMode.SIGN_UP)
        viewModel.onSubmit()

        assertEquals(listOf("signIn", "signUp"), repository.calls)
    }

    @Test
    fun `a session with a pending invite opens the invite screen`() = runTest {
        repository.invitePending = true
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.fill()
        viewModel.onSubmit()

        assertEquals(listOf(AuthEvent.OpenInvite), events)
    }

    @Test
    fun `a settled session opens home`() = runTest {
        repository.invitePending = false
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.fill()
        viewModel.onSubmit()

        assertEquals(listOf(AuthEvent.OpenHome), events)
    }

    @Test
    fun `nothing else can start while a request is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val viewModel = viewModel()

        viewModel.fill()
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.submitting)

        viewModel.onSubmit()
        viewModel.onGoogleClick(mockk(relaxed = true))
        viewModel.onForgotPasswordClick()
        gate.complete(Unit)

        assertEquals(listOf("signIn"), repository.calls)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun `switching mode keeps what was typed and clears the mark`() = runTest {
        repository.failWith = AuthFailure.WRONG_PASSWORD
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onSubmit()

        viewModel.onModeChange(AuthMode.SIGN_UP)

        assertEquals(AuthMode.SIGN_UP, viewModel.uiState.value.mode)
        assertEquals(EMAIL, viewModel.uiState.value.email)
        assertEquals(PASSWORD, viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.invalidField)
    }

    @Test
    fun `typing in the untouched field leaves the other mark alone`() = runTest {
        repository.failWith = AuthFailure.WRONG_PASSWORD
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onSubmit()

        viewModel.onEmailChange("other@ioaura.app")

        assertEquals(AuthField.PASSWORD, viewModel.uiState.value.invalidField)

        viewModel.onPasswordChange("OtherPassword1")

        assertNull(viewModel.uiState.value.invalidField)
    }

    @Test
    fun `a reset asks the repository with the email on screen`() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.onEmailChange(EMAIL)
        viewModel.onForgotPasswordClick()

        assertEquals(listOf("reset"), repository.calls)
        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.RESET_LINK_SENT)), events)
    }

    @Test
    fun `a google failure the client raises never reaches the network`() = runTest {
        coEvery { googleSignInClient.idToken(any()) } throws
            AuthException(AuthFailure.GOOGLE_UNAVAILABLE)
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.onGoogleClick(mockk(relaxed = true))

        assertTrue(repository.calls.isEmpty())
        assertEquals(listOf(AuthEvent.ShowToast(AuthToast.GOOGLE_UNAVAILABLE)), events)
    }

    private fun viewModel(): AuthViewModel {
        val viewModel = AuthViewModel(
            signIn = SignInUseCase(repository),
            signUp = SignUpUseCase(repository),
            continueWithGoogle = ContinueWithGoogleUseCase(repository),
            requestPasswordReset = RequestPasswordResetUseCase(repository),
            googleSignInClient = googleSignInClient,
        )
        viewModelStore.put("auth${created++}", viewModel)
        return viewModel
    }

    private fun AuthViewModel.fill() {
        onEmailChange(EMAIL)
        onPasswordChange(PASSWORD)
    }

    private fun TestScope.events(viewModel: AuthViewModel): List<AuthEvent> {
        val collected = mutableListOf<AuthEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collected += it }
        }
        return collected
    }

    private class ProgrammableAuthRepository : AuthRepository {
        var failWith: AuthFailure? = null
        var failWithRaw: Throwable? = null
        var invitePending = false
        var gate: CompletableDeferred<Unit>? = null
        val calls = mutableListOf<String>()

        override suspend fun currentAccount(): Account? = null

        override suspend fun restoreSession(): StartDestination = StartDestination.AUTH

        override suspend fun signIn(email: String, password: String): Result<AuthSession> {
            calls += "signIn"
            return answer()
        }

        override suspend fun signUp(email: String, password: String): Result<AuthSession> {
            calls += "signUp"
            return answer()
        }

        override suspend fun continueWithGoogle(idToken: String): Result<AuthSession> {
            calls += "google"
            return answer()
        }

        override suspend fun requestPasswordReset(email: String): Result<Unit> {
            calls += "reset"
            gate?.await()
            failWithRaw?.let { return Result.failure(it) }
            failWith?.let { return Result.failure(AuthException(it)) }
            return Result.success(Unit)
        }

        private suspend fun answer(): Result<AuthSession> {
            gate?.await()
            failWithRaw?.let { return Result.failure(it) }
            failWith?.let { return Result.failure(AuthException(it)) }
            return Result.success(
                AuthSession(
                    account = Account(
                        id = "39",
                        email = EMAIL,
                        handle = "said",
                        inviteLink = "https://ioaura.app/i/SYREX482",
                        authProvider = AuthProvider.EMAIL,
                    ),
                    invitePending = invitePending,
                )
            )
        }
    }
}
