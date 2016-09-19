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
 * Created by rahul on 18/2/15.
 */
public class RichPushNotification {
    private final static String LOG_TAG = RichPushNotification.class.getSimpleName();

    public static void handleMessage(final Context context, final Message message) {
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

    private static void buildAndShowAlertDialog(Context context, Message message) {
        Intent notificationIntent = new Intent(context, NotificationActivity.class);
        notificationIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(notificationIntent);
    }

    private static void buildAndShowNotification(Context context, Message message) {
        int notificationID = 0;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);

        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
        if (configuration != null) {
            builder.setSmallIcon(configuration.getAppIcon());

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

        builder.setContentTitle(message.getTitle());
        builder.setContentText(message.getBody());

        if (!TextUtils.isEmpty(message.getImage_url())) {
            try {
                URL imageURL = new URL(message.getImage_url());
                Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());
                if (bitmap != null) {
                    builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not load image. " + e.getMessage());
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());

        // Tracking the notification display.
        Blueshift.getInstance(context).trackNotificationView(message, true);
    }

    private static void buildAndShowCustomNotifications(Context context, Message message) {
        CustomNotificationFactory notificationFactory = CustomNotificationFactory.getInstance();

        switch (message.getCategory()) {
            case AnimatedCarousel:
                notificationFactory.createAndShowAnimatedCarousel(context, message);
                break;

            case Carousel:
                break;

            case GifNotification:
                break;
        }
    }
}