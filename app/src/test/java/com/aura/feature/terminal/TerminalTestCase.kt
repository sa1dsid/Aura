package com.aura.feature.terminal

import com.aura.core.common.LoadStatus
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.promo.presentation.PromoCodesUiState
import com.aura.feature.promo.presentation.PromoCodesViewModel
import com.aura.feature.terminal.presentation.TerminalUiState
import com.aura.feature.terminal.presentation.TerminalViewModel
import com.aura.feature.transactions.presentation.TransactionsUiState
import com.aura.feature.transactions.presentation.TransactionsViewModel
import com.aura.testing.IntegrationTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

abstract class TerminalTestCase : IntegrationTestCase() {

    internal fun terminal(
        news: List<NewsItem> = emptyList(),
        signedIn: Boolean = true,
        body: suspend CoroutineScope.(TerminalStack) -> Unit,
    ) = runBlocking {
        val stack = TerminalStack(news)
        if (signedIn) stack.signIn()
        try {
            body(stack)
        } finally {
            stack.close()
        }
    }

    internal fun terminalScreenOf(
        stack: TerminalStack,
    ): Pair<TerminalViewModel, List<TerminalUiState>> {
        val viewModel = stack.terminalViewModel()
        val states = stack.contentOf(viewModel)
        viewModel.onScreenResumed()
        return viewModel to states
    }

    internal fun transactionsScreenOf(
        stack: TerminalStack,
    ): Pair<TransactionsViewModel, List<TransactionsUiState>> {
        val viewModel = stack.transactionsViewModel()
        val states = stack.contentOf(viewModel)
        viewModel.onScreenResumed()
        return viewModel to states
    }

    internal fun promoScreenOf(
        stack: TerminalStack,
    ): Pair<PromoCodesViewModel, List<PromoCodesUiState>> {
        val viewModel = stack.promoCodesViewModel()
        val states = stack.contentOf(viewModel)
        viewModel.onScreenResumed()
        return viewModel to states
    }

    internal fun awaitCounters(
        states: List<TerminalUiState>,
        what: String,
        condition: (TerminalUiState) -> Boolean,
    ): TerminalUiState {
        awaitUntil(what) { states.lastOrNull()?.let(condition) == true }
        return states.last()
    }

    internal fun awaitTransactions(
        states: List<TransactionsUiState>,
        status: LoadStatus = LoadStatus.READY,
    ): TransactionsUiState {
        awaitUntil("the transactions screen to reach $status") {
            states.lastOrNull()?.status == status
        }
        return states.last()
    }

    internal fun awaitPromoCodes(
        states: List<PromoCodesUiState>,
        status: LoadStatus = LoadStatus.READY,
    ): PromoCodesUiState {
        awaitUntil("the promo screen to reach $status") { states.lastOrNull()?.status == status }
        return states.last()
    }

    internal fun awaitRequest(stack: TerminalStack, path: String, count: Int = 1) =
        awaitUntil("$count request(s) to $path") { stack.server.hits(path) >= count }
}
