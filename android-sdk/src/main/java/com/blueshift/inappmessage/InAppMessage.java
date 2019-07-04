package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.ViewGroup;

import com.blueshift.util.CommonUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

public class InAppMessage {
    public static final int INVALID_INT = -999;

    private static final String KEY_TYPE = "type";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_TRIGGER = "trigger";
    private static final String KEY_TEMPLATE_STYLE = "template_style";
    private static final String KEY_CONTENT_STYLE = "content_style";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_MARGIN = "margin";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private InAppTemplate type;
    private long expires_at;
    private JSONObject trigger;
    private JSONObject template_style;
    private JSONObject content_style;
    private JSONObject content;

    public static InAppMessage getInstance(JSONObject payload) {
        try {
            InAppMessage inAppMessage = new InAppMessage();

            String templateStr = payload.getString(KEY_TYPE);
            inAppMessage.type = InAppTemplate.fromString(templateStr);

            inAppMessage.expires_at = payload.getLong(KEY_EXPIRES_AT);

            inAppMessage.trigger = payload.getJSONObject(KEY_TRIGGER);
            inAppMessage.template_style = payload.getJSONObject(KEY_TEMPLATE_STYLE);
            inAppMessage.content_style = payload.getJSONObject(KEY_CONTENT_STYLE);
            inAppMessage.content = payload.getJSONObject(KEY_CONTENT);

            return inAppMessage;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public InAppTemplate getTemplate() {
        return type;
    }

    public String getContentString(String contentName) {
        try {
            return content.getString(contentName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getContentStyleString(String contentName) {
        return null;
    }

    public int getContentStyleInt(String contentName) {
        return -999;
    }

    public Rect getTemplateMargin() {
        try {
            String json = template_style.getString(KEY_MARGIN);
            return new Gson().fromJson(json, Rect.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Rect();
    }

    public int getTemplateHeight(Context context) {
        try {
            int height = template_style.getInt(KEY_HEIGHT);
            if (height >= 0) return CommonUtils.dpToPx(height, context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public int getTemplateWidth(Context context) {
        try {
            int width = template_style.getInt(KEY_WIDTH);
            if (width >= 0) return CommonUtils.dpToPx(width, context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public int getTemplateBackgroundColor() {
        try {
            String color = template_style.getString(KEY_BACKGROUND_COLOR);
            if (color != null && color.startsWith("#")) {
                int len = color.length();
                if (len == 4 || len == 7 || len == 9) {
                    return Color.parseColor(color);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
