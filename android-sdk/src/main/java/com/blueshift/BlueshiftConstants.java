package com.blueshift;

import android.content.Context;

import com.blueshift.util.BlueshiftUtils;

/**
 * @author Rahul Raveendran V P
 * Created on 4/3/15 @ 3:02 PM
 * <a href="https://github.com/rahulrvp">rahulrvp</a>
 */
@SuppressWarnings("WeakerAccess")
public class BlueshiftConstants {

    /**
     * URLs for server communication
     */
    public static final String BASE_URL_US = "https://api.getblueshift.com";
    public static final String BASE_URL_EU = "https://api.eu.getblueshift.com";

    public static String BASE_URL(Context context) {
        BlueshiftRegion region = BlueshiftUtils.getBlueshiftRegion(context);
        return BlueshiftRegion.EU == region ? BASE_URL_EU : BASE_URL_US;
    }

    public static String TRACK_API_URL(Context context) {
        return BASE_URL(context) + "/track";
    }

    public static String EVENT_API_URL(Context context) {
        return BASE_URL(context) + "/api/v1/event";
    }

    public static String BULK_EVENT_API_URL(Context context) {
        return BASE_URL(context) + "/api/v1/bulkevents";
    }

    public static String IN_APP_API_URL(Context context) {
        return BASE_URL(context) + "/inapp/msg";
    }

    public static String LIVE_CONTENT_API_URL(Context context) {
        return BASE_URL(context) + "/live";
    }

    public static String INBOX_STATUS(Context context) {
        return BASE_URL(context) + "/inbox/api/v1/status";
    }

    public static String INBOX_MESSAGES(Context context) {
        return BASE_URL(context) + "/inbox/api/v1/messages";
    }

    public static String INBOX_UPDATE(Context context) {
        return BASE_URL(context) + "/inbox/api/v1/update";
    }

    /**
     * Event names sent to Blueshift server
     */
    public static final String EVENT_IDENTIFY = "identify";
    public static final String EVENT_PAGE_LOAD = "pageload";
    public static final String EVENT_SUBSCRIBE = "subscribe";
    public static final String EVENT_UNSUBSCRIBE = "unsubscribe";
    public static final String EVENT_VIEW = "view";
    public static final String EVENT_ADD_TO_CART = "add_to_cart";
    public static final String EVENT_CHECKOUT = "checkout";
    public static final String EVENT_PURCHASE = "purchase";
    public static final String EVENT_CANCEL = "cancel";
    public static final String EVENT_RETURN = "return";
    public static final String EVENT_SEARCH = "search";
    public static final String EVENT_SUBSCRIPTION_UPGRADE = "subscription_upgrade";
    public static final String EVENT_SUBSCRIPTION_DOWNGRADE = "subscription_downgrade";
    public static final String EVENT_SUBSCRIPTION_BILLING = "subscription_billing";
    public static final String EVENT_SUBSCRIPTION_CANCEL = "subscription_cancel";
    public static final String EVENT_APP_OPEN = "app_open";
    public static final String EVENT_APP_INSTALL = "app_install";
    public static final String EVENT_PUSH_DELIVERED = "delivered";
    public static final String EVENT_PUSH_CLICK = "click";
    public static final String EVENT_DISMISS_ALERT = "dismiss_alert";

    /**
     * Names of parameters (key) we send to Blueshift server along with events
     */

    // App
    public static final String KEY_APP_NAME = "app_name";
    public static final String KEY_APP_VERSION = "app_version";

    // Device
    public static final String KEY_LIMIT_AD_TRACKING = "limit_ad_tracking";
    public static final String KEY_DEVICE_IDENTIFIER = "device_id";
    public static final String KEY_ADVERTISING_ID = "advertising_id";
    public static final String KEY_FIREBASE_INSTANCE_ID = "firebase_instance_id";
    public static final String KEY_DEVICE_TYPE = "device_type";
    public static final String KEY_DEVICE_TOKEN = "device_token";
    public static final String KEY_DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String KEY_OS_NAME = "os_name";
    public static final String KEY_NETWORK_CARRIER = "network_carrier";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_COUNTRY_CODE = "country_code";
    public static final String KEY_LANGUAGE_CODE = "language_code";

    // User
    public static final String KEY_EMAIL = "email";
    public static final String KEY_CUSTOMER_ID = "customer_id";
    public static final String KEY_FIRST_NAME = "firstname";
    public static final String KEY_LAST_NAME = "lastname";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_JOINED_AT = "joined_at";
    public static final String KEY_FACEBOOK_ID = "facebook_id";
    public static final String KEY_EDUCATION = "education";
    public static final String KEY_UNSUBSCRIBED_PUSH = "unsubscribed_push";
    public static final String KEY_DATE_OF_BIRTH = "date_of_birth";

    // Events
    public static final String KEY_EVENT = "event";
    public static final String KEY_SCREEN_VIEWED = "screen_viewed";
    public static final String KEY_SKU = "sku";
    public static final String KEY_SKUS = "skus";
    public static final String KEY_CATEGORY_ID = "category_id";
    public static final String KEY_QUANTITY = "quantity";
    public static final String KEY_PRODUCTS = "products";
    public static final String KEY_REVENUE = "revenue";
    public static final String KEY_DISCOUNT = "discount";
    public static final String KEY_COUPON = "coupon";
    public static final String KEY_ORDER_ID = "order_id";
    public static final String KEY_SHIPPING_COST = "shipping_cost";
    public static final String KEY_NUMBER_OF_RESULTS = "number_of_results";
    public static final String KEY_PAGE_NUMBER = "page_number";
    public static final String KEY_QUERY = "query";
    public static final String KEY_FILTERS = "filters";
    public static final String KEY_SUBSCRIPTION_PERIOD_TYPE = "subscription_period_type";
    public static final String KEY_SUBSCRIPTION_PERIOD_LENGTH = "subscription_period_length";
    public static final String KEY_SUBSCRIPTION_PLAN_TYPE = "subscription_plan_type";
    public static final String KEY_SUBSCRIPTION_AMOUNT = "subscription_amount";
    public static final String KEY_SUBSCRIPTION_START_DATE = "subscription_start_date";
    public static final String KEY_SUBSCRIPTION_STATUS = "subscription_status";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_BROWSER_PLATFORM = "browser_platform";
    public static final String KEY_SDK_VERSION = "bsft_sdk_version";
    public static final String KEY_CLICK_URL = "clk_url";
    public static final String KEY_CLICK_ELEMENT = "clk_elmt";
    public static final String KEY_UID = "uid";
    public static final String KEY_MID = "mid";
    public static final String KEY_EID = "eid";
    public static final String KEY_BSFT_AAID = "bsft_aaid";
    public static final String KEY_TXNID = "txnid";
    public static final String KEY_ACTION = "a";

    // live content
    public static final String KEY_SLOT = "slot";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_USER = "user";
    public static final String KEY_CONTEXT = "context";

    // in-app message
    public static final String KEY_ENABLE_INAPP = "enable_inapp";
    public static final String KEY_LAST_TIMESTAMP = "last_timestamp";

    // push
    public static final String KEY_ENABLE_PUSH = "enable_push";

    // inbox broadcast actions
    public static final String ACTION_INBOX_SYNC_COMPLETE = "com.blueshift.ACTION_INBOX_SYNC_COMPLETE";
    public static final String ACTION_INBOX_MESSAGE_READ = "com.blueshift.ACTION_INBOX_MESSAGE_READ";
    public static final String ACTION_INBOX_MESSAGE_DELETED = "com.blueshift.ACTION_INBOX_MESSAGE_DELETED";
    public static final String EXTRA_INBOX_DATA_CHANGED = "com.blueshift.EXTRA_INBOX_DATA_CHANGED";
    public static final String EXTRA_INBOX_MESSAGE_ID = "com.blueshift.EXTRA_INBOX_MESSAGE_ID";

    /**
     * Subscription status values
     */
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELED = "canceled";

    /**
     * Bulk Event
     */
    public static final int BULK_EVENT_PAGE_SIZE = 100;

    /*
     * Silent push
     */
    public static final String SILENT_PUSH = "silent_push";
    public static final String SILENT_PUSH_ACTION = "action";
    public static final String ACTION_IN_APP_BACKGROUND_FETCH = "in_app_background_fetch";
    public static final String ACTION_IN_APP_MARK_AS_OPEN = "in_app_mark_as_open";
    public static final String OPENED_IN_APP_MESSAGE_UUIDS = "opened_in_app_message_uuids";

    // Universal links
    public static final String KEY_REDIR = "redir";

    public static String BTN_(int index) {
        return "btn_" + index;
    }

    public static final String INBOX_ACTIVITY_TITLE = "bsft_inbox_activity_title";
    public static final String INBOX_LIST_ITEM_LAYOUT = "bsft_inbox_item_layout";
    public static final String INBOX_UNREAD_INDICATOR_COLOR = "bsft_unread_indicator_color";
    public static final String INBOX_REFRESH_INDICATOR_COLORS = "bsft_refresh_indicator_color";
    public static final String INBOX_EMPTY_MESSAGE = "bsft_inbox_empty_message";
}
