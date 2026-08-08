package com.aura.feature.home.presentation.preview

import com.aura.feature.home.domain.model.BonusWithdrawalTeaser
import com.aura.feature.home.domain.model.ConnectionState
import com.aura.feature.home.domain.model.GeoPoint
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.InviteState
import com.aura.feature.home.domain.model.IonBalances
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.NetworkType
import com.aura.feature.home.domain.model.NewsState
import com.aura.feature.home.domain.model.NodeStatus
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.NodesOnline
import com.aura.feature.home.domain.model.SparkTeaser
import com.aura.feature.home.domain.model.Teasers
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.UserPresence
import com.aura.feature.home.domain.model.VpnCodeTeaser
import com.aura.feature.home.presentation.HomeUiState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object HomePreviewData {

    val content = HomeUiState.Content(
        home = HomeState(
            balances = IonBalances(accrued = 4_210, availableToWithdraw = 3_000),
            nodeStatus = NodeStatus(
                currentTier = NodeTier.CORE_NODE,
                referralRate = 2.5,
                progressToNext = 12_090,
                progressTarget = 20_000,
                nextTier = NodeTier.IONIC_PRIME,
            ),
            teasers = Teasers(
                bonusWithdrawal = BonusWithdrawalTeaser(completedSteps = 2, totalSteps = 3),
                spark = SparkTeaser(collected = 142_800, target = 240_000),
                vpnCode = VpnCodeTeaser(
                    isEnabled = true,
                    tierGaugePercent = 100,
                    contributionPercent = 62,
                ),
            ),
            connection = ConnectionState(
                networkType = NetworkType.MOBILE_4G,
                isVpnActive = false,
                rewardIon = 20,
            ),
            session = TestSessionState.Running(
                remaining = 2.minutes + 47.seconds,
                total = 3.minutes,
                rewardIon = 20,
            ),
            invite = InviteState(
                friendsJoined = 3,
                friendsTarget = 4,
                referralRatePercent = 10,
                inviteLink = "https://ioaura.app/i/syrex",
            ),
            news = NewsState(hasUnread = false),
        ),
        mesh = MeshState(
            cities = listOf(
                MeshCity("toronto", "Toronto", GeoPoint(43.65, -79.38), isLive = true),
                MeshCity("moscow", "Moscow", GeoPoint(55.76, 37.62), isLive = true),
                MeshCity("sydney", "Sydney", GeoPoint(-33.87, 151.21), isLive = true),
                MeshCity("tokyo", "Tokyo", GeoPoint(35.68, 139.65), isLive = false),
                MeshCity("berlin", "Berlin", GeoPoint(52.52, 13.40), isLive = false),
            ),
            nodesOnline = NodesOnline.Live(4_210),
            userPresence = UserPresence(
                location = GeoPoint(51.51, -0.13),
                cityName = "London",
                isPinnedByVpn = false,
            ),
        ),
    )
}
