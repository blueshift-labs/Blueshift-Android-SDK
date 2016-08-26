package com.blueshift.batch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;

/**
 * Created by rahul on 25/8/16.
 */
public class BulkEventManager {

    private static final String LOG_TAG = BulkEventManager.class.getSimpleName();

    private static PendingIntent getAlarmIntent(Context context) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void startAlarmManager(Context context) {
        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
        if (configuration == null) {
            Log.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with a valid configuration object.");
        } else {
            long interval = configuration.getBatchInterval();

            Log.d(LOG_TAG, "Bulk event time interval: " + interval + "ms");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME,
                        interval, // start in interval time from now.
                        interval, // repeat every interval time.
                        getAlarmIntent(context)
                );
            }
        }
    }

    public static void stopAlarmManager(Context context) {
        PendingIntent alarmIntent = getAlarmIntent(context);
        if (alarmIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(alarmIntent);
                alarmIntent.cancel();
            }
        }
    }
}
