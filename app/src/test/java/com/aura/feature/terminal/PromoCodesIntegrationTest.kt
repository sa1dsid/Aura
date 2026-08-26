package com.aura.feature.terminal

import com.aura.core.common.LoadStatus
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.promo.domain.model.sectionCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class PromoCodesIntegrationTest : TerminalTestCase() {

    @Test
    fun `the codes come from the promo endpoint`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(
                listOf(
                    Terminal.promo(id = 1, code = "A8X4-KP92-QW01"),
                    Terminal.promo(id = 2, code = "VP-7K2M-XQ19", campaign = Terminal.CAMPAIGN_VPN),
                )
            ),
        )
        val (_, states) = promoScreenOf(stack)

        val codes = awaitPromoCodes(states).codes

        assertEquals(listOf("1", "2"), codes.map { it.id })
        assertEquals(listOf("A8X4-KP92-QW01", "VP-7K2M-XQ19"), codes.map { it.code })
    }

    @Test
    fun `a campaign that mentions vpn lands in the vpn section`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(
                listOf(
                    Terminal.promo(id = 1, campaign = Terminal.CAMPAIGN_VPN),
                    Terminal.promo(id = 2, campaign = "VPN_YEAR"),
                    Terminal.promo(id = 3, campaign = "planet_vpn_promo"),
                )
            ),
        )
        val (_, states) = promoScreenOf(stack)

        val codes = awaitPromoCodes(states).codes

        assertTrue(codes.all { it.kind == PromoCodeKind.VPN })
    }

    @Test
    fun `everything else lands in the spark section`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(
                listOf(
                    Terminal.promo(id = 1, campaign = Terminal.CAMPAIGN_SPARK),
                    Terminal.promo(id = 2, campaign = "something_new"),
                    Terminal.promo(id = 3, campaign = ""),
                )
            ),
        )
        val (_, states) = promoScreenOf(stack)

        assertTrue(awaitPromoCodes(states).codes.all { it.kind == PromoCodeKind.SPARK })
    }

    @Test
    fun `a code without a readable date never reaches the screen`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(
                listOf(
                    Terminal.promo(id = 1, createdAt = "not-a-date"),
                    Terminal.promo(id = 2),
                )
            ),
        )
        val (_, states) = promoScreenOf(stack)

        assertEquals(listOf("2"), awaitPromoCodes(states).codes.map { it.id })
    }

    @Test
    fun `codes keep the server order until a section sorts them`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(
                listOf(
                    Terminal.promo(id = 1, createdAt = Terminal.isoAt(day = 10)),
                    Terminal.promo(id = 2, createdAt = Terminal.isoAt(day = 20)),
                    Terminal.promo(id = 3, createdAt = Terminal.isoAt(day = 15)),
                )
            ),
        )
        val (_, states) = promoScreenOf(stack)

        val codes = awaitPromoCodes(states).codes

        assertEquals(listOf("1", "2", "3"), codes.map { it.id })
        assertEquals(
            listOf("2", "3", "1"),
            codes.sectionCodes(PromoCodeKind.SPARK).map { it.id },
        )
    }

    @Test
    fun `every code arrives unused because the server never says otherwise`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(listOf(Terminal.promo(id = 1))),
        )
        val (_, states) = promoScreenOf(stack)

        assertFalse(awaitPromoCodes(states).codes.single().used)
    }

    @Test
    fun `opening the screen clears the promo counter and leaves the transactions one`() =
        terminal { stack ->
            stack.server.always(
                TerminalPaths.TERMINAL,
                body = Terminal.counters(transactionsNew = 105, promoCodesNew = 2),
            )
            val (_, hub) = terminalScreenOf(stack)
            awaitCounters(hub, "the counters") { it.counters.unreadPromoCodes == 2 }

            val (_, states) = promoScreenOf(stack)
            awaitPromoCodes(states)

            val counters = awaitCounters(hub, "the cleared counter") {
                it.counters.unreadPromoCodes == 0
            }.counters
            assertEquals(105, counters.unreadTransactions)
        }

    @Test
    fun `a failing request leaves the screen in the failed state`() = terminal { stack ->
        stack.server.always(TerminalPaths.PROMO_CODES, code = SERVER_ERROR, body = "[]")
        val (_, states) = promoScreenOf(stack)

        assertTrue(awaitPromoCodes(states, LoadStatus.FAILED).codes.isEmpty())
    }

    @Test
    fun `a retry after a failure loads the codes`() = terminal { stack ->
        stack.server.always(TerminalPaths.PROMO_CODES, code = SERVER_ERROR, body = "[]")
        val (viewModel, states) = promoScreenOf(stack)
        awaitPromoCodes(states, LoadStatus.FAILED)

        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(listOf(Terminal.promo(id = 1))),
        )
        viewModel.onRetryClick()

        assertEquals(listOf("1"), awaitPromoCodes(states).codes.map { it.id })
    }

    @Test
    fun `a failing refresh keeps the codes already on the screen`() = terminal { stack ->
        stack.server.always(
            TerminalPaths.PROMO_CODES,
            body = Terminal.list(listOf(Terminal.promo(id = 1))),
        )
        val (viewModel, states) = promoScreenOf(stack)
        awaitPromoCodes(states)

        stack.server.always(TerminalPaths.PROMO_CODES, code = SERVER_ERROR, body = "[]")
        viewModel.onScreenResumed()

        assertEquals(
            listOf("1"),
            awaitPromoCodes(states, LoadStatus.FAILED).codes.map { it.id },
        )
    }

    @Test
    fun `an empty wallet is a ready screen without codes`() = terminal { stack ->
        val (_, states) = promoScreenOf(stack)

        assertTrue(awaitPromoCodes(states).codes.isEmpty())
    }

    @Test
    fun `the handle in the top bar comes from the session`() = terminal { stack ->
        stack.signIn(handle = "syrex")
        val (_, states) = promoScreenOf(stack)

        assertEquals("syrex", awaitPromoCodes(states).handle)
    }

    @Test
    fun `without a session the top bar has no handle`() = terminal(signedIn = false) { stack ->
        val (_, states) = promoScreenOf(stack)

        assertNull(awaitPromoCodes(states).handle)
    }

    @Test
    fun `unread news lights up the planet`() = terminal(news = unreadNews()) { stack ->
        val (_, states) = promoScreenOf(stack)

        assertTrue(awaitPromoCodes(states).hasUnreadNews)
    }
}
