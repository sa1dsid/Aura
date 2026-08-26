package com.aura.feature.home.data.remote

import com.aura.core.api.RoutingApiServer
import com.aura.feature.home.Home
import com.aura.feature.home.HomePaths
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SESSION = "session-1"

class HomeRemoteDataSourceTest {

    private val server = RoutingApiServer()

    private val remote = ApiHomeRemoteDataSource(server.api)

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `a tap start carries the network, the vpn and the integrity token`() = runTest {
        server.always(HomePaths.INTEGRITY, body = Home.integrity(requestHash = "hash-1"))
        server.always(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))

        remote.startTap(networkType = "wifi", vpn = true, emulator = false)

        assertEquals(
            """{"network_type":"wifi","vpn":true,"emulator":false,"integrity_token":"hash-1"}""",
            server.bodyOf(HomePaths.TAP_START),
        )
    }

    @Test
    fun `an integrity challenge the server refuses drops the field entirely`() = runTest {
        server.always(HomePaths.INTEGRITY, code = 503, body = Home.detail("unavailable"))
        server.always(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))

        remote.startTap(networkType = "mobile", vpn = false, emulator = true)

        assertEquals(
            """{"network_type":"mobile","vpn":false,"emulator":true}""",
            server.bodyOf(HomePaths.TAP_START),
        )
    }

    @Test
    fun `the integrity challenge is asked for before every start`() = runTest {
        server.always(HomePaths.INTEGRITY, body = Home.integrity())
        server.always(HomePaths.TAP_START, body = Home.tap(sessionId = SESSION))

        remote.startTap(networkType = "wifi", vpn = false, emulator = false)
        remote.startTap(networkType = "wifi", vpn = false, emulator = false)

        assertEquals(2, server.hits(HomePaths.INTEGRITY))
        assertEquals("", server.bodyOf(HomePaths.INTEGRITY))
    }

    @Test
    fun `a heartbeat names the session in the path and carries no body`() = runTest {
        server.always(
            HomePaths.heartbeatOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "running"),
        )

        val state = remote.tapHeartbeat(SESSION)

        assertEquals("running", state.status)
        assertEquals("", server.bodyOf(HomePaths.heartbeatOf(SESSION)))
    }

    @Test
    fun `the ways a session can be finished`() = runTest {
        server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(sessionId = SESSION, status = "completed"),
        )

        remote.finishTap(SESSION, interrupted = false, networkLost = false, appBackgrounded = false)
        remote.finishTap(SESSION, interrupted = true, networkLost = true, appBackgrounded = false)
        remote.finishTap(SESSION, interrupted = true, networkLost = false, appBackgrounded = true)

        assertEquals(
            listOf(
                """{"interrupted":false,"network_lost":false,"app_backgrounded":false}""",
                """{"interrupted":true,"network_lost":true,"app_backgrounded":false}""",
                """{"interrupted":true,"network_lost":false,"app_backgrounded":true}""",
            ),
            server.bodiesOf(HomePaths.finishOf(SESSION)),
        )
    }

    @Test
    fun `the earning state carries whatever it was told`() = runTest {
        server.always(HomePaths.EARNING_STATE, body = Home.earningState(paused = true))

        val state = remote.updateEarningState(vpn = true, emulator = null)

        assertTrue(state.paused)
        assertEquals("""{"vpn":true}""", server.bodyOf(HomePaths.EARNING_STATE))
    }

    @Test
    fun `a completed tap answer carries the reward and the cooldown`() = runTest {
        server.always(
            HomePaths.finishOf(SESSION),
            body = Home.tap(
                sessionId = SESSION,
                status = "completed",
                tapCount = 1,
                accruedIon = 20,
                cooldownAvailableAt = "2026-08-26T12:00:00Z",
                sparkWindowRate = 40_000,
            ),
        )

        val state = remote.finishTap(SESSION, false, false, false)

        assertEquals(1, state.tapCount)
        assertEquals(20L, state.accruedIon)
        assertEquals("2026-08-26T12:00:00Z", state.cooldownAvailableAt)
        assertEquals(40_000, state.sparkWindowRate)
    }

    @Test
    fun `the location update tells the server about the vpn and hears the city back`() = runTest {
        server.always(HomePaths.LOCATION, body = Home.city("Tallinn"))

        assertEquals("Tallinn", remote.updateLocation(vpn = true))
        assertEquals("""{"vpn":true}""", server.bodyOf(HomePaths.LOCATION))
    }

    @Test
    fun `a server that cannot place the user answers with no city`() = runTest {
        server.always(HomePaths.LOCATION, body = """{"city":null}""")

        assertNull(remote.updateLocation(vpn = false))
    }

    @Test
    fun `the mesh answer carries the cities and the counter`() = runTest {
        server.always(
            HomePaths.MESH,
            body = Home.mesh(nodesOnline = 12_048, cities = listOf("Tallinn"), stale = true),
        )

        val mesh = remote.mesh()

        assertEquals(listOf("Tallinn"), mesh.glowingCities)
        assertEquals(12_048, mesh.nodesOnline)
        assertTrue(mesh.nodesOnlineStale)
    }

    @Test
    fun `the battery endpoints each have their own address`() = runTest {
        server.always(HomePaths.BATTERY, body = Home.battery(shouldShow = true))
        server.always(HomePaths.BATTERY_DECLINE, body = Home.battery())
        server.always(HomePaths.BATTERY_DISABLED, body = Home.battery(optimizationDisabled = true))

        assertTrue(remote.batteryOptimization().shouldShow)
        assertEquals(false, remote.declineBatteryOptimization().shouldShow)
        assertTrue(remote.confirmBatteryOptimizationDisabled().optimizationDisabled)
    }

    @Test
    fun `the bonus teaser mark carries no body`() = runTest {
        server.always(HomePaths.BONUS_TEASER_SEEN, body = """{"bonus_teaser_seen":true}""")

        remote.markBonusTeaserSeen()

        assertEquals("", server.bodyOf(HomePaths.BONUS_TEASER_SEEN))
    }
}
