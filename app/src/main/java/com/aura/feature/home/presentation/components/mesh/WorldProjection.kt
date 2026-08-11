package com.aura.feature.home.presentation.components.mesh

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.aura.feature.home.domain.model.GeoPoint

@Immutable
data class GeoBounds(
    val north: Double,
    val south: Double,
    val west: Double,
    val east: Double,
) {
    val latitudeSpan: Double get() = north - south

    val longitudeSpan: Double get() = east - west
}

@Immutable
data class WorldProjection(
    val bounds: GeoBounds,
    val aspectRatio: Float,
) {

    fun normalize(point: GeoPoint): Offset {
        val longitude = alignToBounds(point.longitude)
        return Offset(
            x = ((longitude - bounds.west) / bounds.longitudeSpan).toFloat(),
            y = ((bounds.north - point.latitude) / bounds.latitudeSpan).toFloat(),
        )
    }

    fun covers(point: GeoPoint): Boolean {
        val longitude = alignToBounds(point.longitude)
        return longitude in bounds.west..bounds.east &&
            point.latitude in bounds.south..bounds.north
    }

    private fun alignToBounds(longitude: Double): Double = when {
        longitude < bounds.west -> longitude + FULL_TURN
        longitude > bounds.east -> longitude - FULL_TURN
        else -> longitude
    }

    private companion object {
        const val FULL_TURN = 360.0
    }
}
