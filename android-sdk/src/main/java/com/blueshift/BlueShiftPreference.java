package com.blueshift;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import java.util.UUID;

/**
 * This class is responsible for tracking the preferences of sdk.
 *
 * @author Rahul Raveendran V P
 * Created on 17/11/16 @ 1:07 PM
 * https://github.com/rahulrvp
 */

public class BlueShiftPreference {

    private static final String PREF_FILE = "com.blueshift.sdk_preferences";
    private static final String PREF_KEY_APP_VERSION = "blueshift_app_version";
    private static final String PREF_KEY_DEVICE_ID = "blueshift_device_id";
    private static final String PREF_KEY_DEVICE_TOKEN = "blueshift_device_token";
    private static final String PREF_KEY_PUSH_ENABLED = "blueshift_push_enabled";
    private static final String PREF_KEY_LEGACY_SYNC_COMPLETE = "blueshift_legacy_sync_complete";
    private static final String PREF_KEY_APP_OPEN_TRACKED_AT = "blueshift_app_open_tracked_at";
    private static final String PREF_FILE_EMAIL = "BsftEmailPrefFile";

    private static final String TAG = "BlueShiftPreference";

    static String getSavedDeviceToken(Context context) {
        String token = null;

        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            token = preferences.getString(PREF_KEY_DEVICE_TOKEN, token);
        }

        return token;
    }

    static void saveDeviceToken(Context context, String token) {
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            preferences.edit().putString(PREF_KEY_DEVICE_TOKEN, token).apply();
        }
    }

    static void markLegacyEventSyncAsComplete(Context context) {
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            preferences.edit().putBoolean(PREF_KEY_LEGACY_SYNC_COMPLETE, true).apply();
        }
    }

    static boolean isLegacyEventSyncComplete(Context context) {
        boolean isComplete = false;
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            isComplete = preferences.getBoolean(PREF_KEY_LEGACY_SYNC_COMPLETE, isComplete);
        }
        return isComplete;
    }

    static void saveAppVersionString(Context context, String appVersionString) {
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            preferences.edit().putString(PREF_KEY_APP_VERSION, appVersionString).apply();
        }
    }

    static String getStoredAppVersionString(Context context) {
        SharedPreferences preferences = getBlueshiftPreferences(context);
        if (preferences != null) {
            return preferences.getString(PREF_KEY_APP_VERSION, null);
        }

        return null;
    }

    static void incrementPermissionDenialCount(Context context, String permission) {
        try {
            SharedPreferences preferences = getBlueshiftPreferences(context);
            if (preferences != null) {
                int count = preferences.getInt(permission, 0);
                preferences.edit().putInt(permission, count + 1).apply();
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    static int getPermissionDenialCount(Context context, String permission) {
        try {
            SharedPreferences preferences = getBlueshiftPreferences(context);
            if (preferences != null) {
                return preferences.getInt(permission, 0);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return 0;
    }

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

    private static SharedPreferences getBlueshiftPreferences(Context context) {
        SharedPreferences preferences = null;

        if (context != null) {
            preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        }

        return preferences;
    }

    public static void removeCachedEmailAddress(Context context) {
        SharedPreferences preferences = getEmailPreference(context);
        if (preferences != null) {
            preferences.edit().clear().apply();
        }
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
