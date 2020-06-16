package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;


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
    public void onReceive(final Context context, Intent intent) {

        BlueshiftLogger.d(LOG_TAG, "Received alarm for batch creation.");

        BlueshiftExecutor.getInstance().runOnDiskIOThread(
                new Runnable() {
                    @Override
                    public void run() {
                        BlueshiftLogger.d(LOG_TAG, "Enqueue bulk events started.");
                        BulkEventManager.enqueueBulkEvents(context);
                        BlueshiftLogger.d(LOG_TAG, "Enqueue bulk events completed.");
                    }
                }
        );
    }
}
