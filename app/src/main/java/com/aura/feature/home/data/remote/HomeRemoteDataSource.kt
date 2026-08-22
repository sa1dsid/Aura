package com.aura.feature.home.data.remote

import com.aura.feature.home.data.remote.dto.HomeSnapshotDto
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

interface HomeRemoteDataSource {
    suspend fun fetchHome(): HomeSnapshotDto

    suspend fun creditTestReward(amount: Int)
}

@Singleton
class MockHomeRemoteDataSource @Inject constructor() : HomeRemoteDataSource {

    private val creditedIon = AtomicLong(0)

    override suspend fun creditTestReward(amount: Int) {
        creditedIon.addAndGet(amount.toLong())
    }

    override suspend fun fetchHome(): HomeSnapshotDto {
        delay(NETWORK_DELAY_MILLIS)
        return HomeSnapshotDto(
            accruedIon = 4_210 + creditedIon.get(),
            availableToWithdrawIon = 3_000,
            tier = "CORE_NODE",
            referralRate = 2.5,
            tierProgress = 12_090,
            tierTarget = 20_000,
            nextTier = "IONIC_PRIME",
            bonusStepsComplete = 2,
            bonusStepsTotal = 3,
            sparkCollected = 142_800,
            sparkTarget = 240_000,
            vpnSaleEnabled = true,
            vpnTierGaugePercent = 100,
            vpnContributionPercent = 62,
            networkType = "MOBILE_4G",
            vpnActive = false,
            testRewardIon = 20,
            friendsJoined = 3,
            friendsTarget = 4,
            referralRatePercent = 10,
            inviteLink = "https://ioaura.app/i/syrex",
        )
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 300L
    }
}
