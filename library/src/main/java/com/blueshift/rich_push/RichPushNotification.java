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
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;

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
                    Log.e(LOG_TAG, "Unknown notification type");
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
                        if (configuration.getProductPage() != null) {
                            Intent intent = new Intent(RichPushConstants.ACTION_VIEW(context));
                            intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                            intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                    0, intent, PendingIntent.FLAG_ONE_SHOT);

                            builder.addAction(0, "View", pendingIntent);
                        }

                        if (configuration.getCartPage() != null) {
                            Intent intent = new Intent(RichPushConstants.ACTION_BUY(context));
                            intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                            intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                    0, intent, PendingIntent.FLAG_ONE_SHOT);

                            builder.addAction(0, "Buy", pendingIntent);
                        }

                        break;

                    case ViewCart:
                        notificationID = NotificationCategory.ViewCart.getNotificationId();
                        if (configuration.getCartPage() != null) {
                            Intent intent = new Intent(RichPushConstants.ACTION_OPEN_CART(context));
                            intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                            intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                    0, intent, PendingIntent.FLAG_ONE_SHOT);

                            builder.addAction(0, "Open Cart", pendingIntent);
                        }

                        break;

                    case Promotion:
                        notificationID = NotificationCategory.Promotion.getNotificationId();
                        if (configuration.getOfferDisplayPage() != null) {
                            Intent intent = new Intent(RichPushConstants.ACTION_OPEN_OFFER_PAGE(context));
                            intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                            intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                    0, intent, PendingIntent.FLAG_ONE_SHOT);

                            builder.setContentIntent(pendingIntent);
                        }

                        break;

                    default:
                        /**
                         * Default action is to open app and send all details as extra inside intent
                         */
                        Intent intent = new Intent(RichPushConstants.ACTION_OPEN_APP(context));
                        intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                        intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                0, intent, PendingIntent.FLAG_ONE_SHOT);

                        builder.setContentIntent(pendingIntent);
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
                    Log.e(LOG_TAG, "Could not load image. " + e.getMessage());
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
}