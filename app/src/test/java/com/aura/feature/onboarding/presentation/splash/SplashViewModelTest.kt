package com.aura.feature.onboarding.presentation.splash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.usecase.BootstrapUseCase
import com.aura.feature.onboarding.domain.usecase.ResolveStartDestinationUseCase
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ALMOST_MINIMUM_MILLIS = 1_400L

private const val PAST_MINIMUM_MILLIS = 200L

private const val BOOTSTRAP_TIMEOUT_MILLIS = 4_000L

private const val HANG_MILLIS = 600_000L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SplashViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val boot = ProgrammableBootRepository()

    private val auth = ProgrammableStartRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the log starts on the built in node count and nothing is printed yet`() = splashTest {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.log.contains("4,210"))
        assertEquals(0, viewModel.uiState.value.printedLength)
        assertNull(viewModel.uiState.value.startDestination)
    }

    @Test
    fun `the log carries every line of the boot sequence`() = splashTest {
        val viewModel = viewModel()

        assertEquals(7, viewModel.uiState.value.log.lines().size)
        assertTrue(viewModel.uiState.value.log.startsWith("> in the beginning was the ION"))
        assertTrue(viewModel.uiState.value.log.endsWith("> opening the gate"))
    }

    @Test
    fun `the log types itself out character by character`() = splashTest {
        val viewModel = viewModel()

        advanceTimeBy(71)
        runCurrent()

        assertEquals(10, viewModel.uiState.value.printedLength)
    }

    @Test
    fun `the gate stays shut until the splash has been on screen long enough`() = splashTest {
        val viewModel = viewModel()

        advanceTimeBy(ALMOST_MINIMUM_MILLIS)
        runCurrent()

        assertNull(viewModel.uiState.value.startDestination)

        advanceTimeBy(PAST_MINIMUM_MILLIS)
        runCurrent()

        assertEquals(StartDestination.HOME, viewModel.uiState.value.startDestination)
    }

    @Test
    fun `the destination is whatever the session says`() = splashTest {
        auth.destination = StartDestination.INVITE
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(StartDestination.INVITE, viewModel.uiState.value.startDestination)
    }

    @Test
    fun `a live node count is written back into the log`() = splashTest {
        boot.config = BootConfig(nodeCount = 12_048)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.log.contains("12,048"))
    }

    @Test
    fun `a shorter log never leaves the cursor past its end`() = splashTest {
        boot.config = BootConfig(nodeCount = 1)
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.log.contains("> 1 nodes carry the signal"))
        assertTrue(state.printedLength <= state.log.length)
    }

    @Test
    fun `a mesh that never answers does not hold the splash forever`() = splashTest {
        boot.hang = true
        val viewModel = viewModel()

        advanceTimeBy(BOOTSTRAP_TIMEOUT_MILLIS)
        runCurrent()

        assertNull(viewModel.uiState.value.startDestination)

        advanceUntilIdle()

        assertEquals(StartDestination.HOME, viewModel.uiState.value.startDestination)
        assertTrue(viewModel.uiState.value.log.contains("4,210"))
    }

    @Test
    fun `a session that never answers holds the splash open`() = splashTest {
        auth.hang = true
        val viewModel = viewModel()

        advanceTimeBy(BOOTSTRAP_TIMEOUT_MILLIS * 2)
        runCurrent()

        assertNull(viewModel.uiState.value.startDestination)
    }

    private fun TestScope.viewModel() = SplashViewModel(
        context = context,
        bootstrap = BootstrapUseCase(boot),
        resolveStartDestination = ResolveStartDestinationUseCase(auth),
    ).also { runCurrent() }

    private fun splashTest(body: suspend TestScope.() -> Unit) = runTest { body() }

    private class ProgrammableBootRepository : BootRepository {
        var config = BootConfig.FALLBACK
        var hang = false

        override suspend fun bootstrap(): BootConfig {
            if (hang) delay(HANG_MILLIS)
            return config
        }
    }

    private class ProgrammableStartRepository : AuthRepository {
        var destination = StartDestination.HOME
        var hang = false

        override suspend fun currentAccount(): Account? = null

        override suspend fun restoreSession(): StartDestination {
            if (hang) delay(HANG_MILLIS)
            return destination
        }

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
}
