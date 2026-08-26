package com.aura.feature.home

import com.aura.core.network.NetworkType
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.NodesOnline
import com.aura.feature.home.presentation.HomeEvent
import com.aura.feature.home.presentation.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDataIntegrationTest : HomeTestCase() {

    @Test
    fun `the dashboard fills the balances and the tap counter`() = home { stack ->
        stack.server.always(
            HomePaths.DASHBOARD,
            body = Home.dashboard(
                accruedIon = 140,
                availableToWithdrawIon = 0,
                reservedBonusIon = 3_000,
                tapCount = 7,
            ),
        )
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val home = awaitContent(states).home

        assertEquals(140L, home.balances.accrued)
        assertEquals(0L, home.balances.availableToWithdraw)
        assertEquals(3_000L, home.balances.reservedBonus)
        assertEquals(7, home.tapCount)
    }

    @Test
    fun `the node status arrives with its tier and progress`() = home { stack ->
        stack.server.always(
            HomePaths.DASHBOARD,
            body = Home.dashboard(
                node = Home.node(
                    tier = "active_signal",
                    progressCurrent = 4,
                    progressTarget = 6,
                    rateMultiplierPercent = 250,
                    sparkReferralPercent = 5,
                )
            ),
        )
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val status = awaitContent(states).home.nodeStatus

        assertEquals(NodeTier.ACTIVE_SIGNAL, status.currentTier)
        assertEquals(NodeTier.STABLE_LINK, status.nextTier)
        assertEquals(4L, status.progressToNext)
        assertEquals(6L, status.progressTarget)
        assertEquals(250, status.rateMultiplierPercent)
        assertEquals(5, status.sparkReferralPercent)
    }

    @Test
    fun `the bonus teaser carries every gauge the popup shows`() = home { stack ->
        stack.server.always(
            HomePaths.DASHBOARD,
            body = Home.dashboard(
                bonus = Home.bonus(
                    completed = 2,
                    total = 4,
                    signalLock = 20,
                    networkSync = 4,
                    fullUplink = 10,
                    dataShareGb = 0,
                ),
                bonusTeaserShouldBlink = true,
            ),
        )
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val teaser = awaitContent(states).home.teasers.bonusWithdrawal

        assertEquals(2, teaser.completedSteps)
        assertEquals(4, teaser.totalSteps)
        assertEquals(20, teaser.signalLockTaps)
        assertEquals(4, teaser.networkSyncFriends)
        assertEquals(10, teaser.fullUplinkDays)
        assertTrue(teaser.isBlinking)
        assertTrue(teaser.isDataShareSoon)
        assertFalse(teaser.isComplete)
    }

    @Test
    fun `the vpn teaser stays dark while the server flag is off`() = home { stack ->
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val teaser = awaitContent(states).home.teasers.vpnCode

        assertFalse(teaser.isEnabled)
        assertFalse(teaser.isCodeReady)
    }

    @Test
    fun `the mesh brings the glowing cities and the node count`() = home { stack ->
        stack.server.always(
            HomePaths.MESH,
            body = Home.mesh(nodesOnline = 12_048, cities = listOf("Tallinn", "Rostov-na-Donu")),
        )
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        awaitUntil("the mesh cities") {
            (states.lastOrNull { it is HomeUiState.Content } as? HomeUiState.Content)
                ?.mesh?.cities?.isNotEmpty() == true
        }
        val mesh = awaitContent(states).mesh

        assertEquals(listOf("Tallinn", "Rostov-na-Donu"), mesh.cities.map { it.name })
        assertEquals(NodesOnline.Live(12_048), mesh.nodesOnline)
    }

    @Test
    fun `a stale node count is never shown as live`() = home { stack ->
        stack.server.always(HomePaths.MESH, body = Home.mesh(nodesOnline = 900, stale = true))
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        awaitUntil("the node count") {
            (states.lastOrNull { it is HomeUiState.Content } as? HomeUiState.Content)
                ?.mesh?.nodesOnline != NodesOnline.Unknown
        }

        assertEquals(NodesOnline.LastKnown(900), awaitContent(states).mesh.nodesOnline)
    }

    @Test
    fun `an unreachable dashboard leaves the screen loading`() = home { stack ->
        stack.server.always(
            HomePaths.DASHBOARD,
            code = 500,
            body = Home.detail("Internal Server Error"),
        )
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        awaitRequest(stack, HomePaths.DASHBOARD)

        assertTrue(states.all { it is HomeUiState.Loading })
    }

    @Test
    fun `the screen keeps beating while it is open`() = home { stack ->
        val (viewModel, _) = screenOf(stack)

        viewModel.onScreenResumed()
        awaitRequest(stack, HomePaths.HEARTBEAT)

        assertTrue(stack.server.hits(HomePaths.HEARTBEAT) >= 1)
    }

    @Test
    fun `opening the bonus popup tells the server and reloads`() = home { stack ->
        stack.server.always(HomePaths.BONUS_TEASER_SEEN, body = """{"bonus_teaser_seen":true}""")
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)
        val dashboards = stack.server.hits(HomePaths.DASHBOARD)

        viewModel.onBonusTeaserOpened()
        awaitRequest(stack, HomePaths.BONUS_TEASER_SEEN)
        awaitUntil("the reload after the popup") {
            stack.server.hits(HomePaths.DASHBOARD) > dashboards
        }

        assertEquals(1, stack.server.hits(HomePaths.BONUS_TEASER_SEEN))
    }

    @Test
    fun `the invite row stays empty until the nodes feature answers`() = home { stack ->
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val invite = awaitContent(states).home.invite

        assertEquals(0, invite.friendsJoined)
        assertEquals("", invite.inviteLink)
        assertEquals(1, stack.nodesRepository.refreshes)
    }

    @Test
    fun `the battery popup is asked for on every return to the screen`() = home { stack ->
        stack.server.always(HomePaths.BATTERY, body = Home.battery(shouldShow = true))
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()
        val home = awaitHome(states, "the battery popup") { it.batteryOptimization.shouldShow }

        assertTrue(home.batteryOptimization.shouldShow)
        assertFalse(home.batteryOptimization.isDisabled)
        assertTrue(stack.server.hits(HomePaths.BATTERY) >= 1)
    }

    @Test
    fun `turning the optimisation off is confirmed and cheered`() = home { stack ->
        stack.server.always(HomePaths.BATTERY, body = Home.battery(shouldShow = true))
        stack.server.always(
            HomePaths.BATTERY_DISABLED,
            body = Home.battery(shouldShow = false, optimizationDisabled = true),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onBatteryOptimizationConfirmed()
        awaitEvent(events)
        val home = awaitHome(states, "the optimisation off") { it.batteryOptimization.isDisabled }

        assertEquals(listOf(HomeEvent.BatteryOptimizationDisabled), events)
        assertFalse(home.batteryOptimization.shouldShow)
        assertEquals("", stack.server.bodyOf(HomePaths.BATTERY_DISABLED))
    }

    @Test
    fun `finding the optimisation already off says nothing out loud`() = home { stack ->
        stack.server.always(
            HomePaths.BATTERY_DISABLED,
            body = Home.battery(shouldShow = false, optimizationDisabled = true),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onBatteryOptimizationSatisfied()
        awaitRequest(stack, HomePaths.BATTERY_DISABLED)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `refusing the battery popup is recorded on the server`() = home { stack ->
        stack.server.always(HomePaths.BATTERY, body = Home.battery(shouldShow = true))
        stack.server.always(
            HomePaths.BATTERY_DECLINE,
            body = Home.battery(shouldShow = false, declinedAt = "2026-08-26T00:00:00Z"),
        )
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onBatteryOptimizationDeclined()
        val home = awaitHome(states, "the popup to go away") { !it.batteryOptimization.shouldShow }

        assertFalse(home.batteryOptimization.shouldShow)
        assertEquals(1, stack.server.hits(HomePaths.BATTERY_DECLINE))
    }

    @Test
    fun `a battery answer the server refuses leaves the popup as it was`() = home { stack ->
        stack.server.always(HomePaths.BATTERY, body = Home.battery(shouldShow = true))
        stack.server.always(
            HomePaths.BATTERY_DECLINE,
            code = 500,
            body = Home.detail("Internal Server Error"),
        )
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitHome(states, "the battery popup") { it.batteryOptimization.shouldShow }

        viewModel.onBatteryOptimizationDeclined()
        awaitRequest(stack, HomePaths.BATTERY_DECLINE)

        assertTrue(awaitContent(states).home.batteryOptimization.shouldShow)
    }

    @Test
    fun `a network change reloads the screen on its own`() = home { stack ->
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)
        val dashboards = stack.server.hits(HomePaths.DASHBOARD)

        stack.networkMonitor.set(type = NetworkType.MOBILE_4G)
        awaitUntil("the reload after the network change") {
            stack.server.hits(HomePaths.DASHBOARD) > dashboards
        }

        assertTrue(stack.server.hits(HomePaths.DASHBOARD) > dashboards)
    }

    @Test
    fun `the unread news pulse comes from the news feature`() = home { stack ->
        val (viewModel, states) = screenOf(stack)

        viewModel.onScreenResumed()

        assertFalse(awaitContent(states).hasUnreadNews)
    }

    @Test
    fun `a vpn pins the blue dot instead of moving it`() = home { stack ->
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitUntil("the blue dot") {
            (states.lastOrNull { it is HomeUiState.Content } as? HomeUiState.Content)
                ?.mesh?.userPresence != null
        }

        stack.networkMonitor.set(isVpnActive = true)
        awaitUntil("the pinned dot") {
            (states.lastOrNull { it is HomeUiState.Content } as? HomeUiState.Content)
                ?.mesh?.userPresence?.isPinnedByVpn == true
        }

        val presence = awaitContent(states).mesh.userPresence
        assertEquals("Tallinn", presence?.cityName)
        assertTrue(presence?.isPinnedByVpn == true)
    }
}
