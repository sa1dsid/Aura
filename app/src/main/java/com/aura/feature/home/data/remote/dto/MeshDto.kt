package com.aura.feature.home.data.remote.dto

data class MeshSnapshotDto(
    val cities: List<MeshCityDto>,
    val nodesOnline: Int,
)

data class MeshCityDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val live: Boolean,
)

data class UserLocationDto(
    val lat: Double?,
    val lon: Double?,
    val city: String?,
    val vpnActive: Boolean,
)
