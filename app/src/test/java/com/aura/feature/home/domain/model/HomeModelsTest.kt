package com.aura.feature.home.domain.model

import com.aura.core.network.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HomeModelsTest {

    @Test
    fun `the running session reports how far it has come`() {
        val session = TestSessionState.Running(
            remaining = 1.minutes,
            total = 3.minutes,
            rewardIon = 20,
        )

        assertEquals(2f / 3f, session.progress, 0.0001f)
    }

    @Test
    fun `a session that has not started shows no progress`() {
        val session = TestSessionState.Running(
            remaining = 3.minutes,
            total = 3.minutes,
            rewardIon = 20,
        )

        assertEquals(0f, session.progress, 0.0001f)
    }

    @Test
    fun `a session with no length at all does not divide by zero`() {
        val session = TestSessionState.Running(
            remaining = 0.seconds,
            total = 0.seconds,
            rewardIon = 20,
        )

        assertEquals(0f, session.progress, 0.0001f)
    }

    @Test
    fun `the test lasts three minutes, pays twenty and locks the button for twelve hours`() {
        assertEquals(3.minutes, TEST_DURATION)
        assertEquals(12.hours, COOLDOWN_DURATION)
        assertEquals(20, TAP_REWARD_ION)
    }

    @Test
    fun `the spark window spreads its rate over twelve hours`() {
        assertEquals(43_200.0, SPARK_WINDOW_SECONDS, 0.0)
        assertEquals(20_000, SPARK_RATE_WIFI)
        assertEquals(40_000, SPARK_RATE_MOBILE)

        assertEquals(
            SPARK_RATE_WIFI / SPARK_WINDOW_SECONDS,
            SparkWindow(ratePerWindow = SPARK_RATE_WIFI).perSecond,
            0.000001,
        )
    }

    @Test
    fun `a window without a rate does not tick`() {
        assertEquals(0.0, SparkWindow().perSecond, 0.0)
    }

    @Test
    fun `the bonus teaser is complete only when every step is closed`() {
        assertFalse(BonusWithdrawalTeaser(completedSteps = 3, totalSteps = 4).isComplete)
        assertTrue(BonusWithdrawalTeaser(completedSteps = 4, totalSteps = 4).isComplete)
        assertTrue(BonusWithdrawalTeaser(completedSteps = 5, totalSteps = 4).isComplete)
    }

    @Test
    fun `the spark target is reached at the threshold, not after it`() {
        val short = SparkTeaser(collected = 239_999, target = SPARK_COUPON_THRESHOLD)
        val full = SparkTeaser(collected = 240_000, target = SPARK_COUPON_THRESHOLD)

        assertFalse(short.isTargetReached)
        assertTrue(full.isTargetReached)
        assertEquals(240_000L, SPARK_COUPON_THRESHOLD)
    }

    @Test
    fun `the spark code is ready only when the server issued one`() {
        val reached = SparkTeaser(collected = 240_000, target = SPARK_COUPON_THRESHOLD)
        val issued = SparkTeaser(collected = 0, target = SPARK_COUPON_THRESHOLD, readyCode = "A8X4")

        assertFalse(reached.isCodeReady)
        assertFalse(reached.copy(readyCode = " ").isCodeReady)
        assertTrue(issued.isCodeReady)
    }

    @Test
    fun `the spark percent floors the ratio and never leaves the scale`() {
        assertEquals(0, SparkTeaser(collected = 0, target = 240_000).percentToCode)
        assertEquals(59, SparkTeaser(collected = 142_800, target = 240_000).percentToCode)
        assertEquals(100, SparkTeaser(collected = 480_000, target = 240_000).percentToCode)
        assertEquals(0, SparkTeaser(collected = 100, target = 0).percentToCode)
    }

    @Test
    fun `the vpn code needs the flag and both gauges`() {
        assertFalse(VpnCodeTeaser(false, 100, 100).isCodeReady)
        assertFalse(VpnCodeTeaser(true, 99, 100).isCodeReady)
        assertFalse(VpnCodeTeaser(true, 100, 99).isCodeReady)
        assertTrue(VpnCodeTeaser(true, 100, 100).isCodeReady)
    }

    @Test
    fun `the connection badge is only tappable while a vpn is on`() {
        assertFalse(
            ConnectionState(NetworkType.WIFI, isVpnActive = false, rewardIon = 20).isClickable
        )
        assertTrue(
            ConnectionState(NetworkType.WIFI, isVpnActive = true, rewardIon = 20).isClickable
        )
    }

    @Test
    fun `the tiers climb in the order the spec names`() {
        assertEquals(
            listOf(
                NodeTier.IDLE_NODE,
                NodeTier.ACTIVE_SIGNAL,
                NodeTier.STABLE_LINK,
                NodeTier.CORE_NODE,
                NodeTier.IONIC_PRIME,
            ),
            NodeTier.entries,
        )
    }

    @Test
    fun `a vpn stops the tap before anything else does`() {
        val rejection = testStartRejection(
            session = TestSessionState.Cooldown(1.hours, 12.hours, isPausedByVpn = true),
            isVpnActive = true,
        )

        assertEquals(TestStartRejection.VpnDetected, rejection)
    }

    @Test
    fun `a running cooldown stops the tap and says how long is left`() {
        val rejection = testStartRejection(
            session = TestSessionState.Cooldown(2.hours, 12.hours, isPausedByVpn = false),
            isVpnActive = false,
        )

        assertEquals(TestStartRejection.CooldownNotFinished(2.hours), rejection)
    }

    @Test
    fun `data share never blocks the tap in the release build`() {
        val rejection = testStartRejection(
            session = TestSessionState.Ready(20),
            isVpnActive = false,
        )

        assertEquals(null, rejection)
    }

    @Test
    fun `data share blocks the tap only when it is asked to`() {
        assertEquals(
            TestStartRejection.DataShareDisabled,
            testStartRejection(
                session = TestSessionState.Ready(20),
                isVpnActive = false,
                isDataShareRequired = true,
                isDataShareEnabled = false,
            ),
        )
    }

    @Test
    fun `a ready session with nothing in the way lets the tap through`() {
        assertEquals(
            null,
            testStartRejection(session = TestSessionState.Ready(20), isVpnActive = false),
        )
    }
}
