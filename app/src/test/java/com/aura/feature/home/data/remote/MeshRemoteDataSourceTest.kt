package com.aura.feature.home.data.remote

import com.aura.core.api.RoutingApiServer
import com.aura.core.geo.City
import com.aura.core.geo.CityGazetteer
import com.aura.core.geo.UserLocationSource
import com.aura.feature.home.Home
import com.aura.feature.home.HomePaths
import com.aura.feature.home.MutableNetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshRemoteDataSourceTest {

    private val server = RoutingApiServer()

    private val networkMonitor = MutableNetworkMonitor()

    private val userLocationSource = UserLocationSource()

    private val gazetteer: CityGazetteer = mockk()

    private val known = mutableMapOf(
        "Tallinn" to City("Tallinn", 59.437, 24.753),
        "Rostov-na-Donu" to City("Rostov-na-Donu", 47.235, 39.701),
    )

    private val remote = ApiMeshRemoteDataSource(
        home = ApiHomeRemoteDataSource(server.api),
        gazetteer = gazetteer,
        userLocationSource = userLocationSource,
        networkMonitor = networkMonitor,
    )

    init {
        coEvery { gazetteer.findAll(any()) } answers {
            firstArg<List<String>>().mapNotNull(known::get)
        }
        coEvery { gazetteer.find(any()) } answers { firstArg<String?>()?.let(known::get) }
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `the glowing cities are placed on the map by the gazetteer`() = runTest {
        server.always(
            HomePaths.MESH,
            body = Home.mesh(nodesOnline = 7, cities = listOf("Tallinn", "Rostov-na-Donu")),
        )

        val snapshot = remote.fetchMeshSnapshot()

        assertEquals(listOf("Tallinn", "Rostov-na-Donu"), snapshot.cities.map { it.name })
        assertEquals(59.437, snapshot.cities.first().lat, 0.0001)
        assertEquals(7, snapshot.nodesOnline)
        assertTrue(snapshot.cities.all { it.live })
    }

    @Test
    fun `a city the gazetteer cannot place is left off the map`() = runTest {
        server.always(HomePaths.MESH, body = Home.mesh(cities = listOf("Atlantis", "Tallinn")))

        val snapshot = remote.fetchMeshSnapshot()

        assertEquals(listOf("Tallinn"), snapshot.cities.map { it.name })
    }

    @Test
    fun `a stale counter is carried through as stale`() = runTest {
        server.always(HomePaths.MESH, body = Home.mesh(nodesOnline = 3, stale = true))

        assertTrue(remote.fetchMeshSnapshot().stale)
    }

    @Test
    fun `the location the server names is remembered and placed`() = runTest {
        server.always(HomePaths.LOCATION, body = Home.city("Tallinn"))

        val location = remote.fetchUserLocation()

        assertEquals("Tallinn", location.city)
        assertEquals(59.437, location.lat!!, 0.0001)
        assertEquals(24.753, location.lon!!, 0.0001)
        assertEquals(false, location.vpnActive)
        assertEquals("Tallinn", userLocationSource.city.value)
    }

    @Test
    fun `a vpn is confessed to the server and marked on the answer`() = runTest {
        server.always(HomePaths.LOCATION, body = Home.city("Tallinn"))
        networkMonitor.set(isVpnActive = true)

        val location = remote.fetchUserLocation()

        assertEquals("""{"vpn":true}""", server.bodyOf(HomePaths.LOCATION))
        assertTrue(location.vpnActive)
    }

    @Test
    fun `a city the server does not know keeps the last one remembered`() = runTest {
        server.next(HomePaths.LOCATION, body = Home.city("Tallinn"))
        server.always(HomePaths.LOCATION, body = """{"city":null}""")
        remote.fetchUserLocation()

        val location = remote.fetchUserLocation()

        assertEquals("Tallinn", location.city)
        assertEquals("Tallinn", userLocationSource.city.value)
    }

    @Test
    fun `a user nobody could place has no coordinates`() = runTest {
        server.always(HomePaths.LOCATION, body = """{"city":"Atlantis"}""")

        val location = remote.fetchUserLocation()

        assertEquals("Atlantis", location.city)
        assertNull(location.lat)
        assertNull(location.lon)
    }
}
