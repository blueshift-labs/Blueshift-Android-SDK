package com.blueshift.core.network

class FakeNetworkRepoWithAPISuccess : BlueshiftNetworkRepository {
    override fun makeRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return BlueshiftNetworkResponse(responseCode = 200, responseBody = "{\"status\" : \"error\"}")
    }
}