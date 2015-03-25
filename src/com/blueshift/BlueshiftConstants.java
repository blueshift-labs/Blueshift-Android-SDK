package com.blueshift;

/**
 * Created by rahul on 4/3/15.
 */
public class BlueshiftConstants {

    /**
     * URLs for server communication
     */
    public static final String BASE_URL = "https://api.getblueshift.com";
    public static final String EVENT_API_URL = BASE_URL + "/api/v1/event";

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
    public static final String EVENT_PUSH_VIEW  = "push_view";
    public static final String EVENT_PUSH_CLICK  = "push_click";

    /**
     * Names of parameters (key) we send to Blueshift server along with events
     */

    // Device
    public static final String KEY_DEVICE_IDENTIFIER = "device_identifier";
    public static final String KEY_DEVICE_TYPE = "device_type";
    public static final String KEY_DEVICE_TOKEN = "device_token";
    public static final String KEY_DEVICE_IDFA = "device_idfa";
    public static final String KEY_DEVICE_IDFV = "device_idfv";
    public static final String KEY_DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String KEY_OS_NAME = "os_name";
    public static final String KEY_NETWORK_CARRIER = "network_carrier";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";

    // User
    public static final String KEY_EMAIL = "email";
    public static final String KEY_RETAILER_CUSTOMER_ID = "retailer_customer_id";
    public static final String KEY_FIRST_NAME = "firstname";
    public static final String KEY_LAST_NAME = "lastname";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_JOINED_AT = "joined_at";
    public static final String KEY_FACEBOOK_ID = "facebook_id";
    public static final String KEY_EDUCATION = "education";
    public static final String KEY_UNSUBSCRIBED = "unsubscribed";
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
    public static final String KEY_NOTIFICATION_ID = "notification_id";
    public static final String KEY_TIMESTAMP = "timestamp";

    /**
     * Subscription status values
     */
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELED = "canceled";
}
