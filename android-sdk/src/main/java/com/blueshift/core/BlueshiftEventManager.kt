package com.blueshift.core

import com.blueshift.core.common.BlueshiftAPI
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.core.events.BlueshiftEvent
import com.blueshift.core.events.BlueshiftEventRepository
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object BlueshiftEventManager {
    private const val TAG = "BlueshiftEventManager"
    private lateinit var eventRepository: BlueshiftEventRepository
    private lateinit var networkRequestRepository: BlueshiftNetworkRequestRepository

    fun initialize(
        eventRepository: BlueshiftEventRepository,
        networkRequestRepository: BlueshiftNetworkRequestRepository,
    ) {
        this.eventRepository = eventRepository
        this.networkRequestRepository = networkRequestRepository
    }

    fun trackEventAsync(event: BlueshiftEvent, isBatchEvent: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            trackEvent(event, isBatchEvent)
            BlueshiftNetworkQueueManager.sync()
        }
    }

    suspend fun trackEvent(event: BlueshiftEvent, isBatchEvent: Boolean) {
        if (isBatchEvent) {
            BlueshiftLogger.d("$TAG: Inserting 1 batch event. Event = ${event.eventParams}")
            eventRepository.insertEvent(event)
        } else {
            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.EVENTS,
                authorization = BlueshiftNetworkConfiguration.authorization,
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = event.eventParams,
            )

            BlueshiftLogger.d("$TAG: Inserting 1 real-time event. Event = ${event.eventParams}")
            networkRequestRepository.insertRequest(request)
        }
    }

    suspend fun sync() {
        while (true) {
            val events = eventRepository.readOneBatch()

            if (events.isEmpty()) break

            val eventsArray = JSONArray()
            events.forEach { eventsArray.put(it.eventParams) }

            BlueshiftLogger.d("$TAG: Creating 1 bulk event with ${eventsArray.length()} event(s). Events = $eventsArray")

            val bulkEventPayload = JSONObject().apply {
                put("events", eventsArray)
            }

            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.BULK_EVENTS,
                authorization = BlueshiftNetworkConfiguration.authorization,
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = bulkEventPayload,
            )

            networkRequestRepository.insertRequest(request)

            eventRepository.deleteEvents(events)
        }
    }
}