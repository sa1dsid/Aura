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
import org.junit.Assert.assertTrue
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
        val dismissals = dismissals(viewModel)
        viewModel.onScreenResumed()
        viewModel.onDismiss()

        authRepository.account = testAccount("2")
        viewModel.onScreenResumed()
        viewModel.onDismiss()

        assertEquals(2, dismissals.size)
        assertEquals(listOf("1", "2"), flagsRepository.markedAccounts)
    }

    @Test
    fun `closes the popup before the server confirms it was seen`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val dismissals = dismissals(viewModel)

        viewModel.onDismiss()

        assertEquals(1, dismissals.size)
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
    fun `a signed out popup asks the server for nothing`() = runTest {
        authRepository.account = null
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val dismissals = dismissals(viewModel)

        viewModel.onScreenResumed()
        viewModel.onDismiss()

        assertEquals(1, dismissals.size)
        assertTrue(flagsRepository.markedAccounts.isEmpty())
        assertEquals(3_000L, viewModel.bonusIon.value)
    }

    @Test
    fun `every close of the popup is reported to the server again`() = runTest {
        val viewModel = WelcomeBonusViewModel(authRepository, flagsRepository)
        val dismissals = dismissals(viewModel)

        viewModel.onDismiss()
        viewModel.onDismiss()

        assertEquals(2, dismissals.size)
        assertEquals(listOf("1", "1"), flagsRepository.markedAccounts)
    }

    private fun TestScope.dismissals(viewModel: WelcomeBonusViewModel): List<Unit> {
        val collected = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dismissed.collect { collected += it }
        }
        return collected
    }
}
