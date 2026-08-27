package com.aura.feature.home.presentation.components

import com.aura.feature.home.domain.model.IoniCard
import com.aura.feature.home.domain.model.IoniState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class IoniCardModeTest {

    @Test
    fun `a user outside the launch countries only ever sees the soon row`() {
        val card = IoniCard(state = IoniState.GEO_BLOCKED)

        assertEquals(
            IoniCardMode.SOON,
            ioniCardMode(card, 18.hours, isBatteryOptimizationDisabled = true),
        )
        assertEquals(
            IoniCardMode.SOON,
            ioniCardMode(card, Duration.ZERO, isBatteryOptimizationDisabled = false),
        )
    }

    @Test
    fun `the pre-release row ignores the charge as well`() {
        val card = IoniCard(state = IoniState.COMING)

        assertEquals(
            IoniCardMode.SOON,
            ioniCardMode(card, 18.hours, isBatteryOptimizationDisabled = true),
        )
    }

    @Test
    fun `the charge is checked before battery optimization`() {
        val card = IoniCard(state = IoniState.ACTIVE)

        assertEquals(
            IoniCardMode.IDLE,
            ioniCardMode(card, Duration.ZERO, isBatteryOptimizationDisabled = false),
        )
    }

    @Test
    fun `a charged card with battery optimization on is blocked`() {
        val card = IoniCard(state = IoniState.ACTIVE)

        assertEquals(
            IoniCardMode.BLOCKED,
            ioniCardMode(card, 18.hours, isBatteryOptimizationDisabled = false),
        )
    }

    @Test
    fun `a charged card with battery optimization off is live`() {
        val card = IoniCard(state = IoniState.ACTIVE)

        assertEquals(
            IoniCardMode.POWERED,
            ioniCardMode(card, 18.hours, isBatteryOptimizationDisabled = true),
        )
    }
}
