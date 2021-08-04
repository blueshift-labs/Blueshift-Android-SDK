package com.blueshift;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import android.text.TextUtils;

import java.util.UUID;

/**
 * This class is responsible for tracking the preferences of sdk.
 *
 * @author Rahul Raveendran V P
 *         Created on 17/11/16 @ 1:07 PM
 *         https://github.com/rahulrvp
 */

public class BlueShiftPreference {

    private static final String PREF_FILE = "com.blueshift.sdk_preferences";
    private static final String PREF_KEY_DEVICE_ID = "blueshift_device_id";
    private static final String PREF_KEY_PUSH_ENABLED = "blueshift_push_enabled";
    private static final String PREF_KEY_APP_OPEN_TRACKED_AT = "blueshift_app_open_tracked_at";
    private static final String PREF_FILE_EMAIL = "BsftEmailPrefFile";

    private static final String TAG = "BlueShiftPreference";

    static void resetDeviceID(Context context) {
        try {
            SharedPreferences preferences = getBlueshiftPreferences(context);
            if (preferences != null) {
                String deviceId = UUID.randomUUID().toString();
                preferences.edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply();
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public static String getDeviceID(Context context) {
        String deviceId = null;

        try {
            SharedPreferences preferences = getBlueshiftPreferences(context);
            if (preferences != null) {
                deviceId = preferences.getString(PREF_KEY_DEVICE_ID, null);

                if (deviceId == null) {
                    deviceId = UUID.randomUUID().toString();
                    preferences.edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply();
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return deviceId;
    }

    public static void saveCurrentPushPermissionStatus(Context context) {
        NotificationManagerCompat mgr = NotificationManagerCompat.from(context);
        boolean isPushEnabled = mgr.areNotificationsEnabled();

        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            preferences.edit().putBoolean(PREF_KEY_PUSH_ENABLED, isPushEnabled).apply();
        }
    }

    public static boolean didPushPermissionStatusChange(Context context) {
        NotificationManagerCompat mgr = NotificationManagerCompat.from(context);
        boolean isPushEnabled = mgr.areNotificationsEnabled();

        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null && preferences.contains(PREF_KEY_PUSH_ENABLED)) {
            boolean cachedVal = preferences.getBoolean(PREF_KEY_PUSH_ENABLED, isPushEnabled);
            return cachedVal != isPushEnabled;
        }

        BlueshiftLogger.d(TAG, "No existing value found for " + PREF_KEY_PUSH_ENABLED);
        // ensure an identify call on any error or missing value
        return true;
    }

    public static boolean isEmailAlreadyIdentified(Context context, String email) {
        boolean result = false;

        if (context != null && !TextUtils.isEmpty(email)) {
            SharedPreferences preferences = getEmailPreference(context);
            if (preferences != null) {
                result = preferences.getBoolean(email, false);
            }
        }

        return result;
    }

    public static void markEmailAsIdentified(Context context, String email) {
        if (context != null && !TextUtils.isEmpty(email)) {
            SharedPreferences preferences = getEmailPreference(context);
            if (preferences != null) {
                preferences
                        .edit()
                        .putBoolean(email, true)
                        .apply();
            }
        }
    }

    private static SharedPreferences getBlueshiftPreferences(Context context) {
        SharedPreferences preferences = null;

        if (context != null) {
            preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        }

        return preferences;
    }

    private static SharedPreferences getEmailPreference(Context context) {
        SharedPreferences preferences = null;

        if (context != null) {
            preferences = context
                    .getSharedPreferences(
                            getPreferenceFileName(context, PREF_FILE_EMAIL),
                            Context.MODE_PRIVATE);
        }

        return preferences;
    }

    private static String getPreferenceFileName(@NonNull Context context, @NonNull String fileName) {
        return context.getPackageName() + "." + fileName;
    }

    public static long getAppOpenTrackedAt(Context context) {
        long val = 0;

        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            val = preferences.getLong(PREF_KEY_APP_OPEN_TRACKED_AT, 0);
        }

        return val;
    }

    public static void setAppOpenTrackedAt(Context context, long seconds) {
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            preferences.edit().putLong(PREF_KEY_APP_OPEN_TRACKED_AT, seconds).apply();
        }
    }
}
