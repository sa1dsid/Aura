package com.aura.feature.transactions.presentation

import androidx.lifecycle.SavedStateHandle
import com.aura.core.common.LoadStatus
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.terminal.FakeTerminalRepository
import com.aura.feature.transactions.FakeTransactionsRepository
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionFilter
import com.aura.feature.transactions.domain.model.TransactionKind
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

private val EVENTS = listOf(
    TransactionEvent(
        id = "1",
        timestamp = 1L,
        kind = TransactionKind.ION,
        detail = "tap_reward",
        amount = 20,
        currency = "ION",
        isCredit = true,
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val transactionsRepository = FakeTransactionsRepository(EVENTS)
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
    fun `a broken load is repeated the next time the screen opens`() = runTest {
        transactionsRepository.failNextLoad = true
        val viewModel = collected(viewModel())

        viewModel.onScreenResumed()

        assertEquals(LoadStatus.FAILED, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.events.isEmpty())

        viewModel.onScreenResumed()

        assertEquals(2, transactionsRepository.loadCount)
        assertEquals(LoadStatus.READY, viewModel.uiState.value.status)
        assertEquals(EVENTS, viewModel.uiState.value.events)
    }

    @Test
    fun `a load already in flight is not started twice`() = runTest {
        val gate = CompletableDeferred<Unit>()
        transactionsRepository.gate = gate
        val viewModel = collected(viewModel())

        viewModel.onScreenResumed()
        viewModel.onScreenResumed()

        assertEquals(1, transactionsRepository.loadCount)
        assertEquals(LoadStatus.LOADING, viewModel.uiState.value.status)

        gate.complete(Unit)

        assertEquals(LoadStatus.READY, viewModel.uiState.value.status)
        assertEquals(EVENTS, viewModel.uiState.value.events)
    }

    @Test
    fun `the chosen filter survives a recreated screen`() = runTest {
        val savedStateHandle = SavedStateHandle()
        collected(viewModel(savedStateHandle)).onFilterClick(TransactionFilter.SPARK)

        val restored = collected(viewModel(savedStateHandle))

        assertEquals(TransactionFilter.SPARK, restored.uiState.value.filter)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = TransactionsViewModel(
        transactionsRepository = transactionsRepository,
        terminalRepository = terminalRepository,
        newsRepository = newsRepository,
        sessionStore = sessionStore,
        savedStateHandle = savedStateHandle,
    )

    private fun TestScope.collected(viewModel: TransactionsViewModel): TransactionsViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        return viewModel
    }
}
