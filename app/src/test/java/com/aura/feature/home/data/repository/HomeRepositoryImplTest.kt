package com.aura.feature.home.data.repository

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.EarningStateDto
import com.aura.core.api.dto.HeartbeatDto
import com.aura.core.api.dto.MeshDto
import com.aura.core.api.dto.NodeStatusDto
import com.aura.core.api.dto.TapStateDto
import com.aura.core.config.AppConfigRepository
import com.aura.core.network.NetworkStatus
import com.aura.core.network.NetworkType
import com.aura.feature.home.MutableNetworkMonitor
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.SparkWindow
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val REWARD_ION = 20

class HomeRepositoryImplTest {

    private val remote = RecordingHomeRemote()

    private val networkMonitor = MutableNetworkMonitor()

    private val nodesRepository = SilentNodesRepository()

    private val sessionState = MutableStateFlow<TestSessionState>(
        TestSessionState.Ready(REWARD_ION)
    )

    private val sparkState = MutableStateFlow(SparkWindow())

    private val syncedDashboards = mutableListOf<Pair<String?, String>>()

    private val sessionEngine: TestSessionEngine = mockk(relaxed = true)

    private val api = mockk<com.aura.core.api.AuraApi>(relaxed = true)

    private fun repository() = HomeRepositoryImpl(
        remote = remote,
        sessionEngine = sessionEngine,
        networkMonitor = networkMonitor,
        appConfigRepository = AppConfigRepository(api, Dispatchers.Unconfined),
        nodesRepository = nodesRepository,
        ioDispatcher = Dispatchers.Unconfined,
    )

    init {
        every { sessionEngine.state } returns sessionState
        every { sessionEngine.spark } returns sparkState
        coEvery { sessionEngine.syncFromDashboard(any(), any()) } answers {
            syncedDashboards += firstArg<String?>() to secondArg<String>()
        }
    }

    @Test
    fun `the screen shows nothing at all before the first load`() = runTest {
        val state = withTimeoutOrNull(200) { repository().observeHome().first() }

        assertNull(state)
    }

    @Test
    fun `a loaded dashboard becomes the home screen`() = runTest {
        remote.dashboard = DashboardDto(accruedIon = 140, tapCount = 7)
        val repository = repository()

        repository.refresh()
        val home = repository.observeHome().first()

        assertEquals(140L, home.balances.accrued)
        assertEquals(7, home.tapCount)
        assertEquals(NodeTier.IDLE_NODE, home.nodeStatus.currentTier)
    }

    @Test
    fun `loading tells the session engine what the server knows`() = runTest {
        remote.dashboard = DashboardDto(
            cooldownAvailableAt = "2026-08-26T12:00:00Z",
            sparkBalance = "1234.5",
        )

        repository().refresh()

        assertEquals(listOf("2026-08-26T12:00:00Z" to "1234.5"), syncedDashboards)
    }

    @Test
    fun `loading the screen also reloads the nodes feature`() = runTest {
        repository().refresh()

        assertEquals(1, nodesRepository.refreshes)
    }

    @Test
    fun `a dashboard the server refuses leaves the screen as it was`() = runTest {
        remote.dashboard = DashboardDto(accruedIon = 140, tapCount = 7)
        val repository = repository()
        repository.refresh()
        remote.dashboardError = IOException("offline")

        repository.refresh()

        val home = repository.observeHome().first()
        assertEquals(7, home.tapCount)
        assertEquals(140L, home.balances.accrued)
        assertEquals(1, nodesRepository.refreshes)
    }

    @Test
    fun `a cancelled load is not swallowed`() {
        remote.dashboardError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().refresh() }
        }
    }

    @Test
    fun `the live network reaches the connection badge`() = runTest {
        val repository = repository()
        repository.refresh()

        networkMonitor.set(isVpnActive = true, type = NetworkType.MOBILE_4G)

        val connection = repository.observeHome().first().connection
        assertEquals(NetworkType.MOBILE_4G, connection.networkType)
        assertTrue(connection.isVpnActive)
    }

    @Test
    fun `the running session reaches the main button`() = runTest {
        val repository = repository()
        repository.refresh()

        sessionState.value = TestSessionState.Cooldown(
            remaining = kotlin.time.Duration.ZERO,
            total = kotlin.time.Duration.ZERO,
            isPausedByVpn = true,
        )

        assertTrue(repository.observeHome().first().session is TestSessionState.Cooldown)
    }

    @Test
    fun `the battery answer of the dashboard opens the popup`() = runTest {
        remote.dashboard = DashboardDto(
            batteryOptimization = BatteryOptimizationDto(shouldShow = true)
        )
        val repository = repository()

        repository.refresh()

        assertTrue(repository.observeHome().first().batteryOptimization.shouldShow)
    }

    @Test
    fun `refusing the popup closes it`() = runTest {
        remote.dashboard = DashboardDto(
            batteryOptimization = BatteryOptimizationDto(shouldShow = true)
        )
        remote.battery = BatteryOptimizationDto(shouldShow = false)
        val repository = repository()
        repository.refresh()

        repository.declineBatteryOptimization()

        assertFalse(repository.observeHome().first().batteryOptimization.shouldShow)
        assertEquals(1, remote.declines)
    }

    @Test
    fun `confirming the optimisation is off marks it disabled`() = runTest {
        remote.battery = BatteryOptimizationDto(shouldShow = false, optimizationDisabled = true)
        val repository = repository()
        repository.refresh()

        repository.confirmBatteryOptimizationDisabled()

        assertTrue(repository.observeHome().first().batteryOptimization.isDisabled)
        assertEquals(1, remote.confirms)
    }

    @Test
    fun `a battery answer the server refuses changes nothing`() = runTest {
        remote.dashboard = DashboardDto(
            batteryOptimization = BatteryOptimizationDto(shouldShow = true)
        )
        val repository = repository()
        repository.refresh()
        remote.batteryError = httpError(500)

        repository.declineBatteryOptimization()

        assertTrue(repository.observeHome().first().batteryOptimization.shouldShow)
    }

    @Test
    fun `a cancelled battery answer is not swallowed`() {
        remote.batteryError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().refreshBatteryOptimization() }
        }
    }

    @Test
    fun `a heartbeat the server drops is never reported to the screen`() = runTest {
        remote.heartbeatError = IOException("offline")

        repository().sendHeartbeat()

        assertEquals(1, remote.heartbeats)
    }

    @Test
    fun `opening the bonus popup marks it and reloads the screen`() = runTest {
        val repository = repository()

        repository.markBonusTeaserSeen()

        assertEquals(1, remote.bonusTeaserSeen)
        assertEquals(1, remote.dashboards)
    }

    @Test
    fun `a bonus mark the server refuses still reloads the screen`() = runTest {
        remote.bonusTeaserError = IOException("offline")
        val repository = repository()

        repository.markBonusTeaserSeen()

        assertEquals(1, remote.dashboards)
    }

    @Test
    fun `seeing the spark code marks the coupon and reloads the screen`() = runTest {
        val repository = repository()

        repository.markSparkCouponSeen()

        assertEquals(1, remote.sparkCouponSeen)
        assertEquals(1, remote.dashboards)
    }

    @Test
    fun `a spark mark the server refuses still reloads the screen`() = runTest {
        remote.sparkCouponError = IOException("offline")
        val repository = repository()

        repository.markSparkCouponSeen()

        assertEquals(1, remote.dashboards)
    }

    @Test
    fun `logging out empties the screen`() = runTest {
        val repository = repository()
        repository.refresh()
        assertEquals(0L, repository.observeHome().first().balances.accrued)

        repository.clearSession()

        assertNull(withTimeoutOrNull(200) { repository.observeHome().first() })
    }

    private fun httpError(code: Int, body: String = "{}") = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )

    private class SilentNodesRepository : NodesRepository {
        var refreshes = 0
            private set

        override fun observeNodes(): Flow<NodesState> = flow { }

        override suspend fun refresh() {
            refreshes++
        }
    }

    private class RecordingHomeRemote : HomeRemoteDataSource {

        var dashboard = DashboardDto(node = NodeStatusDto())
        var battery = BatteryOptimizationDto()

        var dashboardError: Throwable? = null
        var batteryError: Throwable? = null
        var heartbeatError: Throwable? = null
        var bonusTeaserError: Throwable? = null
        var sparkCouponError: Throwable? = null

        var dashboards = 0
        var heartbeats = 0
        var declines = 0
        var confirms = 0
        var bonusTeaserSeen = 0
        var sparkCouponSeen = 0

        var congratulationSeen = 0

        override suspend fun dashboard(): DashboardDto {
            dashboards++
            dashboardError?.let { throw it }
            return dashboard
        }

        override suspend fun startTap(
            networkType: String,
            vpn: Boolean,
            emulator: Boolean,
        ): TapStateDto = throw UnsupportedOperationException()

        override suspend fun tapHeartbeat(sessionId: String): TapStateDto =
            throw UnsupportedOperationException()

        override suspend fun finishTap(
            sessionId: String,
            interrupted: Boolean,
            networkLost: Boolean,
            appBackgrounded: Boolean,
        ): TapStateDto = throw UnsupportedOperationException()

        override suspend fun updateEarningState(vpn: Boolean?, emulator: Boolean?): EarningStateDto =
            throw UnsupportedOperationException()

        override suspend fun batteryOptimization(): BatteryOptimizationDto {
            batteryError?.let { throw it }
            return battery
        }

        override suspend fun declineBatteryOptimization(): BatteryOptimizationDto {
            declines++
            batteryError?.let { throw it }
            return battery
        }

        override suspend fun confirmBatteryOptimizationDisabled(): BatteryOptimizationDto {
            confirms++
            batteryError?.let { throw it }
            return battery
        }

        override suspend fun heartbeat(): HeartbeatDto {
            heartbeats++
            heartbeatError?.let { throw it }
            return HeartbeatDto()
        }

        override suspend fun updateLocation(vpn: Boolean): String? = null

        override suspend fun mesh(): MeshDto = MeshDto()

        override suspend fun markBonusTeaserSeen() {
            bonusTeaserSeen++
            bonusTeaserError?.let { throw it }
        }

        override suspend fun markBonusCongratulationSeen() {
            congratulationSeen++
        }

        override suspend fun markSparkCouponSeen() {
            sparkCouponSeen++
            sparkCouponError?.let { throw it }
        }
    }
}
