package com.blueshift.fcm;

import android.util.Log;

import com.blueshift.Blueshift;
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
        String newToken = FirebaseInstanceId.getInstance().getToken();

        Blueshift.updateDeviceToken(newToken);

        Log.d("Blueshift", "FCM token: " + newToken);
    }
}
