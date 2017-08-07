package com.blueshift.rich_push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Rahul Raveendran V P
 *         Created on 07/08/17 @ 12:27 PM
 *         https://github.com/rahulrvp
 */

public class ScheduledPushReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ScheduledPushReceiver", "Scheduled local push received.");

        if (intent != null) {
            Message message = (Message) intent.getSerializableExtra(Message.EXTRA_MESSAGE);
            RichPushNotification.handleMessage(context.getApplicationContext(), message);
        }
    }
}
