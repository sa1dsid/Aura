package com.aura.feature.home.presentation

import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import com.aura.core.common.TimeSource
import com.aura.core.system.EmulatorDetector
import com.aura.feature.home.data.session.FakeHomeRemoteDataSource
import com.aura.feature.home.data.session.FakeTapSessionStore
import com.aura.feature.home.domain.usecase.ConfirmBatteryOptimizationDisabledUseCase
import com.aura.feature.home.domain.usecase.DeclineBatteryOptimizationUseCase
import com.aura.feature.home.domain.usecase.MarkBonusCongratulationSeenUseCase
import com.aura.feature.home.domain.usecase.MarkBonusTeaserSeenUseCase
import com.aura.feature.home.domain.usecase.MarkSparkCouponSeenUseCase
import com.aura.feature.home.domain.usecase.ObserveHomeStateUseCase
import com.aura.feature.home.domain.usecase.ObserveMeshStateUseCase
import com.aura.feature.home.domain.usecase.RefreshBatteryOptimizationUseCase
import com.aura.feature.home.domain.usecase.RefreshHomeUseCase
import com.aura.feature.home.domain.usecase.SendHeartbeatUseCase
import com.aura.feature.home.presentation.preview.HomePreviewData
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val REWARD_ION = 20

private val PAST_TICK = 1.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val homeRepository = FakeHomeRepository()
    private val meshRepository = FakeMeshRepository()
    private val networkMonitor = FakeNetworkMonitor()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `burns the session when the network drops`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        assertTrue(engine.state.value is TestSessionState.Running)

        networkMonitor.set(NetworkStatus(isOnline = false, isVpnActive = false))
        runCurrent()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertEquals(listOf(HomeEvent.TestInterrupted), events)
    }

    @Test
    fun `burns the session when the screen is left`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        assertTrue(engine.state.value is TestSessionState.Running)

        viewModel.onScreenLeft()
        runCurrent()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `shows the interruption toast once the screen is back`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        assertTrue(engine.state.value is TestSessionState.Running)
        viewModel.onScreenLeft()
        runCurrent()

        viewModel.onScreenResumed()
        runCurrent()

        assertEquals(listOf(HomeEvent.TestInterrupted), events)
    }

    @Test
    fun `reports the completed session`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        assertTrue(engine.state.value is TestSessionState.Running)

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(listOf(HomeEvent.TestCompleted(REWARD_ION)), events)
    }

    @Test
    fun `pauses the cooldown while a vpn is on and reports the resume`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        advanceTimeBy(3.minutes + PAST_TICK)

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = true))
        advanceTimeBy(1.minutes)

        val paused = engine.state.value as TestSessionState.Cooldown
        assertTrue(paused.isPausedByVpn)
        assertEquals(12.hours - PAST_TICK, paused.remaining)

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = false))
        runCurrent()

        assertTrue(HomeEvent.CooldownResumed in events)
    }

    @Test
    fun `beats as soon as the screen exists and keeps beating`() = homeTest {
        viewModel(watchedEngine())
        runCurrent()

        assertEquals(1, homeRepository.heartbeats)

        advanceTimeBy(5.minutes + PAST_TICK)

        assertEquals(2, homeRepository.heartbeats)
    }

    @Test
    fun `reloads the screen and the battery answer on every return`() = homeTest {
        val viewModel = viewModel(watchedEngine())
        runCurrent()

        viewModel.onScreenResumed()
        runCurrent()

        assertEquals(1, homeRepository.refreshes)
        assertEquals(1, homeRepository.batteryRefreshes)
        assertEquals(listOf(false), meshRepository.forces)
    }

    @Test
    fun `the first network reading does not count as a change`() = homeTest {
        viewModel(watchedEngine())
        runCurrent()

        assertEquals(0, homeRepository.refreshes)
    }

    @Test
    fun `reloads the screen when the network changes underneath`() = homeTest {
        viewModel(watchedEngine())
        runCurrent()

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = true))
        runCurrent()

        assertEquals(1, homeRepository.refreshes)
    }

    @Test
    fun `ignores the main button while a session is already running`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        watchedState(viewModel)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        homeRepository.set(
            homeRepository.current().copy(
                session = TestSessionState.Running(
                    remaining = 1.minutes,
                    total = 3.minutes,
                    rewardIon = REWARD_ION,
                )
            )
        )
        runCurrent()

        viewModel.onMainButtonClick()
        runCurrent()

        assertTrue(events.isEmpty())
        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `ignores the main button before the screen has anything on it`() = homeTest {
        homeRepository.set(null)
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        watchedState(viewModel)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        runCurrent()

        viewModel.onMainButtonClick()
        runCurrent()

        assertTrue(events.isEmpty())
        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `refuses the tap while a vpn is on and never asks the engine`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        watchedState(viewModel)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        val home = homeRepository.current()
        homeRepository.set(
            home.copy(
                session = TestSessionState.Ready(REWARD_ION),
                connection = home.connection.copy(isVpnActive = true),
            )
        )
        runCurrent()

        viewModel.onMainButtonClick()
        runCurrent()

        assertEquals(listOf(HomeEvent.TestRejected(TestStartRejection.VpnDetected)), events)
        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `hands the battery answers to the repository`() = homeTest {
        val viewModel = viewModel(watchedEngine())
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        runCurrent()

        viewModel.onBatteryOptimizationDeclined()
        viewModel.onBatteryOptimizationSatisfied()
        runCurrent()

        assertEquals(1, homeRepository.declines)
        assertEquals(1, homeRepository.confirms)
        assertTrue(events.isEmpty())

        viewModel.onBatteryOptimizationConfirmed()
        runCurrent()

        assertEquals(2, homeRepository.confirms)
        assertEquals(listOf(HomeEvent.BatteryOptimizationDisabled), events)
    }

    @Test
    fun `marks the bonus teaser the moment its popup opens`() = homeTest {
        val viewModel = viewModel(watchedEngine())
        viewModel.onScreenResumed()
        runCurrent()

        viewModel.onBonusTeaserOpened()
        runCurrent()

        assertEquals(1, homeRepository.bonusTeaserSeen)
    }

    @Test
    fun `marks the spark coupon once its code has been shown`() = homeTest {
        val viewModel = viewModel(watchedEngine())
        viewModel.onScreenResumed()
        runCurrent()

        viewModel.onSparkCodeSeen()
        runCurrent()

        assertEquals(1, homeRepository.sparkCouponSeen)
    }

    @Test
    fun `a completed session reloads the screen behind the toast`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.start()
        runCurrent()
        val refreshes = homeRepository.refreshes

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(listOf(HomeEvent.TestCompleted(REWARD_ION)), events)
        assertTrue(homeRepository.refreshes > refreshes)
    }

    @Test
    fun `a running session is checked before the vpn is`() = homeTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        watchedState(viewModel)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        val home = homeRepository.current()
        homeRepository.set(
            home.copy(
                session = TestSessionState.Running(
                    remaining = 1.minutes,
                    total = 3.minutes,
                    rewardIon = REWARD_ION,
                ),
                connection = home.connection.copy(isVpnActive = true),
            )
        )
        runCurrent()

        viewModel.onMainButtonClick()
        runCurrent()

        assertTrue(events.isEmpty())
    }

    private fun TestScope.watchedEngine(): TestSessionEngine {
        val engine = TestSessionEngine(
            scope = backgroundScope,
            remote = FakeHomeRemoteDataSource(scheduler = testScheduler),
            tapSessionStore = FakeTapSessionStore(),
            networkMonitor = networkMonitor,
            emulatorDetector = EmulatorDetector(),
            pingHistory = FakePingHistoryRepository(),
            timeSource = TimeSource { testScheduler.currentTime },
        )
        backgroundScope.launch { engine.state.collect { } }
        return engine
    }

    private val liveViewModels = mutableListOf<HomeViewModel>()

    private fun homeTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            liveViewModels.forEach { it.viewModelScope.cancel() }
            liveViewModels.clear()
        }
    }

    private fun viewModel(engine: TestSessionEngine) = HomeViewModel(
        observeHomeState = ObserveHomeStateUseCase(homeRepository),
        observeMeshState = ObserveMeshStateUseCase(meshRepository),
        refreshHome = RefreshHomeUseCase(homeRepository, meshRepository),
        sendHeartbeat = SendHeartbeatUseCase(homeRepository),
        declineBatteryOptimization = DeclineBatteryOptimizationUseCase(homeRepository),
        confirmBatteryOptimizationDisabled =
            ConfirmBatteryOptimizationDisabledUseCase(homeRepository),
        refreshBatteryOptimization = RefreshBatteryOptimizationUseCase(homeRepository),
        markBonusTeaserSeen = MarkBonusTeaserSeenUseCase(homeRepository),
        markBonusCongratulationSeen = MarkBonusCongratulationSeenUseCase(homeRepository),
        markSparkCouponSeen = MarkSparkCouponSeenUseCase(homeRepository),
        sessionEngine = engine,
        newsRepository = FakeNewsRepository(),
        networkMonitor = networkMonitor,
    ).also { liveViewModels += it }

    private fun TestScope.watchedState(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun TestScope.collectedEvents(viewModel: HomeViewModel): List<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    private class FakeHomeRepository : HomeRepository {

        private val state = MutableStateFlow<HomeState?>(HomePreviewData.content.home)

        var refreshes = 0
            private set
        var declines = 0
            private set
        var confirms = 0
            private set
        var batteryRefreshes = 0
            private set
        var heartbeats = 0
            private set
        var bonusTeaserSeen = 0

        var congratulationSeen = 0
            private set
        var sparkCouponSeen = 0
            private set

        fun set(home: HomeState?) {
            state.value = home
        }

        fun current(): HomeState = requireNotNull(state.value)

        override fun observeHome(): Flow<HomeState> = state.filterNotNull()

        override suspend fun refresh() {
            refreshes++
        }

        override suspend fun declineBatteryOptimization() {
            declines++
        }

        override suspend fun confirmBatteryOptimizationDisabled() {
            confirms++
        }

        override suspend fun refreshBatteryOptimization() {
            batteryRefreshes++
        }

        override suspend fun sendHeartbeat() {
            heartbeats++
        }

        override suspend fun markBonusTeaserSeen() {
            bonusTeaserSeen++
        }

        override suspend fun markBonusCongratulationSeen() {
            congratulationSeen++
        }

        override suspend fun markSparkCouponSeen() {
            sparkCouponSeen++
        }
    }

    private class FakeMeshRepository : MeshRepository {
        val forces = mutableListOf<Boolean>()

        override fun observeMesh(): Flow<MeshState> = flowOf(HomePreviewData.content.mesh)

        override suspend fun refresh(force: Boolean) {
            forces += force
        }
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        private val state = MutableStateFlow(NetworkStatus(isOnline = true, isVpnActive = false))

        override val status: StateFlow<NetworkStatus> = state

        override fun current(): NetworkStatus = state.value

        fun set(value: NetworkStatus) {
            state.value = value
        }
    }

    private class FakePingHistoryRepository : PingHistoryRepository {
        override fun observeHistory(): Flow<List<PingRecord>> = flowOf(emptyList())

        override suspend fun refresh() = Unit

        override suspend fun recordProbe(source: PingSource) = Unit

        override suspend fun record(result: SpeedTestResult, source: PingSource) = Unit
    }
}
