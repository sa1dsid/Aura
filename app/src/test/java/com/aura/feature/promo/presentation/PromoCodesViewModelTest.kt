package com.aura.feature.promo.presentation

import com.aura.core.common.LoadStatus
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.promo.FakePromoCodesRepository
import com.aura.feature.terminal.FakeTerminalRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val CODES = listOf(
    PromoCode(
        id = "1",
        code = "A8X4-KP92-QW01",
        kind = PromoCodeKind.SPARK,
        issuedAt = 1L,
        used = false,
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PromoCodesViewModelTest {

    private val promoCodesRepository = FakePromoCodesRepository(CODES)
    private val terminalRepository = FakeTerminalRepository()
    private val newsRepository = FakeNewsRepository()
    private val sessionStore = SessionStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the screen opens on loading and nothing is fetched on its own`() = runTest {
        val viewModel = collected(viewModel())

        assertEquals(LoadStatus.LOADING, viewModel.uiState.value.status)
        assertEquals(0, promoCodesRepository.loadCount)
    }

    @Test
    fun `a broken load is repeated the next time the screen opens`() = runTest {
        promoCodesRepository.failNextLoad = true
        val viewModel = collected(viewModel())

        viewModel.onScreenResumed()

        assertEquals(LoadStatus.FAILED, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.codes.isEmpty())

        viewModel.onScreenResumed()

        assertEquals(2, promoCodesRepository.loadCount)
        assertEquals(LoadStatus.READY, viewModel.uiState.value.status)
        assertEquals(CODES, viewModel.uiState.value.codes)
    }

    @Test
    fun `a retry after a failure loads the codes`() = runTest {
        promoCodesRepository.failNextLoad = true
        val viewModel = collected(viewModel())
        viewModel.onScreenResumed()

        viewModel.onRetryClick()

        assertEquals(LoadStatus.READY, viewModel.uiState.value.status)
        assertEquals(CODES, viewModel.uiState.value.codes)
    }

    @Test
    fun `a load already in flight is not started twice`() = runTest {
        val gate = CompletableDeferred<Unit>()
        promoCodesRepository.gate = gate
        val viewModel = collected(viewModel())

        viewModel.onScreenResumed()
        viewModel.onScreenResumed()

        assertEquals(1, promoCodesRepository.loadCount)
        assertEquals(LoadStatus.LOADING, viewModel.uiState.value.status)

        gate.complete(Unit)

        assertEquals(LoadStatus.READY, viewModel.uiState.value.status)
        assertEquals(CODES, viewModel.uiState.value.codes)
    }

    private fun viewModel() = PromoCodesViewModel(
        promoCodesRepository = promoCodesRepository,
        terminalRepository = terminalRepository,
        newsRepository = newsRepository,
        sessionStore = sessionStore,
    )

    private fun TestScope.collected(viewModel: PromoCodesViewModel): PromoCodesViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        return viewModel
    }
}
