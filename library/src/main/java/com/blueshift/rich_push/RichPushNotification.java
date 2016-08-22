package com.blueshift.rich_push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.util.NetworkUtils;

import java.io.File;

/**
 * Created by rahul on 18/2/15.
 */
public class RichPushNotification {
    private final static String LOG_TAG = RichPushNotification.class.getSimpleName();

    public static void handleMessage(Context context, Message message) {
        buildAndShowNotification(context, message);

        // Tracking the notification display.
        Blueshift.getInstance(context).trackNotificationView(message.getId());
    }

    private static void buildAndShowNotification(Context context, Message message) {
        int notificationID = 0;

        Notification.Builder builder = new Notification.Builder(context);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);

        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
        if (configuration != null) {
            builder.setSmallIcon(configuration.getAppIcon());

            if (message.category.equals(Message.CATEGORY_BUY)) {
                notificationID = 100;
                if (configuration.getProductPage() != null) {
                    Intent intent = new Intent(RichPushConstants.ACTION_VIEW(context));
                    intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                    intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                            0, intent, PendingIntent.FLAG_ONE_SHOT);

                    builder.addAction(0, "View", pendingIntent);
                }

                if (configuration.getCartPage() !=null) {
                    Intent intent = new Intent(RichPushConstants.ACTION_BUY(context));
                    intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                    intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                            0, intent, PendingIntent.FLAG_ONE_SHOT);

                    builder.addAction(0, "Buy", pendingIntent);
                }
            } else if (message.category.equals(Message.CATEGORY_VIEW_CART)) {
                notificationID = 200;
                if (configuration.getCartPage() !=null) {
                    Intent intent = new Intent(RichPushConstants.ACTION_OPEN_CART(context));
                    intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                    intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                            0, intent, PendingIntent.FLAG_ONE_SHOT);

                    builder.addAction(0, "Open Cart", pendingIntent);
                }
            } else if (message.category.equals(Message.CATEGORY_OFFER)) {
                notificationID = 300;
                if (configuration.getOfferDisplayPage() != null) {
                    Intent intent = new Intent(RichPushConstants.ACTION_OPEN_OFFER_PAGE(context));
                    intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                    intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationID);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                            0, intent, PendingIntent.FLAG_ONE_SHOT);

                    builder.setContentIntent(pendingIntent);
                }
            }
        }

        builder.setContentTitle(message.getTitle());
        builder.setContentText(message.getBody());

        if (message.getImage_url() != null && !message.getImage_url().isEmpty()) {
            String destinationPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.jpg";
            if (NetworkUtils.downloadFile(message.getImage_url(), destinationPath)) {
                Bitmap bitmap = BitmapFactory.decodeFile(destinationPath);
                builder.setStyle(new Notification.BigPictureStyle().bigPicture(bitmap));
                File file = new File(destinationPath);
                Log.d(LOG_TAG, "Deleting cached image " + (file.delete() ? "success." : "failed." ));
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());
    }
}