package com.blueshift;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by rahul on 25/2/15.
 *
 * This class is used for testing the SDK. Will be removed in the end.
 */
public class SamplePushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * If the category specified in the push message is not included in the list of categories handled by SDK,
         * then a broadcast with action "com.blueshift.rich_push.ACTION_PUSH_RECEIVED" is sent.
         * This receiver will receive that broadcast and the host app developer can do his implementation here.
         */
        Log.i("SamplePushReceiver", "Received message from SDK since it does not have implementation for the category came with this push message.");
    }
}
