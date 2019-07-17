package com.blueshift.util;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppMessage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InAppUtils {

    private static final String LOG_TAG = "AssetsUtils";

    public static String readSamplePayload(Context context, String fileName) {
        String result = null;

        if (context != null && !TextUtils.isEmpty(fileName)) {
            BufferedReader reader = null;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                InputStreamReader inputStreamReader = new InputStreamReader(context.getAssets().open(fileName));
                reader = new BufferedReader(inputStreamReader);

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                result = stringBuilder.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
            }
        }

        return result;

    }

    public static String getStringFromJSONObject(JSONObject jsonObject, String key) {
        try {
            if (jsonObject != null && !TextUtils.isEmpty(key)) {
                return jsonObject.getString(key);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static boolean validateColorString(String colorString) {
        try {
            int len = colorString != null ? colorString.length() : 0;
            return len > 0 && colorString.startsWith("#") && (len == 4 || len == 7 || len == 9);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return false;
    }

    public static int getContentColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                String colorVal = inAppMessage.getContentStyle().optString(contentName + "_color");
                if (validateColorString(colorVal)) {
                    return Color.parseColor(colorVal);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return Color.parseColor("#000000");
    }

    public static int getContentBackgroundColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                String colorVal = inAppMessage.getContentStyle().optString(contentName + "_background_color");
                if (validateColorString(colorVal)) {
                    return Color.parseColor(colorVal);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return Color.parseColor("#00000000");
    }

    public static int getContentSize(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return inAppMessage.getContentStyle().optInt(contentName + "_size", 14);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return 14;
    }

    public static void applyTextColor(TextView textView, String colorStr) {
        if (textView != null) {
            if (validateColorString(colorStr)) {
                int color = Color.parseColor(colorStr);
                textView.setTextColor(color);
            }
        }
    }

    public static void applyBackgroundColor(View view, String colorStr) {
        if (view != null) {
            if (validateColorString(colorStr)) {
                int color = Color.parseColor(colorStr);
                view.setBackgroundColor(color);
            }
        }
    }
}
