package com.aura.feature.onboarding.presentation.invite

import com.aura.feature.onboarding.FakeAuthRepository
import com.aura.feature.onboarding.FakeInviteRepository
import com.aura.feature.onboarding.FakeOnboardingFlagsRepository
import com.aura.feature.onboarding.domain.model.InviteAttribution
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

        assertEquals(listOf("1"), inviteRepository.skippedAccounts)
        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = true)), events)
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

    private fun TestScope.events(viewModel: InviteViewModel): List<InviteEvent> {
        val collected = mutableListOf<InviteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collected += it }
        }
        return collected
    }
}
