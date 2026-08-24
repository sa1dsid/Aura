package com.aura.feature.home.data.session

import com.aura.core.common.TimeSource
import com.aura.core.system.EmulatorDetector
import com.aura.feature.home.data.local.TapSessionStore
import com.aura.feature.home.domain.model.TestSessionEvent
import com.aura.feature.home.domain.model.TestSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val REWARD_ION = 20

private val PAST_TICK = 1.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TestSessionEngineTest {

    @Test
    fun `waits for a tap instead of counting down on its own`() = runTest {
        val engine = watchedEngine()

        advanceTimeBy(1.minutes)

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `tap starts a three minute session`() = runTest {
        val engine = watchedEngine()

        engine.start()
        runCurrent()

        val session = engine.state.value as TestSessionState.Running
        assertEquals(3.minutes, session.remaining)
    }

    @Test
    fun `counts the started session down second by second`() = runTest {
        val engine = watchedEngine()
        engine.start()
        runCurrent()

        advanceTimeBy(13.seconds + PAST_TICK)

        val session = engine.state.value as TestSessionState.Running
        assertEquals(2.minutes + 47.seconds, session.remaining)
    }

    @Test
    fun `locks the button for twelve hours once the session ends`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val engine = watchedEngine(remote)
        engine.start()

        advanceTimeBy(3.minutes + PAST_TICK)

        val cooldown = engine.state.value as TestSessionState.Cooldown
        assertEquals(12.hours, cooldown.remaining)
    }

    @Test
    fun `ignores a tap while the cooldown runs`() = runTest {
        val engine = watchedEngine()
        engine.start()
        advanceTimeBy(3.minutes + PAST_TICK)

        engine.start()
        runCurrent()

        assertTrue(engine.state.value is TestSessionState.Cooldown)
    }

    @Test
    fun `becomes tappable again when the cooldown ends`() = runTest {
        val engine = watchedEngine()
        engine.start()

        advanceTimeBy(3.minutes + 12.hours + PAST_TICK)

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `burns the running session and reports it`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val engine = watchedEngine(remote)
        val events = collectedEvents(engine)
        engine.start()
        advanceTimeBy(20.seconds + PAST_TICK)

        engine.interrupt()
        runCurrent()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertEquals(listOf(TestSessionEvent.Interrupted), events)
        assertTrue(remote.interruptedSessions.isNotEmpty())
    }

    @Test
    fun `keeps quiet when nothing is running`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)

        engine.interrupt()
        runCurrent()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `reports the finished session with its reward`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)
        engine.start()

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(listOf(TestSessionEvent.Completed(REWARD_ION)), events)
    }

    @Test
    fun `keeps the session alive with a heartbeat`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val engine = watchedEngine(remote)
        engine.start()

        advanceTimeBy(1.minutes)

        assertTrue(remote.heartbeats >= 10)
    }

    @Test
    fun `drops the session when the server reports it interrupted`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val engine = watchedEngine(remote)
        val events = collectedEvents(engine)
        engine.start()
        advanceTimeBy(10.seconds)

        remote.heartbeatStatus = "interrupted"
        advanceTimeBy(10.seconds)

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertTrue(TestSessionEvent.Interrupted in events)
    }

    @Test
    fun `releases a session that arrives after the screen was left`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler, startDelay = 2.seconds)
        val store = FakeTapSessionStore()
        val engine = watchedEngine(remote, store)

        engine.start()
        engine.interrupt()
        advanceTimeBy(5.seconds)

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertEquals(1, remote.interruptedSessions.size)
        assertNull(store.pendingSessionId())
    }

    @Test
    fun `freezes the cooldown while a vpn is on`() = runTest {
        val engine = watchedEngine()
        engine.start()
        advanceTimeBy(3.minutes + PAST_TICK)

        engine.onVpnChanged(true)
        runCurrent()
        val frozen = (engine.state.value as TestSessionState.Cooldown).remaining

        advanceTimeBy(1.minutes)

        val cooldown = engine.state.value as TestSessionState.Cooldown
        assertTrue(cooldown.isPausedByVpn)
        assertEquals(frozen, cooldown.remaining)
    }

    @Test
    fun `resumes the cooldown once the vpn goes off`() = runTest {
        val remote = FakeHomeRemoteDataSource(scheduler = testScheduler)
        val engine = watchedEngine(remote)
        val events = collectedEvents(engine)
        engine.start()
        advanceTimeBy(3.minutes + PAST_TICK)
        engine.onVpnChanged(true)
        advanceTimeBy(1.minutes)

        val frozen = (engine.state.value as TestSessionState.Cooldown).remaining

        remote.cooldownShift = 1.minutes.inWholeMilliseconds
        engine.onVpnChanged(false)
        runCurrent()
        advanceTimeBy(10.seconds + PAST_TICK)

        val cooldown = engine.state.value as TestSessionState.Cooldown
        val elapsed = frozen - cooldown.remaining
        assertTrue("пауза съела окно: $elapsed", elapsed in 9.seconds..11.seconds)
        assertTrue(TestSessionEvent.CooldownResumed in events)
    }

    private fun TestScope.collectedEvents(engine: TestSessionEngine): List<TestSessionEvent> {
        val events = mutableListOf<TestSessionEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.events.collect { events += it }
        }
        return events
    }

    private fun TestScope.watchedEngine(
        remote: FakeHomeRemoteDataSource = FakeHomeRemoteDataSource(scheduler = testScheduler),
        store: TapSessionStore = FakeTapSessionStore(),
    ): TestSessionEngine {
        val engine = TestSessionEngine(
            scope = backgroundScope,
            remote = remote,
            tapSessionStore = store,
            networkMonitor = FakeNetworkMonitor(),
            emulatorDetector = EmulatorDetector(),
            pingHistory = FakePingHistoryRepository(),
            timeSource = TimeSource { testScheduler.currentTime },
        )
        backgroundScope.launch { engine.state.collect { } }
        runCurrent()
        return engine
    }
}
