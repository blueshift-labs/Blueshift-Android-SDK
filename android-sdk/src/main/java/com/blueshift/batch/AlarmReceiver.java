package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blueshift.BlueshiftConstants;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueue;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rahul on 25/8/16.
 * <p/>
 * This class receives the alarm manager's trigger.
 * It will be creating a new batch and sending it to request queue.
 * The integrating app should add this in their AndroidManifest.xml as,
 * <receiver android:name="com.blueshift.batch.AlarmReceiver"/>
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        // Log.d(LOG_TAG, "Received alarm for batch creation.");

        FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(context);
        ArrayList<HashMap<String, Object>> bulkEventsApiParams = failedEventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

        // Log.d(LOG_TAG, "Found " + bulkEventsApiParams.size() + " items inside failed events table.");

        int spaceAvailableInBatch = BlueshiftConstants.BULK_EVENT_PAGE_SIZE - bulkEventsApiParams.size();

        if (spaceAvailableInBatch > 0) {
            // there is space for some more events. Take it from Events table.
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> eventParams = eventsTable.getBulkEventParameters(spaceAvailableInBatch);

            // Log.d(LOG_TAG, "Found " + eventParams.size() + " items inside batch events table.");

            bulkEventsApiParams.addAll(eventParams);
        }

        if (bulkEventsApiParams.size() > 0) {
            BulkEvent bulkEvent = new BulkEvent();
            bulkEvent.setEvents(bulkEventsApiParams);

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
