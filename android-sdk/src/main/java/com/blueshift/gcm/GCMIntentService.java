package com.blueshift.gcm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blueshift.Blueshift;

/**
 * @author Rahul Raveendran V P
 *         Created on 18/2/15 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class GCMIntentService extends GCMBaseIntentService {

    private final static String LOG_TAG = GCMIntentService.class.getSimpleName();

    public GCMIntentService() {
        super(GCMRegistrar.getGCMSenderId());
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.v(LOG_TAG, "onReceive: " + intent.getAction());

        /**
         * Local broadcast with Host-App's package name.
         * This is used for distinguishing between push messages when multiple apps in the device uses Blueshift SDK.
         */
        String bcAction = context.getPackageName() + ".RICH_PUSH_RECEIVED";
        Intent bcIntent = new Intent(bcAction);
        bcIntent.putExtras(intent.getExtras());
        sendBroadcast(bcIntent);
    }

    @Override
    protected void onError(Context context, String errorId) {
        Log.i(TAG, "Error code: " + errorId);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        GCMRegistrar.setRegisteredOnServer(context, true);
        Blueshift.updateDeviceToken(registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
    }

}
