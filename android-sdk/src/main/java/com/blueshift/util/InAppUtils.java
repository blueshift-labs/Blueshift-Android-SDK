package com.blueshift.util;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
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

    public static String getContentString(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                String stringValue = inAppMessage.getContentStyle().optString(contentName);
                return TextUtils.isEmpty(stringValue) ? null : stringValue;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static int getContentInt(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return inAppMessage.getContentStyle().optInt(contentName, fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static String getTemplateString(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getTemplateStyle() != null && contentName != null) {
                String stringValue = inAppMessage.getTemplateStyle().optString(contentName);
                return TextUtils.isEmpty(stringValue) ? null : stringValue;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static String getContentColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentString(inAppMessage, contentName + "_color");
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static String getContentBackgroundColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentString(inAppMessage, contentName + "_background_color");
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static int getContentSize(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentInt(inAppMessage, contentName + "_size", fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    private static int parseGravityString(String gravityString) {
        int gravity = Gravity.CENTER;

        if (gravityString != null) {
            switch (gravityString) {
                case "start":
                    gravity = Gravity.START;
                    break;

                case "end":
                    gravity = Gravity.END;
                    break;

                case "top":
                    gravity = Gravity.TOP;
                    break;

                case "bottom":
                    gravity = Gravity.BOTTOM;
                    break;

                case "center":
                    gravity = Gravity.CENTER;
                    break;
            }
        }

        return gravity;
    }

    public static int getContentGravity(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                String gravity = getContentString(inAppMessage, contentName + "_gravity");
                return parseGravityString(gravity);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return Gravity.START;
    }

    public static int getContentLayoutGravity(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                String gravity = getContentString(inAppMessage, contentName + "_layout_gravity");
                return parseGravityString(gravity);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return Gravity.START;
    }

    public static int getContentPadding(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentInt(inAppMessage, contentName + "_padding", fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static boolean isTemplateFullScreen(InAppMessage inAppMessage) {
        boolean isFullscreen = false;

        try {
            String position = getTemplateString(inAppMessage, "fullscreen");
            isFullscreen = Boolean.valueOf(position);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return isFullscreen;
    }

    public static int getTemplateGravity(InAppMessage inAppMessage) {
        String position = getTemplateString(inAppMessage, "position");
        return parseGravityString(position);
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
