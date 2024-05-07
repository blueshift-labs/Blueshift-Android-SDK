package com.blueshift.core.network

class FakeNetworkRepoWith500 : BlueshiftNetworkRepository {
    override fun makeRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 500, responseBody = "{\"status\" : \"error\"}")
    }
}