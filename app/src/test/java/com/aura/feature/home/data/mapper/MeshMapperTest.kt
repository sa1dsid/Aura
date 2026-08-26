package com.aura.feature.home.data.mapper

import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import com.aura.feature.home.domain.model.GeoPoint
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.UserPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeshMapperTest {

    @Test
    fun `a glowing city keeps its name and its place on the map`() {
        val city = MeshCityDto(
            id = "Tallinn",
            name = "Tallinn",
            lat = 59.437,
            lon = 24.753,
            live = true,
        ).toDomain()

        assertEquals(
            MeshCity(
                id = "Tallinn",
                name = "Tallinn",
                location = GeoPoint(latitude = 59.437, longitude = 24.753),
                isLive = true,
            ),
            city,
        )
    }

    @Test
    fun `a city the server calls dark stays dark`() {
        val city = MeshCityDto(id = "x", name = "x", lat = 0.0, lon = 0.0, live = false).toDomain()

        assertEquals(false, city.isLive)
    }

    @Test
    fun `a located user becomes the blue dot`() {
        val presence = UserLocationDto(
            lat = 59.437,
            lon = 24.753,
            city = "Tallinn",
            vpnActive = false,
        ).toDomain()

        assertEquals(
            UserPresence(
                location = GeoPoint(latitude = 59.437, longitude = 24.753),
                cityName = "Tallinn",
                isPinnedByVpn = false,
            ),
            presence,
        )
    }

    @Test
    fun `a user the gazetteer could not place has no dot`() {
        assertNull(UserLocationDto(lat = null, lon = 24.753, city = "x", vpnActive = false).toDomain())
        assertNull(UserLocationDto(lat = 59.437, lon = null, city = "x", vpnActive = false).toDomain())
    }

    @Test
    fun `a dot without a city name is still a dot`() {
        val presence = UserLocationDto(
            lat = 59.437,
            lon = 24.753,
            city = null,
            vpnActive = true,
        ).toDomain()

        assertEquals("", presence?.cityName)
        assertEquals(true, presence?.isPinnedByVpn)
    }
}
