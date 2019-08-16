package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteModel;
import com.blueshift.rich_push.Message;
import com.blueshift.util.InAppUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InAppMessage extends BlueshiftBaseSQLiteModel {
    public static final String TAG = InAppMessage.class.getSimpleName();
    public static final String EXTRA_IN_APP = "inapp";

    private long id;
    private String type;
    private long expires_at;
    private String trigger; // timestamp/event-name/now
    private JSONObject template_style;
    private JSONObject content_style;
    private JSONObject content;
    private JSONObject campaign_params;
    private JSONObject extras;

    public static InAppMessage getInstance(Map<String, String> pushPayload) {
        try {
            String json = pushPayload.get(InAppMessage.EXTRA_IN_APP);
            JSONObject inAppPayload = new JSONObject(json);

            InAppMessage inAppMessage = new InAppMessage();
            inAppMessage.type = inAppPayload.optString(InAppConstants.TYPE);
            inAppMessage.expires_at = inAppPayload.optLong(InAppConstants.EXPIRES_AT);
            inAppMessage.trigger = inAppPayload.optString(InAppConstants.TRIGGER);
            inAppMessage.template_style = inAppPayload.optJSONObject(InAppConstants.TEMPLATE_STYLE);
            inAppMessage.content_style = inAppPayload.optJSONObject(InAppConstants.CONTENT_STYLE);
            inAppMessage.content = inAppPayload.optJSONObject(InAppConstants.CONTENT);
            inAppMessage.extras = inAppPayload.optJSONObject(InAppConstants.EXTRAS);

            JSONObject campaignAttr = new JSONObject();

            // CAMPAIGN METADATA CHECK
            campaignAttr.put(Message.EXTRA_BSFT_MESSAGE_UUID, pushPayload.get(Message.EXTRA_BSFT_MESSAGE_UUID));
            campaignAttr.put(Message.EXTRA_BSFT_EXPERIMENT_UUID, pushPayload.get(Message.EXTRA_BSFT_EXPERIMENT_UUID));
            campaignAttr.put(Message.EXTRA_BSFT_USER_UUID, pushPayload.get(Message.EXTRA_BSFT_USER_UUID));
            campaignAttr.put(Message.EXTRA_BSFT_TRANSACTIONAL_UUID, pushPayload.get(Message.EXTRA_BSFT_TRANSACTIONAL_UUID));

            inAppMessage.campaign_params = campaignAttr;

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

    public JSONObject getActionsJSONObject() {
        if (content != null) {
            try {
                return content.optJSONObject(InAppConstants.ACTIONS);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return null;
    }

    public String getContentString(String contentName) {
        try {
            return content.optString(contentName);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    public Rect getTemplateMargin() {
        try {
            String json = InAppUtils.getTemplateString(this, InAppConstants.MARGIN);
            return new Gson().fromJson(json, Rect.class);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return new Rect();
    }

    public int getTemplateHeight() {
        try {
            return InAppUtils.getTemplateInt(this, InAppConstants.HEIGHT, -1);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public int getTemplateWidth() {
        try {
            return InAppUtils.getTemplateInt(this, InAppConstants.WIDTH, -1);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    public int getTemplateBackgroundColor() {
        try {
            String color = InAppUtils.getTemplateString(this, InAppConstants.BACKGROUND_COLOR);
            if (InAppUtils.validateColorString(color)) {
                return Color.parseColor(color);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return 0;
    }

    public String getCampaignParamsJson() {
        return campaign_params != null ? campaign_params.toString() : null;
    }

    public HashMap<String, Object> getCampaignParamsMap() {
        HashMap<String, Object> map = new HashMap<>();

        if (campaign_params != null) {
            Iterator<String> keys = campaign_params.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = campaign_params.opt(key);
                map.put(key, val);
            }
        }

        return map;
    }

    public void setCampaignParams(JSONObject campaignParams) {
        this.campaign_params = campaignParams;
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
}
