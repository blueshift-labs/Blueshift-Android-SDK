package com.blueshift.rich_push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Created by rahul on 17/3/15.
 */
public class RichPushBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = RichPushBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v(LOG_TAG, "onReceive: " + intent.getAction());

        String messageJSON = intent.getStringExtra(RichPushConstants.EXTRA_MESSAGE);
        if (messageJSON != null) {
            try {
                final Message message = new Gson().fromJson(messageJSON, Message.class);
                /**
                 * The rich push rendering require network access (ex: image download)
                 * Since network operations are not allowed in main thread, we
                 * are rendering the push message in a different thread.
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RichPushNotification.handleMessage(context, message);
                    }
                }).start();
            } catch (JsonSyntaxException e) {
                Log.e(LOG_TAG, "Invalid JSON in push message: " + e.getMessage());
            }
        } else {
            Log.d(LOG_TAG, "Message not found. Passing the push message to host app via broadcast.");
            /**
             * Handing over the push message to host app if message is not found.
             */
            Intent bcIntent = new Intent(RichPushConstants.ACTION_PUSH_RECEIVED(context));
            bcIntent.putExtras(intent.getExtras());
            context.sendBroadcast(bcIntent);
        }
    }
}
