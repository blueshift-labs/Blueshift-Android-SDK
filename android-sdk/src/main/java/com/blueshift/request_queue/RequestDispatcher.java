package com.blueshift.request_queue;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import com.blueshift.model.UserInfo;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.SdkLog;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;

class RequestDispatcher {
    private static final String LOG_TAG = "RequestDispatcher";
    private static final long RETRY_INTERVAL = 5 * 60 * 1000;

    private Context mContext;
    private Request mRequest;
    private Callback mCallback;

    private RequestDispatcher(Context context, Request request, Callback callback) {
        mContext = context;
        mRequest = request;
        mCallback = callback;
    }

    synchronized void dispatch() {
        if (mRequest == null) {
            Log.e(LOG_TAG, "No request object available.");
            return;
        }

        if (mContext == null) {
            Log.e(LOG_TAG, "No context object available.");
            return;
        }

        // now, get the latest device token from FCM and call dispatch
        try {
            getLatestFCMTokenAndDispatch();
        } catch (Exception e) {
            try {
                FirebaseApp.initializeApp(mContext);
                getLatestFCMTokenAndDispatch();
            } catch (Exception ex) {
                SdkLog.e(LOG_TAG, ex.getMessage());
            }
        }
    }

    private void getLatestFCMTokenAndDispatch() {
        Task<InstanceIdResult> result = FirebaseInstanceId.getInstance().getInstanceId();
        result.addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                try {
                    String latestToken = instanceIdResult.getToken();
                    new RequestDispatchTask(latestToken, RequestDispatcher.this).execute();
                } catch (Exception e) {
                    SdkLog.e(LOG_TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * Method that process the {@link Request}. This calls all the methods
     * required for sending the event and handling the response.
     */
    private synchronized void processRequest(String fcmRegistrationToken) {
        String url = mRequest.getUrl();
        if (!TextUtils.isEmpty(url)) {
            HTTPManager httpManager = new HTTPManager(url);
            httpManager = appendAuthentication(httpManager);

            addDeviceTokenToParams(fcmRegistrationToken);
            doAutoIdentifyCheck();

            Response response = makeAPICall(httpManager);
            boolean apiStatus = response.getStatusCode() == 200;
            updateRequestQueue(apiStatus);
        }
    }

    private void invokeDispatchBegin() {
        if (mCallback != null) {
            mCallback.onDispatchBegin();
        }
    }

    private void invokeDispatchComplete() {
        if (mCallback != null) {
            mCallback.onDispatchComplete();
        }
    }

    /**
     * Append basic authentication to the request header of httpManager
     *
     * @param httpManager Valid {@link HTTPManager} object
     * @return {@link HTTPManager} object with basic auth enabled
     */
    private HTTPManager appendAuthentication(HTTPManager httpManager) {
        String apiKey = BlueshiftUtils.getApiKey(mContext);
        if (apiKey != null) {
            httpManager.addBasicAuthentication(apiKey, "");
        }

        return httpManager;
    }

    /**
     * The minimum requirement for an event to be valid is to have a valid device_token in it.
     * <p>
     * This method modifies the {@link Request} object by adding latest device token
     * to the parameters JSON.
     * <p>
     * This method ensures that the event always has the latest device token in it.
     */
    private void addDeviceTokenToParams(String token) {
        if (mRequest != null) {
            try {
                String paramsJson = mRequest.getParamJson();
                if (!TextUtils.isEmpty(paramsJson)) {
                    JSONObject jsonObject = new JSONObject(mRequest.getParamJson());
                    jsonObject.put(BlueshiftConstants.KEY_DEVICE_TOKEN, token);

                    mRequest.setParamJson(jsonObject.toString());
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
            }
        }
    }

    /**
     * This method checks if an identify is called with the email address present
     * inside {@link UserInfo}. If not, this method will schedule an identify call.
     * <p>
     * This is required to handle the case when multiple users sign in using same app.
     */
    private void doAutoIdentifyCheck() {
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

    /**
     * Makes the actual API call with the server, based on the details provided inside
     * the {@link Request} object.
     *
     * @param httpManager valid {@link HTTPManager} object
     * @return the {@link Response} of the API call
     */
    private Response makeAPICall(HTTPManager httpManager) {
        Response response = null;

        if (mRequest != null) {
            switch (mRequest.getMethod()) {
                case POST:
                    String json = mRequest.getParamJson();
                    SdkLog.d(LOG_TAG, "POST: Request params: " + json);

                    response = httpManager.post(json);
                    String eventName = getEventName(json);
                    String apiStatus = getStatusFromResponse(response);

                    Log.d(LOG_TAG, "Event name: " + eventName + ", API Status: " + apiStatus);

                    break;

                case GET:
                    response = httpManager.get();

                    Log.d(LOG_TAG, "Method: GET, " +
                            "URL: " + mRequest.getUrl() + ", " +
                            "Status: " + getStatusFromResponse(response));

                    break;

                default:
                    SdkLog.e(LOG_TAG, "Unknown method" + mRequest.getMethod());
            }
        }

        return response;
    }

    /**
     * Checks the params JSON and returns the event name.
     *
     * @param json valid JSON
     * @return name of the event
     */
    private String getEventName(String json) {
        String event = "Unknown";

        if (!TextUtils.isEmpty(json)) {
            try {
                JSONObject jsonObject = new JSONObject(json);

                // has one event? take it's name
                if (jsonObject.has("event")) {
                    event = jsonObject.optString("event");
                }

                // has events in it? mark it as bulk_events
                if (jsonObject.has("events")) {
                    event = "bulk_events";
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
        }

        return event;
    }

    /**
     * Checks the status code from {@link Response} object and returns the message based on that.
     *
     * @param response Valid {@link Response} object
     * @return Message based on the status code
     */
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

    /**
     * Update the request queue based on the status of the API call.
     *
     * @param status status of the API call made.
     */
    private void updateRequestQueue(boolean status) {
        // we will be re-adding this to queue if the request was failed.
        // this is to avoid blocking the queue when a request fails continuously.
        RequestQueue requestQueue = RequestQueue.getInstance();
        requestQueue.remove(mContext, mRequest);

        if (!status) {
            // check if it is a failed high priority event.
            String api = mRequest.getUrl();

            if (BlueshiftConstants.EVENT_API_URL.equals(api)) {
                // this is a case where request sent to non-bulk events api fails.
                String paramsJson = mRequest.getUrlParamsAsJSON();

                if (!TextUtils.isEmpty(paramsJson)) {
                    Type type = new TypeToken<HashMap<String, Object>>() {
                    }.getType();
                    HashMap<String, Object> paramsMap = new Gson().fromJson(paramsJson, type);

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

        requestQueue.markQueueAvailable();
    }

    public interface Callback {
        void onDispatchBegin();

        void onDispatchComplete();
    }

    public static class Builder {
        private Context mContext;
        private Request mRequest;
        private Callback mCallback;

        public Builder setContext(@NonNull Context context) {
            mContext = context;
            return this;
        }

        public Builder setRequest(@NonNull Request request) {
            mRequest = request;
            return this;
        }

        public Builder setCallback(@NonNull Callback callback) {
            mCallback = callback;
            return this;
        }

        public synchronized RequestDispatcher build() {
            return new RequestDispatcher(mContext, mRequest, mCallback);
        }
    }

    private static class RequestDispatchTask extends AsyncTask<Void, Void, Void> {
        String mFcmToken;
        RequestDispatcher mDispatcher;

        RequestDispatchTask(String fcmToken, RequestDispatcher dispatcher) {
            mFcmToken = fcmToken;
            mDispatcher = dispatcher;
        }

        @Override
        protected void onPreExecute() {
            if (mDispatcher != null) {
                mDispatcher.invokeDispatchBegin();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mDispatcher != null) {
                mDispatcher.processRequest(mFcmToken);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mDispatcher != null) {
                mDispatcher.invokeDispatchComplete();
            }
        }
    }
}
