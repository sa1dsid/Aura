package com.aura.feature.home.presentation.components.mesh

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.aura.feature.home.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.tan

@Immutable
data class GeoBounds(
    val north: Double = 84.0,
    val south: Double = -84.0,
    val west: Double = -180.0,
    val east: Double = 180.0,
)

@Immutable
sealed class WorldProjection(val bounds: GeoBounds) {

    protected abstract fun projectLatitude(latitude: Double): Double

    fun normalize(point: GeoPoint): Offset {
        val x = (point.longitude - bounds.west) / (bounds.east - bounds.west)
        val top = projectLatitude(bounds.north)
        val bottom = projectLatitude(bounds.south)
        val y = (projectLatitude(point.latitude) - top) / (bottom - top)
        return Offset(x.toFloat(), y.toFloat())
    }

    val aspectRatio: Float
        get() {
            val lonSpan = bounds.east - bounds.west
            val latSpan = abs(projectLatitude(bounds.south) - projectLatitude(bounds.north))
            return (lonSpan / latSpan).toFloat()
        }

    @Immutable
    class Equirectangular(bounds: GeoBounds = GeoBounds()) : WorldProjection(bounds) {
        override fun projectLatitude(latitude: Double): Double = -latitude
    }

    @Immutable
    class Mercator(bounds: GeoBounds = GeoBounds()) : WorldProjection(bounds) {
        override fun projectLatitude(latitude: Double): Double {
            val clamped = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
            val radians = clamped * PI / 180.0
            return -ln(tan(PI / 4 + radians / 2)) * 180.0 / PI
        }

        private companion object {
            const val MAX_LATITUDE = 85.05
        }
    }
}
