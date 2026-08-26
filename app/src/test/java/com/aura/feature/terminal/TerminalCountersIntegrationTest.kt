package com.aura.feature.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class TerminalCountersIntegrationTest : TerminalTestCase() {

    @Test
    fun `both counters come from the terminal endpoint`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TERMINAL,
            body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
        )
        val (_, states) = terminalScreenOf(stack)

        val counters = awaitCounters(states, "the counters") {
            it.counters.unreadTransactions == 105
        }.counters

        assertEquals(105, counters.unreadTransactions)
        assertEquals(2, counters.unreadPromoCodes)
    }

    @Test
    fun `the screen starts with quiet cards`() = terminal { stack ->
        val (_, states) = terminalScreenOf(stack)
        awaitRequest(stack, TerminalPaths.TERMINAL)

        val counters = states.last().counters

        assertEquals(0, counters.unreadTransactions)
        assertEquals(0, counters.unreadPromoCodes)
    }

    @Test
    fun `coming back to the screen asks the server again`() = terminal { stack ->
        val (viewModel, _) = terminalScreenOf(stack)
        awaitRequest(stack, TerminalPaths.TERMINAL)

        viewModel.onScreenResumed()

        awaitRequest(stack, TerminalPaths.TERMINAL, count = 2)
    }

    @Test
    fun `a failing refresh keeps the counters already on the screen`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TERMINAL,
            body = Terminal.counters(transactionsNew = 7, promoCodesNew = 3),
        )
        val (viewModel, states) = terminalScreenOf(stack)
        awaitCounters(states, "the first counters") { it.counters.unreadTransactions == 7 }

        stack.server.always(TerminalPaths.TERMINAL, code = SERVER_ERROR, body = "{}")
        viewModel.onScreenResumed()
        awaitRequest(stack, TerminalPaths.TERMINAL, count = 2)

        assertEquals(7, states.last().counters.unreadTransactions)
        assertEquals(3, states.last().counters.unreadPromoCodes)
    }

    @Test
    fun `tapping the transactions card clears only its own counter`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TERMINAL,
            body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
        )
        val (viewModel, states) = terminalScreenOf(stack)
        awaitCounters(states, "the counters") { it.counters.unreadTransactions == 105 }

        viewModel.onTransactionsOpened()

        val counters = awaitCounters(states, "the cleared counter") {
            it.counters.unreadTransactions == 0
        }.counters
        assertEquals(2, counters.unreadPromoCodes)
    }

    @Test
    fun `tapping the promo card clears only its own counter`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TERMINAL,
            body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
        )
        val (viewModel, states) = terminalScreenOf(stack)
        awaitCounters(states, "the counters") { it.counters.unreadPromoCodes == 2 }

        viewModel.onPromoCodesOpened()

        val counters = awaitCounters(states, "the cleared counter") {
            it.counters.unreadPromoCodes == 0
        }.counters
        assertEquals(105, counters.unreadTransactions)
    }

    @Test
    fun `a refresh brings a cleared counter back when the server still counts it`() =
        terminal { stack ->
            stack.server.always(
                TerminalPaths.TERMINAL,
                body = Terminal.counters(transactionsNew = 105),
            )
            val (viewModel, states) = terminalScreenOf(stack)
            awaitCounters(states, "the counter") { it.counters.unreadTransactions == 105 }
            viewModel.onTransactionsOpened()
            awaitCounters(states, "the cleared counter") { it.counters.unreadTransactions == 0 }

            viewModel.onScreenResumed()

            awaitCounters(states, "the counter again") { it.counters.unreadTransactions == 105 }
        }

    @Test
    fun `closing the session drops the counters`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TERMINAL,
            body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
        )
        val (_, states) = terminalScreenOf(stack)
        awaitCounters(states, "the counters") { it.counters.unreadTransactions == 105 }

        stack.repository.clearSession()

        awaitCounters(states, "the empty counters") {
            it.counters.unreadTransactions == 0 && it.counters.unreadPromoCodes == 0
        }
    }

    @Test
    fun `unread news lights up the planet`() = terminal(news = unreadNews()) { stack ->
        val (_, states) = terminalScreenOf(stack)

        awaitCounters(states, "the lit planet") { it.hasUnreadNews }
    }

    @Test
    fun `without news the planet stays dark`() = terminal { stack ->
        val (_, states) = terminalScreenOf(stack)
        awaitRequest(stack, TerminalPaths.TERMINAL)

        assertFalse(states.last().hasUnreadNews)
    }

    @Test
    fun `the terminal endpoint is the only one the hub touches`() = terminal { stack ->
        terminalScreenOf(stack)
        awaitRequest(stack, TerminalPaths.TERMINAL)

        assertTrue(stack.server.hits(TerminalPaths.TRANSACTIONS) == 0)
        assertTrue(stack.server.hits(TerminalPaths.PROMO_CODES) == 0)
    }
}
