package com.aura.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PressedStateTest {

    @Test
    fun `быстрый тап держит нажатие до конца минимального времени`() = runTest {
        val source = MutableInteractionSource()
        val states = mutableListOf<Boolean>()

        val collector = launch {
            source.collectPressedState(
                minPressMillis = MIN_PRESS,
                currentTimeMillis = { testScheduler.currentTime },
            ) { states += it }
        }
        runCurrent()

        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        source.emit(PressInteraction.Release(press))
        runCurrent()

        assertEquals("палец отпущен, но отклик обязан ещё держаться", listOf(true), states)

        advanceTimeBy(MIN_PRESS - 1)
        runCurrent()
        assertEquals(listOf(true), states)

        advanceTimeBy(2)
        runCurrent()
        assertEquals("отклик снимается сам, без новых событий", listOf(true, false), states)

        collector.cancel()
    }

    @Test
    fun `долгое нажатие снимается сразу после отпускания`() = runTest {
        val source = MutableInteractionSource()
        val states = mutableListOf<Boolean>()

        val collector = launch {
            source.collectPressedState(
                minPressMillis = MIN_PRESS,
                currentTimeMillis = { testScheduler.currentTime },
            ) { states += it }
        }
        runCurrent()

        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        runCurrent()

        advanceTimeBy(MIN_PRESS * 3)
        runCurrent()

        source.emit(PressInteraction.Release(press))
        runCurrent()

        assertEquals("удержание дольше минимума не добавляет задержки", listOf(true, false), states)

        collector.cancel()
    }

    @Test
    fun `отмена жеста снимает нажатие по тем же правилам`() = runTest {
        val source = MutableInteractionSource()
        val states = mutableListOf<Boolean>()

        val collector = launch {
            source.collectPressedState(
                minPressMillis = MIN_PRESS,
                currentTimeMillis = { testScheduler.currentTime },
            ) { states += it }
        }
        runCurrent()

        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        source.emit(PressInteraction.Cancel(press))
        runCurrent()

        assertEquals(listOf(true), states)

        advanceTimeBy(MIN_PRESS + 1)
        runCurrent()
        assertEquals(listOf(true, false), states)

        collector.cancel()
    }

    @Test
    fun `серия быстрых тапов даёт отдельные отклики, а не одно удержание`() = runTest {
        val source = MutableInteractionSource()
        val states = mutableListOf<Boolean>()

        val collector = launch {
            source.collectPressedState(
                minPressMillis = MIN_PRESS,
                releaseGapMillis = GAP,
                currentTimeMillis = { testScheduler.currentTime },
            ) { states += it }
        }
        runCurrent()

        val first = PressInteraction.Press(Offset.Zero)
        source.emit(first)
        source.emit(PressInteraction.Release(first))
        runCurrent()
        assertEquals(listOf(true), states)

        val second = PressInteraction.Press(Offset.Zero)
        source.emit(second)
        runCurrent()
        assertEquals("первый отклик обязан сняться перед вторым", listOf(true, false), states)

        advanceTimeBy(GAP + 1)
        runCurrent()
        assertEquals(listOf(true, false, true), states)

        collector.cancel()
    }

    private companion object {
        const val MIN_PRESS = 160L
        const val GAP = 45L
    }
}
