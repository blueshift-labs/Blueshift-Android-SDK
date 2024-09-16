package com.blueshift.core.network

class FakeNetworkRepoWithAPISuccess : BlueshiftNetworkRepository {
    override suspend fun makeNetworkRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 200, responseBody = "{\"status\" : \"error\"}")
    }
}