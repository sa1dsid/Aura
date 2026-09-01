package com.aura.feature.home.data.repository

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.core.config.AppConfig
import com.aura.core.config.AppConfigRepository
import com.aura.core.network.NetworkMonitor
import com.aura.core.session.SessionCache
import com.aura.feature.home.data.mapper.toDomain
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.BatteryOptimizationState
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private data class HomeExtras(
    val config: AppConfig,
    val nodes: NodesState?,
    val battery: BatteryOptimizationState,
)

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remote: HomeRemoteDataSource,
    private val sessionEngine: TestSessionEngine,
    private val networkMonitor: NetworkMonitor,
    private val appConfigRepository: AppConfigRepository,
    private val nodesRepository: NodesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository, SessionCache {

    private val snapshot = MutableStateFlow<DashboardDto?>(null)
    private val battery = MutableStateFlow(BatteryOptimizationState())

    override suspend fun clearSession() {
        snapshot.value = null
        battery.value = BatteryOptimizationState()
    }

    override fun observeHome(): Flow<HomeState> =
        combine(
            snapshot.filterNotNull(),
            sessionEngine.state,
            sessionEngine.spark,
            networkMonitor.status,
            extras(),
        ) { dashboard, session, spark, network, extras ->
            dashboard
                .toDomain(
                    session = session,
                    spark = spark,
                    network = network,
                    flags = extras.config.featureFlags,
                    nodes = extras.nodes,
                )
                .copy(batteryOptimization = extras.battery)
        }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            appConfigRepository.refresh()

            val dashboard = runCatchingCancellable { remote.dashboard() }.getOrNull()
                ?: return@withContext

            snapshot.value = dashboard
            battery.value = dashboard.batteryOptimization.toDomain()

            sessionEngine.syncFromDashboard(
                cooldownAvailableAt = dashboard.cooldownAvailableAt,
                sparkBalance = dashboard.sparkBalance,
            )

            nodesRepository.refresh()
        }
    }

    override suspend fun declineBatteryOptimization() {
        update { remote.declineBatteryOptimization() }
    }

    override suspend fun confirmBatteryOptimizationDisabled() {
        update { remote.confirmBatteryOptimizationDisabled() }
    }

    override suspend fun refreshBatteryOptimization() {
        update { remote.batteryOptimization() }
    }

    override suspend fun sendHeartbeat() {
        withContext(ioDispatcher) { runCatchingCancellable { remote.heartbeat() } }
    }

    override suspend fun markBonusCongratulationSeen() {
        withContext(ioDispatcher) {
            runCatchingCancellable { remote.markBonusCongratulationSeen() }
        }
        refresh()
    }

    override suspend fun markBonusTeaserSeen() {
        withContext(ioDispatcher) { runCatchingCancellable { remote.markBonusTeaserSeen() } }
        refresh()
    }

    private fun extras(): Flow<HomeExtras> = combine(
        appConfigRepository.config,
        nodesRepository.observeNodes().map<NodesState, NodesState?> { it }.onStart { emit(null) },
        battery,
        ::HomeExtras,
    )

    private suspend fun update(request: suspend () -> BatteryOptimizationDto) {
        withContext(ioDispatcher) {
            runCatchingCancellable { request() }
                .getOrNull()
                ?.let { state -> battery.value = state.toDomain() }
        }
    }
}
