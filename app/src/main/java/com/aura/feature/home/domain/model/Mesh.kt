package com.aura.feature.home.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class MeshCity(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val isLive: Boolean,
)

sealed interface NodesOnline {
    data class Live(val count: Int) : NodesOnline
    data class LastKnown(val count: Int) : NodesOnline
    data object Unknown : NodesOnline
}

data class UserPresence(
    val location: GeoPoint,
    val cityName: String,
    val isPinnedByVpn: Boolean,
)

data class MeshState(
    val cities: List<MeshCity>,
    val nodesOnline: NodesOnline,
    val userPresence: UserPresence?,
)
