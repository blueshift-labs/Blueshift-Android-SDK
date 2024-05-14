package com.blueshift.core

import com.blueshift.core.events.BlueshiftEvent
import com.blueshift.core.events.FakeEventsRepo
import com.blueshift.core.network.FakeNetworkRequestRepo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class BlueshiftEventManagerTest {
    private lateinit var fakeNetworkRequestRepo: FakeNetworkRequestRepo
    private lateinit var fakeEventsRepo: FakeEventsRepo

    @Before
    fun setUp() {
        fakeNetworkRequestRepo = FakeNetworkRequestRepo()
        fakeEventsRepo = FakeEventsRepo()

        BlueshiftEventManager.initialize(fakeEventsRepo, fakeNetworkRequestRepo)
    }

    @Test
    fun trackEvent_shouldInsertOneNetworkRequestWhenOnline() = runBlocking {
        val isOnline = true

        BlueshiftEventManager.trackEvent(
            event = BlueshiftEvent(
                id = 1,
                eventName = "test",
                eventParams = JSONObject(),
                timestamp = System.currentTimeMillis()
            ),
            isBatchEvent = isOnline
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
            isBatchEvent = false
        )

        // No request should be added to network request repo
        assert(fakeNetworkRequestRepo.requests.size == 0)
        // A new request should be added to events repo (offline events)
        assert(fakeEventsRepo.blueshiftEvents.size == 1)
    }

    private suspend fun insertOfflineEvents(count: Int) {
        for (i in 1..count) {
            BlueshiftEventManager.trackEvent(
                event = BlueshiftEvent(
                    id = i.toLong(),
                    eventName = "test",
                    eventParams = JSONObject(),
                    timestamp = 0L
                ),
                isBatchEvent = false
            )
        }
    }

    @Test
    fun sync_shouldInsertABulkEventRequestPer100OfflineEvents() = runBlocking {
        insertOfflineEvents(200)

        BlueshiftEventManager.sync()

        // Two bulk event requests should be added in the network request repo
        assert(fakeNetworkRequestRepo.requests.size == 2)
        // After sync, the events repo should be empty
        assert(fakeEventsRepo.blueshiftEvents.size == 0)
    }

    @Test
    fun sync_shouldInsertOneBulkEventRequestForLessThan100OfflineEvents() = runBlocking {
        insertOfflineEvents(50)

        BlueshiftEventManager.sync()

        // Two bulk event requests should be added in the network request repo
        assert(fakeNetworkRequestRepo.requests.size == 1)
        // After sync, the events repo should be empty
        assert(fakeEventsRepo.blueshiftEvents.size == 0)
    }
}
