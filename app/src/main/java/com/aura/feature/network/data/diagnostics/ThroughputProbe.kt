package com.aura.feature.network.data.diagnostics

import com.aura.core.common.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=%d"

private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"

private const val DOWNLOAD_BYTES = 25_000_000L

private const val UPLOAD_BYTES = 8_000_000L

private const val CHUNK_BYTES = 64 * 1024

private const val TIMEOUT_SECONDS = 60L

private const val BITS_IN_BYTE = 8.0

private const val BITS_IN_MEGABIT = 1_000_000.0

private const val NANOS_IN_SECOND = 1_000_000_000.0

data class Throughput(
    val megabitsPerSecond: Double,
    val transferredBytes: Long,
)

interface ThroughputProbe {
    suspend fun download(onProgress: (Float) -> Unit): Throughput?

    suspend fun upload(onProgress: (Float) -> Unit): Throughput?
}

@Singleton
class HttpThroughputProbe @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ThroughputProbe {

    private val client: OkHttpClient by lazy(LazyThreadSafetyMode.PUBLICATION) {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun download(onProgress: (Float) -> Unit): Throughput? =
        withContext(ioDispatcher) {
            val request = Request.Builder()
                .url(DOWNLOAD_URL.format(DOWNLOAD_BYTES))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body ?: return@withContext null
                    if (!response.isSuccessful) return@withContext null

                    val buffer = ByteArray(CHUNK_BYTES)
                    var received = 0L
                    val startedAt = System.nanoTime()

                    body.byteStream().use { stream ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = stream.read(buffer)
                            if (read <= 0) break
                            received += read
                            onProgress((received.toFloat() / DOWNLOAD_BYTES).coerceIn(0f, 1f))
                        }
                    }

                    throughput(received, System.nanoTime() - startedAt)
                }
            } catch (error: IOException) {
                null
            }
        }

    override suspend fun upload(onProgress: (Float) -> Unit): Throughput? =
        withContext(ioDispatcher) {
            var sent = 0L

            val body = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()

                override fun contentLength() = UPLOAD_BYTES

                override fun writeTo(sink: BufferedSink) {
                    val chunk = ByteArray(CHUNK_BYTES)
                    while (sent < UPLOAD_BYTES) {
                        val size = minOf(CHUNK_BYTES.toLong(), UPLOAD_BYTES - sent).toInt()
                        sink.write(chunk, 0, size)
                        sent += size
                        onProgress((sent.toFloat() / UPLOAD_BYTES).coerceIn(0f, 1f))
                    }
                }
            }

            val request = Request.Builder().url(UPLOAD_URL).post(body).build()

            try {
                val startedAt = System.nanoTime()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    throughput(sent, System.nanoTime() - startedAt)
                }
            } catch (error: IOException) {
                null
            }
        }

    private fun throughput(bytes: Long, elapsedNanos: Long): Throughput? {
        if (bytes <= 0 || elapsedNanos <= 0) return null
        val seconds = elapsedNanos / NANOS_IN_SECOND

        return Throughput(
            megabitsPerSecond = bytes * BITS_IN_BYTE / BITS_IN_MEGABIT / seconds,
            transferredBytes = bytes,
        )
    }
}
