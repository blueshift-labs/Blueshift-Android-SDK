package com.blueshift.request_queue;

import android.content.Context;
import android.os.AsyncTask;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;

public class RequestDispatcher {
    private static final String LOG_TAG = "RequestDispatcher";
    private static final long RETRY_INTERVAL = 5 * 60 * 1000;

    private Context mContext;
    private Request mRequest;
    private Callback mCallback;

    private RequestDispatcher() {
        // do nothing
    }

    public static class Builder {
        private RequestDispatcher mDispatcher;

        Builder() {
            mDispatcher = new RequestDispatcher();
        }

        public Builder setContext(Context context) {
            mDispatcher.mContext = context;
            return this;
        }

        public Builder setRequest(Request request) {
            mDispatcher.mRequest = request;
            return this;
        }

        public Builder setCallback(Callback callback) {
            mDispatcher.mCallback = callback;
            return this;
        }

        public synchronized RequestDispatcher build() {
            return mDispatcher;
        }
    }

    public synchronized void dispatch() {
        if (mRequest == null) {
            Log.e(LOG_TAG, "No request object available.");
            return;
        }

        if (mContext == null) {
            Log.e(LOG_TAG, "No context object available.");
            return;
        }

        new RequestDispatchTask(this).execute();
    }

    private synchronized void processRequest() {
        String url = mRequest.getUrl();
        if (!TextUtils.isEmpty(url)) {
            HTTPManager httpManager = new HTTPManager(url);
            httpManager = appendAuthentication(httpManager);

            addDeviceTokenToParams();
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

    private static class RequestDispatchTask extends AsyncTask<Void, Void, Void> {
        RequestDispatcher mDispatcher;

        RequestDispatchTask(RequestDispatcher dispatcher) {
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
                mDispatcher.processRequest();
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

    private HTTPManager appendAuthentication(HTTPManager httpManager) {
        String apiKey = BlueshiftUtils.getApiKey(mContext);
        if (apiKey != null) {
            httpManager.addBasicAuthentication(apiKey, "");
        }

        return httpManager;
    }

    private void addDeviceTokenToParams() {
        String token = FirebaseInstanceId.getInstance().getToken();
        try {
            /*
             * Update the params with latest device token. The minimum requirement
             * for an event to be valid is to have a device_token in it.
             *
             * This code will ensure we have latest device
             * token in the event all the time.
             */
            JSONObject jsonObject = new JSONObject(mRequest.getParamJson());
            jsonObject.put(BlueshiftConstants.KEY_DEVICE_TOKEN, token);

            mRequest.setParamJson(jsonObject.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }

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

    private Response makeAPICall(HTTPManager httpManager) {
        Response response = null;

        switch (mRequest.getMethod()) {
            case POST:
                response = httpManager.post(mRequest.getParamJson());
                String eventName = getEventName(mRequest.getParamJson());
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

        return response;
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
}
