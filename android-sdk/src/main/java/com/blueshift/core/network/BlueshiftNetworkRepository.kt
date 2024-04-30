package com.blueshift.core.network

interface BlueshiftNetworkRepository {
    fun makeRequest(networkRequest: BlueshiftNetworkRequest) : BlueshiftNetworkResponse
}