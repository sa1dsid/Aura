package com.aura.feature.network.data.remote

import com.aura.core.geo.IpInfoSource
import com.aura.core.network.NetworkType
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkRemoteDataSource {
    suspend fun fetchSnapshot(): NetworkSnapshotDto
}

@Singleton
class MockNetworkRemoteDataSource @Inject constructor(
    private val ipInfoSource: IpInfoSource,
) : NetworkRemoteDataSource {

    override suspend fun fetchSnapshot(): NetworkSnapshotDto {
        val info = ipInfoSource.fetch()
        return NetworkSnapshotDto(
            networkType = NetworkType.MOBILE_4G.name,
            operator = info.operator,
            ipAddress = info.ipAddress,
            ipV6 = info.isIpV6,
            city = info.city,
            countryCode = info.countryCode,
        )
    }
}
