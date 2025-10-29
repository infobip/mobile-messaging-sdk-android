/*
 * NetworkState.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

internal enum class NetworkState {
    AVAILABLE, UNAVAILABLE
}

internal fun ConnectivityManager.networkStateFlow(
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
): StateFlow<NetworkState> {
    return NetworkStateProviderImpl(this, coroutineScope, coroutineDispatcher).getFlow()
}

internal interface NetworkStateProvider {
    fun getFlow(): StateFlow<NetworkState>
}

internal class NetworkStateProviderImpl(
    private val connectivityManager: ConnectivityManager,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher,
) : NetworkStateProvider {

    override fun getFlow() = createStateFlow()

    private fun createStateFlow(): StateFlow<NetworkState> {
        return callbackFlow {
            val networkStatusCallback = object : ConnectivityManager.NetworkCallback() {

                override fun onUnavailable() {
                    trySend(NetworkState.UNAVAILABLE)
                }

                override fun onAvailable(network: Network) {
                    trySend(NetworkState.AVAILABLE)
                }

                override fun onLost(network: Network) {
                    trySend(NetworkState.UNAVAILABLE)
                }
            }

            val request: NetworkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkStatusCallback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(networkStatusCallback)
            }
        }
            .flowOn(coroutineDispatcher)
            .stateIn(coroutineScope, SharingStarted.Eagerly, getState())
    }

    @Suppress("DEPRECATION")
    private fun getState(): NetworkState {
        val hasConnection: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
        return if (hasConnection) NetworkState.AVAILABLE else NetworkState.UNAVAILABLE
    }

}