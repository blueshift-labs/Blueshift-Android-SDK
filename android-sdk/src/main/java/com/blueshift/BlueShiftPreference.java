package com.blueshift;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
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

    @SuppressLint("CommitPrefEdits")
    public static void markEmailAsIdentified(Context context, String email) {
        if (context != null && !TextUtils.isEmpty(email)) {
            SharedPreferences preferences = getEmailPreference(context);
            if (preferences != null) {
                preferences
                        .edit()
                        .putBoolean(email, true)
                        .commit();
            }
        }
    }

    private static SharedPreferences getBlueshiftPreferences(Context context) {
        SharedPreferences preferences = null;

        if (context != null) {
            preferences = context.getSharedPreferences(
                    getPreferenceFileName(context, PREF_FILE),
                    Context.MODE_PRIVATE
            );
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
}
