package com.tdull.webdavviewer.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络状态
 */
data class NetworkStatus(
    val isAvailable: Boolean,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false
)

/**
 * 网络状态监控工具
 * 提供实时网络状态变化监听
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * 网络状态Flow
     */
    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val status = getCurrentNetworkStatus()
                trySend(status)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus(isAvailable = false))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val status = NetworkStatus(
                    isAvailable = true,
                    isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
                trySend(status)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // 发送当前状态
        trySend(getCurrentNetworkStatus())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * 获取当前网络状态
     */
    fun getCurrentNetworkStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return if (capabilities != null && 
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            NetworkStatus(
                isAvailable = true,
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            )
        } else {
            NetworkStatus(isAvailable = false)
        }
    }

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return getCurrentNetworkStatus().isAvailable
    }

    /**
     * 检查是否为WiFi连接
     */
    fun isWifiConnection(): Boolean {
        return getCurrentNetworkStatus().let { it.isAvailable && it.isWifi }
    }

    /**
     * 检查是否为移动网络
     */
    fun isCellularConnection(): Boolean {
        return getCurrentNetworkStatus().let { it.isAvailable && it.isCellular }
    }
}
