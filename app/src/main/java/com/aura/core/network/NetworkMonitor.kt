package com.aura.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
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
    val type: NetworkType = NetworkType.NONE,
    val operator: String? = null,
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

    private val telephony: TelephonyManager?
        get() = context.getSystemService(TelephonyManager::class.java)

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
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        return NetworkStatus(
            isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isVpnActive = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            type = capabilities.readType(),
            operator = if (isCellular) telephony?.networkOperatorName?.ifBlank { null } else null,
        )
    }

    private fun NetworkCapabilities.readType(): NetworkType = when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularGeneration()
        else -> NetworkType.NONE
    }

    private fun cellularGeneration(): NetworkType {
        val manager = telephony ?: return NetworkType.MOBILE_4G

        val dataNetworkType = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                manager.dataNetworkType
            } else {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            }
        } catch (security: SecurityException) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }

        return when (dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> NetworkType.MOBILE_2G

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.MOBILE_3G

            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> NetworkType.MOBILE_4G

            NETWORK_TYPE_NR -> NetworkType.MOBILE_5G

            else -> NetworkType.MOBILE_4G
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val NETWORK_TYPE_NR = 20
    }
}
