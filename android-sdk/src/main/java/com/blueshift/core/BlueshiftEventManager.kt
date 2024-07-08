package com.blueshift.core

import android.content.Context
import com.blueshift.BlueshiftAttributesApp
import com.blueshift.BlueshiftAttributesUser
import com.blueshift.BlueshiftConstants
import com.blueshift.BlueshiftJSONObject
import com.blueshift.core.common.BlueshiftAPI
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.core.events.BlueshiftEvent
import com.blueshift.core.events.BlueshiftEventRepository
import com.blueshift.core.network.BlueshiftNetworkConfiguration
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import com.blueshift.util.CommonUtils
import com.blueshift.util.NetworkUtils
import org.json.JSONArray
import org.json.JSONObject

object BlueshiftEventManager {
    private const val TAG = "BlueshiftEventManager"
    private lateinit var eventRepository: BlueshiftEventRepository
    private lateinit var networkRequestRepository: BlueshiftNetworkRequestRepository
    private lateinit var blueshiftLambdaQueue: BlueshiftLambdaQueue

    fun initialize(
        eventRepository: BlueshiftEventRepository,
        networkRequestRepository: BlueshiftNetworkRequestRepository,
        blueshiftLambdaQueue: BlueshiftLambdaQueue
    ) {
        this.eventRepository = eventRepository
        this.networkRequestRepository = networkRequestRepository
        this.blueshiftLambdaQueue = blueshiftLambdaQueue
    }

    fun trackEventWithData(
        context: Context, eventName: String, data: HashMap<String, Any>?, isBatchEvent: Boolean
    ) {
        val eventParams = BlueshiftJSONObject()
        eventParams.put(BlueshiftConstants.KEY_EVENT, eventName)
        eventParams.put(BlueshiftConstants.KEY_TIMESTAMP, CommonUtils.getCurrentUtcTimestamp())

        val appInfo = BlueshiftAttributesApp.getInstance().sync(context)
        eventParams.putAll(appInfo)

        val userInfo = BlueshiftAttributesUser.getInstance().sync(context)
        eventParams.putAll(userInfo)

        data?.forEach { eventParams.put(it.key, it.value) }

        val isConnected = NetworkUtils.isConnected(context)
        val blueshiftEvent = BlueshiftEvent(-1L, eventName, eventParams, System.currentTimeMillis())

        // We should insert an event as batch event in two cases.
        // 1. If the app asks us to make it a batch event
        // 2. If the app didn't ask, but we had no internet connection at the time of tracking
        trackEventAsync(blueshiftEvent, isBatchEvent || !isConnected)
    }

    private fun trackEventAsync(event: BlueshiftEvent, isBatchEvent: Boolean) {
        blueshiftLambdaQueue.push {
            trackEvent(event, isBatchEvent)
            BlueshiftNetworkRequestQueueManager.sync()
        }
    }

    fun trackCampaignEventAsync(queryString: String) {
        blueshiftLambdaQueue.push {
            trackCampaignEvent(queryString)
            BlueshiftNetworkRequestQueueManager.sync()
        }
    }

    suspend fun trackCampaignEvent(queryString: String) {
        if (queryString.isNotEmpty()) {
            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.trackURL(queryString),
                method = BlueshiftNetworkRequest.Method.GET,
            )
            networkRequestRepository.insertRequest(request)
        }
    }

    suspend fun trackEvent(event: BlueshiftEvent, isBatchEvent: Boolean) {
        if (isBatchEvent) {
            BlueshiftLogger.d("$TAG: Inserting 1 batch event. Event = ${event.eventParams}")
            eventRepository.insertEvent(event)
        } else {
            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.eventURL(),
                authorization = BlueshiftNetworkConfiguration.authorization,
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = event.eventParams,
            )

            BlueshiftLogger.d("$TAG: Inserting 1 real-time event. Event = ${event.eventParams}")
            networkRequestRepository.insertRequest(request)
        }
    }

    suspend fun buildAndEnqueueBatchEvents() {
        while (true) {
            val events = eventRepository.readOneBatch()
            // break the loop when there are no pending events available for making a batch
            if (events.isEmpty()) break

            val eventsArray = JSONArray()
            events.forEach { eventsArray.put(it.eventParams) }

            BlueshiftLogger.d("$TAG: Creating 1 bulk event with ${eventsArray.length()} event(s). Events = $eventsArray")

            val bulkEventPayload = JSONObject().apply {
                put("events", eventsArray)
            }

            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.bulkEventsURL(),
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