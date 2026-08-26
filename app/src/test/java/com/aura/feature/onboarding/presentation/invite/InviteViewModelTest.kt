package com.aura.feature.onboarding.presentation.invite

import com.aura.feature.onboarding.FakeAuthRepository
import com.aura.feature.onboarding.FakeInviteRepository
import com.aura.feature.onboarding.FakeOnboardingFlagsRepository
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.usecase.ApplyInviteCodeUseCase
import com.aura.feature.onboarding.domain.usecase.ObserveInviteAttributionUseCase
import com.aura.feature.onboarding.domain.usecase.SkipInviteUseCase
import com.aura.feature.onboarding.testAccount
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

@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val flagsRepository = FakeOnboardingFlagsRepository()
    private val inviteRepository = FakeInviteRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = InviteViewModel(
        authRepository = authRepository,
        flagsRepository = flagsRepository,
        observeInviteAttribution = ObserveInviteAttributionUseCase(inviteRepository),
        applyInviteCode = ApplyInviteCodeUseCase(inviteRepository),
        skipInvite = SkipInviteUseCase(inviteRepository),
    )

    @Test
    fun `sends the skipped account to the bonus popup`() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.onScreenResumed()

        viewModel.onSkipClick()

        assertEquals(1, inviteRepository.skips)
        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = true)), events)
    }

    @Test
    fun `sends the skipped account home when the popup was already shown`() = runTest {
        flagsRepository.flags = OnboardingFlags.FALLBACK.copy(bonusPopupShown = true)
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.onScreenResumed()

        viewModel.onSkipClick()

        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = false)), events)
    }

    @Test
    fun `starts empty for the next account signed in without a restart`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()
        viewModel.onCodeChange("ABCD1234")

        authRepository.account = testAccount("2")
        viewModel.onScreenResumed()

        assertEquals(InviteUiState(), viewModel.uiState.value)
    }

    @Test
    fun `keeps the typed code when the same account returns to the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()
        viewModel.onCodeChange("ABCD1234")

        viewModel.onScreenResumed()

        assertEquals("ABCD1234", viewModel.uiState.value.code)
    }

    @Test
    fun `locks the code that came from a link`() = runTest {
        inviteRepository.attribution = InviteAttribution.FromLink("LINKCODE")
        val viewModel = viewModel()

        viewModel.onScreenResumed()

        assertEquals("LINKCODE", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.locked)
    }

    @Test
    fun `a locked field refuses everything typed or pasted into it`() = runTest {
        inviteRepository.attribution = InviteAttribution.FromLink("LINKCODE")
        val viewModel = viewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("OTHER123")
        viewModel.onPaste("PASTED12")

        assertEquals("LINKCODE", viewModel.uiState.value.code)
    }

    @Test
    fun `typing drops the punctuation and raises the case`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("sy-rex 482")

        assertEquals("SYREX482", viewModel.uiState.value.code)
    }

    @Test
    fun `pasting is normalised the same way as typing`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()

        viewModel.onPaste("  sy-rex 482  ")

        assertEquals("SYREX482", viewModel.uiState.value.code)
    }

    @Test
    fun `an empty clipboard leaves the field alone`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()
        viewModel.onCodeChange("ABCD1234")

        viewModel.onPaste(null)
        viewModel.onPaste("   ")

        assertEquals("ABCD1234", viewModel.uiState.value.code)
    }

    @Test
    fun `apply needs a whole code of eight characters`() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("ABCD123")
        viewModel.onApplyClick()

        assertTrue(inviteRepository.appliedCodes.isEmpty())

        viewModel.onCodeChange("ABCD1234")
        viewModel.onApplyClick()

        assertEquals(listOf("ABCD1234"), inviteRepository.appliedCodes)
    }

    @Test
    fun `a refused code is reported and no one leaves the screen`() = runTest {
        inviteRepository.result = Result.failure(InviteException(InviteFailure.UNKNOWN_CODE))
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.onScreenResumed()

        viewModel.onCodeChange("ABCD1234")
        viewModel.onApplyClick()

        assertTrue(events.isEmpty())
        assertEquals(InviteFailure.UNKNOWN_CODE, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun `a failure that is not ours reads as a dead network`() = runTest {
        inviteRepository.result = Result.failure(IllegalStateException("boom"))
        val viewModel = viewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("ABCD1234")
        viewModel.onApplyClick()

        assertEquals(InviteFailure.NETWORK, viewModel.uiState.value.failure)
    }

    @Test
    fun `typing again clears the refusal on screen`() = runTest {
        inviteRepository.result = Result.failure(InviteException(InviteFailure.UNKNOWN_CODE))
        val viewModel = viewModel()
        viewModel.onScreenResumed()
        viewModel.onCodeChange("ABCD1234")
        viewModel.onApplyClick()

        viewModel.onCodeChange("ABCD1235")

        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `a signed out screen says the session is gone instead of going quiet`() = runTest {
        authRepository.account = null
        val viewModel = viewModel()
        val events = events(viewModel)

        viewModel.onScreenResumed()
        viewModel.onCodeChange("ABCD1234")
        viewModel.onApplyClick()

        assertTrue(inviteRepository.appliedCodes.isEmpty())
        assertEquals(listOf(InviteEvent.SessionLost, InviteEvent.SessionLost), events)
    }

    private fun TestScope.events(viewModel: InviteViewModel): List<InviteEvent> {
        val collected = mutableListOf<InviteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collected += it }
        }
        return collected
    }
}
