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
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.BuildConfig;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.inappmessage.InAppMessage;
import com.blueshift.inappmessage.InAppMessageStore;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.NotificationFactory;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.NotificationUtils;
import com.blueshift.util.SdkLog;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * @author Rahul Raveendran V P
 * Created on 19/12/16 @ 11:59 AM
 * https://github.com/rahulrvp
 */

public class BlueshiftMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = "MessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // check if push is enabled. if not, skip the received push.
        Configuration config = BlueshiftUtils.getConfiguration(this);
        if (config == null || !config.isPushEnabled()) return;

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
                    titleText = packageManager.getApplicationLabel(info).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, String.valueOf(e.getMessage()));
                }
            }

            if (TextUtils.isEmpty(titleText)) {
                titleText = "Notification";
            }

            /*
             * Create simple notification.
             */
            Configuration configuration = BlueshiftUtils.getConfiguration(this);
            if (configuration == null) return; // No app icon available. Can't show notification.

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String channelId = NotificationUtils.getNotificationChannelId(this, null);

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
                            NotificationUtils.createNotificationChannel(this, null);
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
            if (BuildConfig.DEBUG) {
                logPayload(data);
            }

            if (isBlueshiftPushNotification(data)) {
                processPushNotification(data);
            } else if (isBlueshiftInAppMessage(data)) {
                processInAppMessage(data);
            } else if (isSilentPush(data)) {
                processSilentPush(data);
            } else {
                SdkLog.d(LOG_TAG, "Passing the push payload to host app via callback.");

                /*
                 * Handing over the push message to host app if message is not found.
                 */
                onMessageNotFound(data);
            }
        }
    }

    private boolean isBlueshiftPushNotification(Map<String, String> data) {
        return data != null && data.containsKey(Message.EXTRA_MESSAGE);
    }

    private boolean isBlueshiftInAppMessage(Map<String, String> data) {
        return data != null && data.containsKey(InAppMessage.EXTRA_IN_APP);
    }

    public boolean isSilentPush(Map<String, String> data) {
        return data != null && data.containsKey(BlueshiftConstants.SILENT_PUSH);
    }

    private void processPushNotification(Map<String, String> data) {
        try {
            String msgJson = data.get(Message.EXTRA_MESSAGE);
            if (!TextUtils.isEmpty(msgJson)) {
                Message message = new Gson().fromJson(msgJson, Message.class);
                if (message != null) {
                    try {
                        // CAMPAIGN METADATA CHECK
                        message.setBsftMessageUuid(data.get(Message.EXTRA_BSFT_MESSAGE_UUID));
                        message.setBsftExperimentUuid(data.get(Message.EXTRA_BSFT_EXPERIMENT_UUID));
                        message.setBsftUserUuid(data.get(Message.EXTRA_BSFT_USER_UUID));
                        message.setBsftTransactionUuid(data.get(Message.EXTRA_BSFT_TRANSACTIONAL_UUID));
                    } catch (Exception e) {
                        SdkLog.e(LOG_TAG, "Error parsing campaign data. " + e.getMessage());
                    }

                    try {
                        // SEED LIST FLAG CHECK
                        String seedListSendValue = data.get(Message.EXTRA_BSFT_SEED_LIST_SEND);
                        message.setBsftSeedListSend(isSeedListSend(seedListSendValue));
                    } catch (Exception e) {
                        SdkLog.e(LOG_TAG, "Error parsing seed list flag. " + e.getMessage());
                    }

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
            }
        } catch (JsonSyntaxException e) {
            Log.e(LOG_TAG, "Invalid JSON in push message: " + e.getMessage());
        }
    }

    private void processInAppMessage(Map<String, String> data) {
        try {
            InAppMessage inAppMessage = InAppMessage.getInstance(data);
            if (inAppMessage != null) {
                InAppManager.onInAppMessageReceived(this, inAppMessage);
                InAppMessageStore.getInstance(this).clean();
                InAppManager.invokeTriggerWithinSdk();
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private void processSilentPush(Map<String, String> data) {
        try {
            if (data != null) {
                String silentPushStr = data.get(BlueshiftConstants.SILENT_PUSH);
                if (silentPushStr != null) {
                    JSONObject silentPushJson = new JSONObject(silentPushStr);
                    String action = silentPushJson.optString(BlueshiftConstants.SILENT_PUSH_ACTION);
                    BlueshiftLogger.d(LOG_TAG, "Silent push with action '" + action + "' received.");
                    if (BlueshiftConstants.ACTION_IN_APP_BACKGROUND_FETCH.equals(action)) {
                        triggerInAppBackgroundFetch();
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    protected void triggerInAppBackgroundFetch() {
        try {
            final Configuration config = BlueshiftUtils.getConfiguration(this);
            if (config != null && config.isInAppBackgroundFetchEnabled()) {
                InAppManager.fetchInAppFromServer(this, new InAppApiCallback() {
                    @Override
                    public void onSuccess() {
                        InAppManager.invokeTriggerWithinSdk();
                    }

                    @Override
                    public void onFailure(int code, String message) {
                        BlueshiftLogger.e(LOG_TAG, "InApp API, error code: " + code + ", message:" + message);
                    }
                });
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private boolean isSeedListSend(String seedListSendValue) {
        if (!TextUtils.isEmpty(seedListSendValue)) {
            try {
                return Boolean.parseBoolean(seedListSendValue);
            } catch (Exception e) {
                SdkLog.e(LOG_TAG, String.valueOf(e.getMessage()));
            }
        }

        return false;
    }

    private void logPayload(Map<String, String> map) {
        if (map != null) {
            try {
                Set<String> keySet = map.keySet();
                JSONObject jsonObject = new JSONObject();
                for (String key : keySet) {
                    jsonObject.put(key, map.get(key));
                }

                BlueshiftLogger.d(LOG_TAG, "Push payload received:" +
                        "\n" + jsonObject.toString(2));
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }
    }

    protected void onMessageNotFound(Map<String, String> data) {

    }

    @Override
    public void onNewToken(String newToken) {
        Log.d("Blueshift", "FCM token: " + newToken);

        Blueshift.updateDeviceToken(newToken);
        callIdentify();
    }

    /**
     * We are calling an identify here to make sure that the change in
     * device token is notified to the blueshift servers.
     */
    private void callIdentify() {
        String adId = DeviceUtils.getAdvertisingID(this);
        Blueshift
                .getInstance(this)
                .identifyUserByDeviceId(adId, null, false);
    }
}
