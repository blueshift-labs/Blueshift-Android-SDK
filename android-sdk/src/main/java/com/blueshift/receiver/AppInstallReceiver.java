package com.blueshift.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftLogger;

/**
 * @author Rahul Raveendran V P
 * Created on 17/3/15 @ 3:04 PM
 * <a href="https://github.com/rahulrvp">...</a>
 * @deprecated Since version <a href="https://github.com/blueshift-labs/Blueshift-Android-SDK/releases/tag/v3.4.6">3.4.6</a>, the Blueshift SDK automatically detects app install and update events.
 * Using this class to track app installs can lead to confusion. We recommend using the automatic detection instead.
 * This class will be removed in a future release.
 */
@Deprecated
public class AppInstallReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = AppInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.keySet().contains("referrer")) {
            String referrer = intent.getExtras().getString("referrer");
            if (referrer != null) {
                Blueshift.getInstance(context).trackAppInstall(referrer, false);
            } else {
                BlueshiftLogger.w(LOG_TAG, "The referrer url parameters are not found in the INSTALL_REFERRER broadcast message.");
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "The broadcast message does not contain any extras.");
        }
    }
}
