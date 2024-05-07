package com.blueshift.core.network

class FakeNetworkRepoWith200 : BlueshiftNetworkRepository {
    override fun makeRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 200, responseBody = "{\"status\" : \"error\"}")
    }
}