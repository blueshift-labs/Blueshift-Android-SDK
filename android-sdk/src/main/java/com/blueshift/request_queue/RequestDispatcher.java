package com.blueshift.request_queue;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blueshift.BlueShiftPreference;
import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftHttpManager;
import com.blueshift.BlueshiftHttpRequest;
import com.blueshift.BlueshiftHttpResponse;
import com.blueshift.BlueshiftLogger;
import com.blueshift.batch.Event;
import com.blueshift.batch.FailedEventsTable;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.Response;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
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
            BlueshiftLogger.e(LOG_TAG, "No request object available.");
            return;
        }

        if (mContext == null) {
            BlueshiftLogger.e(LOG_TAG, "No context object available.");
            return;
        }

        Configuration config = BlueshiftUtils.getConfiguration(mContext);
        if (config != null) {
            if (config.isPushEnabled()) {
                dispatchWithPushToken();
            } else {
                dispatchWithoutPushToken();
            }
        }
    }

    private void dispatchWithPushToken() {
        try {
            getLatestFCMTokenAndDispatch();
        } catch (Exception e) {
            try {
                FirebaseApp.initializeApp(mContext);
                getLatestFCMTokenAndDispatch();
            } catch (Exception ex) {
                // Possible error on Firebase initialization. Send event without token.
                dispatchWithoutPushToken();
                BlueshiftLogger.e(LOG_TAG, ex);
            }
        }
    }

    private void dispatchWithoutPushToken() {
        try {
            dispatchWithToken(null);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private void getLatestFCMTokenAndDispatch() {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String token) {
                            dispatchWithToken(token);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            BlueshiftLogger.w(LOG_TAG, e.getMessage());
                            dispatchWithoutPushToken();
                        }
                    });
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
            dispatchWithoutPushToken();
        }
    }

    /**
     * Method that process the {@link Request}. This calls all the methods
     * required for sending the event and handling the response.
     */
    private synchronized void processRequest(String fcmRegistrationToken) {
        String url = mRequest.getUrl();
        if (!TextUtils.isEmpty(url)) {
            String deviceId = DeviceUtils.getDeviceId(mContext);
            if (addDeviceIdAndTokenToParams(deviceId, fcmRegistrationToken)) {
                doAutoIdentifyCheck(mContext);

                BlueshiftHttpResponse response = makeAPICall();
                boolean apiStatus = response != null && response.getCode() == 200;
                updateRequestQueue(apiStatus);
            } else {
                // device_id is not present. let's try again later
                updateRequestQueue(false);
            }
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

    private boolean deviceIdCheckFails(JSONObject eventJson, String deviceId) throws JSONException {
        if (!eventJson.has(BlueshiftConstants.KEY_DEVICE_IDENTIFIER)
                || TextUtils.isEmpty(eventJson.getString(BlueshiftConstants.KEY_DEVICE_IDENTIFIER))) {
            if (deviceId != null) {
                eventJson.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, deviceId);
            } else {
                deviceId = DeviceUtils.getDeviceId(mContext);
                if (deviceId != null) {
                    eventJson.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, deviceId);
                } else {
                    // We could not add device_id in the event,
                    // let's try again later.
                    BlueshiftLogger.e(LOG_TAG, "We could not add the device id in bulk event params");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * The minimum requirement for an event to be valid is to have a valid device_id in it.
     * <p>
     * This method modifies the {@link Request} object by adding latest device token
     * to the parameters JSON.
     * <p>
     * This method ensures that the event always has the latest device token in it.
     */
    private boolean addDeviceIdAndTokenToParams(String deviceId, String token) {
        if (mRequest != null) {
            try {
                String url = mRequest.getUrl();
                if (BlueshiftConstants.BULK_EVENT_API_URL(mContext).equals(url)) {
                    // when bulk event is being updated, we need to add token in all child events
                    String payload = mRequest.getParamJson();
                    if (!TextUtils.isEmpty(payload)) {
                        try {
                            JSONObject payloadJson = new JSONObject(payload);
                            String eventsKey = "events";
                            if (payloadJson.has(eventsKey)) {
                                JSONArray eventArray = payloadJson.getJSONArray(eventsKey);
                                for (int index = 0; index < eventArray.length(); index++) {
                                    JSONObject event = eventArray.getJSONObject(index);
                                    event.put(BlueshiftConstants.KEY_DEVICE_TOKEN, token);
                                    if (deviceIdCheckFails(event, deviceId)) return false;
                                    eventArray.put(index, event);
                                }
                                payloadJson.putOpt(eventsKey, eventArray);
                                mRequest.setParamJson(payloadJson.toString());
                                return true;
                            }
                        } catch (Exception e) {
                            BlueshiftLogger.e(LOG_TAG, e);
                        }
                    }
                } else {
                    String paramsJson = mRequest.getParamJson();
                    if (!TextUtils.isEmpty(paramsJson)) {
                        JSONObject jsonObject = new JSONObject(mRequest.getParamJson());
                        jsonObject.put(BlueshiftConstants.KEY_DEVICE_TOKEN, token);
                        if (deviceIdCheckFails(jsonObject, deviceId)) return false;
                        mRequest.setParamJson(jsonObject.toString());
                        return true;
                    }
                }
            } catch (JSONException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }

        return true;
    }

    private void doAutoIdentifyCheck(Context context) {
        boolean identified = didEmailChange(context) || didPushPermissionChange(context);
        if (identified) BlueshiftLogger.d(LOG_TAG, "Auto identify call fired.");
    }

    private boolean didEmailChange(Context context) {
        UserInfo userInfo = UserInfo.getInstance(context);
        if (userInfo != null && !TextUtils.isEmpty(userInfo.getEmail())) {
            if (!BlueShiftPreference.isEmailAlreadyIdentified(context, userInfo.getEmail())) {
                identify(context);
                BlueShiftPreference.markEmailAsIdentified(context, userInfo.getEmail());
                BlueshiftLogger.d(LOG_TAG, "Change in email detected. Sending \"identify\".");
                return true;
            }
        }

        return false;
    }

    private boolean didPushPermissionChange(Context context) {
        if (BlueShiftPreference.didPushPermissionStatusChange(context)) {
            identify(context);
            BlueShiftPreference.saveCurrentPushPermissionStatus(context);
            BlueshiftLogger.d(LOG_TAG, "Change in push permission detected. Sending \"identify\".");
            return true;
        }

        return false;
    }

    private void identify(Context context) {
        Blueshift.getInstance(context).identifyUser(null, false);
    }

    /**
     * Makes the actual API call with the server, based on the details provided inside
     * the {@link Request} object.
     *
     * @return the {@link Response} of the API call
     */
    private BlueshiftHttpResponse makeAPICall() {
        String apiKey = BlueshiftUtils.getApiKey(mContext);

        if (apiKey != null && mRequest != null) {
            BlueshiftHttpRequest.Builder builder = new BlueshiftHttpRequest.Builder()
                    .setUrl(mRequest.getUrl())
                    .addBasicAuth(apiKey, "");

            if (mRequest.getMethod() == Method.POST) {
                builder.setMethod(BlueshiftHttpRequest.Method.POST);
            }

            String json = mRequest.getParamJson();
            if (json != null) {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    builder.setReqBodyJson(jsonObject);
                } catch (JSONException e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            }

            return BlueshiftHttpManager.getInstance().send(builder.build());
        }

        return null;
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

            if (BlueshiftConstants.EVENT_API_URL(mContext).equals(api)) {
                // this is a case where request sent to non-bulk events api fails.
                String paramsJson = mRequest.getParamJson();

                if (!TextUtils.isEmpty(paramsJson)) {
                    Type type = new TypeToken<HashMap<String, Object>>() {
                    }.getType();
                    HashMap<String, Object> paramsMap = new Gson().fromJson(paramsJson, type);

                    Event event = new Event();
                    event.setEventParams(paramsMap);

                    BlueshiftLogger.d(LOG_TAG, "Adding failed request to failed events table");

                    try {
                        FailedEventsTable failedEventsTable = FailedEventsTable.getInstance(mContext);
                        failedEventsTable.insert(event);
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                        // could not add to failed requests. To avoid event drop, add it to
                        // existing request queue instead of failed events table.
                        requestQueue.add(mContext, mRequest);
                    }
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

    private void dispatchWithToken(final String deviceToken) {
        invokeDispatchBegin();
        BlueshiftExecutor.getInstance().runOnNetworkThread(
                new Runnable() {
                    @Override
                    public void run() {
                        processRequest(deviceToken);
                        invokeDispatchComplete();
                    }
                }
        );
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
}
