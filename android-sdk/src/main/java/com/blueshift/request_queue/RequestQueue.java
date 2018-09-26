package com.blueshift.request_queue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.BlueShiftPreference;
import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.batch.Event;
import com.blueshift.batch.FailedEventsTable;
import com.blueshift.httpmanager.HTTPManager;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.Response;
import com.blueshift.httpmanager.request_queue.RequestQueueJobService;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.NetworkUtils;
import com.blueshift.util.SdkLog;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 26/2/15 @ 3:07 PM
 *         https://github.com/rahulrvp
 */
public class RequestQueue {
    public static final int DEFAULT_RETRY_COUNT = 3;

    private static final String LOG_TAG = RequestQueue.class.getSimpleName();
    private static final Boolean lock = true;
    private static final long RETRY_INTERVAL = 5 * 60 * 1000;

    private Status mStatus;
    private static RequestQueue mInstance = null;

    public static void scheduleQueueSyncJob(Context context) {
        if (context != null) {
            Configuration config = Blueshift.getInstance(context).getConfiguration();
            if (config != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    JobScheduler jobScheduler
                            = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    if (jobScheduler != null) {
                        @SuppressLint("JobSchedulerService")
                        ComponentName componentName
                                = new ComponentName(context, RequestQueueJobService.class);
                        int jobId = config.getNetworkChangeListenerJobId();
                        Log.d(LOG_TAG, "Job Id: " + jobId);
                        JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);

                        builder
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                .setPeriodic(30 * 60 * 1000);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            builder.setRequiresBatteryNotLow(true);
                        }

                        JobInfo jobInfo = builder.build();

                        if (JobScheduler.RESULT_SUCCESS == jobScheduler.schedule(jobInfo)) {
                            SdkLog.i(LOG_TAG, "Successfully scheduled request queue " +
                                    "sync job on network change");
                        } else {
                            // for some reason job scheduling failed. log this.
                            SdkLog.w(LOG_TAG, "Could not schedule request queue sync " +
                                    "job on network change");
                        }
                    }
                }
            } else {
                Log.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with " +
                        "a valid configuration object.");
            }
        }
    }

    private RequestQueue() {
        markQueueAvailable();
    }

    public synchronized static RequestQueue getInstance() {
        if (mInstance == null) {
            mInstance = new RequestQueue();
        }

        return mInstance;
    }

    public void add(Context context, Request request) {
        if (request != null) {
            SdkLog.d(LOG_TAG, "Adding new request to the Queue.");

            RequestQueueTable db = RequestQueueTable.getInstance(context);
            db.insert(request);

            sync(context);
        }
    }

    void remove(Context context, Request request) {
        if (request != null) {
            SdkLog.d(LOG_TAG, "Removing request with id:" + request.getId() + " from the Queue");

            RequestQueueTable db = RequestQueueTable.getInstance(context);
            db.delete(request);
        }
    }

    private Request fetch(Context context) {
        synchronized (lock) {
            markQueueBusy();

            RequestQueueTable db = RequestQueueTable.getInstance(context);
            return db.getNextRequest();
        }
    }

    void markQueueAvailable() {
        mStatus = Status.AVAILABLE;
    }

    private void markQueueBusy() {
        mStatus = Status.BUSY;
    }

    public void sync(final Context context) {
        synchronized (lock) {
            if (mStatus == Status.AVAILABLE && NetworkUtils.isConnected(context)) {
                Request request = fetch(context);
                if (request != null) {
                    if (request.getPendingRetryCount() != 0) {
                        long nextRetryTime = request.getNextRetryTime();
                        // Checks if next retry time had passed or not.
                        // (0 is the default time for normal requests.)
                        if (nextRetryTime == 0 || nextRetryTime < System.currentTimeMillis()) {
                            RequestDispatcher dispatcher = new RequestDispatcher.Builder()
                                    .setContext(context)
                                    .setRequest(request)
                                    .setCallback(new RequestDispatcher.Callback() {
                                        @Override
                                        public void onDispatchBegin() {
                                            // empty listener method
                                        }

                                        @Override
                                        public void onDispatchComplete() {
                                            sync(context);
                                        }
                                    })
                                    .build();

                            dispatcher.dispatch();
                        } else {
                            // The request has a next retry time which had not passed yet,
                            // so we need to move that to back of the queue.
                            remove(context, request);
                            markQueueAvailable();
                            add(context, request);
                        }
                    } else {
                        // Request expired its retries. Need to be removed from queue.
                        // This is an escape plan. This case will not happen normally.
                        remove(context, request);
                        markQueueAvailable();
                    }
                } else {
                    SdkLog.d(LOG_TAG, "Request queue is empty.");

                    markQueueAvailable();
                }
            }
        }
    }

    private enum Status {
        AVAILABLE,
        BUSY
    }
}
