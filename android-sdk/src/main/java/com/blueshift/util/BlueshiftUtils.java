package com.blueshift.util;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.blueshift.BlueShiftPreference;
import com.blueshift.Blueshift;
import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.Message;
import com.google.firebase.messaging.RemoteMessage;

public class BlueshiftUtils {
    private static final String LOG_TAG = "Blueshift";

    /**
     * This method checks if a non-empty API key is supplied to the
     * SDK while initialization and returns the same if available.
     *
     * @param context Context object to get the Blueshift instance.
     * @return API Key, if present. Else null.
     */
    public static String getApiKey(Context context) {
        Configuration config = getConfiguration(context);
        if (config != null) {
            String apiKey = config.getApiKey();
            if (!TextUtils.isEmpty(apiKey)) {
                return apiKey;
            } else {
                BlueshiftLogger.e(LOG_TAG, "No API key provided. Call Configuration.setApiKey() to set " +
                        "a valid API key before initializing SDK.");
            }
        }

        return null;
    }

    /**
     * Checks if a not-null {@link Configuration} object is supplied during
     * the SDK initialization and returns the same if available.
     *
     * @param context Context object to get the Blueshift instance.
     * @return Valid {@link Configuration} object, if found. Else null.
     */
    public static Configuration getConfiguration(Context context) {
        Configuration config = Blueshift.getInstance(context).getConfiguration();
        if (config != null) {
            return config;
        } else {
            BlueshiftLogger.e(LOG_TAG, "Blueshift Android SDK is not initialized! " +
                    "Please call the initialize() method of Blueshift class from your app\\'s " +
                    "Application file\\'s onCreate() method with a valid Configuration object. " +
                    "Ex: Blueshift.getInstance(Context).initialize(Configuration); " +
                    "See docs: https://help.blueshift.com/hc/en-us/articles/115002731534-Android-SDK#Initialize");
        }

        return null;
    }

    public static boolean isInAppEnabled(Context context) {
        boolean isEnabled = false;

        Configuration config = getConfiguration(context);
        if (config != null) {
            isEnabled = config.isInAppEnabled();
            if (!isEnabled) {
                BlueshiftLogger.e(LOG_TAG, "In-App is not enabled. Please call setInAppEnabled(true) " +
                        "to enable it during SDK initialization.");
            }
        }

        return isEnabled;
    }

    public static boolean isAutomaticAppOpenFiringEnabled(Context context) {
        boolean isEnabled = false;

        Configuration config = getConfiguration(context);
        if (config != null) {
            isEnabled = config.isAutoAppOpenFiringEnabled();
            if (!isEnabled) {
                BlueshiftLogger.w(LOG_TAG, "Automatic app_open firing is not enabled. You will have to fire this event explicitly.");
            }
        }

        return isEnabled;
    }

    public static boolean canAutomaticAppOpenBeSentNow(Context context) {
        Configuration config = BlueshiftUtils.getConfiguration(context);
        if (config != null && config.getAutoAppOpenInterval() > 0) {
            long trackedAt = BlueShiftPreference.getAppOpenTrackedAt(context);
            if (trackedAt > 0) {
                long now = System.currentTimeMillis() / 1000;
                long diff = now - trackedAt;
                return diff > 0 && diff > config.getAutoAppOpenInterval();
            } else {
                BlueshiftLogger.d(LOG_TAG, "app_open default behavior (trackedAt == 0)");
            }
        } else {
            BlueshiftLogger.d(LOG_TAG, "app_open default behavior (interval == 0)");
        }

        // the fall back value is set to true to keep the default behaviour
        return true;
    }

    public static boolean isPushEnabled(Context context) {
        Configuration config = getConfiguration(context);
        boolean isEnabled = config != null && config.isPushEnabled();
        if (!isEnabled) {
            BlueshiftLogger.w(LOG_TAG, "Push Notification is not enabled.");
        }

        return isEnabled;
    }

    /**
     * Checks the payload received and looks for bsft_message_uuid in it to confirm if
     * the push message belongs to Blueshift.
     *
     * @param intent from push message receiver
     * @return true if push belongs to Blueshift, else false
     */
    public static boolean isBlueshiftPushMessage(Intent intent) {
        return intent != null
                && intent.getExtras() != null
                && intent.getExtras().containsKey(Message.EXTRA_BSFT_MESSAGE_UUID);
    }

    /**
     * Checks the payload received and looks for bsft_message_uuid in it to confirm if
     * the push message belongs to Blueshift.
     *
     * @param remoteMessage from push message receiver
     * @return true if push belongs to Blueshift, else false
     */
    public static boolean isBlueshiftPushMessage(RemoteMessage remoteMessage) {
        return remoteMessage != null
                && remoteMessage.getData() != null
                && remoteMessage.getData().containsKey(Message.EXTRA_BSFT_MESSAGE_UUID);
    }
}
