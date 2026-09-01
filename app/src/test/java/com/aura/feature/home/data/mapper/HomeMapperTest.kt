package com.aura.feature.home.data.mapper

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.BonusProgressDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.NodeStatusDto
import com.aura.core.api.dto.SparkCouponDto
import com.aura.core.config.FeatureFlags
import com.aura.core.network.NetworkStatus
import com.aura.core.network.NetworkType
import com.aura.feature.home.domain.model.BatteryOptimizationState
import com.aura.feature.home.domain.model.NETWORK_SYNC_FRIENDS
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.SPARK_COUPON_THRESHOLD
import com.aura.feature.home.domain.model.SparkWindow
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.TierRates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val REWARD_ION = 20

class HomeMapperTest {

    @Test
    fun `balances cross over as the server counted them`() {
        val home = dashboard(
            DashboardDto(
                accruedIon = 140,
                availableToWithdrawIon = 3_000,
                reservedBonusIon = 3_000,
                tapCount = 7,
            )
        )

        assertEquals(140L, home.balances.accrued)
        assertEquals(3_000L, home.balances.availableToWithdraw)
        assertEquals(3_000L, home.balances.reservedBonus)
        assertEquals(7, home.tapCount)
    }

    @Test
    fun `the tiers the server can name`() {
        val named = mapOf(
            "idle" to NodeTier.IDLE_NODE,
            "IDLE" to NodeTier.IDLE_NODE,
            "active_signal" to NodeTier.ACTIVE_SIGNAL,
            "ACTIVE_SIGNAL" to NodeTier.ACTIVE_SIGNAL,
            "stable_link" to NodeTier.STABLE_LINK,
            "core_node" to NodeTier.CORE_NODE,
            "ionic_prime" to NodeTier.IONIC_PRIME,
        )

        for ((sent, expected) in named) {
            assertEquals(sent, expected, tierOf(sent))
        }
    }

    @Test
    fun `a tier nobody knows falls back to idle`() {
        assertEquals(NodeTier.IDLE_NODE, tierOf("legendary"))
        assertEquals(NodeTier.IDLE_NODE, tierOf(""))
    }

    @Test
    fun `the top tier has nothing to grow into`() {
        val home = dashboard(DashboardDto(node = NodeStatusDto(tier = "ionic_prime")))

        assertEquals(NodeTier.IONIC_PRIME, home.nodeStatus.currentTier)
        assertNull(home.nodeStatus.nextTier)
    }

    @Test
    fun `the rate multiplier doubles as the referral rate`() {
        val home = dashboard(DashboardDto(node = NodeStatusDto(rateMultiplierPercent = 250)))

        assertEquals(2.5, home.nodeStatus.referralRate, 0.0001)
        assertEquals(250, home.nodeStatus.rateMultiplierPercent)
    }

    @Test
    fun `the vpn gauge is the tier progress in percent`() {
        val flags = FeatureFlags(vpnCode = true)

        assertEquals(
            50,
            dashboard(
                DashboardDto(node = NodeStatusDto(progressCurrent = 3, progressTarget = 6)),
                flags = flags,
            ).teasers.vpnCode.tierGaugePercent,
        )
    }

    @Test
    fun `a tier without a target is counted as fully grown`() {
        val flags = FeatureFlags(vpnCode = true)

        assertEquals(
            100,
            dashboard(
                DashboardDto(node = NodeStatusDto(progressCurrent = 9, progressTarget = null)),
                flags = flags,
            ).teasers.vpnCode.tierGaugePercent,
        )
        assertEquals(
            100,
            dashboard(
                DashboardDto(node = NodeStatusDto(progressCurrent = 9, progressTarget = 0)),
                flags = flags,
            ).teasers.vpnCode.tierGaugePercent,
        )
    }

    @Test
    fun `the gauge never runs past a hundred`() {
        val home = dashboard(
            DashboardDto(node = NodeStatusDto(progressCurrent = 99, progressTarget = 6)),
            flags = FeatureFlags(vpnCode = true),
        )

        assertEquals(100, home.teasers.vpnCode.tierGaugePercent)
    }

    @Test
    fun `the vpn teaser is ready only when the flag and both gauges are full`() {
        val ready = dashboard(
            DashboardDto(node = NodeStatusDto(progressCurrent = 6, progressTarget = 6)),
            flags = FeatureFlags(vpnCode = true),
        ).teasers.vpnCode

        assertTrue(ready.isEnabled)
        assertEquals(100, ready.tierGaugePercent)
        assertEquals(0, ready.contributionPercent)
        assertFalse(ready.isCodeReady)
    }

    @Test
    fun `data share stays soon while the release flag is off`() {
        val off = dashboard(
            DashboardDto(bonus = BonusProgressDto(dataShareSoon = false)),
            flags = FeatureFlags(dataShare = false),
        )
        val on = dashboard(
            DashboardDto(bonus = BonusProgressDto(dataShareSoon = false)),
            flags = FeatureFlags(dataShare = true),
        )

        assertTrue(off.teasers.bonusWithdrawal.isDataShareSoon)
        assertFalse(on.teasers.bonusWithdrawal.isDataShareSoon)
    }

    @Test
    fun `a silent server does not collapse the bonus card to zero steps`() {
        val home = dashboard(DashboardDto(bonus = BonusProgressDto(completed = 1, total = 0)))

        assertEquals(4, home.teasers.bonusWithdrawal.totalSteps)
        assertFalse(home.teasers.bonusWithdrawal.isComplete)
    }

    @Test
    fun `the spark teaser counts the running window against the coupon threshold`() {
        val home = dashboard(
            DashboardDto(sparkCoupon = SparkCouponDto(issued = 1, limit = 4)),
            spark = SparkWindow(balance = 241_000.7, ratePerWindow = 20_000),
        )

        assertEquals(241_000L, home.teasers.spark.collected)
        assertEquals(SPARK_COUPON_THRESHOLD, home.teasers.spark.target)
        assertEquals(1, home.teasers.spark.issuedCoupons)
        assertEquals(4, home.teasers.spark.couponLimit)
        assertTrue(home.teasers.spark.isTargetReached)
    }

    @Test
    fun `the spark teaser carries the issued code and drops a blank one`() {
        val issued = dashboard(
            DashboardDto(sparkCoupon = SparkCouponDto(issued = 1, limit = 4, readyCode = "A8X4-KP92-QW01")),
        )
        val blank = dashboard(DashboardDto(sparkCoupon = SparkCouponDto(readyCode = "  ")))

        assertEquals("A8X4-KP92-QW01", issued.teasers.spark.readyCode)
        assertTrue(issued.teasers.spark.isCodeReady)
        assertNull(blank.teasers.spark.readyCode)
        assertFalse(blank.teasers.spark.isCodeReady)
    }

    @Test
    fun `the connection badge mirrors the live network`() {
        val home = dashboard(
            DashboardDto(),
            network = NetworkStatus(
                isOnline = true,
                isVpnActive = true,
                type = NetworkType.MOBILE_4G,
            ),
        )

        assertEquals(NetworkType.MOBILE_4G, home.connection.networkType)
        assertTrue(home.connection.isVpnActive)
        assertTrue(home.connection.isClickable)
        assertEquals(REWARD_ION, home.connection.rewardIon)
    }

    @Test
    fun `the invite row counts the network sync friends against a fixed target`() {
        val home = dashboard(
            DashboardDto(
                node = NodeStatusDto(sparkReferralPercent = 5),
                bonus = BonusProgressDto(networkSync = 2),
            ),
        )

        assertEquals(2, home.invite.activeFriends)
        assertEquals(NETWORK_SYNC_FRIENDS, home.invite.friendsTarget)
        assertEquals(5, home.invite.referralRatePercent)
    }

    @Test
    fun `the invite row never overfills its slots`() {
        val home = dashboard(DashboardDto(bonus = BonusProgressDto(networkSync = 9)))

        assertEquals(NETWORK_SYNC_FRIENDS, home.invite.activeFriends)
    }

    @Test
    fun `the invite link waits for the nodes feature`() {
        assertEquals("", dashboard(DashboardDto()).invite.inviteLink)
        assertEquals(
            "https://ioaura.app/i/SYREX482",
            dashboard(DashboardDto(), nodes = nodesState()).invite.inviteLink,
        )
    }

    @Test
    fun `the battery answer belongs to the repository, not to the mapper`() {
        val home = dashboard(
            DashboardDto(
                batteryOptimization = BatteryOptimizationDto(
                    shouldShow = true,
                    optimizationDisabled = false,
                )
            )
        )

        assertEquals(BatteryOptimizationState(), home.batteryOptimization)
    }

    @Test
    fun `a battery answer becomes its own piece of state`() {
        val state = BatteryOptimizationDto(shouldShow = true, optimizationDisabled = true)
            .toDomain()

        assertTrue(state.shouldShow)
        assertTrue(state.isDisabled)
    }

    @Test
    fun `the session and its state are passed straight through`() {
        val session = TestSessionState.Ready(REWARD_ION)

        assertEquals(session, dashboard(DashboardDto(), session = session).session)
    }

    private fun tierOf(tier: String): NodeTier =
        dashboard(DashboardDto(node = NodeStatusDto(tier = tier))).nodeStatus.currentTier

    private fun dashboard(
        dto: DashboardDto,
        session: TestSessionState = TestSessionState.Ready(REWARD_ION),
        spark: SparkWindow = SparkWindow(),
        network: NetworkStatus = NetworkStatus(
            isOnline = true,
            isVpnActive = false,
            type = NetworkType.WIFI,
        ),
        flags: FeatureFlags = FeatureFlags(),
        nodes: NodesState? = null,
    ) = dto.toDomain(
        session = session,
        spark = spark,
        network = network,
        flags = flags,
        nodes = nodes,
    )

    private fun nodesState() = NodesState(
        handle = "said",
        invite = InviteOffer(
            code = "SYREX482",
            link = "https://ioaura.app/i/SYREX482",
            shareText = null,
        ),
        friendsJoined = 3,
        activeFriends = 1,
        tier = ReferralTier.ACTIVE_SIGNAL,
        tierRates = TierRates(sparkPercent = 5, withdrawalPercent = 0.0),
        nextTier = ReferralTier.STABLE_LINK,
        friendsToNextTier = 2,
        rewards = ReferralRewards(spark = 0, ion = 0),
        friends = emptyList(),
        socials = emptyList(),
    )
}
