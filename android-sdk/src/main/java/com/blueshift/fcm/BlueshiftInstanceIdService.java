package com.blueshift.fcm;

import android.util.Log;

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
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d("Blueshift", "FCM token: " + token);
    }
}
