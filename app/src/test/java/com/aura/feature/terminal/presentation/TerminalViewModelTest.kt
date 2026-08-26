package com.aura.feature.terminal.presentation

import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.terminal.FakeTerminalRepository
import com.aura.feature.terminal.domain.model.TerminalCounters
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val UNREAD = listOf(NewsItem("1", "Mesh update", "body", 3L, read = false))

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private val repository = FakeTerminalRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the hub opens with quiet cards`() = runTest {
        val viewModel = collected(viewModelOf())

        assertEquals(TerminalCounters(), viewModel.uiState.value.counters)
        assertFalse(viewModel.uiState.value.hasUnreadNews)
    }

    @Test
    fun `the view model waits for the screen instead of fetching on its own`() = runTest {
        collected(viewModelOf())

        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun `every time the screen comes back it refreshes`() = runTest {
        val viewModel = collected(viewModelOf())

        viewModel.onScreenResumed()
        viewModel.onScreenResumed()

        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun `the counters reach the cards`() = runTest {
        repository.emit(TerminalCounters(unreadTransactions = 105, unreadPromoCodes = 2))
        val viewModel = collected(viewModelOf())

        assertEquals(105, viewModel.uiState.value.counters.unreadTransactions)
        assertEquals(2, viewModel.uiState.value.counters.unreadPromoCodes)
    }

    @Test
    fun `opening the transactions clears only its own counter`() = runTest {
        repository.emit(TerminalCounters(unreadTransactions = 105, unreadPromoCodes = 2))
        val viewModel = collected(viewModelOf())

        viewModel.onTransactionsOpened()

        assertEquals(0, viewModel.uiState.value.counters.unreadTransactions)
        assertEquals(2, viewModel.uiState.value.counters.unreadPromoCodes)
    }

    @Test
    fun `opening the promo codes clears only its own counter`() = runTest {
        repository.emit(TerminalCounters(unreadTransactions = 105, unreadPromoCodes = 2))
        val viewModel = collected(viewModelOf())

        viewModel.onPromoCodesOpened()

        assertEquals(0, viewModel.uiState.value.counters.unreadPromoCodes)
        assertEquals(105, viewModel.uiState.value.counters.unreadTransactions)
    }

    @Test
    fun `unread news lights up the planet`() = runTest {
        val viewModel = collected(viewModelOf(FakeNewsRepository(UNREAD)))

        assertTrue(viewModel.uiState.value.hasUnreadNews)
    }

    private fun viewModelOf(
        news: FakeNewsRepository = FakeNewsRepository(),
    ) = TerminalViewModel(terminalRepository = repository, newsRepository = news)

    private fun TestScope.collected(viewModel: TerminalViewModel): TerminalViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        return viewModel
    }
}
