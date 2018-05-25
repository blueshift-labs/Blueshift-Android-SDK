package com.blueshift.rich_push;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.util.NotificationUtils;
import com.blueshift.util.SdkLog;

/**
 * @author Rahul Raveendran V P
 *         Created on 25/2/15 @ 3:07 PM
 *         https://github.com/rahulrvp
 */
public class RichPushActionReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "RichPushActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        SdkLog.d(LOG_TAG, "onReceive - " + intent.getAction());

        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                if (action.equals(RichPushConstants.ACTION_VIEW(context))) {
                    displayProductPage(context, bundle);
                } else if (action.equals(RichPushConstants.ACTION_BUY(context))) {
                    addToCart(context, bundle);
                } else if (action.equals(RichPushConstants.ACTION_OPEN_CART(context))) {
                    displayCartPage(context, bundle);
                } else if (action.equals(RichPushConstants.ACTION_OPEN_OFFER_PAGE(context))) {
                    displayOfferDisplayPage(context, bundle);
                } else if (action.equals(RichPushConstants.ACTION_OPEN_APP(context))) {
                    openApp(context, bundle);
                }
            } else {
                SdkLog.d(LOG_TAG, "No bundle data available with the broadcast.");
            }

            Message message = null;
            try {
                message = (Message) intent.getSerializableExtra(RichPushConstants.EXTRA_MESSAGE);
            } catch (Exception ignore) {
            }

            if (message == null) return;

            // remove cached images(if any) for this notification
            NotificationUtils.removeCachedCarouselImages(context, message);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                int notificationID = intent.getIntExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, 0);
                notificationManager.cancel(notificationID);
            }

            Blueshift.getInstance(context).trackNotificationClick(message);

            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } else {
            SdkLog.d(LOG_TAG, "No action found with the broadcast.");
        }
    }

    public void displayProductPage(Context context, Bundle bundle) {
        Message message = (Message) bundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
        if (message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null && configuration.getProductPage() != null) {
                Intent pageLauncherIntent = new Intent(context, configuration.getProductPage());
                // add product specific items.
                pageLauncherIntent.putExtra("product_id", message.getProductId());
                pageLauncherIntent.putExtra("mrp", message.getMrp());
                pageLauncherIntent.putExtra("price", message.getPrice());
                pageLauncherIntent.putExtra("data", message.getData());
                // add the whole bundle. typically contains message object.
                pageLauncherIntent.putExtras(bundle);

                pageLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(pageLauncherIntent);

                trackAppOpen(context, message);
            } else {
                Log.i(LOG_TAG, "Could not find product activity class inside configuration. Opening MAIN activity.");

                // default action is to open app
                openApp(context, bundle);
            }
        }
    }

    public void addToCart(Context context, Bundle bundle) {
        Message message = (Message) bundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
        if (message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null && configuration.getCartPage() != null) {
                Intent pageLauncherIntent = new Intent(context, configuration.getCartPage());
                // add product specific items.
                pageLauncherIntent.putExtra("product_id", message.getProductId());
                pageLauncherIntent.putExtra("mrp", message.getMrp());
                pageLauncherIntent.putExtra("price", message.getPrice());
                pageLauncherIntent.putExtra("data", message.getData());
                // add the whole bundle. typically contains message object.
                pageLauncherIntent.putExtras(bundle);

                pageLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(pageLauncherIntent);

                trackAppOpen(context, message);
            } else {
                Log.i(LOG_TAG, "Could not find cart activity class inside configuration. Opening MAIN activity.");

                // default action is to open app
                openApp(context, bundle);
            }
        }
    }

    public void displayCartPage(Context context, Bundle bundle) {
        Message message = (Message) bundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
        if (message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null && configuration.getCartPage() != null) {
                Intent pageLauncherIntent = new Intent(context, configuration.getCartPage());
                // add the whole bundle. typically contains message object.
                pageLauncherIntent.putExtras(bundle);

                pageLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(pageLauncherIntent);

                trackAppOpen(context, message);
            } else {
                Log.i(LOG_TAG, "Could not find cart activity class inside configuration. Opening MAIN activity.");

                // default action is to open app
                openApp(context, bundle);
            }
        }
    }

    public void displayOfferDisplayPage(Context context, Bundle bundle) {
        Message message = (Message) bundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
        if (message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null && configuration.getOfferDisplayPage() != null) {
                Intent pageLauncherIntent = new Intent(context, configuration.getOfferDisplayPage());
                // add the whole bundle. typically contains message object.
                pageLauncherIntent.putExtras(bundle);

                pageLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(pageLauncherIntent);

                trackAppOpen(context, message);
            } else {
                Log.i(LOG_TAG, "Could not find offer's page activity class inside configuration. Opening MAIN activity.");

                // default action is to open app
                openApp(context, bundle);
            }
        }
    }

    public void openApp(Context context, Bundle bundle) {
        Message message = (Message) bundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
        if (message != null) {
            PackageManager packageManager = context.getPackageManager();
            Intent launcherIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            // add the whole bundle. typically contains message object.
            if (launcherIntent != null) {
                launcherIntent.putExtras(bundle);

                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launcherIntent);
            }

            trackAppOpen(context, message);
        }
    }

    protected void trackAppOpen(Context context, Message message) {
        Blueshift.getInstance(context).trackNotificationPageOpen(message, false);
    }
}
