package com.aura.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.aura.core.api.Bodies
import com.aura.core.api.RoutingApiServer
import com.aura.core.common.TimeSource
import com.aura.core.config.AppConfigRepository
import com.aura.core.geo.City
import com.aura.core.geo.CityGazetteer
import com.aura.core.geo.UserLocationSource
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.core.network.NetworkType
import com.aura.core.system.EmulatorDetector
import com.aura.feature.home.data.local.TapSessionStore
import com.aura.feature.home.data.remote.ApiHomeRemoteDataSource
import com.aura.feature.home.data.remote.ApiMeshRemoteDataSource
import com.aura.feature.home.data.repository.HomeRepositoryImpl
import com.aura.feature.home.data.repository.MeshRepositoryImpl
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.usecase.ConfirmBatteryOptimizationDisabledUseCase
import com.aura.feature.home.domain.usecase.DeclineBatteryOptimizationUseCase
import com.aura.feature.home.domain.usecase.MarkBonusTeaserSeenUseCase
import com.aura.feature.home.domain.usecase.MarkSparkCouponSeenUseCase
import com.aura.feature.home.domain.usecase.ObserveHomeStateUseCase
import com.aura.feature.home.domain.usecase.ObserveMeshStateUseCase
import com.aura.feature.home.domain.usecase.RefreshBatteryOptimizationUseCase
import com.aura.feature.home.domain.usecase.RefreshHomeUseCase
import com.aura.feature.home.domain.usecase.SendHeartbeatUseCase
import com.aura.feature.home.presentation.HomeUiState
import com.aura.feature.home.presentation.HomeViewModel
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

internal class MutableClock(var nowMillis: Long = CLOCK_START) : TimeSource {
    override fun nowMillis(): Long = nowMillis

    fun advanceBy(millis: Long) {
        nowMillis += millis
    }

    fun isoAt(millis: Long): String =
        com.aura.feature.home.data.session.isoAt(millis)

    fun isoIn(millis: Long): String = isoAt(nowMillis + millis)
}

internal class MutableNetworkMonitor : NetworkMonitor {

    private val state = MutableStateFlow(
        NetworkStatus(isOnline = true, isVpnActive = false, type = NetworkType.WIFI)
    )

    override val status: StateFlow<NetworkStatus> = state

    override fun current(): NetworkStatus = state.value

    fun set(
        isOnline: Boolean = state.value.isOnline,
        isVpnActive: Boolean = state.value.isVpnActive,
        type: NetworkType = state.value.type,
        operator: String? = state.value.operator,
    ) {
        state.value = NetworkStatus(
            isOnline = isOnline,
            isVpnActive = isVpnActive,
            type = type,
            operator = operator,
        )
    }
}

internal class RecordingTapSessionStore : TapSessionStore {

    private var storedRate = 0

    var pending: String? = null
        private set

    val savedRates = mutableListOf<Int>()

    override suspend fun rate(): Int = storedRate

    override suspend fun saveRate(rate: Int) {
        storedRate = rate
        savedRates += rate
    }

    override suspend fun pendingSessionId(): String? = pending

    override suspend fun savePendingSessionId(sessionId: String) {
        pending = sessionId
    }

    override suspend fun clearPendingSessionId() {
        pending = null
    }
}

internal class RecordingPingHistoryRepository : PingHistoryRepository {

    val probes = Collections.synchronizedList(mutableListOf<PingSource>())

    override fun observeHistory(): Flow<List<PingRecord>> = MutableStateFlow(emptyList())

    override suspend fun refresh() = Unit

    override suspend fun recordProbe(source: PingSource) {
        probes += source
    }

    override suspend fun record(result: SpeedTestResult, source: PingSource) = Unit
}

internal class FakeNodesRepository : NodesRepository {

    private val state = MutableStateFlow<NodesState?>(null)

    var refreshes = 0
        private set

    override fun observeNodes(): Flow<NodesState> = state.filterNotNull()

    override suspend fun refresh() {
        refreshes++
    }

    fun set(nodes: NodesState) {
        state.value = nodes
    }
}

internal class HomeStack(isEmulator: Boolean = false) {

    val server = RoutingApiServer()

    val clock = MutableClock()

    private val ioDispatcher = Dispatchers.Unconfined

    private val stackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val viewModelStore = ViewModelStore()

    private val tracked = mutableListOf<ViewModel>()

    val networkMonitor = MutableNetworkMonitor()

    val tapSessionStore = RecordingTapSessionStore()

    val pingHistory = RecordingPingHistoryRepository()

    val newsRepository = FakeNewsRepository()

    val nodesRepository = FakeNodesRepository()

    val userLocationSource = UserLocationSource()

    val gazetteer: CityGazetteer = mockk()

    val emulatorDetector: EmulatorDetector = mockk()

    val knownCities = mutableMapOf(
        "Tallinn" to City(name = "Tallinn", latitude = 59.437, longitude = 24.753),
        "Rostov-na-Donu" to City(name = "Rostov-na-Donu", latitude = 47.235, longitude = 39.701),
    )

    init {
        every { emulatorDetector.isEmulator } returns isEmulator
        coEvery { gazetteer.findAll(any()) } answers {
            firstArg<List<String>>().mapNotNull(knownCities::get)
        }
        coEvery { gazetteer.find(any()) } answers {
            firstArg<String?>()?.let(knownCities::get)
        }

        server.always(HomePaths.CONFIG, body = Bodies.CONFIG)
        server.always(HomePaths.DASHBOARD, body = Home.dashboard())
        server.always(HomePaths.MESH, body = Home.mesh())
        server.always(HomePaths.HEARTBEAT, body = Home.heartbeat())
        server.always(HomePaths.LOCATION, body = Home.city())
        server.always(HomePaths.BATTERY, body = Home.battery())
    }

    val appConfigRepository = AppConfigRepository(server.api, ioDispatcher)

    val homeRemote = ApiHomeRemoteDataSource(server.api)

    val meshRemote = ApiMeshRemoteDataSource(
        home = homeRemote,
        gazetteer = gazetteer,
        userLocationSource = userLocationSource,
        networkMonitor = networkMonitor,
    )

    val sessionEngine = TestSessionEngine(
        scope = stackScope,
        remote = homeRemote,
        tapSessionStore = tapSessionStore,
        networkMonitor = networkMonitor,
        emulatorDetector = emulatorDetector,
        pingHistory = pingHistory,
        timeSource = clock,
    )

    val homeRepository = HomeRepositoryImpl(
        remote = homeRemote,
        sessionEngine = sessionEngine,
        networkMonitor = networkMonitor,
        appConfigRepository = appConfigRepository,
        nodesRepository = nodesRepository,
        ioDispatcher = ioDispatcher,
    )

    val meshRepository = MeshRepositoryImpl(
        remote = meshRemote,
        networkMonitor = networkMonitor,
        timeSource = clock,
        ioDispatcher = ioDispatcher,
    )

    val refreshHome = RefreshHomeUseCase(homeRepository, meshRepository)

    fun <T> eventsOf(flow: Flow<T>): List<T> {
        val collected = Collections.synchronizedList(mutableListOf<T>())
        stackScope.launch { flow.collect { collected += it } }
        return collected
    }

    fun homeViewModel(): HomeViewModel = track(
        "home",
        HomeViewModel(
            observeHomeState = ObserveHomeStateUseCase(homeRepository),
            observeMeshState = ObserveMeshStateUseCase(meshRepository),
            refreshHome = refreshHome,
            sendHeartbeat = SendHeartbeatUseCase(homeRepository),
            declineBatteryOptimization = DeclineBatteryOptimizationUseCase(homeRepository),
            confirmBatteryOptimizationDisabled =
                ConfirmBatteryOptimizationDisabledUseCase(homeRepository),
            refreshBatteryOptimization = RefreshBatteryOptimizationUseCase(homeRepository),
            markBonusTeaserSeen = MarkBonusTeaserSeenUseCase(homeRepository),
            markSparkCouponSeen = MarkSparkCouponSeenUseCase(homeRepository),
            sessionEngine = sessionEngine,
            newsRepository = newsRepository,
            networkMonitor = networkMonitor,
        ),
    )

    fun contentOf(viewModel: HomeViewModel): List<HomeUiState> = eventsOf(viewModel.uiState)

    suspend fun close() {
        withTimeoutOrNull(SHUTDOWN_MILLIS) {
            tracked.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
            stackScope.coroutineContext.job.cancelAndJoin()
        }
        viewModelStore.clear()
        server.shutdown()
    }

    private fun <T : ViewModel> track(key: String, viewModel: T): T {
        viewModelStore.put(key, viewModel)
        tracked += viewModel
        return viewModel
    }

    private companion object {
        const val SHUTDOWN_MILLIS = 5_000L
    }
}
