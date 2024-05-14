package com.blueshift.core

import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRepository
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.atomic.AtomicBoolean

object BlueshiftNetworkQueueManager {
    private const val TAG = "RequestQueueManager"
    private lateinit var networkRequestRepository: BlueshiftNetworkRequestRepository
    private lateinit var networkRepository: BlueshiftNetworkRepository
    private val isSyncing = AtomicBoolean(false) // to prevent concurrent access to the sync method
    private val lock = Any() // to prevent concurrent access to the database

    fun initialize(
        networkRequestRepository: BlueshiftNetworkRequestRepository,
        networkRepository: BlueshiftNetworkRepository
    ) {
        synchronized(lock) {
            this.networkRequestRepository = networkRequestRepository
            this.networkRepository = networkRepository
        }
    }

    suspend fun insertNewRequest(request: BlueshiftNetworkRequest) {
        networkRequestRepository.insertRequest(request)
    }

    suspend fun updateRequest(request: BlueshiftNetworkRequest) {
        networkRequestRepository.updateRequest(request)
    }

    suspend fun deleteRequest(request: BlueshiftNetworkRequest) {
        networkRequestRepository.deleteRequest(request)
    }

    suspend fun readNextRequest(): BlueshiftNetworkRequest? {
        return networkRequestRepository.readNextRequest()
    }

    suspend fun sync() {
        // Prevent concurrent access to the sync method.
        if (!isSyncing.compareAndSet(false, true)) {
            BlueshiftLogger.d("$TAG: Syncing = true. Ignoring the duplicate call.")
            return
        }

        try {
            while (true) {
                // break the look when networkRequest is null.
                val networkRequest = readNextRequest()
                BlueshiftLogger.d("$TAG: Dequeue request = $networkRequest")
                if (networkRequest == null) break

                if (networkRequest.authorizationRequired) {
                    networkRequest.authorization = BlueshiftNetworkConfiguration.authorization
                }

                val response = networkRepository.makeNetworkRequest(networkRequest = networkRequest)
                if (response.responseCode == HTTP_OK) {
                    BlueshiftLogger.d("$TAG: Event sent to Blueshift. Delete request = $networkRequest")
                    deleteRequest(networkRequest)
                } else if (response.responseCode == 0) {
                    BlueshiftLogger.d("$TAG: No internet connection. Sync paused.")
                    break
                } else {
                    networkRequest.retryAttemptBalance--

                    if (networkRequest.retryAttemptBalance > 0) {
                        val intervalMs =
                            BlueshiftNetworkConfiguration.requestRetryIntervalInMilliseconds

                        networkRequest.retryAttemptTimestamp =
                            System.currentTimeMillis() + intervalMs

                        // reset authorization to avoid storing it in db
                        networkRequest.authorization = null

                        BlueshiftLogger.d("$TAG: We should retry $networkRequest")
                        updateRequest(networkRequest)
                    } else {
                        BlueshiftLogger.d("$TAG: Retry limit exceeded! Delete request = $networkRequest")
                        deleteRequest(networkRequest)
                    }
                }
            }
        } finally {
            isSyncing.set(false)
        }
    }
}