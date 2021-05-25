package com.blueshift.pn;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.blueshift.BlueshiftLogger;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;
import com.blueshift.util.NotificationUtils;

import java.io.Serializable;


/**
 * @author Rahul Raveendran V P
 * Created on 22/12/17 @ 3:25 PM
 * https://github.com/rahulrvp
 */

public class BlueshiftNotificationEventsActivity extends AppCompatActivity {
    private static final String TAG = "PNEventsActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // We're not supposed to have the Message or CarouselElement inside the savedInstanceState.
        // If found, remove them inorder to avoid any crashes inside the super.onCreate call.
        if (savedInstanceState != null) {
            removeMessage(savedInstanceState);
            removeCarouselElement(savedInstanceState);
        }

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) processAction(intent.getAction(), intent.getExtras());

        // finish this activity to avoid onResume execution (mobl-639)
        if (!isFinishing()) {
            finish();
        } else {
            BlueshiftLogger.d(TAG, "finish() called externally.");
        }
    }

    private void removeMessage(Bundle bundle) {
        if (bundle != null && bundle.containsKey(RichPushConstants.EXTRA_MESSAGE)) {
            BlueshiftLogger.d(TAG, "Removing the message object to avoid crashes.");
            bundle.remove(RichPushConstants.EXTRA_MESSAGE);
        }
    }

    private void removeCarouselElement(Bundle bundle) {
        if (bundle != null && bundle.containsKey(RichPushConstants.EXTRA_CAROUSEL_ELEMENT)) {
            BlueshiftLogger.d(TAG, "Removing the carousel element object to avoid crashes.");
            bundle.remove(RichPushConstants.EXTRA_CAROUSEL_ELEMENT);
        }
    }

    protected void processAction(String action, Bundle extraBundle) {
        NotificationUtils.processNotificationClick(this, action, extraBundle);
    }
}
