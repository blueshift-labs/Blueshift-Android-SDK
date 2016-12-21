package com.blueshift.fcm;

import android.util.Log;

import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushNotification;
import com.blueshift.util.SdkLog;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Map;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/12/16 @ 11:59 AM
 *         https://github.com/rahulrvp
 */

public class BlueshiftMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = "MessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data != null) {
            handleMessage(data);
        } else {
            super.onMessageReceived(remoteMessage);
        }
    }

    private void handleMessage(Map<String, String> data) {
        if (data != null) {
            String msgJson = data.get(Message.EXTRA_MESSAGE);
            if (msgJson != null) {
                try {
                    Message message = new Gson().fromJson(msgJson, Message.class);
                    if (message != null) {
                        // trying to fetch campaign params
                        String experimentUUID = data.get(Message.EXTRA_BSFT_EXPERIMENT_UUID);
                        String userUUID = data.get(Message.EXTRA_BSFT_USER_UUID);

                        // adding campaign parameters inside message.
                        message.setBsftExperimentUuid(experimentUUID);
                        message.setBsftUserUuid(userUUID);

                        if (message.isSilentPush()) {
                            /**
                             * This is a silent push to track uninstalls.
                             * SDK has nothing to do with this. If this push was not delivered,
                             * server will track GCM registrations fails and will decide if the app is uninstalled or not.
                             */
                            SdkLog.i(LOG_TAG, "A silent push received.");
                        } else {
                            RichPushNotification.handleMessage(this, message);
                        }
                    } else {
                        Log.e(LOG_TAG, "Null message found in push message.");
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(LOG_TAG, "Invalid JSON in push message: " + e.getMessage());
                }
            } else {
                SdkLog.d(LOG_TAG, "Message not found. Passing the push message to host app via broadcast.");

                /**
                 * Handing over the push message to host app if message is not found.
                 */
                onMessageNotFound(data);
            }
        }
    }

    protected void onMessageNotFound(Map<String, String> data) {

    }
}
