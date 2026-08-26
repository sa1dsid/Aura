package com.aura.feature.onboarding.presentation.bonus

import com.aura.feature.onboarding.FakeAuthRepository
import com.aura.feature.onboarding.FakeOnboardingFlagsRepository
import com.aura.feature.onboarding.domain.model.OnboardingFlags
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeBonusViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val flagsRepository = FakeOnboardingFlagsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `closes the popup of the next account signed in without a restart`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val events = events(viewModel)
        viewModel.onScreenResumed()
        viewModel.onDismiss()

        authRepository.account = testAccount("2")
        viewModel.onScreenResumed()
        viewModel.onDismiss()

        assertEquals(listOf(WelcomeBonusEvent.Finished, WelcomeBonusEvent.Finished), events)
        assertEquals(2, flagsRepository.bonusPopupMarks)
    }

    @Test
    fun `closes the popup before the server confirms it was seen`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val events = events(viewModel)

        viewModel.onDismiss()

        assertEquals(listOf(WelcomeBonusEvent.Finished), events)
    }

    @Test
    fun `reads the reserved bonus of the account on the screen`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        flagsRepository.flags = OnboardingFlags.FALLBACK.copy(reservedBonusIon = 5_000L)

        viewModel.onScreenResumed()

        assertEquals(5_000L, viewModel.bonusIon.value)
    }

    @Test
    fun `shows the three thousand of the layout before anything is read`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)

        assertEquals(3_000L, viewModel.bonusIon.value)
        assertEquals(3_000L, OnboardingFlags.DEFAULT_RESERVED_BONUS_ION)
    }

    @Test
    fun `a signed out popup says the session is gone instead of going quiet`() = runTest {
        authRepository.account = null
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val events = events(viewModel)

        viewModel.onScreenResumed()
        viewModel.onDismiss()

        assertEquals(
            listOf(WelcomeBonusEvent.SessionLost, WelcomeBonusEvent.SessionLost),
            events,
        )
        assertEquals(0, flagsRepository.bonusPopupMarks)
        assertEquals(3_000L, viewModel.bonusIon.value)
    }

    @Test
    fun `the popup is reported to the server once per account`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val events = events(viewModel)

        viewModel.onDismiss()
        viewModel.onDismiss()

        assertEquals(listOf(WelcomeBonusEvent.Finished, WelcomeBonusEvent.Finished), events)
        assertEquals(1, flagsRepository.bonusPopupMarks)
    }

    private fun TestScope.events(viewModel: WelcomeBonusViewModel): List<WelcomeBonusEvent> {
        val collected = mutableListOf<WelcomeBonusEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { collected += it }
        }
        return collected
    }
}
