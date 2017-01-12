package com.blueshift.rich_push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.util.SdkLog;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * @author Rahul Raveendran V P
 *         Created on 17/3/15 @ 3:05 PM
 *         https://github.com/rahulrvp
 */
public class RichPushBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = RichPushBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        SdkLog.d(LOG_TAG, "onReceive: " + intent.getAction());

        String messageJSON = intent.getStringExtra(Message.EXTRA_MESSAGE);
        if (messageJSON != null) {
            try {
                Message message = new Gson().fromJson(messageJSON, Message.class);
                if (message != null) {
                    // trying to fetch campaign params
                    String msgUUID = intent.getStringExtra(Message.EXTRA_BSFT_MESSAGE_UUID);
                    String experimentUUID = intent.getStringExtra(Message.EXTRA_BSFT_EXPERIMENT_UUID);
                    String userUUID = intent.getStringExtra(Message.EXTRA_BSFT_USER_UUID);
                    String txnUUID = intent.getStringExtra(Message.EXTRA_BSFT_TRANSACTIONAL_UUID);

                    // adding message uuid
                    message.setBsftMessageUuid(msgUUID);

                    // adding campaign parameters inside message.
                    message.setBsftExperimentUuid(experimentUUID);
                    message.setBsftUserUuid(userUUID);
                    message.setBsftTransactionUuid(txnUUID);

                    if (message.isSilentPush()) {
                        /**
                         * This is a silent push to track uninstalls.
                         * SDK has nothing to do with this. If this push was not delivered,
                         * server will track GCM registrations fails and will decide if the app is uninstalled or not.
                         */
                        SdkLog.i(LOG_TAG, "A silent push received.");
                    } else {
                        RichPushNotification.handleMessage(context, message);
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
            Intent bcIntent = new Intent(RichPushConstants.ACTION_PUSH_RECEIVED(context));
            bcIntent.putExtras(intent.getExtras());
            context.sendBroadcast(bcIntent);
        }
    }
}
