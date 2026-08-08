package com.aura.feature.home.presentation.components.mesh

import com.aura.feature.home.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldProjectionTest {

    private val bounds = GeoBounds(north = 84.0, south = -84.0)
    private val projection = WorldProjection.Equirectangular(bounds)

    @Test
    fun `null island lands in the middle of the map`() {
        val point = projection.normalize(GeoPoint(latitude = 0.0, longitude = 0.0))

        assertEquals(0.5f, point.x, TOLERANCE)
        assertEquals(0.5f, point.y, TOLERANCE)
    }

    @Test
    fun `date line edges map to left and right borders`() {
        assertEquals(0f, projection.normalize(GeoPoint(0.0, -180.0)).x, TOLERANCE)
        assertEquals(1f, projection.normalize(GeoPoint(0.0, 180.0)).x, TOLERANCE)
    }

    @Test
    fun `north is up`() {
        val north = projection.normalize(GeoPoint(60.0, 0.0))
        val south = projection.normalize(GeoPoint(-60.0, 0.0))

        assertTrue("Северная точка должна быть выше южной", north.y < south.y)
    }

    @Test
    fun `aspect ratio matches the visible slice of the globe`() {
        assertEquals(360f / 168f, projection.aspectRatio, TOLERANCE)
    }

    @Test
    fun `mercator keeps the equator centred on symmetric bounds`() {
        val mercator = WorldProjection.Mercator(bounds)

        assertEquals(0.5f, mercator.normalize(GeoPoint(0.0, 0.0)).y, TOLERANCE)
    }

    @Test
    fun `mercator needs more height for the same slice of the globe`() {
        val mercator = WorldProjection.Mercator(bounds)

        assertTrue(
            "Меркатор раздувает приполярные широты, поэтому карта выше и уже",
            mercator.aspectRatio < projection.aspectRatio,
        )
    }

    @Test
    fun `mercator pushes mid latitudes towards the equator`() {
        val mercator = WorldProjection.Mercator(bounds)

        val equirectangular = projection.normalize(GeoPoint(60.0, 0.0)).y
        val stretched = mercator.normalize(GeoPoint(60.0, 0.0)).y

        assertTrue(stretched > equirectangular)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
