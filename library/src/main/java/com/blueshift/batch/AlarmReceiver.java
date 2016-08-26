package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.BlueshiftConstants;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

        FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(context);
        ArrayList<String> bulkEventsApiParams = failedEventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

        int spaceAvailableInBatch = BlueshiftConstants.BULK_EVENT_PAGE_SIZE - bulkEventsApiParams.size();

        if (spaceAvailableInBatch > 0) {
            // there is space for some more events. Take it from Events table.
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<String> eventParams = eventsTable.getBulkEventParameters(spaceAvailableInBatch);
            bulkEventsApiParams.addAll(eventParams);
        }

        if (bulkEventsApiParams.size() > 0) {
            try {
                JSONObject reqParams = new JSONObject();
                reqParams.put("events", bulkEventsApiParams);

                // Creating the request object.
                Request request = new Request();
                request.setPendingRetryCount(RequestQueue.DEFAULT_RETRY_COUNT);
                request.setUrl(BlueshiftConstants.BULK_EVENT_API_URL);
                request.setMethod(Method.POST);
                request.setParamJson(reqParams.toString());

                // Adding the request to the queue.
                RequestQueue.getInstance(context).add(request);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Invalid JSON. " + e.getMessage());
            }
        }
    }
}
