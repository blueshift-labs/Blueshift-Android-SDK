package com.blueshift;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

/**
 * This class is responsible for tracking the preferences of sdk.
 *
 * @author Rahul Raveendran V P
 *         Created on 17/11/16 @ 1:07 PM
 *         https://github.com/rahulrvp
 */

public class BlueShiftPreference {

    private static final String PREF_FILE_EMAIL = "BsftEmailPrefFile";
    private static final String PREF_FILE_TOKEN = "BsftTokenPrefFile";

    private static final String PREF_KEY_TOKEN = "BsftTokenPrefKey";

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

    public static String getCachedDeviceToken(Context context) {
        String result = "";

        if (context != null) {
            SharedPreferences preferences = getDeviceTokenPreference(context);
            if (preferences != null) {
                result = preferences.getString(PREF_KEY_TOKEN, "");
            }
        }

        return result;
    }

    @SuppressLint("CommitPrefEdits")
    public static void cacheDeviceToken(Context context, String deviceToken) {
        if (context != null) {
            SharedPreferences preferences = getDeviceTokenPreference(context);
            if (preferences != null) {
                if (TextUtils.isEmpty(deviceToken)) {
                    preferences
                            .edit()
                            .remove(PREF_KEY_TOKEN)
                            .commit();
                } else {
                    preferences
                            .edit()
                            .putString(PREF_KEY_TOKEN, deviceToken)
                            .commit();
                }
            }
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

    private static SharedPreferences getDeviceTokenPreference(Context context) {
        SharedPreferences preferences = null;

        if (context != null) {
            preferences = context
                    .getSharedPreferences(
                            getPreferenceFileName(context, PREF_FILE_TOKEN),
                            Context.MODE_PRIVATE);
        }

        return preferences;
    }

    private static String getPreferenceFileName(@NotNull Context context, @NotNull String fileName) {
        return context.getPackageName() + "." + fileName;
    }
}
