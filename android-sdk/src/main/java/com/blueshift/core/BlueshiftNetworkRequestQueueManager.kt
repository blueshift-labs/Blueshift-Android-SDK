package com.blueshift.core

import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRepository
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.atomic.AtomicBoolean

object BlueshiftNetworkRequestQueueManager {
    private const val TAG = "NetworkRequestQueueManager"
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
        // Do not initiate the sync process if the authorization value is not available.
        //
        // Reason: If the authorization value is not set, it means that the SDK is not initialized
        // properly. Without proper event api key in place, most of the events api calls would fail.
        // So, we should not start the sync process until we get the authorization value set.
        //
        // Note: The campaign events doesn't require an event API key. However, syncing them without
        // initializing the SDK would cause compliance issues. Hence we're blocking the sync completely
        // until we get the authorization value set.
        BlueshiftNetworkConfiguration.authorization?.let { basicAuth ->
            // Prevent concurrent access to the sync method.
            if (!isSyncing.compareAndSet(false, true)) {
                BlueshiftLogger.d("$TAG: Sync is in-progress... Skipping the duplicate sync call.")
                return
            }

            try {
                while (true) {
                    // break the look when networkRequest is null.
                    val networkRequest = readNextRequest() ?: break
                    BlueshiftLogger.d("$TAG: Dequeue -> (Request ID: ${networkRequest.id})")

                    if (networkRequest.authorizationRequired) {
                        networkRequest.authorization = basicAuth
                    }

                    val response = networkRepository.makeNetworkRequest(
                        networkRequest = networkRequest
                    )

                    if (response.responseCode == HTTP_OK) {
                        BlueshiftLogger.d("$TAG: Remove -> (Request ID: ${networkRequest.id})")
                        deleteRequest(networkRequest)
                    } else if (response.responseCode == 0) {
                        BlueshiftLogger.d("$TAG: No internet connection. Pause sync!")
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

                            BlueshiftLogger.d("$TAG: Retry later -> (Request ID: ${networkRequest.id})")
                            updateRequest(networkRequest)
                        } else {
                            BlueshiftLogger.d("$TAG: Retry limit exceeded! Remove -> (Request ID: ${networkRequest.id})")
                            deleteRequest(networkRequest)
                        }
                    }
                }
            } finally {
                isSyncing.set(false)
            }
        }
    }
}
