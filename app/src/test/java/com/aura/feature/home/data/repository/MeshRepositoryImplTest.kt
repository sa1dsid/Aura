package com.aura.feature.home.data.repository

import com.aura.feature.home.data.remote.MeshRemoteDataSource
import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.MeshSnapshotDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import com.aura.feature.home.domain.model.NodesOnline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.IOException
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshRepositoryImplTest {

    private val remote = FakeMeshRemoteDataSource()

    private fun repository() = MeshRepositoryImpl(
        remote = remote,
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

    private class FakeMeshRemoteDataSource : MeshRemoteDataSource {
        var failSnapshot = false
        var failLocation = false
        var snapshotCalls = 0

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
            return UserLocationDto(51.51, -0.13, "London", vpnActive = false)
        }
    }
}
