package com.blueshift.core.network

class FakeNetworkRequestRepo : BlueshiftNetworkRequestRepository {
     val requests = mutableListOf<BlueshiftNetworkRequest>()
    override fun insertRequest(networkRequest: BlueshiftNetworkRequest) {
        requests.add(networkRequest)
    }

    override fun updateRequest(networkRequest: BlueshiftNetworkRequest) {
        for (i in requests.indices) {
            if (requests[i].id == networkRequest.id) {
                requests[i] = networkRequest
                break
            }
        }
    }

    override fun deleteRequest(networkRequest: BlueshiftNetworkRequest) {
        requests.remove(networkRequest)
    }

    override fun readNextRequest(): BlueshiftNetworkRequest? {
        return if (requests.isEmpty()) {
            null
        } else {
            requests.find { it.retryAttemptBalance > 0 && it.retryAttemptTimestamp < System.currentTimeMillis() }
        }
    }
}