package com.aura.feature.network

import com.aura.feature.network.data.diagnostics.PingSample
import com.aura.feature.network.data.diagnostics.Throughput
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PING_ATTEMPTS = 12

private fun assertFractions(expected: List<Float>, actual: List<Float>) {
    assertEquals(expected.size, actual.size)
    expected.forEachIndexed { index, value -> assertEquals(value, actual[index], 0.0001f) }
}

class DiagnosticsIntegrationTest : NetworkTestCase() {

    @Test
    fun `the gauge waits before anything is asked of it`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        assertEquals(listOf(SpeedTestState.Idle), states.toList())
    }

    @Test
    fun `a test walks the gauge from nothing to a result`() = network { stack ->
        stack.throughputProbe.downloadProgress = listOf(0.5f, 1f)
        stack.throughputProbe.uploadProgress = listOf(0.5f, 1f)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val seen = watchFractions(stack)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertFractions(listOf(0f, 0.15f, 0.45f, 0.75f, 0.75f, 0.875f, 1f), seen)
    }

    @Test
    fun `the ping phase claims the first fifteen percent`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val seen = watchFractions(stack)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertEquals(0f, seen.first(), 0.0001f)
        assertEquals(0.15f, seen[1], 0.0001f)
    }

    @Test
    fun `the download phase claims the sixty percent after the ping`() = network { stack ->
        stack.throughputProbe.downloadProgress = listOf(0.25f, 0.5f, 1f)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val seen = watchFractions(stack)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertFractions(listOf(0.3f, 0.45f, 0.75f), seen.subList(2, 5))
    }

    @Test
    fun `the upload phase claims the last quarter of the gauge`() = network { stack ->
        stack.throughputProbe.uploadProgress = listOf(0.25f, 0.5f, 1f)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val seen = watchFractions(stack)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertFractions(listOf(0.8125f, 0.875f, 1f), seen.takeLast(3))
    }

    @Test
    fun `the ping is sampled twelve times`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertEquals(listOf(PING_ATTEMPTS), stack.pingProbe.requestedAttempts.toList())
    }

    @Test
    fun `a test without a network never starts`() = network { stack ->
        stack.networkMonitor.set(isOnline = false)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()

        awaitEvent(failures)
        assertEquals(listOf(SpeedTestFailure.NO_CONNECTION), failures.toList())
        assertEquals(listOf(SpeedTestState.Idle), states.toList())
        assertEquals(0, stack.pingProbe.requestedAttempts.size)
    }

    @Test
    fun `a ping that never came back gives up on the test`() = network { stack ->
        stack.pingProbe.sample = PingSample(roundTripsMs = emptyList(), attempts = PING_ATTEMPTS)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()

        awaitEvent(failures)
        assertEquals(listOf(SpeedTestFailure.INTERRUPTED), failures.toList())
        assertEquals(SpeedTestState.Idle, states.last())
        assertEquals(0, stack.throughputProbe.downloads)
    }

    @Test
    fun `a download that never came back gives up on the test`() = network { stack ->
        stack.throughputProbe.downloadResult = null
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()

        awaitEvent(failures)
        assertEquals(listOf(SpeedTestFailure.INTERRUPTED), failures.toList())
        assertEquals(SpeedTestState.Idle, states.last())
        assertEquals(0, stack.throughputProbe.uploads)
    }

    @Test
    fun `an upload that never came back gives up on the test`() = network { stack ->
        stack.throughputProbe.uploadResult = null
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()

        awaitEvent(failures)
        assertEquals(listOf(SpeedTestFailure.INTERRUPTED), failures.toList())
        assertEquals(SpeedTestState.Idle, states.last())
    }

    @Test
    fun `a network lost mid test is blamed on the network`() = network { stack ->
        stack.throughputProbe.downloadResult = null
        stack.throughputProbe.holdsDownload = true
        val failures = stack.eventsOf(stack.speedTestEngine.failures)
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.networkMonitor.set(isOnline = false)
        stack.throughputProbe.releaseDownload()

        awaitEvent(failures)
        assertEquals(listOf(SpeedTestFailure.NO_CONNECTION), failures.toList())
    }

    @Test
    fun `a failed test writes nothing to the journal`() = network { stack ->
        stack.throughputProbe.downloadResult = null
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()
        awaitEvent(failures)

        assertEquals(0, stack.server.hits(NetworkPaths.MEASUREMENTS, NetworkPaths.POST))
    }

    @Test
    fun `a second start while the test runs is ignored`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.speedTestEngine.start()

        assertEquals(1, stack.throughputProbe.downloads)
        stack.throughputProbe.releaseDownload()
    }

    @Test
    fun `cancelling puts the gauge back to waiting`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val states = stack.eventsOf(stack.speedTestEngine.state)
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.speedTestEngine.cancel()

        assertEquals(SpeedTestState.Idle, states.last())
    }

    @Test
    fun `a cancelled test writes nothing to the journal`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.speedTestEngine.cancel()
        stack.throughputProbe.releaseDownload()

        assertEquals(0, stack.server.hits(NetworkPaths.MEASUREMENTS, NetworkPaths.POST))
    }

    @Test
    fun `a cancelled test complains about nothing`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val failures = stack.eventsOf(stack.speedTestEngine.failures)
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.speedTestEngine.cancel()

        assertTrue(failures.isEmpty())
    }

    @Test
    fun `closing the session cancels a running test`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val states = stack.eventsOf(stack.speedTestEngine.state)
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()

        stack.speedTestEngine.clearSession()

        assertEquals(SpeedTestState.Idle, states.last())
    }

    @Test
    fun `a cancelled test can be started again`() = network { stack ->
        stack.throughputProbe.holdsDownload = true
        val states = stack.eventsOf(stack.speedTestEngine.state)
        stack.speedTestEngine.start()
        stack.throughputProbe.awaitDownloadStarted()
        stack.speedTestEngine.cancel()

        stack.throughputProbe.holdsDownload = false
        stack.throughputProbe.releaseDownload()
        stack.speedTestEngine.start()

        awaitDiagnostics(states, "the second result") { it is SpeedTestState.Done }
    }

    @Test
    fun `a finished test can be run again`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)
        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the first result") { it is SpeedTestState.Done }

        stack.throughputProbe.downloadResult = Throughput(90.0, 0)
        stack.speedTestEngine.start()

        awaitDiagnostics(states, "the second result") {
            (it as? SpeedTestState.Done)?.result?.downloadMbps == 90.0
        }
    }

    @Test
    fun `the result is rounded to one decimal`() = network { stack ->
        stack.pingProbe.sample = sampleOf(20.0, 30.0, attempts = 3)
        stack.throughputProbe.downloadResult = Throughput(48.63, 0)
        stack.throughputProbe.uploadResult = Throughput(12.44, 0)
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()

        val result = resultOf(awaitDiagnostics(states, "the result") { it is SpeedTestState.Done })

        assertEquals(48.6, result.downloadMbps, 0.0001)
        assertEquals(12.4, result.uploadMbps, 0.0001)
        assertEquals(33.3, result.packetLossPercent, 0.0001)
        assertEquals(20, result.pingMs)
        assertEquals(5, result.jitterMs)
    }

    @Test
    fun `the grade is judged on the raw numbers and not the rounded ones`() = network { stack ->
        stack.pingProbe.sample = sampleOf(92.0, 180.0)
        stack.throughputProbe.downloadResult = Throughput(24.96, 0)
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()

        val result = resultOf(awaitDiagnostics(states, "the result") { it is SpeedTestState.Done })

        assertEquals(25.0, result.downloadMbps, 0.0001)
        assertEquals(ConnectionGrade.POOR, result.grade)
    }

    @Test
    fun `a ping sampled only once reports no jitter at all`() = network { stack ->
        stack.pingProbe.sample = sampleOf(24.0)
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()

        val result = resultOf(awaitDiagnostics(states, "the result") { it is SpeedTestState.Done })

        assertEquals(0, result.jitterMs)
    }

    @Test
    fun `a clean fast link is graded excellent`() = network { stack ->
        stack.pingProbe.sample = sampleOf(14.0, 16.0, 15.0, 17.0)
        stack.throughputProbe.downloadResult = Throughput(96.0, 0)
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()

        val result = resultOf(awaitDiagnostics(states, "the result") { it is SpeedTestState.Done })

        assertEquals(ConnectionGrade.EXCELLENT, result.grade)
    }

    @Test
    fun `the result goes to the journal as a diagnostic`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        assertTrue(
            stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)
                .contains("\"source\":\"diagnostic\"")
        )
    }

    @Test
    fun `the diagnostic body carries every number of the test`() = network { stack ->
        stack.pingProbe.sample = sampleOf(20.0, 30.0, attempts = 3)
        stack.throughputProbe.downloadResult = Throughput(48.63, 0)
        stack.throughputProbe.uploadResult = Throughput(12.44, 0)
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        val body = stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        assertTrue(body.contains("\"ping_ms\":20.0"))
        assertTrue(body.contains("\"jitter_ms\":5.0"))
        assertTrue(body.contains("\"packet_loss_pct\":33.3"))
        assertTrue(body.contains("\"download_mbps\":48.6"))
        assertTrue(body.contains("\"upload_mbps\":12.4"))
    }

    @Test
    fun `a written result stays on the gauge`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        assertTrue(states.last() is SpeedTestState.Done)
    }

    @Test
    fun `the measured record lands in the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(pingMs = "20"),
            method = NetworkPaths.POST,
        )
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }

        assertEquals(listOf(20), awaitJournal(stack, "the record") { it.isNotEmpty() }.map { it.pingMs })
    }

    @Test
    fun `a run touches nothing outside the network endpoints`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        stack.speedTestEngine.start()
        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        assertTrue(stack.server.paths().all { it.startsWith("/api/v1/network/") })
    }

    @Test
    fun `three runs in a row are all allowed`() = network { stack ->
        val states = stack.eventsOf(stack.speedTestEngine.state)

        for (run in 1..3) {
            stack.throughputProbe.downloadResult = Throughput(40.0 + run, 0)
            stack.speedTestEngine.start()
            awaitDiagnostics(states, "run number $run") { state ->
                (state as? SpeedTestState.Done)?.result?.downloadMbps == 40.0 + run
            }
        }

        assertEquals(3, stack.throughputProbe.downloads)
    }

    @Test
    fun `a test behind a vpn runs all the same`() = network { stack ->
        stack.networkMonitor.set(isVpnActive = true)
        val states = stack.eventsOf(stack.speedTestEngine.state)
        val failures = stack.eventsOf(stack.speedTestEngine.failures)

        stack.speedTestEngine.start()

        awaitDiagnostics(states, "the result") { it is SpeedTestState.Done }
        assertTrue(failures.isEmpty())
    }

    private fun resultOf(state: SpeedTestState): SpeedTestResult =
        (state as SpeedTestState.Done).result

    private fun watchFractions(stack: NetworkStack): List<Float> {
        val seen = mutableListOf<Float>()
        val watch = {
            val running = stack.speedTestEngine.state.value as? SpeedTestState.Running
            if (running != null) seen += running.progress
        }
        stack.pingProbe.onStep = watch
        stack.throughputProbe.onStep = watch
        return seen
    }
}
