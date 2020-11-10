package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteModel;
import com.blueshift.rich_push.Message;
import com.blueshift.util.InAppUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class InAppMessage extends BlueshiftBaseSQLiteModel {
    public static final String TAG = InAppMessage.class.getSimpleName();
    public static final String EXTRA_IN_APP = "inapp";

    private long id;
    private String type;
    private long expires_at;
    private String trigger; // timestamp/event-name/now
    private String display_on; // activity on which it should be displayed
    private JSONObject template_style;
    private JSONObject template_style_dark;
    private JSONObject content_style;
    private JSONObject content_style_dark;
    private JSONObject content;
    private JSONObject extras;

    private long displayed_at;

    // campaign params
    private String message_uuid;
    private String experiment_uuid;
    private String user_uuid;
    private String transaction_uuid;
    private String timestamp;

    public static InAppMessage getInstance(JSONObject jsonObject) {
        try {
            String json = jsonObject.optString(InAppMessage.EXTRA_IN_APP);
            JSONObject inAppPayload = new JSONObject(json);

            InAppMessage inAppMessage = new InAppMessage();
            inAppMessage.type = inAppPayload.optString(InAppConstants.TYPE);
            inAppMessage.expires_at = inAppPayload.optLong(InAppConstants.EXPIRES_AT);
            inAppMessage.trigger = inAppPayload.optString(InAppConstants.TRIGGER);
            inAppMessage.display_on = inAppPayload.optString(InAppConstants.DISPLAY_ON);
            inAppMessage.template_style = inAppPayload.optJSONObject(InAppConstants.TEMPLATE_STYLE);
            inAppMessage.template_style_dark = inAppPayload.optJSONObject(InAppConstants.TEMPLATE_STYLE_DARK);
            inAppMessage.content_style = inAppPayload.optJSONObject(InAppConstants.CONTENT_STYLE);
            inAppMessage.content_style_dark = inAppPayload.optJSONObject(InAppConstants.CONTENT_STYLE_DARK);
            inAppMessage.content = inAppPayload.optJSONObject(InAppConstants.CONTENT);
            inAppMessage.extras = inAppPayload.optJSONObject(InAppConstants.EXTRAS);

            inAppMessage.message_uuid = jsonObject.optString(Message.EXTRA_BSFT_MESSAGE_UUID);
            inAppMessage.experiment_uuid = jsonObject.optString(Message.EXTRA_BSFT_EXPERIMENT_UUID);
            inAppMessage.user_uuid = jsonObject.optString(Message.EXTRA_BSFT_USER_UUID);
            inAppMessage.transaction_uuid = jsonObject.optString(Message.EXTRA_BSFT_TRANSACTIONAL_UUID);
            inAppMessage.timestamp = jsonObject.optString(BlueshiftConstants.KEY_TIMESTAMP);

            return inAppMessage;
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    public static InAppMessage getInstance(Map<String, String> pushPayload) {
        try {
            String json = pushPayload.get(InAppMessage.EXTRA_IN_APP);
            if (json != null) {
                JSONObject inAppPayload = new JSONObject(json);

                InAppMessage inAppMessage = new InAppMessage();
                inAppMessage.type = inAppPayload.optString(InAppConstants.TYPE);
                inAppMessage.expires_at = inAppPayload.optLong(InAppConstants.EXPIRES_AT);
                inAppMessage.trigger = inAppPayload.optString(InAppConstants.TRIGGER);
                inAppMessage.display_on = inAppPayload.optString(InAppConstants.DISPLAY_ON);
                inAppMessage.template_style = inAppPayload.optJSONObject(InAppConstants.TEMPLATE_STYLE);
                inAppMessage.template_style_dark = inAppPayload.optJSONObject(InAppConstants.TEMPLATE_STYLE_DARK);
                inAppMessage.content_style = inAppPayload.optJSONObject(InAppConstants.CONTENT_STYLE);
                inAppMessage.content_style_dark = inAppPayload.optJSONObject(InAppConstants.CONTENT_STYLE_DARK);
                inAppMessage.content = inAppPayload.optJSONObject(InAppConstants.CONTENT);
                inAppMessage.extras = inAppPayload.optJSONObject(InAppConstants.EXTRAS);

                inAppMessage.message_uuid = pushPayload.get(Message.EXTRA_BSFT_MESSAGE_UUID);
                inAppMessage.experiment_uuid = pushPayload.get(Message.EXTRA_BSFT_EXPERIMENT_UUID);
                inAppMessage.user_uuid = pushPayload.get(Message.EXTRA_BSFT_USER_UUID);
                inAppMessage.transaction_uuid = pushPayload.get(Message.EXTRA_BSFT_TRANSACTIONAL_UUID);
                inAppMessage.timestamp = pushPayload.get(BlueshiftConstants.KEY_TIMESTAMP);

                return inAppMessage;
            }
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

    public JSONObject getTemplateStyle() {
        return template_style;
    }

    public void setTemplateStyle(JSONObject templateStyle) {
        this.template_style = templateStyle;
    }

    public String getTemplateStyleJson() {
        return template_style != null ? template_style.toString() : null;
    }

    public JSONObject getContentStyle() {
        return content_style;
    }

    public void setContentStyle(JSONObject contentStyle) {
        this.content_style = contentStyle;
    }

    public String getContentStyleJson() {
        return content_style != null ? content_style.toString() : null;
    }

    public JSONObject getContent() {
        return content;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }

    public String getContentJson() {
        return content != null ? content.toString() : null;
    }

    public JSONArray getActionsJSONArray() {
        if (content != null) {
            try {
                return content.optJSONArray(InAppConstants.ACTIONS);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return null;
    }

    public String getContentString(String contentName) {
        try {
            if (!content.isNull(contentName)) {
                return content.optString(contentName);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    public Rect getTemplateMargin(Context context) {
        try {
            String json = InAppUtils.getTemplateString(context, this, InAppConstants.MARGIN);
            return new Gson().fromJson(json, Rect.class);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return new Rect();
    }

    public int getTemplateHeight(Context context) {
        try {
            return InAppUtils.getTemplateInt(context, this, InAppConstants.HEIGHT, -1);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public int getTemplateWidth(Context context) {
        try {
            return InAppUtils.getTemplateInt(context, this, InAppConstants.WIDTH, -1);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public int getTemplateBackgroundColor(Context context) {
        try {
            String color = InAppUtils.getTemplateString(context, this, InAppConstants.BACKGROUND_COLOR);
            if (InAppUtils.validateColorString(color)) {
                return Color.parseColor(color);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return 0;
    }

    public HashMap<String, Object> getCampaignParamsMap() {
        HashMap<String, Object> map = new HashMap<>();

        map.put(Message.EXTRA_BSFT_MESSAGE_UUID, message_uuid);
        map.put(Message.EXTRA_BSFT_EXPERIMENT_UUID, experiment_uuid);
        map.put(Message.EXTRA_BSFT_USER_UUID, user_uuid);
        map.put(Message.EXTRA_BSFT_TRANSACTIONAL_UUID, transaction_uuid);

        return map;
    }

    public String getExtrasJson() {
        return extras != null ? extras.toString() : null;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }

    public boolean showCloseButton() {
        try {
            return template_style.has(InAppConstants.CLOSE_BUTTON);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return true;
    }

    public InAppTemplate getTemplate() {
        return InAppTemplate.fromString(type);
    }

    public boolean shouldShowNow() {
        return "now".equalsIgnoreCase(trigger);
    }

    public boolean isExpired() {
        return expires_at * 1000 < System.currentTimeMillis();
    }

    public long getDisplayFromMillis() {
        try {
            if (trigger != null && TextUtils.isDigitsOnly(trigger)) {
                return Long.valueOf(trigger) * 1000;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return 0;
    }

    public String getDisplayOn() {
        return display_on != null ? display_on : "";
    }

    public void setDisplayOn(String displayOn) {
        this.display_on = displayOn;
    }

    public String getMessageUuid() {
        return message_uuid;
    }

    public void setMessageUuid(String message_uuid) {
        this.message_uuid = message_uuid;
    }

    public String getExperimentUuid() {
        return experiment_uuid;
    }

    public void setExperimentUuid(String experiment_uuid) {
        this.experiment_uuid = experiment_uuid;
    }

    public String getUserUuid() {
        return user_uuid;
    }

    public void setUserUuid(String user_uuid) {
        this.user_uuid = user_uuid;
    }

    public String getTransactionUuid() {
        return transaction_uuid;
    }

    public void setTransactionUuid(String transaction_uuid) {
        this.transaction_uuid = transaction_uuid;
    }

    public long getDisplayedAt() {
        return displayed_at;
    }

    public void setDisplayedAt(long displayed_at) {
        this.displayed_at = displayed_at;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setTemplateStyleDark(JSONObject templateStyleDark) {
        this.template_style_dark = templateStyleDark;
    }

    public void setContentStyleDark(JSONObject contentStyleDark) {
        this.content_style_dark = contentStyleDark;
    }

    public JSONObject getTemplateStyleDark() {
        return template_style_dark;
    }

    public JSONObject getContentStyleDark() {
        return content_style_dark;
    }

    public String getTemplateStyleDarkJson() {
        return template_style_dark != null ? template_style_dark.toString() : null;
    }

    public String getContentStyleDarkJson() {
        return content_style_dark != null ? content_style_dark.toString() : null;
    }
}
