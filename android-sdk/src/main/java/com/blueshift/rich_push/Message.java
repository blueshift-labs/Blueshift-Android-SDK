package com.blueshift.rich_push;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rahul Raveendran V P
 *         Created on 18/2/15 @ 12:20 PM
 *         https://github.com/rahulrvp
 */
public class Message implements Serializable {
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_BSFT_EXPERIMENT_UUID = "bsft_experiment_uuid";
    public static final String EXTRA_BSFT_USER_UUID = "bsft_user_uuid";
    public static final String EXTRA_BSFT_TRANSACTIONAL_UUID = "bsft_transaction_uuid";
    public static final String EXTRA_BSFT_MESSAGE_UUID = "bsft_message_uuid";
    public static final String EXTRA_BSFT_SEED_LIST_SEND = "bsft_seed_list_send";

    /**
     * Following are the campaign uuids. They come outside the 'message' object in push message.
     * We have added them inside this class to avoid major code change and to use them conveniently
     * as the message object is passed along with all notification methods generally.
     */
    private String bsft_experiment_uuid;
    private String bsft_user_uuid;
    private String bsft_transaction_uuid; // present only with transactional campaign
    private Boolean bsft_seed_list_send; // test messages sent to seed list of users will have this

    /**
     * The following variables are used for parsing the 'message' payload.
     * They are the values used for creating the notification.
     */

    /**
     * id used for tracking the notification events
     */
    private String bsft_message_uuid;

    /**
     * ** mandatory **
     * used for defining the type of notification. currently takes 2 values.
     * <p>
     * <b>notification</b>
     * represents default system notifications
     * <p>
     * <b>alert</b>
     * represents notification shown using alert dialogs
     * <p>
     * <b>custom_notification</b>
     * represents notifications with custom UI.
     */
    private String notification_type;

    /**
     * ** mandatory **
     * used for determining the feature provided by the notification. Takes following values.
     * <p>
     * <b>silent_push</b>
     * push message to track uninstalls. sdk will skip this notification.
     * <p>
     * <b>buy</b>
     * displays notification with 2 action buttons. used with notification_type notification
     * 1. View - deep link to product details page
     * 2. Buy - deep link to cart page
     * <p>
     * <b>view_cart</b>
     * displays notification with one action button. used with notification_type notification
     * 1. View Cart - deep link to cart page
     * <p>
     * <b>promotion</b>
     * displays notification with deep link to launcher activity (opens app).
     * used with notification_type notification
     * <p>
     * alert
     * <b>alert_box</b>
     * displays an alert box notification with 2 action buttons.
     * used with notification_type alert
     * 1. Open - deep link to launcher activity (opens app)
     * 2. Dismiss - close the notification.
     * <p>
     * <b>alert_box_1_button</b>
     * displays an alert box notification with only dismiss button. clicking on dismiss will
     * close the dialog. used with notification_type alert
     * <p>
     * <b>animated_carousel</b>
     * shows a carousel notification with animation. used with notification_type custom_notification.
     * <p>
     * <b>carousel</b>
     * shows a carousel notification with next/prev buttons to change pics.
     * used with notification_type custom_notification.
     * <p>
     * <b>gif</b>
     * shows a notification with GIF image. used with notification_type custom_notification.
     */
    private String category;

    /**
     * Basics notification contents (text)
     */
    private String content_title;
    private String content_text;
    private String content_sub_text;

    /**
     * Big Notification contents (text)
     */
    private String big_content_title;
    private String big_content_summary_text;

    /**
     * This image URL is used for creating notifications with images (both bitmap and GIF)
     */
    private String image_url;

    /**
     * The array of carousel elements. Required to create carousel notifications.
     */
    private CarouselElement[] carousel_elements;

    /**
     * This url is used for deep linking. If this URL is set to a valid value,
     * the sdk will open the app and pass the message object as a parameter.
     * <p>
     * Note: The action specified in the category will be ignored if this key
     * is present
     */
    private String deep_link_url;

    /**
     * URL used when running promotions.
     */
    private String url;

    /**
     * Following are optional product info parameters.
     */
    private String product_id;
    private String sku;
    private String mrp;
    private String price;

    /**
     * Optional additional data as key value pair.
     */
    private HashMap data;

    /*
     * Scheduled messages' payloads
     */
    private List<Message> notifications;

    /*
     * Schedules message's timing
     */
    private long timestamp_to_display;
    private long timestamp_to_expire_display;

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

            if (!TextUtils.isEmpty(getBsftTransactionUuid())) {
                attributes.put(EXTRA_BSFT_TRANSACTIONAL_UUID, getBsftTransactionUuid());
            }
        }

        return attributes;
    }

    public String getId() {
        return bsft_message_uuid;
    }

    public void setBsftMessageUuid(String bsft_message_uuid) {
        this.bsft_message_uuid = bsft_message_uuid;
    }

    public String getBsftTransactionUuid() {
        return bsft_transaction_uuid;
    }

    public void setBsftTransactionUuid(String transactionUuid) {
        this.bsft_transaction_uuid = transactionUuid;
    }

    public Boolean getBsftSeedListSend() {
        return bsft_seed_list_send != null && bsft_seed_list_send;
    }

    public void setBsftSeedListSend(Boolean bsftSeedListSend) {
        this.bsft_seed_list_send = bsftSeedListSend;
    }

    public NotificationType getNotificationType() {
        return NotificationType.fromString(notification_type);
    }

    public NotificationCategory getCategory() {
        return NotificationCategory.fromString(category);
    }

    public String getContentTitle() {
        return content_title;
    }

    public String getContentText() {
        return content_text;
    }

    public String getContentSubText() {
        return content_sub_text;
    }

    public String getBigContentTitle() {
        return big_content_title;
    }

    public String getBigContentSummaryText() {
        return big_content_summary_text;
    }

    /**
     * The {@link #getSku()} method is deprecated now. Use this method instead
     * to read the product id sent along with the push payload.
     *
     * @return {@link #product_id} if available, else {@link #sku}
     */
    public String getProductId() {
        return TextUtils.isEmpty(product_id) ? sku : product_id;
    }

    /**
     * This method is deprecated.
     *
     * @deprecated use {@link #getProductId()} instead
     */
    @Deprecated
    public String getSku() {
        return sku;
    }

    public String getMrp() {
        return mrp;
    }

    public String getPrice() {
        return price;
    }

    public String getDeepLinkUrl() {
        return deep_link_url;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return image_url;
    }

    public HashMap getData() {
        return data;
    }

    public List<Message> getNotifications() {
        return notifications;
    }

    public long getTimestampToDisplay() {
        return timestamp_to_display;
    }

    public long getTimestampToExpireDisplay() {
        return timestamp_to_expire_display;
    }

    public CarouselElement[] getCarouselElements() {
        return carousel_elements;
    }

    public int getCarouselLength() {
        int length = 0;

        if (carousel_elements != null) {
            length = carousel_elements.length;
        }

        return length;
    }

    public int getNextCarouselIndex(int currentIndex) {
        int index = currentIndex;
        int maxValue = getCarouselLength() - 1;

        if (index >= maxValue) {
            index = 0;
        } else {
            index++;
        }

        return index;
    }

    public int getPrevCarouselIndex(int currentIndex) {
        int index = currentIndex;

        if (index <= 0) {
            index = getCarouselLength() - 1;
        } else {
            index--;
        }

        return index;
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

    public boolean isDeepLinkingEnabled() {
        return !TextUtils.isEmpty(deep_link_url);
    }
}
