package com.blueshift;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.blueshift.gcm.GCMRegistrar;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueue;
import com.blueshift.model.Configuration;
import com.blueshift.model.Product;
import com.blueshift.model.Subscription;
import com.blueshift.model.UserInfo;
import com.blueshift.type.SubscriptionState;
import com.blueshift.util.DeviceUtils;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Created by rahul on 17/2/15.
 */
public class Blueshift {
    private static final String LOG_TAG = Blueshift.class.getSimpleName();

    private static Context mContext;
    private static Configuration mConfiguration;

    private static HashMap<String, Object> mDeviceParams;

    private static Blueshift instance = null;

    private Blueshift() {
        /**
         * Registering broadcast receiver to receive network change broadcasts.
         * This will inform the request queue to start sync()
         */
        BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                RequestQueue.getInstance(mContext).sync();
            }
        };

        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    /**
     * This method creates a hash map with all device parameters filled in.
     * @return params: hash-map with device parameters filled in.
     */
    private HashMap<String, Object> getDeviceParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        if (mDeviceParams != null) {
            params.putAll(mDeviceParams);
        }

        return params;
    }

    /**
     * This method initializes the mDeviceParams hash map with device related information.
     */
    private void initializeDeviceParams() {
        mDeviceParams = new HashMap<String, Object>();
        mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TYPE, "android");
        mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TOKEN, GCMRegistrar.getRegistrationId(mContext));
        mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDFA, "");
        mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDFV, "");
        mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER);
        mDeviceParams.put(BlueshiftConstants.KEY_OS_NAME, "Android " + Build.VERSION.RELEASE);

        String simOperatorName = DeviceUtils.getSIMOperatorName(mContext);
        if (simOperatorName != null) {
            mDeviceParams.put(BlueshiftConstants.KEY_NETWORK_CARRIER, simOperatorName);
        }

        new FetchAndUpdateAdIdTask().execute();
    }

    /**
     * Updates the mDeviceParams with new device token
     * @param deviceToken device token for sending push message
     */
    public static void updateDeviceToken(String deviceToken) {
        if (deviceToken != null && mDeviceParams != null) {
            mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TOKEN, deviceToken);
        }
    }

    /**
     * Updates the mDeviceParams with advertising ID
     */
    private class FetchAndUpdateAdIdTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            Log.d(LOG_TAG, "Trying to fetch AdvertisingId");
        }

        @Override
        protected String doInBackground(Void... params) {
            return DeviceUtils.getAdvertisingID(mContext);
        }

        @Override
        protected void onPostExecute(String adId) {
            if (!TextUtils.isEmpty(adId)) {
                mDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, adId);
            } else {
                Log.d(LOG_TAG, "Could not fetch AdvertisingId");
            }
        }
    }

    /**
     * This method return the only object of the class Blueshift
     * @param context valid context object
     * @return instance of Blueshift
     */
    public synchronized static Blueshift getInstance(Context context) {
        mContext = context;

        if (instance == null) {
            instance = new Blueshift();
        }

        return instance;
    }

    /**
     * This method initializes the sdk with the configuration set by user
     * @param configuration this object contains all the mandatory parameters like api key, deep-link pages etc.
     */
    public void initialize(Configuration configuration) {
        mConfiguration = configuration;
        // Registering device for push notification.
        GCMRegistrar.registerForNotification(mContext);
        // Collecting device specific params.
        initializeDeviceParams();
        // Trigger app open event
        trackAppOpen();
        // Sync the http request queue.
        RequestQueue.getInstance(mContext).sync();
    }

    /**
     * Method to get the current configuration
     * @return configuration
     */
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Beginning of event tracking methods.
     */

    /**
     * Checks for presence of credentials required for making api call
     * @return true if everything is OK, else false
     */
    private boolean hasValidCredentials() {
        Configuration configuration = getConfiguration();
        if (configuration == null) {
            Log.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with a valid configuration object.");
            return false;
        } else {
            if (configuration.getApiKey() == null || configuration.getApiKey().isEmpty()) {
                Log.e(LOG_TAG, "Please set a valid API key in your configuration object before initialization.");
                return false;
            }
        }

        return true;
    }

    /**
     * Appending the optional user info to params
     * @param params source hash map to append details
     * @return params - updated params object
     */
    private HashMap<String, Object> appendOptionalUserInfo(HashMap<String, Object> params) {
        if (params != null) {
            UserInfo userInfo = UserInfo.getInstance(mContext);
            if (userInfo != null) {
                params.put(BlueshiftConstants.KEY_FIRST_NAME, userInfo.getFirstname());
                params.put(BlueshiftConstants.KEY_LAST_NAME, userInfo.getLastname());
                params.put(BlueshiftConstants.KEY_GENDER, userInfo.getGender());
                if (userInfo.getJoinedAt() > 0) {
                    params.put(BlueshiftConstants.KEY_JOINED_AT, userInfo.getJoinedAt());
                }
                if (userInfo.getDateOfBirth() != null) {
                    params.put(BlueshiftConstants.KEY_DATE_OF_BIRTH, userInfo.getDateOfBirth().getTime() / 1000);
                }
                params.put(BlueshiftConstants.KEY_FACEBOOK_ID, userInfo.getFacebookId());
                params.put(BlueshiftConstants.KEY_EDUCATION, userInfo.getEducation());
                params.put(BlueshiftConstants.KEY_UNSUBSCRIBED, userInfo.isUnsubscribed());
                if (userInfo.getDetails() != null) {
                    params.putAll(userInfo.getDetails());
                }
            }
        }

        return params;
    }

    /**
     * Private method that receives params and send to server using request queue.
     * @param params hash-map filled with parameters required for api call
     * @return true if everything works fine, else false
     */
    private boolean sendEvent(HashMap<String, Object> params) {
        // Check for presence of API key
        Configuration configuration = getConfiguration();
        if (configuration == null) {
            Log.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with a valid configuration object.");
            return false;
        } else {
            if (TextUtils.isEmpty(configuration.getApiKey())) {
                Log.e(LOG_TAG, "Please set a valid API key in your configuration object before initialization.");
                return false;
            } else {
                if (params != null) {
                    HashMap<String, Object> requestParams = getDeviceParams();
                    if (requestParams != null) {
                        // Appending params with the device dependant details.
                        requestParams.putAll(params);

                        // Appending email and customer id.
                        UserInfo userInfo = UserInfo.getInstance(mContext);
                        if (userInfo != null) {
                            // checks if the hash already contains an email in it (identify event has email as arg).
                            if (userInfo.getEmail() != null && !requestParams.containsKey(BlueshiftConstants.KEY_EMAIL)) {
                                requestParams.put(BlueshiftConstants.KEY_EMAIL, userInfo.getEmail());
                            }

                            if (userInfo.getRetailerCustomerId() != null) {
                                requestParams.put(BlueshiftConstants.KEY_RETAILER_CUSTOMER_ID, userInfo.getRetailerCustomerId());
                            } else {
                                Log.w(LOG_TAG, "Retailer customer id found missing in UserInfo.");
                            }
                        }

                        // append the optional user parameters based on availability.
                        requestParams = appendOptionalUserInfo(requestParams);

                        // setting the last known location parameters.
                        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                        if (locationManager != null) {
                            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                requestParams.put(BlueshiftConstants.KEY_LATITUDE, location.getLatitude());
                                requestParams.put(BlueshiftConstants.KEY_LONGITUDE, location.getLongitude());
                            }
                        }

                        // adding timestamp
                        requestParams.put(BlueshiftConstants.KEY_TIMESTAMP, System.currentTimeMillis() / 1000);

                        // Creating the request object.
                        Request request = new Request();
                        request.setPendingRetryCount(3); // setting the default retry count as 3 for all requests.
                        request.setUrl(BlueshiftConstants.EVENT_API_URL);
                        request.setMethod(Method.POST);
                        request.setParamJson(new Gson().toJson(requestParams));

                        // Adding the request to the queue.
                        RequestQueue.getInstance(mContext).add(request);

                        return true;
                    } else {
                        Log.e(LOG_TAG, "Could not load device specific parameters. Please try again.");
                        return false;
                    }
                } else {
                    Log.e(LOG_TAG, "params can't be null");
                    return false;
                }
            }
        }
    }

    /**
     * Method to send generic events
     * @param eventName name of the event
     * @param params hash map with valid parameters
     * @return true if request is successfully added to queue, els false
     */
    public boolean trackEvent(@NotNull String eventName, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_EVENT, eventName);
        if (params != null) {
            eventParams.putAll(params);
        }

        return sendEvent(eventParams);
    }

    /**
     * Method to track the campaign measurement
     * @param referrer the referrer url sent by playstore app
     */
    public void trackAppInstall(String referrer) {
        if (TextUtils.isEmpty(referrer)) {
            Log.e(LOG_TAG, "No valid referrer url was found for the app installation.");
        } else {
            HashMap<String, Object> utmParamsHash = new HashMap<String, Object>();

            String url = Uri.decode(referrer);
            String[] paramsArray = url.split("&");
            for (String parameter : paramsArray) {
                String[] parameterPair = parameter.split("=");
                if (parameterPair.length == 2) {
                    utmParamsHash.put(parameterPair[0], parameterPair[1]);
                }
            }

            trackEvent(BlueshiftConstants.EVENT_APP_INSTALL, utmParamsHash);
        }
    }

    public boolean identifyUser(String email, HashMap<String, Object> details) {
        if (hasValidCredentials()) {
            if (email == null || email.isEmpty() || !(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
                Log.w(LOG_TAG, "The email address provided in identifyUser method is invalid.");
            }

            HashMap<String, Object> userParams = new HashMap<String, Object>();
            userParams.put(BlueshiftConstants.KEY_EMAIL, email);
            if (details != null) {
                userParams.putAll(details);
            }

            return trackEvent(BlueshiftConstants.EVENT_IDENTIFY, userParams);

        } else {
            Log.e(LOG_TAG, "Error (identifyUser) : Basic credentials validation failed.");
            return false;
        }
    }

    public void trackScreenView(Activity activity) {
        if (activity != null) {
            trackScreenView(activity.getClass().getSimpleName());
        }
    }

    public void trackScreenView(String screenName) {
        HashMap<String, Object> userParams = new HashMap<String, Object>();
        userParams.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        trackEvent(BlueshiftConstants.EVENT_PAGE_LOAD, userParams);
    }

    public void trackAppOpen() {
        trackAppOpen(null);
    }

    public void trackAppOpen(HashMap<String, Object> params) {
        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, params);
    }

    public void trackProductView(String sku, int categoryId) {
        trackProductView(sku, categoryId, null);
    }

    public void trackProductView(String sku, int categoryId, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_SKU, sku);
        eventParams.put(BlueshiftConstants.KEY_CATEGORY_ID, categoryId);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_VIEW, eventParams);
    }

    public void trackAddToCart(String sku, int quantity) {
        trackAddToCart(sku, quantity, null);
    }

    public void trackAddToCart(String sku, int quantity, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_SKU, sku);
        eventParams.put(BlueshiftConstants.KEY_QUANTITY, quantity);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_ADD_TO_CART, eventParams);

    }

    public void trackCheckoutCart(Product[] products, float revenue, float discount, String coupon) {
        trackCheckoutCart(products, revenue, discount, coupon, null);
    }

    public void trackCheckoutCart(Product[] products, float revenue, float discount, String coupon, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        eventParams.put(BlueshiftConstants.KEY_REVENUE, revenue);
        eventParams.put(BlueshiftConstants.KEY_DISCOUNT, discount);
        eventParams.put(BlueshiftConstants.KEY_COUPON, coupon);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_CHECKOUT, eventParams);
    }

    public void trackProductsPurchase(String orderId, Product[] products, float revenue, float shippingCost, float discount, String coupon) {
        trackProductsPurchase(orderId, products, revenue, shippingCost, discount, coupon, null);
    }

    public void trackProductsPurchase(String orderId, Product[] products, float revenue, float shippingCost, float discount, String coupon, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        eventParams.put(BlueshiftConstants.KEY_REVENUE, revenue);
        eventParams.put(BlueshiftConstants.KEY_SHIPPING_COST, shippingCost);
        eventParams.put(BlueshiftConstants.KEY_DISCOUNT, discount);
        eventParams.put(BlueshiftConstants.KEY_COUPON, coupon);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PURCHASE, eventParams);
    }

    public void trackPurchaseCancel(String orderId) {
        trackPurchaseCancel(orderId, null);
    }

    public void trackPurchaseCancel(String orderId, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_CANCEL, eventParams);
    }

    public void trackPurchaseReturn(String orderId, Product[] products) {
        trackPurchaseReturn(orderId, products, null);
    }

    public void trackPurchaseReturn(String orderId, Product[] products, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_RETURN, eventParams);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query) {
        trackProductSearch(skus, numberOfResults, pageNumber, query, null);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, HashMap<String, Object> filters) {
        trackProductSearch(skus, numberOfResults, pageNumber, query, filters, null);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, HashMap<String, Object> filters, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_SKUS, skus);
        eventParams.put(BlueshiftConstants.KEY_NUMBER_OF_RESULTS, numberOfResults);
        eventParams.put(BlueshiftConstants.KEY_PAGE_NUMBER, pageNumber);
        eventParams.put(BlueshiftConstants.KEY_QUERY, query);
        eventParams.put(BlueshiftConstants.KEY_FILTERS, filters);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_SEARCH, eventParams);
    }

    public void trackEmailListSubscription(String email) {
        trackEmailListSubscription(email, null);
    }

    public void trackEmailListSubscription(String email, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_EMAIL, email);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_SUBSCRIBE, eventParams);
    }

    public void trackEmailListUnsubscription(String email) {
        trackEmailListUnsubscription(email, null);
    }

    public void trackEmailListUnsubscription(String email, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_EMAIL, email);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_UNSUBSCRIBE, eventParams);
    }

    public boolean trackSubscriptionInitialization(SubscriptionState subscriptionState, String cycleType, int cycleLength, String subscriptionType, float price, long startDate) {
        return trackSubscriptionInitialization(subscriptionState, cycleType, cycleLength, subscriptionType, price, startDate, null);
    }

    public boolean trackSubscriptionInitialization(SubscriptionState subscriptionState, String cycleType, int cycleLength, @NotNull String subscriptionType, float price, long startDate, HashMap<String, Object> params) {
        Subscription subscription = Subscription.getInstance(mContext);
        subscription.setSubscriptionState(subscriptionState);
        subscription.setCycleType(cycleType);
        subscription.setCycleLength(cycleLength);
        subscription.setSubscriptionType(subscriptionType);
        subscription.setPrice(price);
        subscription.setStartDate(startDate);
        subscription.setParams(params);
        subscription.save(mContext);

        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_TYPE, cycleType);
        eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_LENGTH, cycleLength);
        eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscriptionType);
        eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_AMOUNT, price);
        eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_START_DATE, startDate);

        if (params != null) {
            eventParams.putAll(params);
        }

        if (subscriptionState != null) {
            switch (subscriptionState) {
                case START:
                case UPGRADE:
                    return trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_UPGRADE, eventParams);

                case DOWNGRADE:
                    return trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_DOWNGRADE, eventParams);

                default:
                    return false;
            }
        }

        return false;
    }

    public boolean trackSubscriptionPause() {
        return trackSubscriptionPause(null);
    }

    public boolean trackSubscriptionPause(HashMap<String, Object> params) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<String, Object>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_TYPE, subscription.getCycleType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_LENGTH, subscription.getCycleLength());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_AMOUNT, subscription.getPrice());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_PAUSED);

            if (params != null) {
                eventParams.putAll(params);
            }

            return trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_DOWNGRADE, eventParams);
        } else {
            Log.e(LOG_TAG, "No valid subscription was found to pause.");
            return false;
        }
    }

    public boolean trackSubscriptionUnpause() {
        return trackSubscriptionUnpause(null);
    }

    public boolean trackSubscriptionUnpause(HashMap<String, Object> params) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<String, Object>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_TYPE, subscription.getCycleType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_LENGTH, subscription.getCycleLength());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_AMOUNT, subscription.getPrice());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_ACTIVE);

            if (params != null) {
                eventParams.putAll(params);
            }

            return trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_UPGRADE, eventParams);
        } else {
            Log.e(LOG_TAG, "No valid subscription was found to unpause.");
            return false;
        }
    }

    public boolean trackSubscriptionCancel() {
        return trackSubscriptionCancel(null);
    }

    public boolean trackSubscriptionCancel(HashMap<String, Object> params) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<String, Object>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_CANCELED);

            if (params != null) {
                eventParams.putAll(params);
            }

            return trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_CANCEL, eventParams);
        } else {
            Log.e(LOG_TAG, "No valid subscription was found to cancel.");
            return false;
        }
    }

    public void trackNotificationView(String notificationId) {
        trackNotificationView(notificationId, null);
    }

    public void trackNotificationView(String notificationId,  HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PUSH_VIEW, eventParams);
    }

    public void trackNotificationClick(String notificationId) {
        trackNotificationClick(notificationId, null);
    }

    public void trackNotificationClick(String notificationId, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PUSH_CLICK, eventParams);
    }

    public void trackNotificationPageOpen(String notificationId) {
        trackNotificationPageOpen(notificationId, null);
    }

    public void trackNotificationPageOpen(String notificationId, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<String, Object>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, eventParams);
    }
}
