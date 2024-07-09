package com.blueshift.core

import android.util.Log
import com.blueshift.core.events.BlueshiftEvent
import com.blueshift.core.events.FakeEventsRepo
import com.blueshift.core.network.FakeNetworkRequestRepo
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class BlueshiftEventManagerTest {
    private lateinit var fakeNetworkRequestRepo: FakeNetworkRequestRepo
    private lateinit var fakeEventsRepo: FakeEventsRepo

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        fakeNetworkRequestRepo = FakeNetworkRequestRepo()
        fakeEventsRepo = FakeEventsRepo()

        BlueshiftEventManager.initialize(
            fakeEventsRepo, fakeNetworkRequestRepo, BlueshiftLambdaQueue
        )
    }

    @Test
    fun trackCampaignEvent_shouldNotInsertAnyNetworkRequestWhenQueryStringIsNotEmpty() = runBlocking {
        // Empty query string
        BlueshiftEventManager.trackCampaignEvent(queryString = "")
        assert(fakeNetworkRequestRepo.requests.size == 0)
    }

    @Test
    fun trackCampaignEvent_shouldInsertOneNetworkRequestWhenQueryStringIsNotEmpty() = runBlocking {
        // Query string with key and value
        BlueshiftEventManager.trackCampaignEvent(queryString = "key=value&number=1")
        assert(fakeNetworkRequestRepo.requests.size == 1)

        // The url should contain the query string
        assert(fakeNetworkRequestRepo.requests[0].url.contains("key=value&number=1"))
    }

    @Test
    fun trackEvent_shouldInsertOneNetworkRequestWhenOnline() = runBlocking {
        BlueshiftEventManager.trackEvent(
            event = BlueshiftEvent(
                id = 1,
                eventName = "test",
                eventParams = JSONObject(),
                timestamp = System.currentTimeMillis()
            ),
            isBatchEvent = false // when online, we don't add events as batched events.
        )

        // A new request should be added to network request repo
        assert(fakeNetworkRequestRepo.requests.size == 1)
        // No request should be added to events repo (offline events)
        assert(fakeEventsRepo.blueshiftEvents.size == 0)
    }

    @Test
    fun trackEvent_shouldInsertOneEventWhenOffline() = runBlocking {
        BlueshiftEventManager.trackEvent(
            event = BlueshiftEvent(
                id = 1,
                eventName = "test",
                eventParams = JSONObject(),
                timestamp = 0L
            ),
            isBatchEvent = true // we add a batched event when we are offline
        )

        // No request should be added to network request repo
        assert(fakeNetworkRequestRepo.requests.size == 0)
        // A new request should be added to events repo (offline events)
        assert(fakeEventsRepo.blueshiftEvents.size == 1)
    }

    private suspend fun insertBatchEvents(count: Int) {
        for (i in 1..count) {
            BlueshiftEventManager.trackEvent(
                event = BlueshiftEvent(
                    id = i.toLong(),
                    eventName = "test",
                    eventParams = JSONObject(),
                    timestamp = 0L
                ),
                isBatchEvent = true
            )
        }
    }

    @Test
    fun buildAndEnqueueBatchEvents_shouldInsertABulkEventRequestPer100OfflineEvents() =
        runBlocking {
            insertBatchEvents(200)

            BlueshiftEventManager.buildAndEnqueueBatchEvents()

            // Two bulk event requests should be added in the network request repo
            assert(fakeNetworkRequestRepo.requests.size == 2)
            // After sync, the events repo should be empty
            assert(fakeEventsRepo.blueshiftEvents.size == 0)
        }

    @Test
    fun buildAndEnqueueBatchEvents_shouldInsertOneBulkEventRequestForLessThan100OfflineEvents() =
        runBlocking {
            insertBatchEvents(50)

            BlueshiftEventManager.buildAndEnqueueBatchEvents()

            // Two bulk event requests should be added in the network request repo
            assert(fakeNetworkRequestRepo.requests.size == 1)
            // After sync, the events repo should be empty
            assert(fakeEventsRepo.blueshiftEvents.size == 0)
        }
}
