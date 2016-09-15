package com.blueshift.rich_push;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by rahul on 18/2/15.
 */
public class Message implements Serializable {
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_BSFT_EXPERIMENT_UUID = "bsft_experiment_uuid";
    public static final String EXTRA_BSFT_USER_UUID = "bsft_user_uuid";

    // these parameters comes outside 'message' in push message.
    // we're adding them inside for avoiding major code changes in sdk.
    private String bsft_experiment_uuid;
    private String bsft_user_uuid;

    // Message payload values
    private String id;
    private String notification_type;
    private String title;
    private String body;
    private String category;
    private String sku;
    private String mrp;
    private String price;
    private String url;
    private String image_url;
    private long expiry;
    private HashMap<String, Object> data;

    public String getBsftExperimentUuid() {
        return bsft_experiment_uuid;
    }

    public void setBsftExperimentUuid(String bsftExperimentUuid) {
        this.bsft_experiment_uuid = bsftExperimentUuid;
    }

    public String getBsftUserUuid() {
        return bsft_user_uuid;
    }

    public void setBsftUserUuid(String bsftUserUuid) {
        this.bsft_user_uuid = bsftUserUuid;
    }

    public boolean isCampaignPush() {
        return !TextUtils.isEmpty(bsft_experiment_uuid) || !TextUtils.isEmpty(bsft_user_uuid);
    }

    public HashMap<String, Object> getCampaignAttr() {
        HashMap<String, Object> attributes = null;

        if (isCampaignPush()) {
            attributes = new HashMap<>();
            attributes.put(EXTRA_BSFT_EXPERIMENT_UUID, getBsftExperimentUuid());
            attributes.put(EXTRA_BSFT_USER_UUID, getBsftUserUuid());
        }

        return attributes;
    }

    public String getId() {
        return id;
    }

    public NotificationType getNotificationType() {
        return NotificationType.fromString(notification_type);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationCategory getCategory() {
        return NotificationCategory.fromString(category);
    }

    public String getSku() {
        return sku;
    }

    public String getMrp() {
        return mrp;
    }

    public String getPrice() {
        return price;
    }

    public String getUrl() {
        return url;
    }

    public String getImage_url() {
        return image_url;
    }

    public long getExpiry() {
        return expiry;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public boolean isSilentPush() {
        return getNotificationType() == NotificationType.Notification && getCategory() == NotificationCategory.SilentPush;
    }
}
