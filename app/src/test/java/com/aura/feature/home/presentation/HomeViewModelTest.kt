package com.aura.feature.home.presentation

import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import com.aura.feature.home.domain.usecase.CreditTestRewardUseCase
import com.aura.feature.home.domain.usecase.ObserveHomeStateUseCase
import com.aura.feature.home.domain.usecase.ObserveMeshStateUseCase
import com.aura.feature.home.domain.usecase.RefreshHomeUseCase
import com.aura.feature.home.presentation.preview.HomePreviewData
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
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
    fun `burns the session when the network drops`() = runTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.startTest(REWARD_ION)

        networkMonitor.set(NetworkStatus(isOnline = false, isVpnActive = false))

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertEquals(listOf(HomeEvent.TestInterrupted), events)
    }

    @Test
    fun `burns the session when the screen is left`() = runTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.startTest(REWARD_ION)

        viewModel.onScreenLeft()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `shows the interruption toast once the screen is back`() = runTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.startTest(REWARD_ION)
        viewModel.onScreenLeft()

        viewModel.onScreenResumed()

        assertEquals(listOf(HomeEvent.TestInterrupted), events)
    }

    @Test
    fun `credits the reward when the session completes`() = runTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.startTest(REWARD_ION)

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(REWARD_ION, homeRepository.creditedIon)
        assertEquals(listOf(HomeEvent.TestCompleted(REWARD_ION)), events)
    }

    @Test
    fun `pauses the cooldown while a vpn is on and reports the resume`() = runTest {
        val engine = watchedEngine()
        val viewModel = viewModel(engine)
        val events = collectedEvents(viewModel)
        viewModel.onScreenResumed()
        engine.startTest(REWARD_ION)
        advanceTimeBy(3.minutes + PAST_TICK)

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = true))
        advanceTimeBy(1.minutes)

        val paused = engine.state.value as TestSessionState.Cooldown
        assertTrue(paused.isPausedByVpn)
        assertEquals(12.hours, paused.remaining)

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = false))

        assertTrue(HomeEvent.CooldownResumed in events)
    }

    private fun TestScope.watchedEngine(): TestSessionEngine {
        val engine = TestSessionEngine(backgroundScope, FakePingHistoryRepository())
        backgroundScope.launch { engine.state.collect { } }
        return engine
    }

    private fun viewModel(engine: TestSessionEngine) = HomeViewModel(
        observeHomeState = ObserveHomeStateUseCase(homeRepository),
        observeMeshState = ObserveMeshStateUseCase(meshRepository),
        refreshHome = RefreshHomeUseCase(homeRepository, meshRepository),
        creditTestReward = CreditTestRewardUseCase(homeRepository),
        sessionEngine = engine,
        networkMonitor = networkMonitor,
    )

    private fun TestScope.collectedEvents(viewModel: HomeViewModel): List<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    private class FakeHomeRepository : HomeRepository {
        var creditedIon = 0

        override fun observeHome(): Flow<HomeState> = flowOf(HomePreviewData.content.home)

        override suspend fun refresh() = Unit

        override suspend fun creditTestReward(amount: Int) {
            creditedIon += amount
        }
    }

    private class FakeMeshRepository : MeshRepository {
        override fun observeMesh(): Flow<MeshState> = flowOf(HomePreviewData.content.mesh)

        override suspend fun refresh(force: Boolean) = Unit
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

        override suspend fun recordProbe() = Unit

        override suspend fun record(result: SpeedTestResult) = Unit
    }
}
