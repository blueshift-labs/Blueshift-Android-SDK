package com.blueshift.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.blueshift.httpmanager.request_queue.RequestQueue;


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
        final Context appContext = context.getApplicationContext();

        Runnable taskRunner = new Runnable() {
            @Override
            public void run() {
                RequestQueue.getInstance(appContext).sync();
            }
        };

        HandlerThread handlerThread = new HandlerThread("RequestQueueSyncThread");
        handlerThread.start();

        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        handler.post(taskRunner);
    }
}
