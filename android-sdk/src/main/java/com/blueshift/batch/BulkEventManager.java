package com.blueshift.batch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.util.SdkLog;

/**
 * @author Rahul Raveendran V P
 *         Created on 25/8/16 @ 3:05 PM
 *         https://github.com/rahulrvp
 */
public class BulkEventManager {

    private static final String LOG_TAG = BulkEventManager.class.getSimpleName();

    private static PendingIntent getAlarmPendingIntent(Context context, int flag) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, alarmIntent, flag);
    }

    public static void startAlarmManager(Context context) {
        SdkLog.i(LOG_TAG, "Starting alarm service");

        // stop alarm manager if it is already running
        stopAlarmManager(context);

        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
        if (configuration == null) {
            SdkLog.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with a valid configuration object.");
        } else {
            long interval = configuration.getBatchInterval();

            SdkLog.i(LOG_TAG, "Bulk event time interval: " + (interval / 1000f) / 60f + " min.");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME,
                        interval, // start in interval time from now.
                        interval, // repeat every interval time.
                        getAlarmPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
                );
            }
        }
    }

    public static void stopAlarmManager(Context context) {
        // FLAG_NO_CREATE: if described PendingIntent does not already exist,
        // then simply return null instead of creating it.
        PendingIntent alarmIntent = getAlarmPendingIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null) {
            SdkLog.i(LOG_TAG, "Found an existing alarm. Cancelling it.");

            // we have an intent in alarm manager.
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                // cancel alarm
                alarmManager.cancel(alarmIntent);
                // cancel pending intent
                alarmIntent.cancel();
            }
        }
    }
}
