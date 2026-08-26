package com.aura.feature.network

import com.aura.feature.network.data.diagnostics.PingSample
import com.aura.feature.network.data.diagnostics.Throughput
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.presentation.NetworkEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkActionsIntegrationTest : NetworkTestCase() {

    @Test
    fun `the start button runs the test`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onStartTestClick()

        awaitScreen(states, "the result") { it.diagnostics is SpeedTestState.Done }
    }

    @Test
    fun `the screen keeps the gauge busy while the test runs`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onStartTestClick()

        awaitScreen(states, "the running gauge") { it.diagnostics is SpeedTestState.Running }
        stack.throughputProbe.releaseDownload()
    }

    @Test
    fun `a finished test puts the whole result on the screen`() = network { stack ->
        stack.pingProbe.sample = sampleOf(20.0, 30.0, attempts = 3)
        stack.throughputProbe.downloadResult = Throughput(48.63, 0)
        stack.throughputProbe.uploadResult = Throughput(12.44, 0)
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onStartTestClick()

        val done = awaitScreen(states, "the result") { it.diagnostics is SpeedTestState.Done }
        val result = (done.diagnostics as SpeedTestState.Done).result

        assertEquals(48.6, result.downloadMbps, 0.0001)
        assertEquals(12.4, result.uploadMbps, 0.0001)
        assertEquals(20, result.pingMs)
    }

    @Test
    fun `a failed test leaves the gauge waiting on the screen`() = network { stack ->
        stack.throughputProbe.downloadResult = null
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onStartTestClick()
        awaitEvent(events)

        assertEquals(SpeedTestState.Idle, awaitContent(states).diagnostics)
    }

    @Test
    fun `a test without a network complains on the screen`() = network { stack ->
        stack.networkMonitor.set(isOnline = false)
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onStartTestClick()

        awaitEvent(events)
        assertEquals(
            listOf(NetworkEvent.DiagnosticsFailed(SpeedTestFailure.NO_CONNECTION)),
            events.toList(),
        )
    }

    @Test
    fun `a broken test complains on the screen`() = network { stack ->
        stack.pingProbe.sample = PingSample(roundTripsMs = emptyList(), attempts = 12)
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onStartTestClick()

        awaitEvent(events)
        assertEquals(
            listOf(NetworkEvent.DiagnosticsFailed(SpeedTestFailure.INTERRUPTED)),
            events.toList(),
        )
    }

    @Test
    fun `sharing before a result does nothing`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onShareResultClick()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `sharing while the test runs does nothing`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)
        viewModel.onStartTestClick()
        awaitScreen(states, "the running gauge") { it.diagnostics is SpeedTestState.Running }

        viewModel.onShareResultClick()

        assertTrue(events.isEmpty())
        stack.throughputProbe.releaseDownload()
    }

    @Test
    fun `sharing a result hands over the whole measurement`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)
        viewModel.onStartTestClick()
        val done = awaitScreen(states, "the result") { it.diagnostics is SpeedTestState.Done }

        viewModel.onShareResultClick()

        awaitEvent(events)
        assertEquals(
            listOf(NetworkEvent.ShareResult((done.diagnostics as SpeedTestState.Done).result)),
            events.toList(),
        )
    }

    @Test
    fun `the vpn card asks for the system settings`() = network { stack ->
        stack.networkMonitor.set(isVpnActive = true)
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onVpnCardClick()

        awaitEvent(events)
        assertEquals(listOf(NetworkEvent.OpenVpnSettings), events.toList())
    }

    @Test
    fun `the vpn card asks for settings even when no vpn is up`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        val events = stack.eventsOf(viewModel)
        awaitContent(states)

        viewModel.onVpnCardClick()

        awaitEvent(events)
        assertEquals(listOf(NetworkEvent.OpenVpnSettings), events.toList())
    }

    @Test
    fun `a diagnostic run writes its record into the journal on the screen`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(pingMs = "24"),
            method = NetworkPaths.POST,
        )
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onStartTestClick()

        val history = awaitHistory(states, "the new record") { it.isNotEmpty() }

        assertEquals(listOf(24), history.map { it.pingMs })
    }

    @Test
    fun `a test the user starts twice runs once`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)
        viewModel.onStartTestClick()
        awaitScreen(states, "the running gauge") { it.diagnostics is SpeedTestState.Running }

        viewModel.onStartTestClick()

        assertEquals(1, stack.throughputProbe.downloads)
        stack.throughputProbe.releaseDownload()
    }
}
