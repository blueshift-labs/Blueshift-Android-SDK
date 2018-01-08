package com.blueshift.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.NotificationFactory;
import com.blueshift.rich_push.Message;
import com.blueshift.util.NotificationUtils;
import com.blueshift.util.SdkLog;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.Set;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/12/16 @ 11:59 AM
 *         https://github.com/rahulrvp
 */

public class BlueshiftMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = "MessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        /*
         * If notification payload is present, consider that.
         */
        if (notification != null) {
            Map<String, String> data = remoteMessage.getData();
            handleNotificationMessage(notification, data);
        } else {
            /*
             * If data payload is present, follow the regular notification flow.
             */
            Map<String, String> data = remoteMessage.getData();
            if (data != null) {
                handleDataMessage(data);
            } else {
                /*
                 * Neither data nor notification payload is present
                 */
                super.onMessageReceived(remoteMessage);
            }
        }
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification, Map<String, String> data) {
        if (notification != null) {
            PackageManager packageManager = getPackageManager();

            Intent launcherIntent = packageManager.getLaunchIntentForPackage(getPackageName());
            if (launcherIntent != null) {
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                /*
                 * Add the data payload to the intent's extra
                 */
                if (data != null) {
                    Set<String> keySet = data.keySet();
                    for (String key : keySet) {
                        launcherIntent.putExtra(key, data.get(key));
                    }
                }
            }

            /*
             * Create pending intent for notification click action
             */
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    NotificationFactory.getRandomPIRequestCode(),
                    launcherIntent,
                    PendingIntent.FLAG_ONE_SHOT);

            /*
             * Create the notification title. User defined / app name / "Notification"
             */
            String titleText = notification.getTitle();

            if (TextUtils.isEmpty(titleText)) {
                try {
                    ApplicationInfo info = packageManager.getApplicationInfo(getPackageName(), 0);
                    if (info != null) {
                        titleText = packageManager.getApplicationLabel(info).toString();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }

            if (TextUtils.isEmpty(titleText)) {
                titleText = "Notification";
            }

            /*
             * Create simple notification.
             */
            Configuration configuration = Blueshift.getInstance(this).getConfiguration();
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String channelName = NotificationUtils.getNotificationChannelName(this, null);
            String channelId = NotificationUtils.getNotificationChannelId(channelName);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(configuration.getSmallIconResId())
                    .setContentTitle(titleText)
                    .setContentText(notification.getBody())
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel =
                            NotificationUtils.createNotificationChannel(this,null);
                    if (channel != null) {
                        notificationManager.createNotificationChannel(channel);
                    }
                }

                notificationManager.notify(
                        NotificationFactory.getRandomPIRequestCode(), notificationBuilder.build());
            }

            // TODO: 4/1/17 Decide on how to track this one.
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        if (data != null) {
            String msgJson = data.get(Message.EXTRA_MESSAGE);
            if (msgJson != null) {
                try {
                    Message message = new Gson().fromJson(msgJson, Message.class);
                    if (message != null) {
                        // trying to fetch campaign params
                        String experimentUUID = data.get(Message.EXTRA_BSFT_EXPERIMENT_UUID);
                        String userUUID = data.get(Message.EXTRA_BSFT_USER_UUID);

                        // adding campaign parameters inside message.
                        message.setBsftExperimentUuid(experimentUUID);
                        message.setBsftUserUuid(userUUID);

                        if (message.isSilentPush()) {
                            /*
                             * This is a silent push to track uninstalls.
                             * SDK has nothing to do with this. If this push was not delivered,
                             * server will track GCM registrations fails and will decide if the app is uninstalled or not.
                             */
                            SdkLog.i(LOG_TAG, "A silent push received.");
                        } else {
                            NotificationFactory.handleMessage(this, message);
                        }
                    } else {
                        Log.e(LOG_TAG, "Null message found in push message.");
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(LOG_TAG, "Invalid JSON in push message: " + e.getMessage());
                }
            } else {
                SdkLog.d(LOG_TAG, "Message not found. Passing the push message to host app via broadcast.");

                /*
                 * Handing over the push message to host app if message is not found.
                 */
                onMessageNotFound(data);
            }
        }
    }

    protected void onMessageNotFound(Map<String, String> data) {

    }
}
