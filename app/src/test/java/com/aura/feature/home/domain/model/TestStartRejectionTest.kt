package com.aura.feature.home.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TestStartRejectionTest {

    @Test
    fun `lets the test start from a ready ring`() {
        val rejection = testStartRejection(
            session = TestSessionState.Ready(rewardIon = 20),
            isVpnActive = false,
        )

        assertNull(rejection)
    }

    @Test
    fun `stops the start while a vpn is on`() {
        val rejection = testStartRejection(
            session = TestSessionState.Ready(rewardIon = 20),
            isVpnActive = true,
        )

        assertEquals(TestStartRejection.VpnDetected, rejection)
    }

    @Test
    fun `stops the start until the cooldown is over`() {
        val rejection = testStartRejection(
            session = TestSessionState.Cooldown(
                remaining = 11.hours + 24.minutes,
                total = 12.hours,
                isPausedByVpn = false,
            ),
            isVpnActive = false,
        )

        assertEquals(TestStartRejection.CooldownNotFinished(11.hours + 24.minutes), rejection)
    }

    @Test
    fun `reports the vpn before the cooldown`() {
        val rejection = testStartRejection(
            session = TestSessionState.Cooldown(
                remaining = 11.hours,
                total = 12.hours,
                isPausedByVpn = true,
            ),
            isVpnActive = true,
        )

        assertEquals(TestStartRejection.VpnDetected, rejection)
    }
}
