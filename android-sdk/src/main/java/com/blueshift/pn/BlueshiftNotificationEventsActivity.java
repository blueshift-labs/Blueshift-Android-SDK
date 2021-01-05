package com.blueshift.pn;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;
import com.blueshift.util.NotificationUtils;

import java.util.HashMap;


/**
 * @author Rahul Raveendran V P
 * Created on 22/12/17 @ 3:25 PM
 * https://github.com/rahulrvp
 */

public class BlueshiftNotificationEventsActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent != null) {
            processAction(intent.getAction(), intent.getExtras());
        }
    }

    protected void processAction(String action, Bundle extraBundle) {
        NotificationUtils.processNotificationClick(this, action, extraBundle);
    }
}
