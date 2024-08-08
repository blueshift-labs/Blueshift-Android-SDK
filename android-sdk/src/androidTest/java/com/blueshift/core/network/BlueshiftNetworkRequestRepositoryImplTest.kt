package com.blueshift.core.network

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test

class BlueshiftNetworkRequestRepositoryImplTest {
    private lateinit var repository: BlueshiftNetworkRequestRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = BlueshiftNetworkRequestRepositoryImpl(context)
    }

    @After
    fun tearDown() = runBlocking {
        repository.clear()
    }

    @Test
    fun insertRequest_insertsRequestsToTheSQLiteDatabase(): Unit = runBlocking {
        val url = "https://example.com"
        val method = BlueshiftNetworkRequest.Method.GET
        val header = JSONObject(mapOf("Content-Type" to "application/json"))
        val body = JSONObject()
        val authRequired = true
        val retryBalance = 1
        val retryTimestamp = 1234567890L
        val timestamp = 1234567890L

        val request = BlueshiftNetworkRequest(
            url = url,
            method = method,
            header = header,
            body = body,
            authorizationRequired = authRequired,
            retryAttemptBalance = retryBalance,
            retryAttemptTimestamp = retryTimestamp,
            timestamp = timestamp
        )

        repository.insertRequest(request)
        val request2 = repository.readNextRequest()

        assert(request2 != null)
        request2?.let {
            assert(it.url == url)
            assert(it.method == method)
            assert(it.header.toString() == header.toString())
            assert(it.body.toString() == body.toString())
            assert(it.authorizationRequired == authRequired)
            assert(it.retryAttemptBalance == retryBalance)
            assert(it.retryAttemptTimestamp == retryTimestamp)
            assert(it.timestamp == timestamp)
        }
    }

    @Test
    fun updateRequest_updatesRequestsInTheSQLiteDatabase(): Unit = runBlocking {
        val url = "https://example.com"
        val method = BlueshiftNetworkRequest.Method.GET
        val header = JSONObject(mapOf("Content-Type" to "application/json"))
        val body = JSONObject()
        val authRequired = true
        val retryBalance = 1
        val retryTimestamp = 1234567890L
        val timestamp = 1234567890L

        val request = BlueshiftNetworkRequest(
            url = url,
            method = method,
            header = header,
            body = body,
            authorizationRequired = authRequired,
            retryAttemptBalance = retryBalance,
            retryAttemptTimestamp = retryTimestamp,
            timestamp = timestamp
        )

        repository.insertRequest(request)

        // the code only allows updating the following two fields.
        val retryBalance2 = 2
        val retryTimestamp2 = 9876543210L

        val request2 = repository.readNextRequest()
        request2?.let {
            it.retryAttemptBalance = retryBalance2
            it.retryAttemptTimestamp = retryTimestamp2

            repository.updateRequest(it)
        }

        val request3 = repository.readNextRequest()
        assert(request3 != null)
        request3?.let {
            assert(it.retryAttemptBalance == retryBalance2)
            assert(it.retryAttemptTimestamp == retryTimestamp2)
        }
    }

    @Test
    fun deleteRequest_deletesRequestsInTheSQLiteDatabase(): Unit = runBlocking {
        val url = "https://example.com"
        val method = BlueshiftNetworkRequest.Method.GET
        val header = JSONObject(mapOf("Content-Type" to "application/json"))
        val body = JSONObject()
        val authRequired = true
        val retryBalance = 1
        val retryTimestamp = 1234567890L
        val timestamp = 1234567890L

        val request = BlueshiftNetworkRequest(
            url = url,
            method = method,
            header = header,
            body = body,
            authorizationRequired = authRequired,
            retryAttemptBalance = retryBalance,
            retryAttemptTimestamp = retryTimestamp,
            timestamp = timestamp
        )

        repository.insertRequest(request)

        val request2 = repository.readNextRequest()
        request2?.let {
            repository.deleteRequest(it)
        }

        val request3 = repository.readNextRequest()
        assert(request3 == null)
    }

    @Test
    fun readNextRequest_shouldReturnTheFirstRequestWhenAllRequestsInTheQueueRetryAttemptBalanceGreaterThanZero(): Unit =
        runBlocking {
            for (i in 1..3) {
                val url = "https://api.com/$i"
                val method = BlueshiftNetworkRequest.Method.GET
                val header = JSONObject(mapOf("Content-Type" to "application/json"))
                val body = JSONObject()
                val authRequired = true
                val retryBalance = 3
                val retryTimestamp = 0L
                val timestamp = System.currentTimeMillis()
                val request = BlueshiftNetworkRequest(
                    url = url,
                    method = method,
                    header = header,
                    body = body,
                    authorizationRequired = authRequired,
                    retryAttemptBalance = retryBalance,
                    retryAttemptTimestamp = retryTimestamp,
                    timestamp = timestamp
                )
                repository.insertRequest(request)
            }

            val request = repository.readNextRequest()
            assert(request != null)
            request?.let {
                assert(it.url == "https://api.com/1")
            }
        }

    @Test
    fun readNextRequest_shouldReturnNullWhenAllRequestsInTheQueueHasRetryAttemptBalanceEqualToZero(): Unit =
        runBlocking {
            for (i in 1..3) {
                val url = "https://api.com/$i"
                val method = BlueshiftNetworkRequest.Method.GET
                val header = JSONObject(mapOf("Content-Type" to "application/json"))
                val body = JSONObject()
                val authRequired = true
                val retryBalance = 0
                val retryTimestamp = 0L
                val timestamp = System.currentTimeMillis()
                val request = BlueshiftNetworkRequest(
                    url = url,
                    method = method,
                    header = header,
                    body = body,
                    authorizationRequired = authRequired,
                    retryAttemptBalance = retryBalance,
                    retryAttemptTimestamp = retryTimestamp,
                    timestamp = timestamp
                )
                repository.insertRequest(request)
            }

            val request = repository.readNextRequest()
            assert(request == null)
        }

    @Test
    fun readNextRequest_shouldReturnTheRequestWithRetryAttemptTimestampLessThanCurrentTime(): Unit =
        runBlocking {
            for (i in 1..2) {
                val fiveMinutes = 5 * 60 * 1000
                val url = "https://api.com/$i"
                val method = BlueshiftNetworkRequest.Method.GET
                val header = JSONObject(mapOf("Content-Type" to "application/json"))
                val body = JSONObject()
                val authRequired = true
                val retryBalance = 1
                val retryTimestamp = if (i % 2 == 0) System.currentTimeMillis() + fiveMinutes else System.currentTimeMillis() - fiveMinutes
                val timestamp = System.currentTimeMillis()
                val request = BlueshiftNetworkRequest(
                    url = url,
                    method = method,
                    header = header,
                    body = body,
                    authorizationRequired = authRequired,
                    retryAttemptBalance = retryBalance,
                    retryAttemptTimestamp = retryTimestamp,
                    timestamp = timestamp
                )
                repository.insertRequest(request)
            }

            // i = 1 -> current time - 5min
            // i = 2 -> current time + 5min
            val request = repository.readNextRequest()
            assert(request != null)
            request?.let {
                assert(it.url == "https://api.com/1")
            }
        }
}
