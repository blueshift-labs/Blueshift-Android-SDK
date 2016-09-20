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

    /**
     * Following are the campaign uuids. They come outside the 'message' object in push message.
     * We have added them inside this class to avoid major code change and to use them conveniently
     * as the message object is passed along with all notification methods generally.
     */
    private String bsft_experiment_uuid;
    private String bsft_user_uuid;

    /**
     * The following variables are used for parsing the 'message' payload.
     * They are the values used for creating the notification.
     */
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
    private CarouselElement[] carousel_elements;

    /**
     * Following are the variables used for managing the logics implemented locally.
     *
     * Ex: the index of carousel notification item, this will keep track of currently shown image in
     * a carousel type notification. helps the sdk to implement actions of 'next' & 'prev' buttons
     */
    private int carousel_current_index = 0;


    /**
     * The following are the get / set methods for the above declared variables.
     */

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

    /**
     * This method generates the HashMap with experiment uuid and user uuid filled in (if available)
     *
     * @return valid parameters inside HashMap if it is a campaign push, else null
     */
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

    public CarouselElement[] getCarouselElements() {
        return carousel_elements;
    }

    /**
     * Following are the methods that helps the sdk to understand the type of the notification.
     */

    public boolean isCampaignPush() {
        return !TextUtils.isEmpty(bsft_experiment_uuid) && !TextUtils.isEmpty(bsft_user_uuid);
    }

    public boolean isSilentPush() {
        return getNotificationType() == NotificationType.Notification && getCategory() == NotificationCategory.SilentPush;
    }
}
