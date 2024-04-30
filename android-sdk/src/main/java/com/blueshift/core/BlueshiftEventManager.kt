package com.blueshift.core

import com.blueshift.core.common.BlueshiftAPI
import com.blueshift.core.events.BlueshiftEvent
import com.blueshift.core.events.BlueshiftEventRepository
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import org.json.JSONArray
import org.json.JSONObject

object BlueshiftEventManager {
    private lateinit var eventRepository: BlueshiftEventRepository
    private lateinit var networkRequestRepository: BlueshiftNetworkRequestRepository

    fun initialize(
        eventRepository: BlueshiftEventRepository,
        networkRequestRepository: BlueshiftNetworkRequestRepository,
    ) {
        this.eventRepository = eventRepository
        this.networkRequestRepository = networkRequestRepository
    }

    suspend fun trackEvent(event: BlueshiftEvent, realtimeEvent: Boolean = false) {
        if (realtimeEvent) {
            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.EVENTS,
                authorization = BlueshiftNetworkConfiguration.authorization,
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = event.eventParams,
            )

            networkRequestRepository.insertRequest(request)
        } else {
            eventRepository.insertEvent(event)
        }
    }

    suspend fun sync() {
        while (true) {
            val events = eventRepository.readOneBatch()

            if (events.isEmpty()) break

            val eventsArray = JSONArray()
            events.forEach { eventsArray.put(it.eventParams) }

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