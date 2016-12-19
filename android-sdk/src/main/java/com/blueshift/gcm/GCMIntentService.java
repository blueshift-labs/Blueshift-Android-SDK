package com.blueshift.gcm;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.BlueShiftPreference;
import com.blueshift.Blueshift;
import com.blueshift.model.UserInfo;
import com.blueshift.util.DeviceUtils;

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
        try {
            Log.v(LOG_TAG, "onReceive: " + intent.getAction());

            /**
             * Local broadcast with Host-App's package name.
             * This is used for distinguishing between push messages when multiple apps in the device uses Blueshift SDK.
             */
            String bcAction = context.getPackageName() + ".RICH_PUSH_RECEIVED";
            Intent bcIntent = new Intent(bcAction);
            bcIntent.putExtras(intent.getExtras());
            sendBroadcast(bcIntent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }

    @Override
    protected void onError(Context context, String errorId) {
        try {
            Log.i(TAG, "Error code: " + errorId);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        try {
            Log.i(TAG, "Device registered: regId = " + registrationId);
            GCMRegistrar.setRegisteredOnServer(context, true);
            Blueshift.updateDeviceToken(registrationId);

            /**
             * Let's check if we have an email id present inside UserInfo.
             * If that email id is not identified already, lets fire an identify event
             * with this new device_token and email id.
             */
            UserInfo userInfo = UserInfo.getInstance(context);
            String emailId = userInfo.getEmail();

            if (!BlueShiftPreference.isEmailAlreadyIdentified(context, emailId)) {
                if (!TextUtils.isEmpty(registrationId) && !TextUtils.isEmpty(emailId)) {
                    Blueshift
                            .getInstance(context)
                            .identifyUserByDeviceId(DeviceUtils.getAdvertisingID(context), null, false);

                    BlueShiftPreference.markEmailAsIdentified(context, emailId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        try {
            Log.i(TAG, "Device unregistered");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }
}
