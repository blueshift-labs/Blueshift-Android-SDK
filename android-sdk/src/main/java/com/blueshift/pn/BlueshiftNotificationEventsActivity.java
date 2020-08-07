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
import com.blueshift.util.NetworkUtils;
import com.blueshift.util.NotificationUtils;

import java.util.HashMap;


/**
 * @author Rahul Raveendran V P
 *         Created on 22/12/17 @ 3:25 PM
 *         https://github.com/rahulrvp
 */

public class BlueshiftNotificationEventsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NotificationClick";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Bundle extraBundle = getIntent().getExtras();
            String action = getIntent().getAction();

            processAction(action, extraBundle);
        }

        // close activity once action is taken.
        finish();
    }

    protected void processAction(String action, Bundle extraBundle) {
        if (extraBundle != null) {
            Message message = (Message) extraBundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
            if (message != null) {
                try {
                    HashMap<String, Object> clickAttr = new HashMap<>();
                    String deepLink = extraBundle.getString(RichPushConstants.EXTRA_DEEP_LINK_URL);
                    clickAttr.put(BlueshiftConstants.KEY_CLICK_URL, NetworkUtils.encodeUrlParam(deepLink));

                    // mark 'click'
                    Blueshift.getInstance(this).trackNotificationClick(message, clickAttr);

                    Intent intent = null;

                    if (!TextUtils.isEmpty(action)) {
                        if (action.equals(RichPushConstants.ACTION_OPEN_APP(this))) {
                            intent = NotificationUtils.getOpenAppIntent(this, message);
                        } else if (action.equals(RichPushConstants.ACTION_VIEW(this))) {
                            intent = NotificationUtils.getViewProductActivityIntent(this, message);
                        } else if (action.equals(RichPushConstants.ACTION_BUY(this))) {
                            intent = NotificationUtils.getAddToCartActivityIntent(this, message);
                        } else if (action.equals(RichPushConstants.ACTION_OPEN_CART(this))) {
                            intent = NotificationUtils.getViewCartActivityIntent(this, message);
                        } else if (action.equals(RichPushConstants.ACTION_OPEN_OFFER_PAGE(this))) {
                            intent = NotificationUtils.getViewOffersActivityIntent(this, message);
                        }
                    }

                    if (intent == null) {
                        // make sure the app is opened even if no category deep-links are available
                        intent = NotificationUtils.getOpenAppIntent(this, message);
                    }

                    // add complete bundle to the intent.
                    intent.putExtras(extraBundle);

                    // Note: This will create a new task and launch the app with the corresponding
                    // activity. As per the docs, the dev should add parent activity to all the
                    // activities registered in the manifest in order to get the back stack working
                    // doc: https://developer.android.com/training/notify-user/navigation#DirectEntry
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities();

                    // mark 'app_open'
                    Blueshift.getInstance(this).trackNotificationPageOpen(message, false);

                    // remove cached images(if any) for this notification
                    NotificationUtils.removeCachedCarouselImages(this, message);

                    // remove notification from tray
                    NotificationManager notificationManager =
                            (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        int notificationID = intent.getIntExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, 0);
                        notificationManager.cancel(notificationID);
                    }

                    sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            } else {
                BlueshiftLogger.d(LOG_TAG, "No message found inside bundle.");
            }
        } else {
            BlueshiftLogger.d(LOG_TAG, "No bundle found from the notification click event.");
        }
    }
}
