package com.blueshift.rich_push;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.pn.BlueshiftNotificationEventsActivity;
import com.blueshift.util.NotificationUtils;
import com.blueshift.util.SdkLog;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * @author Rahul Raveendran V P
 *         Created on 18/2/15 @ 12:22 PM
 *         https://github.com/rahulrvp
 */
public class NotificationFactory {
    private final static String LOG_TAG = NotificationFactory.class.getSimpleName();

    private final static Random sRandom = new Random();

    public static int getRandomPIRequestCode() {
        return sRandom.nextInt(Integer.MAX_VALUE);
    }

    public static int getRandomNotificationId() {
        return sRandom.nextInt(Integer.MAX_VALUE);
    }

    public static void handleMessage(final Context context, final Message message) {
        if (context != null && message != null) {
            switch (message.getNotificationType()) {
                case AlertDialog:
                    buildAndShowAlertDialog(context, message);
                    break;

                case Notification:
                    buildAndShowNotification(context, message);
                    break;

                case CustomNotification:
                    buildAndShowCustomNotifications(context, message);
                    break;

                case NotificationScheduler:
                    scheduleNotifications(context, message);
                    break;

                default:
                    SdkLog.e(LOG_TAG, "Unknown notification type");
            }
        }
    }

    private static void buildAndShowAlertDialog(final Context context, final Message message) {
        if (context != null && message != null) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    return isAppInForeground(context);
                }

                @Override
                protected void onPostExecute(Boolean appIsInForeground) {
                    Intent notificationIntent = new Intent(context, NotificationActivity.class);
                    notificationIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    /*
                     * Clear the stack only if the app is in background / killed.
                     */
                    if (!appIsInForeground) {
                        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }

                    context.startActivity(notificationIntent);
                }
            }.execute();
        }
    }

    private static boolean isAppInForeground(Context context) {
        boolean isAppInForeground = false;

        if (context != null) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = null;
            if (activityManager != null) {
                appProcesses = activityManager.getRunningAppProcesses();
            }
            if (appProcesses != null) {
                String packageName = context.getPackageName();
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess != null
                            && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && appProcess.processName.equals(packageName)) {

                        isAppInForeground = true;
                        break;
                    }
                }
            }
        }

        return isAppInForeground;
    }

    private static void buildAndShowNotification(Context context, Message message) {
        if (context != null && message != null) {
            int notificationId = NotificationFactory.getRandomNotificationId();

            String channelName = NotificationUtils.getNotificationChannelName(context, message);
            String channelId = NotificationUtils.getNotificationChannelId(channelName);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setAutoCancel(true);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);

            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null) {
                int smallIconResId = configuration.getSmallIconResId();
                if (smallIconResId != 0) {
                    builder.setSmallIcon(smallIconResId);
                }

                int color = configuration.getNotificationColor();
                if (color != 0) {
                    builder.setColor(color);
                }

                int bigIconResId = configuration.getLargeIconResId();
                if (bigIconResId != 0) {
                    Bitmap bigIcon = BitmapFactory.decodeResource(context.getResources(), bigIconResId);
                    if (bigIcon != null) {
                        builder.setLargeIcon(bigIcon);
                    }
                }

                // pending intent that opens the app using MAIN activity.
                PendingIntent openAppPendingIntent = getOpenAppPendingIntent(context, message, notificationId);

                switch (message.getCategory()) {
                    case Buy:
                        PendingIntent viewPendingIntent = getViewActionPendingIntent(context, message, notificationId);
                        builder.addAction(0, "View", viewPendingIntent);

                        PendingIntent buyPendingIntent = getBuyActionPendingIntent(context, message, notificationId);
                        builder.addAction(0, "Buy", buyPendingIntent);

                        builder.setContentIntent(openAppPendingIntent);

                        break;

                    case ViewCart:
                        PendingIntent openCartPendingIntent = getOpenCartPendingIntent(context, message, notificationId);
                        builder.addAction(0, "Open Cart", openCartPendingIntent);

                        builder.setContentIntent(openAppPendingIntent);

                        break;

                    case Promotion:
                        PendingIntent openPromoPendingIntent = getOpenPromotionPendingIntent(context, message, notificationId);
                        builder.setContentIntent(openPromoPendingIntent);

                        break;

                    default:
                        /*
                         * Default action is to open app and send all details as extra inside intent
                         */
                        builder.setContentIntent(openAppPendingIntent);
                }
            }

            builder.setContentTitle(message.getContentTitle());
            builder.setContentText(message.getContentText());

            // display content sub text
            if (!TextUtils.isEmpty(message.getContentSubText())) {
                builder.setSubText(message.getContentSubText());
            }

            if (!TextUtils.isEmpty(message.getImageUrl())) {
                try {
                    URL imageURL = new URL(message.getImageUrl());
                    Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());
                    if (bitmap != null) {
                        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                        bigPictureStyle.bigPicture(bitmap);

                        if (!TextUtils.isEmpty(message.getBigContentTitle())) {
                            bigPictureStyle.setBigContentTitle(message.getBigContentTitle());
                        }

                        if (!TextUtils.isEmpty(message.getBigContentSummaryText())) {
                            bigPictureStyle.setSummaryText(message.getBigContentSummaryText());
                        }

                        builder.setStyle(bigPictureStyle);
                    }
                } catch (IOException e) {
                    String logMessage = e.getMessage() != null ? e.getMessage() : "";
                    SdkLog.e(LOG_TAG, "Could not load image. " + logMessage);
                }
            } else {
                // enable big text style
                boolean enableBigStyle = false;

                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

                if (!TextUtils.isEmpty(message.getBigContentTitle())) {
                    enableBigStyle = true;
                    bigTextStyle.setBigContentTitle(message.getBigContentTitle());
                }

                if (!TextUtils.isEmpty(message.getBigContentSummaryText())) {
                    enableBigStyle = true;
                    bigTextStyle.setSummaryText(message.getBigContentSummaryText());
                }

                if (!TextUtils.isEmpty(message.getContentText())) {
                    bigTextStyle.bigText(message.getContentText());
                }

                if (enableBigStyle) {
                    builder.setStyle(bigTextStyle);
                }
            }

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel =
                            NotificationUtils.createNotificationChannel(context, message);
                    if (channel != null) {
                        notificationManager.createNotificationChannel(channel);
                    }
                }

                notificationManager.notify(notificationId, builder.build());
            }

            // Tracking the notification display.
            Blueshift.getInstance(context).trackNotificationView(message);
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

    private static void scheduleNotifications(Context context, Message message) {
        if (context != null && message != null) {
            List<Message> messages = message.getNotifications();
            if (messages != null) {
                String pkgName = context.getPackageName();
                if (pkgName != null) {
                    String action = pkgName + ".ACTION_SCHEDULED_PUSH";

                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.getDefault());

                        for (Message item : messages) {
                            long now = System.currentTimeMillis();
                            long timeToDisplay = item.getTimestampToDisplay() * 1000;
                            long timeToExpire = item.getTimestampToExpireDisplay() * 1000;

                            /*
                             * updating the campaign params from the parent push message.
                             * as of now, we will have only one message under one scheduled push.
                             */

                            item.setBsftMessageUuid(message.getId());
                            item.setBsftUserUuid(message.getBsftUserUuid());
                            item.setBsftExperimentUuid(message.getBsftExperimentUuid());
                            item.setBsftTransactionUuid(message.getBsftTransactionUuid());
                            item.setBsftSeedListSend(message.getBsftSeedListSend());

                            if (timeToExpire > now || timeToExpire == 0) {
                                String messageJSON = new Gson().toJson(item);

                                Intent bcIntent = new Intent(action);
                                bcIntent.putExtra(Message.EXTRA_MESSAGE, messageJSON);

                                if (timeToDisplay > now) {
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                            context,
                                            NotificationFactory.getRandomPIRequestCode(),
                                            bcIntent,
                                            PendingIntent.FLAG_ONE_SHOT);

                                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeToDisplay, pendingIntent);
                                    Log.i(LOG_TAG, "Scheduled a notification. Display time: " + sdf.format(timeToDisplay));
                                } else {
                                    Log.i(LOG_TAG, "Display time (" + sdf.format(timeToDisplay) + ") elapsed! Showing the notification now.");
                                    context.sendBroadcast(bcIntent);
                                }
                            } else {
                                Log.i(LOG_TAG, "Expired notification found! Exp time: " + sdf.format(timeToExpire));
                            }
                        }
                    }
                }
            }
        }
    }

    // [BEGIN] PendingIntent builder methods.

    private static PendingIntent getBuyActionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_BUY(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getViewActionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_VIEW(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenCartPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_CART(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenAppPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_APP(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    private static PendingIntent getOpenPromotionPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_OFFER_PAGE(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    public static PendingIntent getNotificationClickPendingIntent(String action, Context context, Message message, int notificationId) {
        // if deep link url is available, despite the fact that we have a category based action,
        // we will use the open app action to launch app and pass the deep link url to it.
        if (TextUtils.isEmpty(action) || (message != null && message.isDeepLinkingEnabled())) {
            action = RichPushConstants.ACTION_OPEN_APP(context);
        }

        Intent bcIntent = new Intent(context, BlueshiftNotificationEventsActivity.class);
        bcIntent.setAction(action);

        if (message != null) {
            bcIntent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);
            bcIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);

            // add deep link URL if available.
            if (message.isDeepLinkingEnabled()) {
                bcIntent.putExtra(RichPushConstants.EXTRA_DEEP_LINK_URL, message.getDeepLinkUrl());
            }
        }

//        return PendingIntent.getBroadcast(context,
//                NotificationFactory.getRandomPIRequestCode(), bcIntent, PendingIntent.FLAG_ONE_SHOT);

        return PendingIntent.getActivity(
                context,
                NotificationFactory.getRandomPIRequestCode(),
                bcIntent,
                PendingIntent.FLAG_ONE_SHOT);
    }

    // [END] PendingIntent builder methods.
}
