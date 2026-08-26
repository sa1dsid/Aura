package com.aura.core.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

private val serverJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}

private class SameThreadExecutorService : AbstractExecutorService() {

    private var stopped = false

    override fun execute(command: Runnable) = command.run()

    override fun shutdown() {
        stopped = true
    }

    override fun shutdownNow(): MutableList<Runnable> {
        stopped = true
        return mutableListOf()
    }

    override fun isShutdown(): Boolean = stopped

    override fun isTerminated(): Boolean = stopped

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}

internal class RoutingApiServer {

    private val replies = mutableMapOf<String, ArrayDeque<MockResponse>>()
    private val standing = mutableMapOf<String, MockResponse>()
    private val recorded = mutableListOf<RecordedRequest>()

    private val server = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(this@RoutingApiServer) {
                    recorded += request
                    val path = request.path?.substringBefore('?').orEmpty()
                    return replies[path]?.removeFirstOrNull()
                        ?: standing[path]
                        ?: MockResponse().setResponseCode(404).setBody("""{"detail":"no stub"}""")
                }
            }
        }
    }

    val api: AuraApi = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(
            OkHttpClient.Builder()
                .dispatcher(okhttp3.Dispatcher(SameThreadExecutorService()))
                .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .callTimeout(CALL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build()
        )
        .addConverterFactory(serverJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AuraApi::class.java)

    fun always(path: String, code: Int = 200, body: String = "{}") = synchronized(this) {
        standing[path] = json(code, body)
    }

    fun next(path: String, code: Int = 200, body: String = "{}") = synchronized(this) {
        replies.getOrPut(path) { ArrayDeque() } += json(code, body)
    }

    fun nextDropsConnection(path: String) = synchronized(this) {
        replies.getOrPut(path) { ArrayDeque() } +=
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
    }

    fun requests(): List<RecordedRequest> = synchronized(this) { recorded.toList() }

    fun paths(): List<String> = requests().mapNotNull { it.path?.substringBefore('?') }

    fun hits(path: String): Int = paths().count { it == path }

    fun bodyOf(path: String): String =
        requests().last { it.path?.substringBefore('?') == path }.body.readUtf8()

    fun bodiesOf(path: String): List<String> = requests()
        .filter { it.path?.substringBefore('?') == path }
        .map { it.body.readUtf8() }

    fun shutdown() = server.shutdown()

    private fun json(code: Int, body: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val READ_TIMEOUT_MILLIS = 400L
        const val CALL_TIMEOUT_MILLIS = 2_000L
    }
}
