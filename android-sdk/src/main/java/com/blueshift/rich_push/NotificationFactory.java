package com.blueshift.rich_push;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftImageCache;
import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.NotificationUtils;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * @author Rahul Raveendran V P
 * Created on 18/2/15 @ 12:22 PM
 * <a href="https://github.com/rahulrvp">...</a>
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
                    BlueshiftLogger.e(LOG_TAG, "Unknown notification type");
            }
        }
    }

    private static void buildAndShowNotification(Context context, Message message) {
        if (context != null && message != null) {
            int notificationId = NotificationFactory.getRandomNotificationId();

            String channelId = NotificationUtils.getNotificationChannelId(context, message);

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

                PendingIntent openAppPendingIntent = getOpenAppPendingIntent(context, message, notificationId);
                builder.setContentIntent(openAppPendingIntent);
            }

            builder.setContentTitle(message.getContentTitle());
            builder.setContentText(message.getContentText());

            // display content sub text
            if (!TextUtils.isEmpty(message.getContentSubText())) {
                builder.setSubText(message.getContentSubText());
            }

            if (!TextUtils.isEmpty(message.getImageUrl())) {
                Bitmap bitmap = BlueshiftImageCache.getScaledBitmap(
                        context,
                        message.getImageUrl(),
                        RichPushConstants.BIG_IMAGE_WIDTH,
                        RichPushConstants.BIG_IMAGE_HEIGHT);

                if (bitmap != null) {
                    NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                    bigPictureStyle.bigPicture(bitmap);

                    // Set the large icon to match the big picture.
                    builder.setLargeIcon(bitmap);
                    // Hide the large icon when the notification is expanded.
                    bigPictureStyle.bigLargeIcon((Bitmap) null);

                    if (!TextUtils.isEmpty(message.getBigContentTitle())) {
                        bigPictureStyle.setBigContentTitle(message.getBigContentTitle());
                    }

                    if (!TextUtils.isEmpty(message.getBigContentSummaryText())) {
                        bigPictureStyle.setSummaryText(message.getBigContentSummaryText());
                    }

                    builder.setStyle(bigPictureStyle);
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

            List<NotificationCompat.Action> actions =
                    NotificationUtils.getActions(context, message, notificationId);

            if (actions != null) {
                for (NotificationCompat.Action action : actions) {
                    builder.addAction(action);
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

                try {
                    notificationManager.notify(notificationId, builder.build());

                    // Acknowledge that we received and displayed the push message.
                    NotificationUtils.invokePushDelivered(context, message);
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            }
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

                            item.setAdapterUUID(message.getAdapterUUID());
                            item.setBsftMessageUuid(message.getId());
                            item.setBsftUserUuid(message.getBsftUserUuid());
                            item.setBsftExperimentUuid(message.getBsftExperimentUuid());
                            item.setBsftExecutionKey(message.getBsftExecutionKey());
                            item.setBsftTransactionUuid(message.getBsftTransactionUuid());
                            item.setBsftSeedListSend(message.getBsftSeedListSend());

                            if (timeToExpire > now || timeToExpire == 0) {
                                String messageJSON = new Gson().toJson(item);

                                Intent bcIntent = new Intent(action);
                                bcIntent.putExtra(Message.EXTRA_MESSAGE, messageJSON);

                                // making the intent explicit to address the implicit pending intent vulnerability
                                // reference: https://support.google.com/faqs/answer/10437428
                                bcIntent.setPackage(pkgName);
                                bcIntent.setComponent(new ComponentName(context, ScheduledPushReceiver.class));

                                if (timeToDisplay > now) {
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                            context,
                                            NotificationFactory.getRandomPIRequestCode(),
                                            bcIntent,
                                            CommonUtils.appendImmutableFlag(PendingIntent.FLAG_ONE_SHOT));

                                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeToDisplay, pendingIntent);
                                    BlueshiftLogger.i(LOG_TAG, "Scheduled a notification. Display time: " + sdf.format(timeToDisplay));
                                } else {
                                    BlueshiftLogger.i(LOG_TAG, "Display time (" + sdf.format(timeToDisplay) + ") elapsed! Showing the notification now.");
                                    bcIntent.setPackage(context.getPackageName());
                                    context.sendBroadcast(bcIntent);
                                }
                            } else {
                                BlueshiftLogger.i(LOG_TAG, "Expired notification found! Exp time: " + sdf.format(timeToExpire));
                            }
                        }
                    }
                }
            }
        }
    }

    private static PendingIntent getOpenAppPendingIntent(Context context, Message message, int notificationId) {
        String action = RichPushConstants.ACTION_OPEN_APP(context);
        return getNotificationClickPendingIntent(action, context, message, notificationId);
    }

    static PendingIntent getNotificationClickPendingIntent(String action, Context context, Message message, int notificationId) {
        // set extra params
        Bundle bundle = new Bundle();
        bundle.putInt(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);

        if (message != null) {
            bundle.putString(RichPushConstants.EXTRA_MESSAGE, message.toJson());

            if (message.isDeepLinkingEnabled()) {
                bundle.putString(RichPushConstants.EXTRA_DEEP_LINK_URL, message.getDeepLinkUrl());
            }
        }

        // get the activity to handle clicks (user defined or sdk defined
        Intent intent = NotificationUtils.getNotificationEventsActivity(context, bundle);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntent(intent);

        int reqCode = NotificationFactory.getRandomPIRequestCode();
        return taskStackBuilder.getPendingIntent(
                reqCode, CommonUtils.appendImmutableFlag(PendingIntent.FLAG_ONE_SHOT));
    }

    public static PendingIntent getNotificationActionPendingIntent(Context context, Message message, Action pushAction, int notificationId) {
        // set extra params
        Bundle bundle = new Bundle();
        bundle.putInt(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);

        if (message != null) {
            bundle.putString(RichPushConstants.EXTRA_MESSAGE, message.toJson());
        }

        if (pushAction != null) {
            bundle.putString(RichPushConstants.EXTRA_DEEP_LINK_URL, pushAction.getDeepLinkUrl());
            bundle.putString(BlueshiftConstants.KEY_CLICK_ELEMENT, pushAction.getTitle());
        }

        // get the activity to handle clicks (user defined or sdk defined
        Intent intent = NotificationUtils.getNotificationEventsActivity(context, bundle);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntent(intent);

        int reqCode = NotificationFactory.getRandomPIRequestCode();
        return taskStackBuilder.getPendingIntent(
                reqCode, CommonUtils.appendImmutableFlag(PendingIntent.FLAG_ONE_SHOT));
    }
}
