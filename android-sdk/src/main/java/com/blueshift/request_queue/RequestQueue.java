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

    private static Status mStatus;
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
        mStatus = Status.AVAILABLE;
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

    public void remove(Context context, Request request) {
        if (request != null) {
            SdkLog.d(LOG_TAG, "Removing request with id:" + request.getId() + " from the Queue");

            RequestQueueTable db = RequestQueueTable.getInstance(context);
            db.delete(request);
        }
    }

    public Request fetch(Context context) {
        synchronized (lock) {
            mStatus = Status.BUSY;

            RequestQueueTable db = RequestQueueTable.getInstance(context);
            return db.getNextRequest();
        }
    }

    public void sync(Context context) {
        synchronized (lock) {
            if (mStatus == Status.AVAILABLE && NetworkUtils.isConnected(context)) {
                Request request = fetch(context);
                if (request != null) {
                    if (request.getPendingRetryCount() != 0) {
                        long nextRetryTime = request.getNextRetryTime();
                        // Checks if next retry time had passed or not.
                        // (0 is the default time for normal requests.)
                        if (nextRetryTime == 0 || nextRetryTime < System.currentTimeMillis()) {
                            new sendRequestTask(context, request).execute();
                        } else {
                            // The request has a next retry time which had not passed yet,
                            // so we need to move that to back of the queue.
                            remove(context, request);
                            mStatus = Status.AVAILABLE;
                            add(context, request);
                        }
                    } else {
                        // Request expired its retries. Need to be removed from queue.
                        // This is an escape plan. This case will not happen normally.
                        remove(context, request);
                        mStatus = Status.AVAILABLE;
                    }
                } else {
                    SdkLog.d(LOG_TAG, "Request queue is empty.");

                    mStatus = Status.AVAILABLE;
                }
            }
        }
    }

    private static class sendRequestTask extends AsyncTask<Void, Void, Boolean> {
        private Request mRequest;
        private Context mContext;
        // TODO: 26/06/18
        // This context will be removed when the dispatching of
        // events is implemented with handler and thread.

        sendRequestTask(Context context, Request request) {
            mContext = context;
            mRequest = request;
        }

        @Override
        protected void onPreExecute() {
            mStatus = RequestQueue.Status.BUSY;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mRequest != null) {
                SdkLog.d(LOG_TAG, "Processing request (id: " + mRequest.getId() + ").");

                HTTPManager httpManager = new HTTPManager(mRequest.getUrl());

                Configuration config = Blueshift.getInstance(mContext).getConfiguration();
                if (config != null && config.getApiKey() != null) {
                    httpManager.addBasicAuthentication(config.getApiKey(), "");
                } else {
                    SdkLog.e(LOG_TAG, "Please set a valid API key in your configuration before initialization.");
                }

                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                if (!TextUtils.isEmpty(deviceToken)) {
                    try {
                        /*
                         * Update the params with latest device token. The minimum requirement
                         * for an event to be valid is to have a device_token in it.
                         *
                         * This code will ensure we have latest device
                         * token in the event all the time.
                         */
                        JSONObject jsonObject = new JSONObject(mRequest.getParamJson());
                        jsonObject.put(BlueshiftConstants.KEY_DEVICE_TOKEN, deviceToken);

                        mRequest.setParamJson(jsonObject.toString());
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
                    }

                    UserInfo userInfo = UserInfo.getInstance(mContext);
                    String emailId = userInfo.getEmail();
                    if (!TextUtils.isEmpty(emailId)) {
                        /*
                         * If user is signed in and email is already not verified, then we
                         * need to call an identify with this new email id.
                         */
                        if (!BlueShiftPreference.isEmailAlreadyIdentified(mContext, emailId)) {
                            Blueshift
                                    .getInstance(mContext)
                                    .identifyUserByDeviceId(
                                            DeviceUtils.getAdvertisingID(mContext), null, false);

                            BlueShiftPreference.markEmailAsIdentified(mContext, emailId);
                        }
                    }
                }

                Response response = null;
                switch (mRequest.getMethod()) {
                    case POST:
                        response = httpManager.post(mRequest.getParamJson());

                        Log.d(LOG_TAG, "Blueshift Event: " + getEventName(mRequest.getParamJson()) +
                                ", API Status: " + getStatusFromResponse(response));

                        break;

                    case GET:
                        response = httpManager.get();

                        Log.d(LOG_TAG, "Blueshift Event\n" +
                                "Method: GET\n" +
                                "URL: " + mRequest.getUrl() + "\n" +
                                "Status: " + getStatusFromResponse(response)
                        );

                        break;

                    default:
                        SdkLog.e(LOG_TAG, "Unknown method" + mRequest.getMethod());
                }

                if (response != null) {
                    int responseCode = response.getStatusCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        SdkLog.d(LOG_TAG, "Request success for request (id: " + mRequest.getId() + "). Status code: " + response.getStatusCode());
                        return true;
                    } else {
                        SdkLog.d(LOG_TAG, "Request failed for request (id: " + mRequest.getId() + "). Status code: " + response.getStatusCode() + ". Response: " + response.getResponseBody());
                        return false;
                    }
                }
            }

            return false;
        }

        private String getEventName(String json) {
            String event = "Unknown";

            try {
                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.has("event")) {
                    event = jsonObject.getString("event");
                }
            } catch (JSONException e) {
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    if (jsonArray.length() > 0) {
                        event = "bulk_event";
                    }
                } catch (JSONException ignored) {
                }
            }

            return event;
        }

        private String getStatusFromResponse(Response response) {
            String status = "Unknown";

            if (response != null) {
                int code = response.getStatusCode();
                switch (code) {
                    case 0:
                        status = "Failed - No internet!";
                        break;

                    case 200:
                        status = "Success - 200";
                        break;

                    default:
                        status = "Failed - Code: " + code;
                }
            }

            return status;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            // we will be re-adding this to queue if the request was failed.
            // this is to avoid blocking the queue when a request fails continuously.
            RequestQueue requestQueue = RequestQueue.getInstance();
            requestQueue.remove(mContext, mRequest);

            if (!status) {
                // check if it is a failed high priority event.
                String api = mRequest.getUrl();

                if (BlueshiftConstants.EVENT_API_URL.equals(api)) {
                    // this is a case where request sent to non-bulk events api fails.
                    HashMap<String, Object> paramsMap;
                    String paramsJson = mRequest.getUrlParamsAsJSON();

                    if (!TextUtils.isEmpty(paramsJson)) {
                        Type type = new TypeToken<HashMap<String,Object>>(){}.getType();
                        paramsMap = new Gson().fromJson(paramsJson, type);

                        Event event = new Event();
                        event.setEventParams(paramsMap);

                        SdkLog.d(LOG_TAG, "Adding failed request to failed events table");

                        FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(mContext);
                        failedEventsTable.insert(event);
                    }
                } else {
                    int retryCount = mRequest.getPendingRetryCount() - 1;
                    if (retryCount > 0) {
                        mRequest.setPendingRetryCount(retryCount);
                        // setting a retry time 5 minutes from now.
                        long nextRetryTime = (RETRY_INTERVAL) + System.currentTimeMillis();
                        mRequest.setNextRetryTime(nextRetryTime);

                        requestQueue.add(mContext, mRequest);
                    }
                }
            }
            mStatus = RequestQueue.Status.AVAILABLE;
            requestQueue.sync(mContext);
        }
    }

    private enum Status {
        AVAILABLE,
        BUSY
    }
}
