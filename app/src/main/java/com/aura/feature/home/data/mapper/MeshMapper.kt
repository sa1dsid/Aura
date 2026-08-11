package com.aura.feature.home.data.mapper

import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import com.aura.feature.home.domain.model.GeoPoint
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.UserPresence

fun MeshCityDto.toDomain(): MeshCity = MeshCity(
    id = id,
    name = name,
    location = GeoPoint(latitude = lat, longitude = lon),
    isLive = live,
)

fun UserLocationDto.toDomain(): UserPresence? {
    val latitude = lat ?: return null
    val longitude = lon ?: return null
    return UserPresence(
        location = GeoPoint(latitude = latitude, longitude = longitude),
        cityName = city.orEmpty(),
        isPinnedByVpn = vpnActive,
    )
}
