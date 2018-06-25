package com.blueshift.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;

public class BlueshiftUtils {
    private static final String LOG_TAG = "Blueshift";

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

    public static Configuration getConfiguration(Context context) {
        Configuration config = Blueshift.getInstance(context).getConfiguration();
        if (config != null) {
            return config;
        } else {
            Log.e(LOG_TAG,
                    "No configuration provided. " +
                            "Please call Blueshift.getInstance().initialize() with valid " +
                            "Configuration object.");
        }

        return null;
    }
}
