package com.aura.core.geo

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

data class IpInfo(
    val ipAddress: String,
    val isIpV6: Boolean,
    val operator: String,
    val city: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
)

interface IpInfoSource {
    suspend fun fetch(): IpInfo
}

@Singleton
class MockIpInfoSource @Inject constructor() : IpInfoSource {

    override suspend fun fetch(): IpInfo {
        delay(NETWORK_DELAY_MILLIS)
        return IpInfo(
            ipAddress = "192.168.1.42",
            isIpV6 = false,
            operator = "T-Mobile",
            city = "London",
            countryCode = "UK",
            latitude = 51.51,
            longitude = -0.13,
        )
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 250L
    }
}
