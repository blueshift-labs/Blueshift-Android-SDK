package com.blueshift.core.network

interface BlueshiftNetworkRequestRepository {
    suspend fun insertRequest(networkRequest: BlueshiftNetworkRequest)
    suspend fun updateRequest(networkRequest: BlueshiftNetworkRequest)
    suspend fun deleteRequest(networkRequest: BlueshiftNetworkRequest)
    suspend fun readNextRequest(): BlueshiftNetworkRequest?
    suspend fun clear()
}