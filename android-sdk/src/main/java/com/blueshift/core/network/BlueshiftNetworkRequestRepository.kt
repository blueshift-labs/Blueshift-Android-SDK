package com.blueshift.core.network

interface BlueshiftNetworkRequestRepository {
    fun insertRequest(networkRequest: BlueshiftNetworkRequest)
    fun updateRequest(networkRequest: BlueshiftNetworkRequest)
    fun deleteRequest(networkRequest: BlueshiftNetworkRequest)
    fun readNextRequest(): BlueshiftNetworkRequest?
}