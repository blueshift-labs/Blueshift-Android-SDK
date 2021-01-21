package com.blueshift.pn;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.blueshift.BlueshiftLogger;
import com.blueshift.util.NotificationUtils;


/**
 * @author Rahul Raveendran V P
 * Created on 22/12/17 @ 3:25 PM
 * https://github.com/rahulrvp
 */

public class BlueshiftNotificationEventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) processAction(intent.getAction(), intent.getExtras());

        // finish this activity to avoid onResume execution (mobl-639)
        if (!isFinishing()) {
            finish();
        } else {
            BlueshiftLogger.d("NotificationEventsActivity", "finish() called externally.");
        }
    }

    protected void processAction(String action, Bundle extraBundle) {
        NotificationUtils.processNotificationClick(this, action, extraBundle);
    }
}
