package com.blueshift;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class BlueshiftAppPreferences extends BlueshiftJSONObject {
    private static final String TAG = "BlueshiftAppPreferences";

    private static final String PREF_FILE = "bsft_app_preferences";
    private static final String PREF_KEY = "bsft_app_preferences_json";

    private static final String KEY_ENABLE_PUSH = "bsft_enable_push";
    private static final String KEY_ENABLE_TRACKING = "bsft_enable_tracking";

    private static final BlueshiftAppPreferences instance = new BlueshiftAppPreferences();

    public static BlueshiftAppPreferences getInstance(Context context) {
        synchronized (instance) {
            if (instance.keys() == null || !instance.keys().hasNext()) {
                // Fresh instance, load value from preferences
                JSONObject cachedInstance = getCachedInstance(context);
                if (cachedInstance != null) {
                    instance.putAll(cachedInstance);
                }
            }

            return instance;
        }
    }

    private static JSONObject getCachedInstance(Context context) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            if (preferences != null) {
                String json = preferences.getString(PREF_KEY, null);
                if (json != null) {
                    try {
                        return new JSONObject(json);
                    } catch (JSONException e) {
                        BlueshiftLogger.e(TAG, e);
                    }
                }
            }
        }

        return null;
    }

    public void save(Context context) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            if (preferences != null) {
                preferences.edit().putString(PREF_KEY, instance.toString()).apply();
            }
        }
    }

    public void setEnablePush(boolean enablePush) {
        synchronized (instance) {
            try {
                instance.put(KEY_ENABLE_PUSH, enablePush);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    public boolean getEnablePush() {
        synchronized (instance) {
            try {
                if (instance.has(KEY_ENABLE_PUSH)) {
                    return instance.getBoolean(KEY_ENABLE_PUSH);
                }
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }

            // default true
            return true;
        }
    }

    void setEnableTracking(Context context, boolean enable) {
        synchronized (instance) {
            try {
                instance.put(KEY_ENABLE_TRACKING, enable);
                save(context);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    boolean getEnableTracking() {
        synchronized (instance) {
            try {
                if (instance.has(KEY_ENABLE_TRACKING)) {
                    return instance.getBoolean(KEY_ENABLE_TRACKING);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }

            // SDK sends events by default.
            return true;
        }
    }
}
