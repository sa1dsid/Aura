package com.aura.feature.network

import com.aura.feature.network.data.diagnostics.PingProbe
import com.aura.feature.network.data.diagnostics.PingSample
import com.aura.feature.network.data.diagnostics.Throughput
import com.aura.feature.network.data.diagnostics.ThroughputProbe
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CopyOnWriteArrayList

internal fun sampleOf(
    vararg roundTripsMs: Double,
    attempts: Int = roundTripsMs.size,
): PingSample = PingSample(roundTripsMs = roundTripsMs.toList(), attempts = attempts)

internal class FakePingProbe : PingProbe {

    @Volatile
    var measurement: Int? = 27

    @Volatile
    var sample: PingSample = sampleOf(24.0, 26.0, 28.0, 30.0)

    val requestedAttempts: MutableList<Int> = CopyOnWriteArrayList()

    @Volatile
    var onStep: (() -> Unit)? = null

    var measures = 0
        private set

    override suspend fun measure(): Int? {
        measures++
        return measurement
    }

    override suspend fun sample(attempts: Int): PingSample {
        requestedAttempts += attempts
        onStep?.invoke()
        return sample
    }
}

internal class FakeThroughputProbe : ThroughputProbe {

    @Volatile
    var downloadResult: Throughput? = Throughput(megabitsPerSecond = 48.63, transferredBytes = 0)

    @Volatile
    var uploadResult: Throughput? = Throughput(megabitsPerSecond = 12.44, transferredBytes = 0)

    @Volatile
    var downloadProgress: List<Float> = listOf(1f)

    @Volatile
    var uploadProgress: List<Float> = listOf(1f)

    @Volatile
    var holdsDownload = false

    @Volatile
    var onStep: (() -> Unit)? = null

    var downloads = 0
        private set

    var uploads = 0
        private set

    private val downloadEntered = CompletableDeferred<Unit>()

    private val downloadRelease = CompletableDeferred<Unit>()

    suspend fun awaitDownloadStarted() = downloadEntered.await()

    fun releaseDownload() {
        downloadRelease.complete(Unit)
    }

    override suspend fun download(onProgress: (Float) -> Unit): Throughput? {
        downloads++
        onStep?.invoke()
        downloadProgress.forEach { progress ->
            onProgress(progress)
            onStep?.invoke()
        }
        downloadEntered.complete(Unit)
        if (holdsDownload) downloadRelease.await()
        return downloadResult
    }

    override suspend fun upload(onProgress: (Float) -> Unit): Throughput? {
        uploads++
        onStep?.invoke()
        uploadProgress.forEach { progress ->
            onProgress(progress)
            onStep?.invoke()
        }
        return uploadResult
    }
}
