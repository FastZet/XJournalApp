package com.brksays.xjournal.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService

class NetworkManager(private val context: Context) {
    private var isNetworkEnabled = false
    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    // Enable network only when explicitly requested
    fun enableNetworkForSync(callback: (Boolean) -> Unit) {
        if (isNetworkEnabled) {
            callback(true)
            return
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isNetworkEnabled = true
                callback(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isNetworkEnabled = false
                callback(false)
            }
        })
    }

    fun disableNetwork() {
        isNetworkEnabled = false
        // Additional cleanup if needed
    }

    fun isNetworkEnabled() = isNetworkEnabled
}
