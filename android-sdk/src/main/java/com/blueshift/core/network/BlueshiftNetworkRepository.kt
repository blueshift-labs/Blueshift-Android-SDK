package com.blueshift.core.network

interface BlueshiftNetworkRepository {
    suspend fun makeNetworkRequest(networkRequest: BlueshiftNetworkRequest) : BlueshiftNetworkResponse
}