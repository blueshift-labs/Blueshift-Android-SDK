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
        String action = "android.net.conn.CONNECTIVITY_CHANGE";
        if (context != null && intent != null && action.equals(intent.getAction())) {
            doBackgroundWork(context);
        }
    }

    private void doBackgroundWork(Context context) {
        final Context appContext = context != null ? context.getApplicationContext() : null;
        if (appContext != null) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            RequestQueue.getInstance().sync(appContext);
                        }
                    }
            ).start();
        }
    }
}
