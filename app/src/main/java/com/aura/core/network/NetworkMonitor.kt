package com.aura.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.aura.core.common.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkStatus(
    val isOnline: Boolean,
    val isVpnActive: Boolean,
) {
    companion object {
        val Offline = NetworkStatus(isOnline = false, isVpnActive = false)
    }
}

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>

    fun current(): NetworkStatus
}

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) : NetworkMonitor {

    private val connectivity: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)

    override fun current(): NetworkStatus = connectivity.readStatus()

    override val status: StateFlow<NetworkStatus> = callbackFlow {
        val manager = connectivity
        if (manager == null) {
            trySend(NetworkStatus.Offline)
            awaitClose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(manager.readStatus())
                }

                override fun onLost(network: Network) {
                    trySend(manager.readStatus())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(manager.readStatus())
                }
            }

            manager.registerDefaultNetworkCallback(callback)
            trySend(manager.readStatus())

            awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = current(),
        )

    private fun ConnectivityManager?.readStatus(): NetworkStatus {
        val capabilities = this?.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus.Offline
        return NetworkStatus(
            isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isVpnActive = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
