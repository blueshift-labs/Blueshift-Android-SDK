package com.blueshift.core.network

class FakeNetworkRepoWithAPIError : BlueshiftNetworkRepository {
    override fun makeRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 500, responseBody = "{\"status\" : \"error\"}")
    }
}