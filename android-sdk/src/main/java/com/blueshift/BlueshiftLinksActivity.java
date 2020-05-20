package com.blueshift;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * This Activity is expected to receive the deep-links intended to be received
 * by the SDK.
 */
public class BlueshiftLinksActivity extends AppCompatActivity {

    private static final String TAG = "BlueshiftLinksActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Uri deepLink = getIntent().getData();
            if (deepLink == null) {
                BlueshiftLogger.e(TAG, "No URL found inside the Intent.");
            } else {
                new BlueshiftLinksHandler(this).handleBlueshiftUniversalLinks(getIntent(), null);
            }
        }
    }
}
