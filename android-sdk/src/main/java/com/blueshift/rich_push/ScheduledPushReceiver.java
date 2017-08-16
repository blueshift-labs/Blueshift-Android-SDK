package com.blueshift.rich_push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

/**
 * @author Rahul Raveendran V P
 *         Created on 07/08/17 @ 12:27 PM
 *         https://github.com/rahulrvp
 */

public class ScheduledPushReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "ScheduledPushReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "Scheduled local push received.");

        if (intent != null) {
            String messageJSON = intent.getStringExtra(Message.EXTRA_MESSAGE);
            if (!TextUtils.isEmpty(messageJSON)) {
                try {
                    Message message = new Gson().fromJson(messageJSON, Message.class);
                    RichPushNotification.handleMessage(context.getApplicationContext(), message);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Invalid message payload received. " + messageJSON);
                }
            } else {
                Log.e(LOG_TAG, "No message payload received. ");
            }
        }
    }
}
