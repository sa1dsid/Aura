package com.aura.feature.home.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val NOW = 1_787_702_400_000L

class IoniTest {

    @Test
    fun `a tap charges the card for a full day`() {
        val card = IoniCard(state = IoniState.ACTIVE, lastCompletedTapAt = NOW)

        assertEquals(24.hours, card.chargeLeft(NOW))
        assertEquals(1f, card.chargeLeft(NOW).chargeFraction(), 0.0001f)
    }

    @Test
    fun `the charge falls off linearly over the day`() {
        val card = IoniCard(state = IoniState.ACTIVE, lastCompletedTapAt = NOW)

        val left = card.chargeLeft(NOW + 6.hours.inWholeMilliseconds)

        assertEquals(18.hours, left)
        assertEquals(0.75f, left.chargeFraction(), 0.0001f)
    }

    @Test
    fun `the charge runs out exactly one day after the tap`() {
        val card = IoniCard(state = IoniState.ACTIVE, lastCompletedTapAt = NOW)

        assertEquals(Duration.ZERO, card.chargeLeft(NOW + 24.hours.inWholeMilliseconds))
        assertEquals(Duration.ZERO, card.chargeLeft(NOW + 30.hours.inWholeMilliseconds))
    }

    @Test
    fun `a card without a tap has no charge`() {
        val card = IoniCard(state = IoniState.ACTIVE, lastCompletedTapAt = null)

        assertEquals(Duration.ZERO, card.chargeLeft(NOW))
    }

    @Test
    fun `nothing is counted while the feature is closed for the user`() {
        val coming = IoniCard(state = IoniState.COMING, lastCompletedTapAt = NOW)
        val blocked = IoniCard(state = IoniState.GEO_BLOCKED, lastCompletedTapAt = NOW)

        assertEquals(Duration.ZERO, coming.chargeLeft(NOW))
        assertEquals(Duration.ZERO, blocked.chargeLeft(NOW))
    }

    @Test
    fun `a tap timestamp from the future still reads as a full charge`() {
        val card = IoniCard(state = IoniState.ACTIVE, lastCompletedTapAt = NOW + 60_000)

        assertEquals(24.hours, card.chargeLeft(NOW))
    }

    @Test
    fun `the hours left round up so a live card never shows zero`() {
        assertEquals(24, 24.hours.chargeHours())
        assertEquals(18, (17.hours + 1.minutes).chargeHours())
        assertEquals(1, 1.minutes.chargeHours())
        assertEquals(0, Duration.ZERO.chargeHours())
    }
}
