package com.blueshift.fcm;

import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.util.DeviceUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/12/16 @ 12:02 PM
 *         https://github.com/rahulrvp
 */

public class BlueshiftInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        /*
         * We don't need to set the new token inside the mDeviceParams when
         * token gets reset. We are updating the parameters with latest token
         * before sending it to server inside RequestQueue#155
         *
         */

        String newToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("Blueshift", "FCM token: " + newToken);

        callIdentify();
    }

    /**
     * We are calling an identify here to make sure that the change in
     * device token is notified to the blueshift servers.
     *
     */
    private void callIdentify() {
        String adId = DeviceUtils.getAdvertisingID(this);
        Blueshift
                .getInstance(this)
                .identifyUserByDeviceId(adId, null, false);
    }
}
