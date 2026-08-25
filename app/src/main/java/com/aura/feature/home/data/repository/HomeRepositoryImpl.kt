package com.aura.feature.home.data.repository

import com.aura.core.api.dto.DashboardDto
import com.aura.core.common.IoDispatcher
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
import kotlinx.coroutines.CancellationException
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

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remote: HomeRemoteDataSource,
    private val sessionEngine: TestSessionEngine,
    private val networkMonitor: NetworkMonitor,
    private val appConfigRepository: AppConfigRepository,
    private val nodesRepository: NodesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
            combine(
                appConfigRepository.config,
                nodesRepository.observeNodes().map<NodesState, NodesState?> { it }.onStart { emit(null) },
                battery,
            ) { config, nodes, batteryState -> Triple(config, nodes, batteryState) },
        ) { dashboard, session, spark, network, extras ->
            val (config, nodes, batteryState) = extras

            dashboard
                .toDomain(
                    session = session,
                    spark = spark,
                    network = network,
                    flags = config.featureFlags,
                    nodes = nodes,
                )
                .copy(batteryOptimization = batteryState)
        }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            appConfigRepository.refresh()

            val dashboard = try {
                remote.dashboard()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                null
            } ?: return@withContext

            snapshot.value = dashboard
            battery.value = BatteryOptimizationState(
                shouldShow = dashboard.batteryOptimization.shouldShow,
                isDisabled = dashboard.batteryOptimization.optimizationDisabled,
            )

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
        withContext(ioDispatcher) {
            runCatching { remote.heartbeat() }
        }
    }

    override suspend fun markBonusTeaserSeen() {
        withContext(ioDispatcher) {
            runCatching { remote.markBonusTeaserSeen() }
        }
        refresh()
    }

    private suspend fun update(request: suspend () -> com.aura.core.api.dto.BatteryOptimizationDto) {
        withContext(ioDispatcher) {
            val state = try {
                request()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                return@withContext
            }

            battery.value = BatteryOptimizationState(
                shouldShow = state.shouldShow,
                isDisabled = state.optimizationDisabled,
            )
        }
    }
}
