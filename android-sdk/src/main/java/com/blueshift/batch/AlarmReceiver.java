package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.util.SdkLog;


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

        new BulkEventEnqueueTask(new BulkEventEnqueueTask.Callback() {
            @Override
            public void onStartTask() {
                Log.d(LOG_TAG, "Enqueue bulk events started.");
            }

            @Override
            public void onStopTask() {
                Log.d(LOG_TAG, "Enqueue bulk events completed.");
            }
        }).execute(context);
    }
}
