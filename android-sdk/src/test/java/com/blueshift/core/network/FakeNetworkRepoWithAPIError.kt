package com.blueshift.core.network

class FakeNetworkRepoWithAPIError : BlueshiftNetworkRepository {
    override suspend fun makeNetworkRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 500, responseBody = "{\"status\" : \"error\"}")
    }
}