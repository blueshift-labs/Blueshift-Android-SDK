package com.blueshift.batch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.request_queue.RequestQueue;


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

    private static final String LOG_TAG = "BatchAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        BlueshiftLogger.d(LOG_TAG, "Received alarm for batch creation.");

        doBackgroundWork(context);
    }

    private void doBackgroundWork(Context context) {
        final Context appContext = context != null ? context.getApplicationContext() : null;
        if (appContext != null) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            BulkEventManager.enqueueBulkEvents(appContext);
                        }
                    }
            ).start();
        }
    }
}
