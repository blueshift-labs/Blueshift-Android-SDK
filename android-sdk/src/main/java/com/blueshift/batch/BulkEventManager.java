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

import androidx.annotation.RequiresApi;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.model.Configuration;
import com.blueshift.request_queue.RequestQueue;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 * Created on 25/8/16 @ 3:05 PM
 * https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class BulkEventManager {

    private static final String LOG_TAG = BulkEventManager.class.getSimpleName();

    public static void scheduleBulkEventEnqueue(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleBulkEventDispatchWithJobScheduler(context);
        } else {
            startAlarmManager(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void scheduleBulkEventDispatchWithJobScheduler(Context context) {
        try {
            if (context != null) {
                Configuration config = BlueshiftUtils.getConfiguration(context);
                if (config != null) {
                    int jobId = config.getBulkEventsJobId();
                    boolean isJobPending = CommonUtils.isJobPending(context, jobId);
                    if (isJobPending) return; // the job is already scheduled, skip the below code.

                    ComponentName componentName = new ComponentName(context, BulkEventJobService.class);
                    JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);

                    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                    builder.setPeriodic(config.getBatchInterval()); // 30 min batch interval by default

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        builder.setRequiresBatteryNotLow(true);
                    }

                    final JobScheduler jobScheduler =
                            (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    final JobInfo jobInfo = builder.build();

                    BlueshiftExecutor.getInstance().runOnNetworkThread(() -> {
                        if (jobScheduler != null) {
                            try {
                                if (JobScheduler.RESULT_SUCCESS == jobScheduler.schedule(jobInfo)) {
                                    BlueshiftLogger.d(LOG_TAG, "Job scheduled successfully! (Bulk Events Job)");
                                } else {
                                    BlueshiftLogger.w(LOG_TAG, "Job scheduling failed! (Bulk Events Job)");
                                }
                            } catch (Exception e) {
                                BlueshiftLogger.e(LOG_TAG, e);
                            }
                        } else {
                            BlueshiftLogger.w(LOG_TAG, "JobScheduler instance is null (Bulk Events Job)");
                        }
                    });
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private static PendingIntent getAlarmPendingIntent(Context context, int flag) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, alarmIntent, CommonUtils.appendImmutableFlag(flag));
    }

    private static void startAlarmManager(Context context) {
        BlueshiftLogger.i(LOG_TAG, "Starting alarm service");

        // stop alarm manager if it is already running
        stopAlarmManager(context);

        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        if (configuration != null) {
            long interval = configuration.getBatchInterval();
            long startAtMillis = SystemClock.elapsedRealtime() + interval;

            BlueshiftLogger.i(LOG_TAG, "Bulk event time interval: " + (interval / 1000f) / 60f + " min.");

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

    private static void stopAlarmManager(Context context) {
        // FLAG_NO_CREATE: if described PendingIntent does not already exist,
        // then simply return null instead of creating it.
        PendingIntent alarmIntent = getAlarmPendingIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null) {
            BlueshiftLogger.i(LOG_TAG, "Found an existing alarm. Cancelling it.");

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

    public static void enqueueBulkEvents(Context context) {
        if (!Blueshift.isTrackingEnabled(context)) {
            BlueshiftLogger.i(LOG_TAG, "Blueshift SDK's event tracking is disabled. Skipping bulk event enqueueing.");
            return;
        }

        ArrayList<HashMap<String, Object>> tempBulkEventsApiParams = new ArrayList<>();

        int failedEventsCount;

        do {
            FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> failedBulkEventsApiParams = failedEventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

            failedEventsCount = failedBulkEventsApiParams.size();

            BlueshiftLogger.d(LOG_TAG, "Found " + failedEventsCount + " items inside failed events table.");

            if (failedEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE) {
                /*
                 * If the failed events count is equal to 100. loop will try
                 * to check if there is more failed events in the queue.If found, create
                 * separate batches to include them and add them to the queue.
                 */

                addToBulkEventsRequestQueue(context, failedBulkEventsApiParams);
            } else {
                /*
                 * If the failed events count is equal to zero, or less than 100, exit the
                 * loop and continue with normal batch-able events from the events queue.
                 */

                tempBulkEventsApiParams.addAll(failedBulkEventsApiParams);
            }
        } while (failedEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

        /*
         * In this line failedEventsCount is the count of last batch created with
         * failed events. It could be a value >= 0 and < 100
         */
        int spaceAvailableInBatch = BlueshiftConstants.BULK_EVENT_PAGE_SIZE - failedEventsCount;

        if (spaceAvailableInBatch > 0) {
            /*
             * If we are here, it means the last batch created using failed events has
             * a size which is > 0 and < 100. So there is still room for some more events.
             *
             * This block will fetch the events for the balance rooms and will create a bulk
             * event api request with it.
             */
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> bulkEventsApiParams = eventsTable.getBulkEventParameters(spaceAvailableInBatch);

            BlueshiftLogger.d(LOG_TAG, "Adding " + bulkEventsApiParams.size() + " items from batch events table to fill the batch.");

            tempBulkEventsApiParams.addAll(bulkEventsApiParams);

            addToBulkEventsRequestQueue(context, tempBulkEventsApiParams);
        }

        /*
         * Now take the events from bulk events queue and create batches for sending to bulk event API.
         */

        int bulkEventsCount;

        do {
            EventsTable eventsTable = EventsTable.getInstance(context);
            ArrayList<HashMap<String, Object>> eventParams = eventsTable.getBulkEventParameters(BlueshiftConstants.BULK_EVENT_PAGE_SIZE);

            bulkEventsCount = eventParams.size();

            BlueshiftLogger.d(LOG_TAG, "Found " + eventParams.size() + " items inside batch events table.");

            if (bulkEventsCount > 0) {
                addToBulkEventsRequestQueue(context, eventParams);
            }

            /*
             * Repeat if the last batch created has 100 events.
             */
        } while (bulkEventsCount == BlueshiftConstants.BULK_EVENT_PAGE_SIZE);
    }

    private static void addToBulkEventsRequestQueue(Context context, ArrayList<HashMap<String, Object>> eventParamsList) {
        if (eventParamsList != null && eventParamsList.size() > 0) {
            BulkEvent bulkEvent = new BulkEvent();
            bulkEvent.setEvents(eventParamsList);

            // Creating the request object.
            Request request = new Request();
            request.setPendingRetryCount(RequestQueue.DEFAULT_RETRY_COUNT);
            request.setUrl(BlueshiftConstants.BULK_EVENT_API_URL(context));
            request.setMethod(Method.POST);
            request.setParamJson(new Gson().toJson(bulkEvent));

            // Adding the request to the queue.
            RequestQueue.getInstance().add(context, request);
        }
    }
}
