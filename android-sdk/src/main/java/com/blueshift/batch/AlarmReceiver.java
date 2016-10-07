package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blueshift.BlueshiftConstants;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueue;
import com.blueshift.util.SdkLog;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * This is the alarm receiver for bulk event sync process.
 *
 * This class checks both failed request queue and normal bulk events queue for cached events.
 * Batches of 100 events will be created and sent to server when this alarm triggers.
 *
 * @author Rahul Raveendran V P
 *         Created on 25/8/16 @ 1:01 PM
 *         https://github.com/rahulrvp
 */

public class AlarmReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        SdkLog.d(LOG_TAG, "Received alarm for batch creation.");

        ArrayList<HashMap<String, Object>> tempBulkEventsApiParams = new ArrayList<>();

        int failedEventsCount;

        do {
            FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> failedBulkEventsApiParams = failedEventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

            failedEventsCount = failedBulkEventsApiParams.size();

            SdkLog.d(LOG_TAG, "Found " + failedEventsCount + " items inside failed events table.");

            if (failedEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE) {
                /**
                 * If the failed events count is equal to 100. loop will try
                 * to check if there is more failed events in the queue.If found, create
                 * separate batches to include them and add them to the queue.
                 */

                addToBulkEventsRequestQueue(context, failedBulkEventsApiParams);
            } else {
                /**
                 * If the failed events count is equal to zero, or less than 100, exit the
                 * loop and continue with normal batch-able events from the events queue.
                 */

                tempBulkEventsApiParams.addAll(failedBulkEventsApiParams);
            }
        } while (failedEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

        /**
         * In this line failedEventsCount is the count of last batch created with
         * failed events. It could be a value >= 0 and < 100
         */
        int spaceAvailableInBatch = BlueshiftConstants.BULK_EVENT_PAGE_SIZE - failedEventsCount;

        if (spaceAvailableInBatch > 0) {
            /**
             * If we are here, it means the last batch created using failed events has
             * a size which is > 0 and < 100. So there is still room for some more events.
             *
             * This block will fetch the events for the balance rooms and will create a bulk
             * event api request with it.
             */
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> bulkEventsApiParams = eventsTable.getBulkEventParameters(spaceAvailableInBatch);

            SdkLog.d(LOG_TAG, "Adding " + bulkEventsApiParams.size() + " items from batch events table to fill the batch.");

            tempBulkEventsApiParams.addAll(bulkEventsApiParams);

            addToBulkEventsRequestQueue(context, tempBulkEventsApiParams);
        }

        /**
         * Now take the events from bulk events queue and create batches for sending to bulk event API.
         */

        int bulkEventsCount;

        do {
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> eventParams = eventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

            bulkEventsCount = eventParams.size();

            SdkLog.d(LOG_TAG, "Found " + eventParams.size() + " items inside batch events table.");

            if (bulkEventsCount > 0) {
                addToBulkEventsRequestQueue(context, eventParams);
            }

            /**
             * Repeat if the last batch created has 100 events.
             */
        } while (bulkEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE);
    }

    private void addToBulkEventsRequestQueue(Context context, ArrayList<HashMap<String, Object>> eventParamsList) {
        if (eventParamsList != null && eventParamsList.size() > 0) {
            BulkEvent bulkEvent = new BulkEvent();
            bulkEvent.setEvents(eventParamsList);

            // Creating the request object.
            Request request = new Request();
            request.setPendingRetryCount(RequestQueue.DEFAULT_RETRY_COUNT);
            request.setUrl(BlueshiftConstants.BULK_EVENT_API_URL);
            request.setMethod(Method.POST);
            request.setParamJson(new Gson().toJson(bulkEvent));

            // Adding the request to the queue.
            RequestQueue.getInstance(context).add(request);
        }
    }
}
