package com.blueshift.rich_push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.util.SdkLog;

import java.io.IOException;
import java.net.URL;

/**
 * @author Rahul Raveendran V P
 *         Created on 18/2/15 @ 12:22 PM
 *         https://github.com/rahulrvp
 */
public class RichPushNotification {
    private final static String LOG_TAG = RichPushNotification.class.getSimpleName();

    public static void handleMessage(final Context context, final Message message) {
        if (context != null && message != null) {
            switch (message.getNotificationType()) {
                case AlertDialog:
                    buildAndShowAlertDialog(context, message);
                    break;

                case Notification:
                    /**
                     * The rich push rendering require network access (ex: image download)
                     * Since network operations are not allowed in main thread, we
                     * are rendering the push message in a different thread.
                     */
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            buildAndShowNotification(context, message);

                            return null;
                        }
                    }.execute();

                    break;

                case CustomNotification:
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            buildAndShowCustomNotifications(context, message);

                            return null;
                        }
                    }.execute();

                    break;

                default:
                    SdkLog.e(LOG_TAG, "Unknown notification type");
            }
        }
    }

    private static void buildAndShowAlertDialog(Context context, Message message) {
        if (context != null && message != null) {
            Intent notificationIntent = new Intent(context, NotificationActivity.class);
            notificationIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(notificationIntent);
        }
    }

    private static void buildAndShowNotification(Context context, Message message) {
        if (context != null && message != null) {
            int notificationID = 0;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setAutoCancel(true);

            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null) {
                builder.setSmallIcon(configuration.getSmallIconResId());

                Bitmap bigIcon = BitmapFactory.decodeResource(context.getResources(), configuration.getLargeIconResId());
                if (bigIcon != null) {
                    builder.setLargeIcon(bigIcon);
                }

                switch (message.getCategory()) {
                    case Buy:
                        notificationID = NotificationCategory.Buy.getNotificationId();

                        PendingIntent viewPendingIntent = getViewActionPendingIntent(context, message, notificationID);
                        builder.addAction(0, "View", viewPendingIntent);

                        PendingIntent buyPendingIntent = getBuyActionPendingIntent(context, message, notificationID);
                        builder.addAction(0, "Buy", buyPendingIntent);

                        break;

                    case ViewCart:
                        notificationID = NotificationCategory.ViewCart.getNotificationId();

                        PendingIntent openCartPendingIntent = getOpenCartPendingIntent(context, message, notificationID);
                        builder.addAction(0, "Open Cart", openCartPendingIntent);

                        break;

                    case Promotion:
                        notificationID = NotificationCategory.Promotion.getNotificationId();

                        PendingIntent pendingIntent = getOpenPromotionPendingIntent(context, message, notificationID);
                        builder.setContentIntent(pendingIntent);

                        break;

                    default:
                        /**
                         * Default action is to open app and send all details as extra inside intent
                         */
                        PendingIntent defaultPendingIntent = getOpenAppPendingIntent(context, message, notificationID);
                        builder.setContentIntent(defaultPendingIntent);
                }
            }

            builder.setContentTitle(message.getContentTitle());
            builder.setContentText(message.getContentText());
            builder.setSubText(message.getContentSubText());

            if (!TextUtils.isEmpty(message.getImageUrl())) {
                try {
                    URL imageURL = new URL(message.getImageUrl());
                    Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());
                    if (bitmap != null) {
                        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                        bigPictureStyle.bigPicture(bitmap);

                        if (message.getBigContentTitle() != null) {
                            bigPictureStyle.setBigContentTitle(message.getBigContentTitle());
                        }

                        if (message.getBigContentSummaryText() != null) {
                            bigPictureStyle.setSummaryText(message.getBigContentSummaryText());
                        }

                        builder.setStyle(bigPictureStyle);
                    }
                } catch (IOException e) {
                    SdkLog.e(LOG_TAG, "Could not load image. " + e.getMessage());
                }
            } else {
                // enable big text style
                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

                if (message.getBigContentTitle() != null) {
                    bigTextStyle.setBigContentTitle(message.getBigContentTitle());
                }

                if (message.getBigContentSummaryText() != null) {
                    bigTextStyle.setSummaryText(message.getBigContentSummaryText());
                }

                if (message.getContentText() != null) {
                    bigTextStyle.bigText(message.getContentText());
                }

                builder.setStyle(bigTextStyle);
            }

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationID, builder.build());

            // Tracking the notification display.
            Blueshift.getInstance(context).trackNotificationView(message, true);
        }
    }

    private static void buildAndShowCustomNotifications(Context context, Message message) {
        if (context != null && message != null) {
            CustomNotificationFactory notificationFactory = CustomNotificationFactory.getInstance();

            switch (message.getCategory()) {
                case AnimatedCarousel:
                    notificationFactory.createAndShowAnimatedCarousel(context, message);
                    break;

                case Carousel:
                    notificationFactory.createAndShowCarousel(context, message);
                    break;

                case GifNotification:
                    break;
            }
        }
    }

    // [BEGIN] PendingIntent builder methods.

    private static PendingIntent getBuyActionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_BUY(context);
        return getBroadcastPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getViewActionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_VIEW(context);
        return getBroadcastPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenCartPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_CART(context);
        return getBroadcastPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenAppPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_APP(context);
        return getBroadcastPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenPromotionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_OFFER_PAGE(context);
        return getBroadcastPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getBroadcastPendingIntent(String action, Context context, Message message, int notificationId) {
        // if deep link url is available, despite the fact that we have a category based action,
        // we will use the open app action to launch app and pass the deep link url to it.
        if (TextUtils.isEmpty(action) || (message != null && message.isDeepLinkingEnabled())) {
            action = RichPushConstants.ACTION_OPEN_APP(context);
        }

        Intent bcIntent = new Intent(action);
        bcIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
        bcIntent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);

        // add deep link URL if available.
        if (message != null && message.isDeepLinkingEnabled()) {
            bcIntent.putExtra(RichPushConstants.EXTRA_DEEP_LINK_URL, message.getDeepLinkUrl());
        }

        return PendingIntent.getBroadcast(context, 0, bcIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    // [END] PendingIntent builder methods.
}