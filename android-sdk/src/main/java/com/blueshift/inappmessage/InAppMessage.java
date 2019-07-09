package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteModel;
import com.blueshift.util.CommonUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

public class InAppMessage extends BlueshiftBaseSQLiteModel {
    public static final String TAG = InAppMessage.class.getSimpleName();
    public static final String EXTRA_IN_APP = "inapp";

    private static final String KEY_TYPE = "type";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_TRIGGER = "trigger";
    private static final String KEY_TEMPLATE_STYLE = "template_style";
    private static final String KEY_CONTENT_STYLE = "content_style";
    private static final String KEY_CONTENT = "content";

    private static final String KEY_MARGIN = "margin";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_CLOSE_BTN = "close_button";
    private static final String KEY_BACKGROUND_COLOR = "background_color";

    private long id;
    private String type;
    private long expires_at;
    private String trigger; // timestamp/event-name/now
    private JSONObject template_style;
    private JSONObject content_style;
    private JSONObject content;

    public static InAppMessage getInstance(JSONObject payload) {
        try {
            InAppMessage inAppMessage = new InAppMessage();
            inAppMessage.type = payload.getString(KEY_TYPE);
            inAppMessage.expires_at = payload.getLong(KEY_EXPIRES_AT);
            inAppMessage.trigger = payload.getString(KEY_TRIGGER);
            inAppMessage.template_style = payload.getJSONObject(KEY_TEMPLATE_STYLE);
            inAppMessage.content_style = payload.getJSONObject(KEY_CONTENT_STYLE);
            inAppMessage.content = payload.getJSONObject(KEY_CONTENT);

            return inAppMessage;
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    protected void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpiresAt() {
        return expires_at;
    }

    public void setExpiresAt(long expiresAt) {
        this.expires_at = expiresAt;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getTemplateStyleJson() {
        return template_style != null ? template_style.toString() : null;
    }

    public void setTemplateStyle(JSONObject templateStyle) {
        this.template_style = templateStyle;
    }

    public String getContentStyleJson() {
        return content_style != null ? content_style.toString() : null;
    }

    public void setContentStyle(JSONObject contentStyle) {
        this.content_style = contentStyle;
    }

    public String getContentJson() {
        return content != null ? content.toString() : null;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }

    public String getContentString(String contentName) {
        try {
            return content.getString(contentName);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    public Rect getTemplateMargin() {
        try {
            String json = template_style.getString(KEY_MARGIN);
            return new Gson().fromJson(json, Rect.class);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return new Rect();
    }

    public int getTemplateHeight(Context context) {
        try {
            int height = template_style.getInt(KEY_HEIGHT);
            if (height >= 0) return CommonUtils.dpToPx(height, context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public int getTemplateWidth(Context context) {
        try {
            int width = template_style.getInt(KEY_WIDTH);
            if (width >= 0) return CommonUtils.dpToPx(width, context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
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
            BlueshiftLogger.e(TAG, e);
        }

        return 0;
    }

    public boolean showCloseButton() {
        try {
            return template_style.has(KEY_CLOSE_BTN)
                    && !TextUtils.isEmpty(template_style.getString(KEY_CLOSE_BTN));
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return true;
    }

    public InAppTemplate getTemplate() {
        return InAppTemplate.fromString(type);
    }
}
