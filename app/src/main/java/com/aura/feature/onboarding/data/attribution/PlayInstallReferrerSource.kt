package com.aura.feature.onboarding.data.attribution

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private val CODE_KEYS = listOf("code", "invite", "invite_code")

@Singleton
class PlayInstallReferrerSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstallReferrerSource {

    override suspend fun inviteCode(): String? {
        val client = InstallReferrerClient.newBuilder(context).build()
        return try {
            if (client.awaitConnection() != InstallReferrerClient.InstallReferrerResponse.OK) {
                null
            } else {
                client.installReferrer.installReferrer?.let(::parseInviteCode)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        } finally {
            runCatching { client.endConnection() }
        }
    }
}

private suspend fun InstallReferrerClient.awaitConnection(): Int =
    suspendCancellableCoroutine { continuation ->
        val settled = AtomicBoolean(false)

        startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (settled.compareAndSet(false, true)) continuation.resume(responseCode)
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (settled.compareAndSet(false, true)) {
                    continuation.resume(InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED)
                }
            }
        })
    }

internal fun parseInviteCode(referrer: String): String? {
    val parameters = referrer.split('&')
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                Uri.decode(part.take(separator)) to Uri.decode(part.substring(separator + 1))
            }
        }
        .toMap()

    val fromKey = CODE_KEYS.firstNotNullOfOrNull { parameters[it] }
    if (fromKey != null) return fromKey

    return referrer.takeIf { '=' !in it && '&' !in it }
}
