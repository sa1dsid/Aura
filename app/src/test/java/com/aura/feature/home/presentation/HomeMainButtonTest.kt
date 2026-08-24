package com.aura.feature.home.presentation

import androidx.lifecycle.viewModelScope
import com.aura.core.common.TimeSource
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.core.network.NetworkType
import com.aura.core.system.EmulatorDetector
import com.aura.feature.home.data.session.FakeHomeRemoteDataSource
import com.aura.feature.home.data.session.FakeTapSessionStore
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import com.aura.feature.home.domain.usecase.ConfirmBatteryOptimizationDisabledUseCase
import com.aura.feature.home.domain.usecase.DeclineBatteryOptimizationUseCase
import com.aura.feature.home.domain.usecase.MarkBonusTeaserSeenUseCase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val SETTLE = 2.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeMainButtonTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val homeRepository = SettableHomeRepository()
    private val networkMonitor = FakeNetworkMonitor()
    private val liveViewModels = mutableListOf<HomeViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a tap during the cooldown answers with the not ready toast`() = homeTest {
        homeRepository.set(cooldown(11.hours))
        val viewModel = viewModel(engine())
        val events = collectedEvents(viewModel)
        watch(viewModel)
        viewModel.onScreenResumed()
        settle()

        viewModel.onMainButtonClick()
        settle()

        val rejection = events.filterIsInstance<HomeEvent.TestRejected>().firstOrNull()
        assertEquals(
            TestStartRejection.CooldownNotFinished(11.hours),
            rejection?.rejection,
        )
    }

    @Test
    fun `a tap during the cooldown never opens a session`() = homeTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        homeRepository.set(cooldown(11.hours))
        val viewModel = viewModel(engine(remote))
        watch(viewModel)
        viewModel.onScreenResumed()
        settle()

        viewModel.onMainButtonClick()
        settle()

        assertTrue(remote.startedSessions.isEmpty())
    }

    @Test
    fun `a tap while a test runs is ignored`() = homeTest {
        homeRepository.set(
            HomePreviewData.content.home.copy(
                session = TestSessionState.Running(
                    remaining = 2.hours,
                    total = 3.hours,
                    rewardIon = 20,
                )
            )
        )
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val viewModel = viewModel(engine(remote))
        watch(viewModel)
        viewModel.onScreenResumed()
        settle()

        viewModel.onMainButtonClick()
        settle()

        assertTrue(remote.startedSessions.isEmpty())
    }

    private fun homeTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            liveViewModels.forEach { it.viewModelScope.cancel() }
            liveViewModels.clear()
        }
    }

    private fun TestScope.watch(viewModel: HomeViewModel) {
        liveViewModels += viewModel
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun TestScope.settle() {
        advanceTimeBy(SETTLE)
        runCurrent()
    }

    private fun cooldown(remaining: Duration): HomeState =
        HomePreviewData.content.home.copy(
            session = TestSessionState.Cooldown(
                remaining = remaining,
                total = 12.hours,
                isPausedByVpn = false,
            ),
        )

    private fun TestScope.engine(
        remote: FakeHomeRemoteDataSource = FakeHomeRemoteDataSource(scheduler = testScheduler),
    ): TestSessionEngine {
        val engine = TestSessionEngine(
            scope = backgroundScope,
            remote = remote,
            tapSessionStore = FakeTapSessionStore(),
            networkMonitor = networkMonitor,
            emulatorDetector = EmulatorDetector(),
            pingHistory = NoPingHistory(),
            timeSource = TimeSource { testScheduler.currentTime },
        )
        backgroundScope.launch { engine.state.collect { } }
        return engine
    }

    private fun viewModel(engine: TestSessionEngine) = HomeViewModel(
        observeHomeState = ObserveHomeStateUseCase(homeRepository),
        observeMeshState = ObserveMeshStateUseCase(StaticMeshRepository()),
        refreshHome = RefreshHomeUseCase(homeRepository, StaticMeshRepository()),
        sendHeartbeat = SendHeartbeatUseCase(homeRepository),
        declineBatteryOptimization = DeclineBatteryOptimizationUseCase(homeRepository),
        confirmBatteryOptimizationDisabled =
            ConfirmBatteryOptimizationDisabledUseCase(homeRepository),
        refreshBatteryOptimization = RefreshBatteryOptimizationUseCase(homeRepository),
        markBonusTeaserSeen = MarkBonusTeaserSeenUseCase(homeRepository),
        sessionEngine = engine,
        newsRepository = FakeNewsRepository(),
        networkMonitor = networkMonitor,
    )

    private fun TestScope.collectedEvents(viewModel: HomeViewModel): List<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    private class SettableHomeRepository : HomeRepository {

        private val state = MutableStateFlow(HomePreviewData.content.home)

        fun set(value: HomeState) {
            state.value = value
        }

        override fun observeHome(): Flow<HomeState> = state

        override suspend fun refresh() = Unit

        override suspend fun declineBatteryOptimization() = Unit

        override suspend fun confirmBatteryOptimizationDisabled() = Unit

        override suspend fun refreshBatteryOptimization() = Unit

        override suspend fun sendHeartbeat() = Unit

        override suspend fun markBonusTeaserSeen() = Unit
    }

    private class StaticMeshRepository : MeshRepository {
        override fun observeMesh(): Flow<MeshState> = flowOf(HomePreviewData.content.mesh)

        override suspend fun refresh(force: Boolean) = Unit
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        private val state = MutableStateFlow(
            NetworkStatus(isOnline = true, isVpnActive = false, type = NetworkType.WIFI)
        )

        override val status: StateFlow<NetworkStatus> = state

        override fun current(): NetworkStatus = state.value
    }

    private class NoPingHistory : PingHistoryRepository {
        override fun observeHistory(): Flow<List<PingRecord>> = flowOf(emptyList())

        override suspend fun refresh() = Unit

        override suspend fun recordProbe(source: PingSource) = Unit

        override suspend fun record(result: SpeedTestResult, source: PingSource) = Unit
    }
}
