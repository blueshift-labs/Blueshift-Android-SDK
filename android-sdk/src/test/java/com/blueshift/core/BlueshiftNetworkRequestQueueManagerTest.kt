package com.blueshift.core

import android.util.Log
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.FakeNetworkRepoWithAPIError
import com.blueshift.core.network.FakeNetworkRepoWithAPISuccess
import com.blueshift.core.network.FakeNetworkRequestRepo
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class BlueshiftNetworkRequestQueueManagerTest {
    private lateinit var networkRequestRepo: FakeNetworkRequestRepo

    companion object {
        const val REQUEST_COUNT = 2
    }

    @Before
    fun setUp() = runBlocking {
        networkRequestRepo = FakeNetworkRequestRepo()

        (1..REQUEST_COUNT).forEach {
            val networkRequest = BlueshiftNetworkRequest(
                id = it.toLong(),
                url = "https://fakeapi.com",
                method = BlueshiftNetworkRequest.Method.GET,
                body = null
            )

            networkRequestRepo.insertRequest(networkRequest = networkRequest)
        }

        BlueshiftNetworkConfiguration.authorization = "basicAuth"
    }

    @After
    fun tearDown() {
        networkRequestRepo.requests.clear()
    }

    @Test
    fun insertRequest_TheQueueShouldContainsTheInsertedNumberOfRequests() = runBlocking {
        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPISuccess())

        val request = BlueshiftNetworkRequest(
            id = 100,
            url = "https://fakeapi.com",
            method = BlueshiftNetworkRequest.Method.GET,
            body = null
        )

        requestQueueManager.insertNewRequest(request)

        // The request count should increment by one
        assert(networkRequestRepo.requests.size == REQUEST_COUNT + 1)
    }

    @Test
    fun deleteRequest_TheQueueShouldNotContainTheDeletedRequest() = runBlocking {
        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPISuccess())

        val request = requestQueueManager.readNextRequest()
        request?.let { requestQueueManager.deleteRequest(request) }

        // The deleted request should not be found in the repo
        assert(networkRequestRepo.requests.find { it.id == request?.id } == null)
    }

    @Test
    fun updateRequest_TheQueueShouldContainTheUpdatedRequestWithUpdatedRetryTimestamp() =
        runBlocking {
            val requestQueueManager = BlueshiftNetworkRequestQueueManager
            requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPISuccess())

            val newRetryTimestamp = System.currentTimeMillis() + 100
            val request = requestQueueManager.readNextRequest()
            request?.let {
                request.retryAttemptTimestamp = newRetryTimestamp
                requestQueueManager.updateRequest(request)
            }

            assert(networkRequestRepo.requests.find { it.id == request?.id }?.retryAttemptTimestamp == newRetryTimestamp)
        }

    @Test
    fun sync_ShouldClearTheQueueWhenApiReturnsSuccess() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPISuccess())

        requestQueueManager.sync()

        assert(networkRequestRepo.requests.isEmpty())
    }

    @Test
    fun sync_ShouldNotClearTheQueueWhenApiReturnsError() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPIError())

        requestQueueManager.sync()

        assert(networkRequestRepo.requests.size == REQUEST_COUNT)
    }

    @Test
    fun sync_ShouldDecrementTheRetryAttemptBalanceByOneWhenApiReturnsError() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPIError())

        requestQueueManager.sync()

        assert(networkRequestRepo.requests.filter { it.retryAttemptBalance == 2 }.size == REQUEST_COUNT)
    }

    @Test
    fun sync_ShouldSetNonZeroValueForRetryAttemptTimestampWhenApiReturnsError() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPIError())

        requestQueueManager.sync()

        assert(networkRequestRepo.requests.filter { it.retryAttemptTimestamp != 0L }.size == REQUEST_COUNT)
    }

    @Test
    fun sync_ShouldNotMakeAnyChangesToTheQueueWhenAuthorizationIsNull() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        BlueshiftNetworkConfiguration.authorization = null

        val requestQueueManager = BlueshiftNetworkRequestQueueManager
        requestQueueManager.initialize(networkRequestRepo, FakeNetworkRepoWithAPISuccess())

        requestQueueManager.sync()

        assert(networkRequestRepo.requests.size == REQUEST_COUNT)
    }
}