package com.blueshift.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;

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
                Log.e(LOG_TAG, "No API key provided. Call Configuration.setApiKey() to set " +
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
            Log.e(LOG_TAG, "Blueshift Android SDK is not initialized! " +
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
                Log.e(LOG_TAG, "In-App is not enabled. Please call setInAppEnabled(true) " +
                        "to enable it during SDK initialization.");
            }
        }

        return isEnabled;
    }
}
