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
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftAttributesApp;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
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
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                handleDataMessage(this, data);
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
                    BlueshiftLogger.e(LOG_TAG, e);
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

                try {
                    int id = NotificationFactory.getRandomPIRequestCode();
                    notificationManager.notify(id, notificationBuilder.build());
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            }

            // TODO: 4/1/17 Decide on how to track this one.
        }
    }

    public static boolean isBlueshiftPush(Intent intent) {
        try {
            if (intent != null) {
                Bundle bundle = intent.getExtras();
                return (bundle != null && bundle.keySet() != null && bundle.keySet().contains("message"));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return false;
    }


    /**
     * This method takes care of parsing the data payload to see if the push belongs to Blueshift
     * and what action needs to be taken based on the payload content.
     *
     * @param context A valid {@link Context} object from the caller
     * @param data    A valid data payload in the form of a {@link Map}
     */
    private void handleDataMessage(Context context, Map<String, String> data) {
        if (data != null) {
            logPayload(data);

            if (isBlueshiftPushNotification(data)) {
                processPushNotification(context, data);
            } else if (isBlueshiftInAppMessage(data)) {
                processInAppMessage(context, data);
            } else if (isSilentPush(data)) {
                processSilentPush(context, data);
            } else {
                BlueshiftLogger.d(LOG_TAG, "Passing the push payload to host app via callback.");

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

    private void processPushNotification(Context context, Map<String, String> data) {
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
                        BlueshiftLogger.e(LOG_TAG, "Error parsing campaign data. " + e.getMessage());
                    }

                    try {
                        // SEED LIST FLAG CHECK
                        String seedListSendValue = data.get(Message.EXTRA_BSFT_SEED_LIST_SEND);
                        message.setBsftSeedListSend(isSeedListSend(seedListSendValue));
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, "Error parsing seed list flag. " + e.getMessage());
                    }

                    if (message.isSilentPush()) {
                        /*
                         * This is a silent push to track uninstalls.
                         * SDK has nothing to do with this. If this push was not delivered,
                         * server will track GCM registrations fails and will decide if the app is uninstalled or not.
                         */
                        BlueshiftLogger.i(LOG_TAG, "A silent push received.");
                    } else {
                        NotificationFactory.handleMessage(context, message);
                    }
                } else {
                    BlueshiftLogger.e(LOG_TAG, "Null message found in push message.");
                }
            }
        } catch (JsonSyntaxException e) {
            BlueshiftLogger.e(LOG_TAG, "Invalid JSON in push message: " + e.getMessage());
        }
    }

    private void processInAppMessage(Context context, Map<String, String> data) {
        try {
            InAppMessage inAppMessage = InAppMessage.getInstance(data);
            if (inAppMessage != null) {
                InAppManager.onInAppMessageReceived(context, inAppMessage);

                InAppMessageStore store = InAppMessageStore.getInstance(context);
                if (store != null) store.clean();

                InAppManager.invokeTriggerWithinSdk();
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private void processSilentPush(Context context, Map<String, String> data) {
        try {
            if (data != null) {
                String silentPushStr = data.get(BlueshiftConstants.SILENT_PUSH);
                if (silentPushStr != null) {
                    JSONObject silentPushJson = new JSONObject(silentPushStr);
                    String action = silentPushJson.optString(BlueshiftConstants.SILENT_PUSH_ACTION);
                    BlueshiftLogger.d(LOG_TAG, "Silent push with action '" + action + "' received.");
                    if (BlueshiftConstants.ACTION_IN_APP_BACKGROUND_FETCH.equals(action)) {
                        triggerInAppBackgroundFetch(context);
                    } else if (BlueshiftConstants.ACTION_IN_APP_MARK_AS_OPEN.equals(action)) {
                        triggerInAppMarkAsRead(context, silentPushJson);
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    protected void triggerInAppBackgroundFetch(Context context) {
        try {
            final Configuration config = BlueshiftUtils.getConfiguration(context);
            if (config != null && config.isInAppEnabled() && config.isInAppBackgroundFetchEnabled()) {
                InAppManager.fetchInAppFromServer(context, new InAppApiCallback() {
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

    protected void triggerInAppMarkAsRead(final Context context, JSONObject jsonObject) {
        if (context != null && jsonObject != null) {
            JSONArray uuidArray = jsonObject.optJSONArray(BlueshiftConstants.OPENED_IN_APP_MESSAGE_UUIDS);
            if (uuidArray != null && uuidArray.length() > 0) {
                final List<String> messageUUIDs = new ArrayList<>();
                for (int i = 0; i < uuidArray.length(); i++) {
                    String uuid = uuidArray.optString(i);
                    if (!TextUtils.isEmpty(uuid)) {
                        messageUUIDs.add(uuid);
                    }
                }

                BlueshiftLogger.d(LOG_TAG, "Message UUIDs present inside '" + BlueshiftConstants.OPENED_IN_APP_MESSAGE_UUIDS + "': " + messageUUIDs.toString());
                if (messageUUIDs.size() > 0) {
                    BlueshiftExecutor.getInstance().runOnDiskIOThread(new Runnable() {
                        @Override
                        public void run() {
                            InAppMessageStore store = InAppMessageStore.getInstance(context);
                            if (store != null) store.markAsRead(messageUUIDs);
                        }
                    });
                }
            } else {
                BlueshiftLogger.d(LOG_TAG, "No message UUIDs present inside '" + BlueshiftConstants.OPENED_IN_APP_MESSAGE_UUIDS + "'.");
            }
        }
    }

    private boolean isSeedListSend(String seedListSendValue) {
        if (!TextUtils.isEmpty(seedListSendValue)) {
            try {
                return Boolean.parseBoolean(seedListSendValue);
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }

        return false;
    }

    private void logPayload(Map<String, String> map) {
        if (map != null) {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> item : map.entrySet()) {
                if (item != null) {
                    try {
                        json.putOpt(item.getKey(), item.getValue());
                    } catch (JSONException ignored) {
                    }
                }
            }

            try {
                BlueshiftLogger.d(LOG_TAG, "\n" + json.toString(2));
            } catch (JSONException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }
    }

    protected void onMessageNotFound(Map<String, String> data) {

    }

    @Override
    public void onNewToken(String newToken) {
        BlueshiftLogger.d(LOG_TAG, "FCM token: " + newToken);

        Blueshift.updateDeviceToken(newToken);
        BlueshiftAttributesApp.getInstance().updateFirebaseToken(newToken);
        callIdentify();
    }

    /**
     * We are calling an identify here to make sure that the change in
     * device token is notified to the blueshift servers.
     */
    private void callIdentify() {
        String deviceId = DeviceUtils.getDeviceId(this);
        Blueshift
                .getInstance(this)
                .identifyUserByDeviceId(deviceId, null, false);
    }

    /**
     * This method is required by the Blueshift-mParticle-Kit to do the push rendering when a push
     * message is handed over to the kit by the mParticle SDK
     *
     * @param context {@link Context} object from the mParticle kit callback
     * @param intent  {@link Intent} object from the mParticle kit callback
     */
    public static void handlePushMessage(Context context, Intent intent) {
        try {
            if (context != null && intent != null) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Map<String, String> map = new HashMap<>();
                    for (String key : bundle.keySet()) {
                        Object val = bundle.get(key);
                        if (val != null) map.put(key, String.valueOf(val));
                    }

                    BlueshiftMessagingService service = new BlueshiftMessagingService();
                    service.handleDataMessage(context, map);
                }
            } else {
                BlueshiftLogger.e(LOG_TAG, "Could not handle push. Context is " + context + " and Intent is" + intent);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * Helper method for the host app to invoke the onMessageReceived method of the BlueshiftMessagingService class
     *
     * @param context       Valid {@link Context} object
     * @param remoteMessage Valid {@link RemoteMessage} object
     */
    public static void handleMessageReceived(Context context, RemoteMessage remoteMessage) {
        if (context != null && remoteMessage != null) {
            new BlueshiftMessagingService().handleDataMessage(context, remoteMessage.getData());
        }
    }

    /**
     * Helper method for the host app to invoke the onNewToken method of the BlueshiftMessagingService class
     *
     * @param newToken Valid new token provided by FCM
     */
    public static void handleNewToken(String newToken) {
        if (newToken != null) {
            new BlueshiftMessagingService().onNewToken(newToken);
        }
    }
}
