package com.blueshift.rich_push;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * @author Rahul Raveendran V P
 *         Created on 22/12/17 @ 3:36 PM
 *         https://github.com/rahulrvp
 */


public class NotificationClickService extends IntentService {

    public NotificationClickService(String name) {
        super(name);
    }

    public NotificationClickService() {
        super("NotificationClickService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("XXX", "Service started.");
    }
}
