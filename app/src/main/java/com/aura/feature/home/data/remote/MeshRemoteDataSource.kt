package com.aura.feature.home.data.remote

import com.aura.core.geo.CityGazetteer
import com.aura.core.network.NetworkMonitor
import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.MeshSnapshotDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import javax.inject.Inject
import javax.inject.Singleton

interface MeshRemoteDataSource {
    suspend fun fetchMeshSnapshot(): MeshSnapshotDto

    suspend fun fetchUserLocation(): UserLocationDto
}

@Singleton
class ApiMeshRemoteDataSource @Inject constructor(
    private val home: HomeRemoteDataSource,
    private val gazetteer: CityGazetteer,
    private val networkMonitor: NetworkMonitor,
) : MeshRemoteDataSource {

    override suspend fun fetchMeshSnapshot(): MeshSnapshotDto {
        val mesh = home.mesh()

        return MeshSnapshotDto(
            cities = gazetteer.findAll(mesh.glowingCities).map { city ->
                MeshCityDto(
                    id = city.name,
                    name = city.name,
                    lat = city.latitude,
                    lon = city.longitude,
                    live = true,
                )
            },
            nodesOnline = mesh.nodesOnline,
            stale = mesh.nodesOnlineStale,
        )
    }

    override suspend fun fetchUserLocation(): UserLocationDto {
        val isVpnActive = networkMonitor.current().isVpnActive
        val city = home.updateLocation(vpn = isVpnActive)
        val located = gazetteer.find(city)

        return UserLocationDto(
            lat = located?.latitude,
            lon = located?.longitude,
            city = city.orEmpty(),
            vpnActive = isVpnActive,
        )
    }
}
