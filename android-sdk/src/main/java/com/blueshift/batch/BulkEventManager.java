package com.blueshift.batch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
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


    public static void scheduleBulkEventDispatch(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleBulkEventDispatchWithJobScheduler(context);
        } else {
            startAlarmManager(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void scheduleBulkEventDispatchWithJobScheduler(Context context) {
        if (context != null) {
            long fiveMinutes = 1000 * 60 * 5;

            Configuration config = Blueshift.getInstance(context).getConfiguration();
            ComponentName componentName = new ComponentName(context, BulkEventJobService.class);
            JobInfo.Builder builder =
                    new JobInfo.Builder(config.getBulkEventsJobId(), componentName);

            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            builder.setBackoffCriteria(fiveMinutes, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresBatteryNotLow(true);
            }

            JobScheduler jobScheduler =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (jobScheduler != null
                    && jobScheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
                Log.d(LOG_TAG, "Bulk event job scheduled.");
            }
        }
    }

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
            long startAtMillis = SystemClock.elapsedRealtime() + interval;

            SdkLog.i(LOG_TAG, "Bulk event time interval: " + (interval / 1000f) / 60f + " min.");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME,
                        startAtMillis,
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
