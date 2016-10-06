package com.blueshift.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.blueshift.Blueshift;

/**
 * @author Rahul Raveendran V P
 *         Created on 17/3/15 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class AppInstallReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = AppInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.keySet().contains("referrer")) {
            String referrer = intent.getExtras().getString("referrer");
            if (referrer != null) {
                // events sent by SDK are always batched.
                Blueshift.getInstance(context).trackAppInstall(referrer, true);
            } else {
                Log.w(LOG_TAG, "The referrer url parameters are not found in the INSTALL_REFERRER broadcast message.");
            }
        } else {
            Log.w(LOG_TAG, "The broadcast message does not contain any extras.");
        }
    }
}
