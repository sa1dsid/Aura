package com.aura.feature.home

import com.aura.core.network.NetworkType
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.home.presentation.HomeEvent
import com.aura.feature.network.domain.model.PingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val SESSION = "session-1"

private const val REWARD_ION = 20

class TapFlowIntegrationTest : HomeTestCase() {

    @Test
    fun `a tap opens a session on the server and names the network`() = home { stack ->
        stack.stubTapStart()
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.TAP_START)

        assertEquals(
            """{"network_type":"wifi","vpn":false,"emulator":false,""" +
                """"integrity_token":"integrity-hash"}""",
            stack.server.bodyOf(HomePaths.TAP_START),
        )
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }
    }

    @Test
    fun `a mobile tap is reported as mobile`() = home { stack ->
        stack.stubTapStart()
        stack.networkMonitor.set(type = NetworkType.MOBILE_4G)
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.TAP_START)

        assertTrue(stack.server.bodyOf(HomePaths.TAP_START).contains(""""network_type":"mobile""""))
    }

    @Test
    fun `an emulator is confessed to the server`() = home(isEmulator = true) { stack ->
        stack.stubTapStart()
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.TAP_START)

        assertTrue(stack.server.bodyOf(HomePaths.TAP_START).contains(""""emulator":true"""))
    }

    @Test
    fun `a tap goes through even when the integrity challenge fails`() = home { stack ->
        stack.server.always(HomePaths.INTEGRITY, code = 503, body = Home.detail("unavailable"))
        stack.server.always(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.TAP_START)

        assertEquals(
            """{"network_type":"wifi","vpn":false,"emulator":false}""",
            stack.server.bodyOf(HomePaths.TAP_START),
        )
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }
    }

    @Test
    fun `a running session beats to keep the server informed`() = home { stack ->
        stack.stubTapStart()
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.heartbeatOf(SESSION))

        assertTrue(stack.server.hits(HomePaths.heartbeatOf(SESSION)) >= 1)
    }

    @Test
    fun `a session that runs its course is finished, rewarded and probed`() = home { stack ->
        stack.stubTapStart()
        stack.server.next(HomePaths.DASHBOARD, body = Home.dashboard())
        stack.server.always(
            HomePaths.DASHBOARD,
            body = Home.dashboard(
                cooldownAvailableAt = stack.clock.isoIn(12.hours.inWholeMilliseconds)
            ),
        )
        stack.server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(
                sessionId = SESSION,
                status = "completed",
                tapCount = 1,
                accruedIon = 20,
                cooldownAvailableAt = stack.clock.isoIn(12.hours.inWholeMilliseconds),
                sparkWindowRate = 20_000,
            ),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)
        viewModel.onMainButtonClick()
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }

        stack.clock.advanceBy(3.minutes.inWholeMilliseconds + 1_000)
        awaitEvent(events)

        assertEquals(
            """{"interrupted":false,"network_lost":false,"app_backgrounded":false}""",
            stack.server.bodyOf(HomePaths.finishOf(SESSION)),
        )
        assertEquals(listOf(HomeEvent.TestCompleted(REWARD_ION)), events)
        assertEquals(listOf(PingSource.HOME), stack.pingHistory.probes)
        awaitUntil("the cooldown") {
            stack.sessionEngine.state.value is TestSessionState.Cooldown
        }
    }

    @Test
    fun `leaving the screen burns the session and tells the server why`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "interrupted"),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)
        viewModel.onMainButtonClick()
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }

        viewModel.onScreenLeft()
        awaitRequest(stack, HomePaths.finishOf(SESSION))
        viewModel.onScreenResumed()
        awaitEvent(events)

        assertEquals(
            """{"interrupted":true,"network_lost":false,"app_backgrounded":true}""",
            stack.server.bodyOf(HomePaths.finishOf(SESSION)),
        )
        assertTrue(HomeEvent.TestInterrupted in events)
        assertTrue(stack.sessionEngine.state.value is TestSessionState.Ready)
    }

    @Test
    fun `losing the network burns the session as a network loss`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "interrupted"),
        )
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)
        viewModel.onMainButtonClick()
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }

        stack.networkMonitor.set(isOnline = false)
        awaitRequest(stack, HomePaths.finishOf(SESSION))

        assertEquals(
            """{"interrupted":true,"network_lost":true,"app_backgrounded":false}""",
            stack.server.bodyOf(HomePaths.finishOf(SESSION)),
        )
    }

    @Test
    fun `a vpn stops the tap before it reaches the server`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(HomePaths.EARNING_STATE, body = Home.earningState(paused = true))
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        stack.networkMonitor.set(isVpnActive = true)
        awaitHome(states, "the vpn badge") { it.connection.isVpnActive }
        val events = stack.eventsOf(viewModel.events)
        viewModel.onMainButtonClick()
        awaitEvent(events)

        assertEquals(listOf(HomeEvent.TestRejected(TestStartRejection.VpnDetected)), events)
        assertEquals(0, stack.server.hits(HomePaths.TAP_START))
    }

    @Test
    fun `a cooldown that has not run out stops the tap`() = home { stack ->
        stack.server.always(
            HomePaths.DASHBOARD,
            body = Home.dashboard(
                cooldownAvailableAt = stack.clock.isoIn(2.hours.inWholeMilliseconds)
            ),
        )
        stack.stubTapStart()
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitHome(states, "the cooldown") { it.session is TestSessionState.Cooldown }

        viewModel.onMainButtonClick()
        awaitEvent(events)

        val rejection = (events.first() as HomeEvent.TestRejected).rejection
        assertTrue(rejection is TestStartRejection.CooldownNotFinished)
        assertEquals(0, stack.server.hits(HomePaths.TAP_START))
    }

    @Test
    fun `a session the server still thinks is running is released and retried`() = home { stack ->
        stack.server.always(HomePaths.INTEGRITY, body = Home.integrity())
        stack.server.next(
            HomePaths.TAP_START,
            code = 400,
            body = Home.detail("A test is already running"),
        )
        stack.server.next(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))
        stack.server.always(
            HomePaths.finishOf("stuck-session"),
            body = Home.tap(sessionId = "stuck-session", status = "interrupted"),
        )
        stack.tapSessionStore.savePendingSessionId("stuck-session")
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitRequest(stack, HomePaths.TAP_START, count = 2)

        assertEquals(2, stack.server.hits(HomePaths.TAP_START))
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }
    }

    @Test
    fun `a start the server refuses outright is reported to the screen`() = home { stack ->
        stack.server.always(HomePaths.INTEGRITY, body = Home.integrity())
        stack.server.always(
            HomePaths.TAP_START,
            code = 400,
            body = Home.detail("ION paused - VPN detected"),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitEvent(events)

        assertEquals(listOf(HomeEvent.TestRejected(TestStartRejection.VpnDetected)), events)
    }

    @Test
    fun `a heartbeat that no longer knows the session drops it`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(
            HomePaths.heartbeatOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "expired"),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)

        viewModel.onMainButtonClick()
        awaitEvent(events)

        assertTrue(HomeEvent.TestInterrupted in events)
        assertTrue(stack.sessionEngine.state.value is TestSessionState.Ready)
        assertNull(stack.tapSessionStore.pending)
    }

    @Test
    fun `switching a vpn on and off tells the server about the earning state`() = home { stack ->
        stack.server.always(HomePaths.EARNING_STATE, body = Home.earningState(paused = true))
        val (viewModel, states) = screenOf(stack)
        viewModel.onScreenResumed()
        awaitContent(states)

        stack.networkMonitor.set(isVpnActive = true)
        awaitRequest(stack, HomePaths.EARNING_STATE)

        assertEquals("""{"vpn":true,"emulator":false}""", stack.server.bodyOf(HomePaths.EARNING_STATE))

        stack.server.always(HomePaths.EARNING_STATE, body = Home.earningState(paused = false))
        stack.networkMonitor.set(isVpnActive = false)
        awaitRequest(stack, HomePaths.EARNING_STATE, count = 2)

        assertEquals(
            """{"vpn":false,"emulator":false}""",
            stack.server.bodyOf(HomePaths.EARNING_STATE),
        )
    }


    @Test
    fun `a dashboard that forgot the cooldown unlocks the button again`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(
                sessionId = SESSION,
                status = "completed",
                cooldownAvailableAt = stack.clock.isoIn(12.hours.inWholeMilliseconds),
            ),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)
        viewModel.onMainButtonClick()
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }

        stack.clock.advanceBy(3.minutes.inWholeMilliseconds + 1_000)
        awaitEvent(events)

        awaitUntil("the button to unlock again") {
            stack.sessionEngine.state.value is TestSessionState.Ready
        }
    }


    @Test
    fun `a completed session is left behind in the local store`() = home { stack ->
        stack.stubTapStart()
        stack.server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "completed"),
        )
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()
        awaitContent(states)
        viewModel.onMainButtonClick()
        awaitUntil("the running session") {
            stack.sessionEngine.state.value is TestSessionState.Running
        }

        stack.clock.advanceBy(3.minutes.inWholeMilliseconds + 1_000)
        awaitEvent(events)

        assertEquals(SESSION, stack.tapSessionStore.pending)
    }

    private fun HomeStack.stubTapStart() {
        server.always(HomePaths.INTEGRITY, body = Home.integrity())
        server.always(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))
        server.always(
            HomePaths.heartbeatOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "running"),
        )
    }
}
