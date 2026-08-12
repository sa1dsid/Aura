package com.aura.feature.home.data.session

import com.aura.feature.home.domain.model.TestSessionEvent
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        engine.startTest(REWARD_ION)

        assertEquals(
            TestSessionState.Running(
                remaining = 3.minutes,
                total = 3.minutes,
                rewardIon = REWARD_ION,
            ),
            engine.state.value,
        )
    }

    @Test
    fun `counts the started session down second by second`() = runTest {
        val engine = watchedEngine()
        engine.startTest(REWARD_ION)

        advanceTimeBy(13.seconds + PAST_TICK)

        val session = engine.state.value as TestSessionState.Running
        assertEquals(2.minutes + 47.seconds, session.remaining)
    }

    @Test
    fun `locks the button for twelve hours once the session ends`() = runTest {
        val engine = watchedEngine()
        engine.startTest(REWARD_ION)

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(
            TestSessionState.Cooldown(
                remaining = 12.hours,
                total = 12.hours,
                isPausedByVpn = false,
            ),
            engine.state.value,
        )
    }

    @Test
    fun `ignores a tap while the cooldown runs`() = runTest {
        val engine = watchedEngine()
        engine.startTest(REWARD_ION)
        advanceTimeBy(3.minutes + PAST_TICK)

        engine.startTest(REWARD_ION)

        assertTrue(engine.state.value is TestSessionState.Cooldown)
    }

    @Test
    fun `becomes tappable again when the cooldown ends`() = runTest {
        val engine = watchedEngine()
        engine.startTest(REWARD_ION)

        advanceTimeBy(3.minutes + 12.hours + PAST_TICK)

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
    }

    @Test
    fun `burns the running session and reports it`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)
        engine.startTest(REWARD_ION)
        advanceTimeBy(20.seconds + PAST_TICK)

        engine.interrupt()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertEquals(listOf(TestSessionEvent.Interrupted), events)
    }

    @Test
    fun `keeps quiet when nothing is running`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)

        engine.interrupt()

        assertEquals(TestSessionState.Ready(REWARD_ION), engine.state.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `reports the finished session with its reward`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)
        engine.startTest(REWARD_ION)

        advanceTimeBy(3.minutes + PAST_TICK)

        assertEquals(listOf(TestSessionEvent.Completed(REWARD_ION)), events)
    }

    @Test
    fun `freezes the cooldown while a vpn is on`() = runTest {
        val engine = watchedEngine()
        engine.startTest(REWARD_ION)
        advanceTimeBy(3.minutes + PAST_TICK)

        engine.onVpnChanged(true)
        advanceTimeBy(1.minutes)

        val cooldown = engine.state.value as TestSessionState.Cooldown
        assertTrue(cooldown.isPausedByVpn)
        assertEquals(12.hours, cooldown.remaining)
    }

    @Test
    fun `resumes the cooldown once the vpn goes off`() = runTest {
        val engine = watchedEngine()
        val events = collectedEvents(engine)
        engine.startTest(REWARD_ION)
        advanceTimeBy(3.minutes + PAST_TICK)
        engine.onVpnChanged(true)
        advanceTimeBy(1.minutes)

        engine.onVpnChanged(false)
        advanceTimeBy(10.seconds + PAST_TICK)

        val cooldown = engine.state.value as TestSessionState.Cooldown
        assertEquals(12.hours - 10.seconds, cooldown.remaining)
        assertTrue(TestSessionEvent.CooldownResumed in events)
    }

    private fun TestScope.collectedEvents(engine: TestSessionEngine): List<TestSessionEvent> {
        val events = mutableListOf<TestSessionEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.events.collect { events += it }
        }
        return events
    }

    private fun TestScope.watchedEngine(): TestSessionEngine {
        val engine = TestSessionEngine(backgroundScope, FakePingHistoryRepository())
        backgroundScope.launch { engine.state.collect { } }
        runCurrent()
        return engine
    }
}

private class FakePingHistoryRepository : PingHistoryRepository {

    override fun observeHistory(): Flow<List<PingRecord>> = flowOf(emptyList())

    override suspend fun recordProbe() = Unit

    override suspend fun record(result: SpeedTestResult) = Unit
}
