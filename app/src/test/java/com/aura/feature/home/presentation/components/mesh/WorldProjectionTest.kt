package com.aura.feature.home.presentation.components.mesh

import com.aura.feature.home.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldProjectionTest {

    private val projection = MeshMapDefaults.Projection

    @Test
    fun `map edges map to the borders of the asset`() {
        val bounds = projection.bounds

        assertEquals(0f, projection.normalize(GeoPoint(0.0, bounds.west)).x, TOLERANCE)
        assertEquals(1f, projection.normalize(GeoPoint(0.0, bounds.east)).x, TOLERANCE)
        assertEquals(0f, projection.normalize(GeoPoint(bounds.north, 0.0)).y, TOLERANCE)
        assertEquals(1f, projection.normalize(GeoPoint(bounds.south, 0.0)).y, TOLERANCE)
    }

    @Test
    fun `map is centred east of greenwich`() {
        val greenwich = projection.normalize(GeoPoint(0.0, 0.0))

        assertEquals(0.4720f, greenwich.x, TOLERANCE)
        assertEquals(0.4823f, greenwich.y, TOLERANCE)
    }

    @Test
    fun `longitudes west of the seam wrap to the far side of the map`() {
        val wrapped = projection.normalize(GeoPoint(0.0, -175.0))

        assertEquals(0.9854f, wrapped.x, TOLERANCE)
    }

    @Test
    fun `north is up`() {
        val north = projection.normalize(GeoPoint(60.0, 0.0))
        val south = projection.normalize(GeoPoint(-60.0, 0.0))

        assertTrue("Северная точка должна быть выше южной", north.y < south.y)
    }

    @Test
    fun `miami lands where it was measured on the design asset`() {
        val miami = projection.normalize(GeoPoint(latitude = 25.76, longitude = -80.19))

        assertEquals(0.2495f, miami.x, TOLERANCE)
        assertEquals(0.3340f, miami.y, TOLERANCE)
    }

    @Test
    fun `tokyo lands where it was measured on the design asset`() {
        val tokyo = projection.normalize(GeoPoint(latitude = 35.68, longitude = 139.69))

        assertEquals(0.8597f, tokyo.x, TOLERANCE)
        assertEquals(0.2769f, tokyo.y, TOLERANCE)
    }

    @Test
    fun `aspect ratio comes from the asset, not from the degree spans`() {
        val degreeRatio = projection.bounds.longitudeSpan / projection.bounds.latitudeSpan

        assertEquals(
            MeshMapDefaults.ASSET_WIDTH_DP / MeshMapDefaults.ASSET_HEIGHT_DP,
            projection.aspectRatio,
            TOLERANCE,
        )
        assertTrue(
            "Ассет слегка растянут по вертикали, поэтому пропорции не равны отношению градусов",
            degreeRatio > projection.aspectRatio,
        )
    }

    @Test
    fun `points above the northern edge are outside the map`() {
        assertFalse(projection.covers(GeoPoint(latitude = 85.0, longitude = 0.0)))
        assertTrue(projection.covers(GeoPoint(latitude = 25.76, longitude = -80.19)))
        assertTrue(projection.covers(GeoPoint(latitude = 0.0, longitude = -175.0)))
    }

    private companion object {
        const val TOLERANCE = 0.0005f
    }
}
