package com.blueshift.pn;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;
import com.blueshift.util.NotificationUtils;


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
                    // mark 'click'
                    Blueshift.getInstance(this).trackNotificationClick(message);

                    Intent intent;

                    if (TextUtils.isEmpty(action)) {
                        // open app if no action is provided.
                        intent = NotificationUtils.getOpenAppIntent(this, message);
                    } else if (action.equals(RichPushConstants.ACTION_VIEW(this))) {
                        intent = NotificationUtils.getViewProductActivityIntent(this, message);
                    } else if (action.equals(RichPushConstants.ACTION_BUY(this))) {
                        intent = NotificationUtils.getAddToCartActivityIntent(this, message);
                    } else if (action.equals(RichPushConstants.ACTION_OPEN_CART(this))) {
                        intent = NotificationUtils.getViewCartActivityIntent(this, message);
                    } else if (action.equals(RichPushConstants.ACTION_OPEN_OFFER_PAGE(this))) {
                        intent = NotificationUtils.getViewOffersActivityIntent(this, message);
                    } else {
                        // for ACTION OPEN and other unknown actions, open the app.
                        intent = NotificationUtils.getOpenAppIntent(this, message);
                    }

                    // add complete bundle to the intent.
                    intent.putExtras(extraBundle);

                    // start the activity
                    startActivity(intent);

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
                    e.printStackTrace();
                }
            } else {
                Log.d(LOG_TAG, "No message found inside bundle.");
            }
        } else {
            Log.d(LOG_TAG, "No bundle found from the notification click event.");
        }
    }
}
