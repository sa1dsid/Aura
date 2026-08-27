package com.aura.feature.terminal

import com.aura.core.common.LoadStatus
import com.aura.feature.transactions.domain.model.TRANSACTIONS_LIMIT
import com.aura.feature.transactions.domain.model.TransactionFilter
import com.aura.feature.transactions.domain.model.TransactionKind
import com.aura.feature.transactions.domain.model.filterBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class TransactionsIntegrationTest : TerminalTestCase() {

    @Test
    fun `the log comes from the transactions endpoint`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(id = 1, amount = "20.000000"),
                    Terminal.transaction(
                        id = 2,
                        kind = Terminal.KIND_SPARK_ACCRUAL,
                        currency = Terminal.CURRENCY_SPARK,
                        amount = "40000.000000",
                    ),
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        val events = awaitTransactions(states).events

        assertEquals(listOf("1", "2"), events.map { it.id }.sorted())
        assertEquals(20L, events.single { it.id == "1" }.amount)
        assertEquals("ION", events.single { it.id == "1" }.currency)
        assertEquals(40_000L, events.single { it.id == "2" }.amount)
        assertEquals("SPARK", events.single { it.id == "2" }.currency)
    }

    @Test
    fun `events arrive newest first whatever order the server used`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(id = 1, createdAt = Terminal.isoAt(day = 10)),
                    Terminal.transaction(id = 2, createdAt = Terminal.isoAt(day = 20)),
                    Terminal.transaction(id = 3, createdAt = Terminal.isoAt(day = 15)),
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        assertEquals(listOf("2", "3", "1"), awaitTransactions(states).events.map { it.id })
    }

    @Test
    fun `only the newest hundred events reach the screen`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                (1..TRANSACTIONS_LIMIT + 20).map { index ->
                    Terminal.transaction(
                        id = index,
                        createdAt = Terminal.isoOf(index),
                    )
                }
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        val events = awaitTransactions(states).events

        assertEquals(TRANSACTIONS_LIMIT, events.size)
        assertEquals("120", events.first().id)
        assertEquals("21", events.last().id)
    }

    @Test
    fun `a row without a readable date never reaches the log`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(id = 1, createdAt = "not-a-date"),
                    Terminal.transaction(id = 2),
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        assertEquals(listOf("2"), awaitTransactions(states).events.map { it.id })
    }

    @Test
    fun `a row without a readable amount never reaches the log`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(id = 1, amount = "unavailable"),
                    Terminal.transaction(id = 2),
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        assertEquals(listOf("2"), awaitTransactions(states).events.map { it.id })
    }

    @Test
    fun `every kind of event lands under its own filter`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(id = 1, kind = Terminal.KIND_TAP_REWARD),
                    Terminal.transaction(
                        id = 2,
                        kind = Terminal.KIND_SPARK_ACCRUAL,
                        currency = Terminal.CURRENCY_SPARK,
                    ),
                    Terminal.transaction(id = 3, kind = Terminal.KIND_REFERRAL_BONUS),
                    Terminal.transaction(id = 4, kind = Terminal.KIND_EXCHANGE, amount = "-110"),
                    Terminal.transaction(id = 5, kind = Terminal.KIND_DATA_SHARE),
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        val events = awaitTransactions(states).events

        assertEquals(
            listOf(TransactionKind.ION),
            events.filterBy(TransactionFilter.ION).map { it.kind },
        )
        assertEquals(
            listOf(TransactionKind.SPARK),
            events.filterBy(TransactionFilter.SPARK).map { it.kind },
        )
        assertEquals(
            listOf(TransactionKind.REFERRAL),
            events.filterBy(TransactionFilter.REFERRAL).map { it.kind },
        )
        assertEquals(
            listOf(TransactionKind.EXCHANGE),
            events.filterBy(TransactionFilter.EXCHANGE).map { it.kind },
        )
        assertEquals(
            listOf(TransactionKind.DATA_SHARE),
            events.filterBy(TransactionFilter.DATA_SHARE).map { it.kind },
        )
        assertEquals(5, events.filterBy(TransactionFilter.ALL).size)
    }

    @Test
    fun `a debit keeps its minus and is not a credit`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(
                listOf(
                    Terminal.transaction(
                        id = 1,
                        kind = Terminal.KIND_EXCHANGE,
                        currency = Terminal.CURRENCY_SPARK,
                        amount = "-240000.000000",
                    )
                )
            ),
        )
        val (_, states) = transactionsScreenOf(stack)

        val event = awaitTransactions(states).events.single()

        assertEquals(240_000L, event.amount)
        assertEquals("SPARK", event.currency)
        assertFalse(event.isCredit)
    }

    @Test
    fun `opening the screen clears the transactions counter and leaves the promo one`() =
        terminal { stack ->
            stack.server.always(
                TerminalPaths.TERMINAL,
                body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
            )
            val (_, hub) = terminalScreenOf(stack)
            awaitCounters(hub, "the counters") { it.counters.unreadTransactions == 105 }

            val (_, states) = transactionsScreenOf(stack)
            awaitTransactions(states)

            val counters = awaitCounters(hub, "the cleared counter") {
                it.counters.unreadTransactions == 0
            }.counters
            assertEquals(2, counters.unreadPromoCodes)
        }

    @Test
    fun `a failing request leaves the screen in the failed state`() = terminal { stack ->
        stack.server.always(TerminalPaths.TRANSACTIONS, code = SERVER_ERROR, body = "[]")
        val (_, states) = transactionsScreenOf(stack)

        val state = awaitTransactions(states, LoadStatus.FAILED)

        assertTrue(state.events.isEmpty())
    }

    @Test
    fun `a retry after a failure loads the log`() = terminal { stack ->
        stack.server.always(TerminalPaths.TRANSACTIONS, code = SERVER_ERROR, body = "[]")
        val (viewModel, states) = transactionsScreenOf(stack)
        awaitTransactions(states, LoadStatus.FAILED)

        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(listOf(Terminal.transaction(id = 1))),
        )
        viewModel.onRetryClick()

        assertEquals(listOf("1"), awaitTransactions(states).events.map { it.id })
    }

    @Test
    fun `a failing refresh keeps the log already on the screen`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(listOf(Terminal.transaction(id = 1))),
        )
        val (viewModel, states) = transactionsScreenOf(stack)
        awaitTransactions(states)

        stack.server.always(TerminalPaths.TRANSACTIONS, code = SERVER_ERROR, body = "[]")
        viewModel.onScreenResumed()

        val state = awaitTransactions(states, LoadStatus.FAILED)
        assertEquals(listOf("1"), state.events.map { it.id })
    }

    @Test
    fun `an empty log is a ready screen without events`() = terminal { stack ->
        val (_, states) = transactionsScreenOf(stack)

        assertTrue(awaitTransactions(states).events.isEmpty())
    }

    @Test
    fun `the handle in the top bar comes from the session`() = terminal { stack ->
        stack.signIn(handle = "syrex")
        val (_, states) = transactionsScreenOf(stack)

        assertEquals("syrex", awaitTransactions(states).handle)
    }

    @Test
    fun `without a session the top bar has no handle`() = terminal(signedIn = false) { stack ->
        val (_, states) = transactionsScreenOf(stack)

        assertNull(awaitTransactions(states).handle)
    }

    @Test
    fun `the screen opens on the all filter`() = terminal { stack ->
        val (_, states) = transactionsScreenOf(stack)

        assertEquals(TransactionFilter.ALL, awaitTransactions(states).filter)
    }

    @Test
    fun `picking a filter never goes back to the server`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.TRANSACTIONS,
            body = Terminal.list(listOf(Terminal.transaction(id = 1))),
        )
        val (viewModel, states) = transactionsScreenOf(stack)
        awaitTransactions(states)

        viewModel.onFilterClick(TransactionFilter.SPARK)

        awaitUntil("the spark filter") {
            states.lastOrNull()?.filter == TransactionFilter.SPARK
        }
        assertEquals(1, stack.server.hits(TerminalPaths.TRANSACTIONS))
    }

    @Test
    fun `unread news lights up the planet`() = terminal(news = unreadNews()) { stack ->
        val (_, states) = transactionsScreenOf(stack)

        assertTrue(awaitTransactions(states).hasUnreadNews)
    }
}
