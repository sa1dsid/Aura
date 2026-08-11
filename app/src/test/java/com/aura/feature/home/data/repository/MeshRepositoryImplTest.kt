package com.aura.feature.home.data.repository

import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.feature.home.data.remote.MeshRemoteDataSource
import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.MeshSnapshotDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import com.aura.feature.home.domain.model.NodesOnline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.IOException
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshRepositoryImplTest {

    private val remote = FakeMeshRemoteDataSource()
    private val networkMonitor = FakeNetworkMonitor()

    private fun repository() = MeshRepositoryImpl(
        remote = remote,
        networkMonitor = networkMonitor,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `reports live count after successful refresh`() = runTest {
        val repository = repository()

        repository.refresh(force = true)

        assertEquals(NodesOnline.Live(4210), repository.observeMesh().first().nodesOnline)
    }

    @Test
    fun `keeps last known count when server stops answering`() = runTest {
        val repository = repository()
        repository.refresh(force = true)

        remote.failSnapshot = true
        repository.refresh(force = true)

        assertEquals(NodesOnline.LastKnown(4210), repository.observeMesh().first().nodesOnline)
    }

    @Test
    fun `never reports zero when server never answered`() = runTest {
        remote.failSnapshot = true
        val repository = repository()

        repository.refresh(force = true)

        assertEquals(NodesOnline.Unknown, repository.observeMesh().first().nodesOnline)
    }

    @Test
    fun `skips network while cache is fresh`() = runTest {
        val repository = repository()
        repository.refresh(force = true)

        repository.refresh(force = false)

        assertEquals(1, remote.snapshotCalls)
    }

    @Test
    fun `keeps previous user position when location request fails`() = runTest {
        val repository = repository()
        repository.refresh(force = true)
        val before = repository.observeMesh().first().userPresence

        remote.failLocation = true
        repository.refresh(force = true)

        assertEquals(before, repository.observeMesh().first().userPresence)
        assertTrue(before != null)
    }

    @Test
    fun `keeps the pre vpn position while vpn is on`() = runTest {
        val repository = repository()
        repository.refresh(force = true)
        val beforeVpn = repository.observeMesh().first().userPresence

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = true))
        remote.location = UserLocationDto(52.37, 4.90, "Amsterdam", vpnActive = true)
        repository.refresh(force = true)

        val duringVpn = repository.observeMesh().first().userPresence
        assertEquals(beforeVpn?.location, duringVpn?.location)
        assertEquals("London", duringVpn?.cityName)
    }

    @Test
    fun `marks the position as pinned while vpn is on`() = runTest {
        val repository = repository()
        repository.refresh(force = true)

        networkMonitor.set(NetworkStatus(isOnline = true, isVpnActive = true))

        assertTrue(repository.observeMesh().first().userPresence?.isPinnedByVpn == true)
    }

    @Test
    fun `hides the dot when the backend cannot resolve the location`() = runTest {
        val repository = repository()
        remote.location = UserLocationDto(null, null, null, vpnActive = false)

        repository.refresh(force = true)

        assertNull(repository.observeMesh().first().userPresence)
    }

    private class FakeMeshRemoteDataSource : MeshRemoteDataSource {
        var failSnapshot = false
        var failLocation = false
        var snapshotCalls = 0
        var location = UserLocationDto(51.51, -0.13, "London", vpnActive = false)

        override suspend fun fetchMeshSnapshot(): MeshSnapshotDto {
            if (failSnapshot) throw IOException("offline")
            snapshotCalls++
            return MeshSnapshotDto(
                cities = listOf(
                    MeshCityDto("london", "London", 51.51, -0.13, live = true),
                ),
                nodesOnline = 4210,
            )
        }

        override suspend fun fetchUserLocation(): UserLocationDto {
            if (failLocation) throw IOException("offline")
            return location
        }
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        private val state = MutableStateFlow(NetworkStatus(isOnline = true, isVpnActive = false))

        override val status: StateFlow<NetworkStatus> = state

        override fun current(): NetworkStatus = state.value

        fun set(value: NetworkStatus) {
            state.value = value
        }
    }
}
