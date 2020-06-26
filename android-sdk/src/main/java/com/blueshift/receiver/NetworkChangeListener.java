package com.blueshift.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 *         Created on 09/03/18 @ 3:16 PM
 *         https://github.com/rahulrvp
 */

public class NetworkChangeListener extends BroadcastReceiver {

    /*
    * This listener is responsible for receiving network change
    * actions for the versions before Android 5.0 */

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null
                && "android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            // call the db sync task to send events to server
            RequestQueue.getInstance().syncInBackground(context);
        }
    }
}
