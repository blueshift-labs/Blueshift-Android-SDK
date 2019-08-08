package com.blueshift.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppConstants;
import com.blueshift.inappmessage.InAppMessage;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class InAppUtils {

    private static final String LOG_TAG = "InAppUtils";

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
                return jsonObject.optString(key);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static int getIntFromJSONObject(JSONObject jsonObject, String key, int fallback) {
        try {
            if (jsonObject != null && !TextUtils.isEmpty(key)) {
                return jsonObject.optInt(key, fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static Rect getRectFromJSONObject(JSONObject jsonObject, String key) {
        try {
            if (jsonObject != null && !TextUtils.isEmpty(key)) {
                String json = jsonObject.optString(key);
                return new Gson().fromJson(json, Rect.class);
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
            if (inAppMessage != null) {
                String stringValue = getStringFromJSONObject(inAppMessage.getContentStyle(), contentName);
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
                return getIntFromJSONObject(inAppMessage.getContentStyle(), contentName, fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static Rect getContentRect(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getRectFromJSONObject(inAppMessage.getContentStyle(), contentName);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static String getTemplateString(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null) {
                String stringValue = getStringFromJSONObject(inAppMessage.getTemplateStyle(), contentName);
                return TextUtils.isEmpty(stringValue) ? null : stringValue;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static int getTemplateInt(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null) {
                return getIntFromJSONObject(inAppMessage.getTemplateStyle(), contentName, fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static int getContentOrientation(InAppMessage inAppMessage, String contentName) {
        try {
            int orientation = getContentInt(inAppMessage, InAppConstants.ORIENTATION(contentName), LinearLayout.HORIZONTAL);
            if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
                return orientation;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return LinearLayout.HORIZONTAL;
    }

    public static String getContentColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentString(inAppMessage, InAppConstants.COLOR(contentName));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static String getContentBackgroundColor(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentString(inAppMessage, InAppConstants.BACKGROUND_COLOR(contentName));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static int getContentBackgroundRadius(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentInt(inAppMessage, InAppConstants.BACKGROUND_RADIUS(contentName), fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return fallback;
    }

    public static GradientDrawable getContentBackgroundDrawable(InAppMessage inAppMessage, String contentName) {
        GradientDrawable shape = new GradientDrawable();
        try {
            String colorVal = getContentBackgroundColor(inAppMessage, contentName);
            if (validateColorString(colorVal)) {
                int color = Color.parseColor(colorVal);
                shape.setColor(color);
            }

            int radius = getContentBackgroundRadius(inAppMessage, contentName, 0);
            if (radius != 0) {
                shape.setCornerRadius(radius);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return shape;
    }

    public static int getContentSize(InAppMessage inAppMessage, String contentName, int fallback) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentInt(inAppMessage, InAppConstants.SIZE(contentName), fallback);
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
                String gravity = getContentString(inAppMessage, InAppConstants.GRAVITY(contentName));
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
                String gravity = getContentString(inAppMessage, InAppConstants.LAYOUT_GRAVITY(contentName));
                return parseGravityString(gravity);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return Gravity.START;
    }

    public static Rect getContentPadding(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentRect(inAppMessage, InAppConstants.PADDING(contentName));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static Rect getContentMargin(InAppMessage inAppMessage, String contentName) {
        try {
            if (inAppMessage != null && inAppMessage.getContentStyle() != null && contentName != null) {
                return getContentRect(inAppMessage, InAppConstants.MARGIN(contentName));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public static boolean isTemplateFullScreen(InAppMessage inAppMessage) {
        boolean isFullscreen = false;

        try {
            String position = getTemplateString(inAppMessage, InAppConstants.FULLSCREEN);
            isFullscreen = Boolean.valueOf(position);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return isFullscreen;
    }

    public static int getTemplateGravity(InAppMessage inAppMessage) {
        String position = getTemplateString(inAppMessage, InAppConstants.POSITION);
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

    public static void loadImageAsync(final ImageView imageView, String path) {
        if (imageView != null && path != null) {
            new LoadImageTask() {
                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }.execute(path);
        }
    }

    public static JSONObject getActionFromName(InAppMessage inAppMessage, String actionName) {
        JSONObject action = null;

        try {
            if (inAppMessage != null && inAppMessage.getActionsJSONObject() != null && !TextUtils.isEmpty(actionName)) {
                JSONObject actions = inAppMessage.getActionsJSONObject();
                if (actions != null) {
                    return actions.optJSONObject(actionName);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return action;
    }

    public static Rect getActionRect(InAppMessage inAppMessage, String actionName, String argName) {
        Rect value = null;

        try {
            JSONObject action = getActionFromName(inAppMessage, actionName);
            if (action != null) {
                String json = action.optString(argName);
                value = new Gson().fromJson(json, Rect.class);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return value;
    }

    public static String getActionString(InAppMessage inAppMessage, String actionName, String argName) {
        String value = null;

        try {
            JSONObject action = getActionFromName(inAppMessage, actionName);
            if (action != null) {
                value = action.optString(argName);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return value;
    }

    public static int getActionInt(InAppMessage inAppMessage, String actionName, String argName, int fallback) {
        int value = fallback;

        try {
            JSONObject action = getActionFromName(inAppMessage, actionName);
            if (action != null) {
                value = action.optInt(argName, fallback);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return value;
    }

    public static String getActionText(InAppMessage inAppMessage, String actionName) {
        return getActionString(inAppMessage, actionName, InAppConstants.TEXT);
    }

    public static Button getActionButtonDefault(Context context) {
        if (context != null) {
            Button button = new Button(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                TypedValue typedValue = new TypedValue();
                boolean isResolved = context
                        .getTheme()
                        .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);

                if (isResolved) {
                    button.setForeground(ContextCompat.getDrawable(context, typedValue.resourceId));
                }
            }

            button.setAllCaps(false);

            Drawable bg = getActionBackgroundDrawableDefault();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                button.setBackground(bg);
            } else {
                button.setBackgroundDrawable(bg);
            }

            button.setTextColor(Color.WHITE);

            return button;
        }

        return null;
    }

    public static Drawable getActionBackgroundDrawableDefault() {
        GradientDrawable shape = new GradientDrawable();

        try {
            // blue
            shape.setColor(Color.parseColor("#2196f3"));
            shape.setCornerRadius(3);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return shape;
    }

    public static Drawable getActionBackgroundDrawable(InAppMessage inAppMessage, String actionName) {
        GradientDrawable shape = new GradientDrawable();

        try {
            int bgColor = getActionBackgroundColor(inAppMessage, actionName);
            if (bgColor != 0) {
                shape.setColor(bgColor);
            }

            int bgRadius = getActionBackgroundRadius(inAppMessage, actionName);
            shape.setCornerRadius(bgRadius);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return shape;
    }

    public static int getActionBackgroundColor(InAppMessage inAppMessage, String actionName) {
        int color = 0;

        try {
            String colorStr = getActionString(inAppMessage, actionName, InAppConstants.BACKGROUND_COLOR);
            if (validateColorString(colorStr)) {
                return Color.parseColor(colorStr);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return color;
    }

    public static int getActionBackgroundRadius(InAppMessage inAppMessage, String actionName) {
        return getActionInt(inAppMessage, actionName, InAppConstants.BACKGROUND_RADIUS, 0);
    }

    public static Rect getActionMargin(InAppMessage inAppMessage, String actionName) {
        Rect margins = getActionRect(inAppMessage, actionName, InAppConstants.MARGIN);
        return margins != null ? margins : new Rect(0, 0, 0, 0);
    }

    public static Rect getActionPadding(InAppMessage inAppMessage, String actionName) {
        Rect padding = getActionRect(inAppMessage, actionName, InAppConstants.PADDING);
        return padding != null ? padding : new Rect(0, 0, 0, 0);
    }

    public static int getActionOrientation(InAppMessage inAppMessage) {
        return getContentOrientation(inAppMessage, InAppConstants.ACTIONS);
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap result = null;

            if (strings != null && strings.length > 0) {
                String url = strings[0];
                try {
                    if (!TextUtils.isEmpty(url)) {
                        InputStream inputStream = new URL(url).openStream();
                        result = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (IOException e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            }

            return result;
        }
    }
}