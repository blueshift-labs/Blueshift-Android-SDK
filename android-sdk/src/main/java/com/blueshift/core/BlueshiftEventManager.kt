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
import com.blueshift.core.network.BlueshiftNetworkRequest
import com.blueshift.core.network.BlueshiftNetworkRequestRepository
import com.blueshift.util.CommonUtils
import com.blueshift.util.NetworkUtils
import org.json.JSONArray
import org.json.JSONObject

object BlueshiftEventManager {

    private const val TAG = "EventManager"
    private var _eventRepository: BlueshiftEventRepository? = null
    private val eventRepository: BlueshiftEventRepository?
        get() = _eventRepository ?: run {
            BlueshiftLogger.d("$TAG eventRepository is null! Did you forget to initialize the SDK?")
            null
        }

    private var _networkRequestRepository: BlueshiftNetworkRequestRepository? = null
    private val networkRequestRepository: BlueshiftNetworkRequestRepository?
        get() = _networkRequestRepository ?: run {
            BlueshiftLogger.d("$TAG networkRequestRepository is null! Did you forget to initialize the SDK?")
            null
        }

    private var _blueshiftLambdaQueue: BlueshiftLambdaQueue? = null
    private val blueshiftLambdaQueue: BlueshiftLambdaQueue?
        get() = _blueshiftLambdaQueue ?: run {
            BlueshiftLogger.d("$TAG blueshiftLambdaQueue is null! Did you forget to initialize the SDK?")
            null
        }

    fun initialize(
        eventRepository: BlueshiftEventRepository,
        networkRequestRepository: BlueshiftNetworkRequestRepository,
        blueshiftLambdaQueue: BlueshiftLambdaQueue
    ) {
        this._eventRepository = eventRepository
        this._networkRequestRepository = networkRequestRepository
        this._blueshiftLambdaQueue = blueshiftLambdaQueue
    }

    /**
     * This method acts as a bridge between the java version of the sdk and the kotlin version of
     * this class. It takes in the legacy params we used to take in when calling the sendEvent
     * method inside the Blueshift.java class and uses the new events module to send the events.
     */
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
        val blueshiftEvent = BlueshiftEvent(
            eventName = eventName, eventParams = eventParams, timestamp = System.currentTimeMillis()
        )

        // We should insert an event as batch event in two cases.
        // 1. If the app asks us to make it a batch event
        // 2. If the app didn't ask, but we had no internet connection at the time of tracking
        enqueueEvent(blueshiftEvent, isBatchEvent || !isConnected)
    }

    /**
     * This method accepts and event and adds it into a queue for processing, once added to the db,
     * the method will call the sync method to send the event to the server (if the event is real-time)
     */
    fun enqueueEvent(event: BlueshiftEvent, isBatchEvent: Boolean) {
        blueshiftLambdaQueue?.push {
            trackEvent(event, isBatchEvent)
            // let's not call sync for bulk events.
            // the sync will be called by the scheduler when it's time.
            if (!isBatchEvent) BlueshiftNetworkRequestQueueManager.sync()
        }
    }

    suspend fun trackEvent(event: BlueshiftEvent, isBatchEvent: Boolean) {
        if (isBatchEvent) {
            BlueshiftLogger.d("$TAG: Inserting 1 batch event -> ${event.eventName}")
            eventRepository?.insertEvent(event)
        } else {
            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.eventURL(),
                header = JSONObject(mapOf("Content-Type" to "application/json")),
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = event.eventParams,
            )

            BlueshiftLogger.d("$TAG: Inserting 1 real-time event -> ${event.eventName}")
            networkRequestRepository?.insertRequest(request)
        }
    }

    /**
     * This method accepts a query string for a campaign event and adds it into a queue for processing,
     * once added to the db, the method will call the sync method to send the event to the server.
     */
    fun enqueueCampaignEvent(queryString: String) {
        blueshiftLambdaQueue?.push {
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
            networkRequestRepository?.insertRequest(request)
        }
    }

    /**
     * Deletes ALL entries from the batch events table as well as the network requests table.
     */
    fun clearAsync() {
        blueshiftLambdaQueue?.push { clear() }
    }

    suspend fun clear() {
        eventRepository?.clear()
        networkRequestRepository?.clear()
    }

    suspend fun buildAndEnqueueBatchEvents() {
        while (true) {
            val events = eventRepository?.readOneBatch() ?: break
            if (events.isEmpty()) break

            val eventsArray = JSONArray()
            events.forEach { eventsArray.put(it.eventParams) }

            BlueshiftLogger.d("$TAG: Creating 1 bulk event with ${eventsArray.length()} event(s). Events = $eventsArray")

            val bulkEventPayload = JSONObject().apply {
                put("events", eventsArray)
            }

            val request = BlueshiftNetworkRequest(
                url = BlueshiftAPI.bulkEventsURL(),
                header = JSONObject(mapOf("Content-Type" to "application/json")),
                authorizationRequired = true,
                method = BlueshiftNetworkRequest.Method.POST,
                body = bulkEventPayload,
            )

            networkRequestRepository?.insertRequest(request)

            eventRepository?.deleteEvents(events)
        }
    }
}