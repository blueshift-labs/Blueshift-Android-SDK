package com.blueshift;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.batch.BulkEventManager;
import com.blueshift.batch.Event;
import com.blueshift.batch.EventsTable;
import com.blueshift.gcm.GCMRegistrar;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueue;
import com.blueshift.model.Configuration;
import com.blueshift.model.Product;
import com.blueshift.model.Subscription;
import com.blueshift.model.UserInfo;
import com.blueshift.rich_push.Message;
import com.blueshift.type.SubscriptionState;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.SdkLog;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 17/2/15 @ 3:08 PM
 *         https://github.com/rahulrvp
 */
public class Blueshift {
    private static final String LOG_TAG = Blueshift.class.getSimpleName();
    private static final HashMap<String, Object> sDeviceParams = new HashMap<>();

    private static Context mContext;
    private static Configuration mConfiguration;
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

                String cachedToken = BlueShiftPreference.getCachedDeviceToken(context);
                if (TextUtils.isEmpty(cachedToken)) {
                    // Registering device for push notification.
                    GCMRegistrar.registerForNotification(mContext);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    /**
     * This method return the only object of the class Blueshift
     *
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
     * This method just returns the current device token available
     * in the sDeviceParams
     *
     * @return GCM token, null if the token is null
     */
    public static String getDeviceToken() {
        synchronized (sDeviceParams) {
            String token = null;

            Object tokenObject = sDeviceParams.get(BlueshiftConstants.KEY_DEVICE_TOKEN);
            if (tokenObject != null && tokenObject instanceof String) {
                token = String.valueOf(tokenObject);
            }

            return token;
        }
    }

    /**
     * Updates the sDeviceParams with new device token
     *
     * @param deviceToken device token for sending push message
     */
    public static void updateDeviceToken(String deviceToken) {
        synchronized (sDeviceParams) {
            if (deviceToken != null) {
                sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TOKEN, deviceToken);
            }
        }
    }

    /**
     * Updates the sDeviceParams with android ad id
     *
     * @param androidAdId android advertising id String
     */
    public static void updateAndroidAdId(String androidAdId) {
        synchronized (sDeviceParams) {
            if (!TextUtils.isEmpty(androidAdId)) {
                sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, androidAdId);
            }
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        boolean status = false;

        try {
            status = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            Log.e(LOG_TAG, String.format("Failure checking permission %s", permission));
        }

        return status;
    }

    /**
     * This method creates a hash map with all device parameters filled in.
     *
     * @return params: hash-map with device parameters filled in.
     */
    private HashMap<String, Object> getDeviceParams() {
        synchronized (sDeviceParams) {
            HashMap<String, Object> params = new HashMap<>();
            params.putAll(sDeviceParams);

            return params;
        }
    }

    /**
     * This method initializes the sDeviceParams hash map with device related information.
     */
    private void initializeDeviceParams() {
        synchronized (sDeviceParams) {
            sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TYPE, "android");
            sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_TOKEN, GCMRegistrar.getRegistrationId(mContext));
            sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDFA, "");
            sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_IDFV, "");
            sDeviceParams.put(BlueshiftConstants.KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER);
            sDeviceParams.put(BlueshiftConstants.KEY_OS_NAME, "Android " + Build.VERSION.RELEASE);

            String simOperatorName = DeviceUtils.getSIMOperatorName(mContext);
            if (simOperatorName != null) {
                sDeviceParams.put(BlueshiftConstants.KEY_NETWORK_CARRIER, simOperatorName);
            }
        }

        new FetchAndUpdateAdIdTask().execute();
    }

    /**
     * This method initializes the sdk with the configuration set by user
     *
     * @param configuration this object contains all the mandatory parameters like api key, deep-link pages etc.
     */
    public void initialize(Configuration configuration) {
        mConfiguration = configuration;
        // Registering device for push notification.
        GCMRegistrar.registerForNotification(mContext);
        // Collecting device specific params.
        initializeDeviceParams();
        // Trigger app open event.
        // events sent by SDK are always batched.
        trackAppOpen(true);
        // Sync the http request queue.
        RequestQueue.getInstance(mContext).sync();
        // register alarm manager
        BulkEventManager.startAlarmManager(mContext);
    }

    /**
     * Method to get the current configuration
     *
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
     *
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
     *
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
     *
     * @param params            hash-map filled with parameters required for api call
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     * @return true if everything works fine, else false
     */
    private boolean sendEvent(HashMap<String, Object> params, boolean canBatchThisEvent) {
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

                        // check if device id (Android Ad Id) is available in parameters' list.
                        // if not found, try to get it now and fill it in.
                        Object deviceId = requestParams.get(BlueshiftConstants.KEY_DEVICE_IDENTIFIER);
                        if (deviceId == null) {
                            String adId = DeviceUtils.getAdvertisingID(mContext);
                            requestParams.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, adId);
                        }

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
                            if (hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                                    || hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                // We have either of the above 2 permissions granted.
                                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                if (location != null) {
                                    requestParams.put(BlueshiftConstants.KEY_LATITUDE, location.getLatitude());
                                    requestParams.put(BlueshiftConstants.KEY_LONGITUDE, location.getLongitude());
                                }
                            } else {
                                // Location permission is not available. The client app needs to grand permission.
                                Log.w(LOG_TAG, "Location access permission unavailable. Require " +
                                        Manifest.permission.ACCESS_FINE_LOCATION + " OR " +
                                        Manifest.permission.ACCESS_COARSE_LOCATION);
                            }
                        }

                        // adding timestamp
                        requestParams.put(BlueshiftConstants.KEY_TIMESTAMP, System.currentTimeMillis() / 1000);

                        String reqParamsJSON = new Gson().toJson(requestParams);

                        if (canBatchThisEvent) {
                            Event event = new Event();
                            event.setEventParams(requestParams);

                            SdkLog.i(LOG_TAG, "Adding event to events table for batching.");

                            EventsTable.getInstance(mContext).insert(event);
                        } else {
                            // Creating the request object.
                            Request request = new Request();
                            request.setPendingRetryCount(RequestQueue.DEFAULT_RETRY_COUNT);
                            request.setUrl(BlueshiftConstants.EVENT_API_URL);
                            request.setMethod(Method.POST);
                            request.setParamJson(reqParamsJSON);

                            SdkLog.i(LOG_TAG, "Adding real-time event to request queue.");

                            // Adding the request to the queue.
                            RequestQueue.getInstance(mContext).add(request);
                        }

                        return true;
                    } else {
                        SdkLog.e(LOG_TAG, "Could not load device specific parameters. Please try again.");
                        return false;
                    }
                } else {
                    SdkLog.e(LOG_TAG, "params can't be null");
                    return false;
                }
            }
        }
    }

    /**
     * Method to send generic events
     *
     * @param eventName         name of the event
     * @param params            hash map with valid parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void trackEvent(@NotNull String eventName, HashMap<String, Object> params, final boolean canBatchThisEvent) {
        final HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_EVENT, eventName);
        if (params != null) {
            eventParams.putAll(params);
        }

        // running on a non-UI thread to avoid possible ANR.
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                try {
                    // call send event and return its result.
                    result = sendEvent(eventParams, canBatchThisEvent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "sendEvent() failed." + ((e.getMessage() != null) ? "\n" + e.getMessage() : ""));
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean isSuccess) {
                SdkLog.i(LOG_TAG, "Event creation " + (isSuccess ? "success." : "failed."));
            }
        }.execute();
    }

    /**
     * Method to track the campaign measurement
     *
     * @param referrer          the referrer url sent by playstore app
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void trackAppInstall(String referrer, boolean canBatchThisEvent) {
        if (TextUtils.isEmpty(referrer)) {
            Log.e(LOG_TAG, "No valid referrer url was found for the app installation.");
        } else {
            HashMap<String, Object> utmParamsHash = new HashMap<>();

            String url = Uri.decode(referrer);
            String[] paramsArray = url.split("&");
            for (String parameter : paramsArray) {
                String[] parameterPair = parameter.split("=");
                if (parameterPair.length == 2) {
                    utmParamsHash.put(parameterPair[0], parameterPair[1]);
                }
            }

            trackEvent(BlueshiftConstants.EVENT_APP_INSTALL, utmParamsHash, canBatchThisEvent);
        }
    }

    /**
     * Helps trigger identify user api call using key 'retailer_customer_id'.
     *
     * @param customerId        if provided, replaces existing one (taken from UserInfo) inside request params.
     * @param details           optional additional parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void identifyUserByCustomerId(String customerId, HashMap<String, Object> details, boolean canBatchThisEvent) {
        if (TextUtils.isEmpty(customerId)) {
            Log.w(LOG_TAG, "identifyUserByCustomerId() - The retailer customer ID provided is empty.");
        }

        identifyUser(BlueshiftConstants.KEY_RETAILER_CUSTOMER_ID, customerId, details, canBatchThisEvent);
    }

    /**
     * Helps trigger identify user api call using key 'device_identifier'.
     *
     * @param androidAdId       android ad id provided by host/customer app.
     * @param details           optional additional parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void identifyUserByDeviceId(String androidAdId, HashMap<String, Object> details, boolean canBatchThisEvent) {
        if (TextUtils.isEmpty(androidAdId)) {
            Log.w(LOG_TAG, "identifyUserByAdId() - The Android Ad ID provided is empty.");
        }

        identifyUser(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, androidAdId, details, canBatchThisEvent);
    }

    /**
     * Helps trigger identify user api call using key 'email'.
     *
     * @param email             registered email address provided by host/customer app
     * @param details           optional additional parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void identifyUserByEmail(String email, HashMap<String, Object> details, boolean canBatchThisEvent) {
        if (email == null || email.isEmpty() || !(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            Log.w(LOG_TAG, "identifyUserByEmail() - The email address provided is invalid.");
        }

        identifyUser(BlueshiftConstants.KEY_EMAIL, email, details, canBatchThisEvent);
    }

    /**
     * Triggers identify user api call. If either key or value is null or empty,
     * that key value pair will be  ignored.
     *
     * @param key               the key used for identify user. Ex: email, device_identifier, retailer_customer_id
     * @param value             the corresponding value for 'key'
     * @param details           optional additional parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void identifyUser(String key, String value, HashMap<String, Object> details, boolean canBatchThisEvent) {
        if (hasValidCredentials()) {
            HashMap<String, Object> userParams = new HashMap<>();

            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                userParams.put(key, value);
            }

            if (details != null) {
                userParams.putAll(details);
            }

            trackEvent(BlueshiftConstants.EVENT_IDENTIFY, userParams, canBatchThisEvent);

        } else {
            SdkLog.e(LOG_TAG, "Error (identifyUser) : Basic credentials validation failed.");
        }
    }

    public void trackScreenView(Activity activity, boolean canBatchThisEvent) {
        if (activity != null) {
            trackScreenView(activity.getClass().getSimpleName(), canBatchThisEvent);
        }
    }

    public void trackScreenView(String screenName, boolean canBatchThisEvent) {
        HashMap<String, Object> userParams = new HashMap<>();
        userParams.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        trackEvent(BlueshiftConstants.EVENT_PAGE_LOAD, userParams, canBatchThisEvent);
    }

    public void trackAppOpen(boolean canBatchThisEvent) {
        trackAppOpen(null, canBatchThisEvent);
    }

    public void trackAppOpen(HashMap<String, Object> params, boolean canBatchThisEvent) {
        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, params, canBatchThisEvent);
    }

    public void trackProductView(String sku, int categoryId, boolean canBatchThisEvent) {
        trackProductView(sku, categoryId, null, canBatchThisEvent);
    }

    public void trackProductView(String sku, int categoryId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_SKU, sku);
        eventParams.put(BlueshiftConstants.KEY_CATEGORY_ID, categoryId);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_VIEW, eventParams, canBatchThisEvent);
    }

    public void trackAddToCart(String sku, int quantity, boolean canBatchThisEvent) {
        trackAddToCart(sku, quantity, null, canBatchThisEvent);
    }

    public void trackAddToCart(String sku, int quantity, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_SKU, sku);
        eventParams.put(BlueshiftConstants.KEY_QUANTITY, quantity);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_ADD_TO_CART, eventParams, canBatchThisEvent);
    }

    public void trackCheckoutCart(Product[] products, float revenue, float discount, String coupon, boolean canBatchThisEvent) {
        trackCheckoutCart(products, revenue, discount, coupon, null, canBatchThisEvent);
    }

    public void trackCheckoutCart(Product[] products, float revenue, float discount, String coupon, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        eventParams.put(BlueshiftConstants.KEY_REVENUE, revenue);
        eventParams.put(BlueshiftConstants.KEY_DISCOUNT, discount);
        eventParams.put(BlueshiftConstants.KEY_COUPON, coupon);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_CHECKOUT, eventParams, canBatchThisEvent);
    }

    public void trackProductsPurchase(String orderId, Product[] products, float revenue, float shippingCost, float discount, String coupon, boolean canBatchThisEvent) {
        trackProductsPurchase(orderId, products, revenue, shippingCost, discount, coupon, null, canBatchThisEvent);
    }

    public void trackProductsPurchase(String orderId, Product[] products, float revenue, float shippingCost, float discount, String coupon, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        eventParams.put(BlueshiftConstants.KEY_REVENUE, revenue);
        eventParams.put(BlueshiftConstants.KEY_SHIPPING_COST, shippingCost);
        eventParams.put(BlueshiftConstants.KEY_DISCOUNT, discount);
        eventParams.put(BlueshiftConstants.KEY_COUPON, coupon);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PURCHASE, eventParams, canBatchThisEvent);
    }

    public void trackPurchaseCancel(String orderId, boolean canBatchThisEvent) {
        trackPurchaseCancel(orderId, null, canBatchThisEvent);
    }

    public void trackPurchaseCancel(String orderId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_CANCEL, eventParams, canBatchThisEvent);
    }

    public void trackPurchaseReturn(String orderId, Product[] products, boolean canBatchThisEvent) {
        trackPurchaseReturn(orderId, products, null, canBatchThisEvent);
    }

    public void trackPurchaseReturn(String orderId, Product[] products, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_ORDER_ID, orderId);
        eventParams.put(BlueshiftConstants.KEY_PRODUCTS, products);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_RETURN, eventParams, canBatchThisEvent);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, boolean canBatchThisEvent) {
        trackProductSearch(skus, numberOfResults, pageNumber, query, null, canBatchThisEvent);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, HashMap<String, Object> filters, boolean canBatchThisEvent) {
        trackProductSearch(skus, numberOfResults, pageNumber, query, filters, null, canBatchThisEvent);
    }

    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, HashMap<String, Object> filters, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_SKUS, skus);
        eventParams.put(BlueshiftConstants.KEY_NUMBER_OF_RESULTS, numberOfResults);
        eventParams.put(BlueshiftConstants.KEY_PAGE_NUMBER, pageNumber);
        eventParams.put(BlueshiftConstants.KEY_QUERY, query);
        eventParams.put(BlueshiftConstants.KEY_FILTERS, filters);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_SEARCH, eventParams, canBatchThisEvent);
    }

    public void trackEmailListSubscription(String email, boolean canBatchThisEvent) {
        trackEmailListSubscription(email, null, canBatchThisEvent);
    }

    public void trackEmailListSubscription(String email, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_EMAIL, email);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_SUBSCRIBE, eventParams, canBatchThisEvent);
    }

    public void trackEmailListUnsubscription(String email, boolean canBatchThisEvent) {
        trackEmailListUnsubscription(email, null, canBatchThisEvent);
    }

    public void trackEmailListUnsubscription(String email, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_EMAIL, email);
        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_UNSUBSCRIBE, eventParams, canBatchThisEvent);
    }

    public void trackSubscriptionInitialization(SubscriptionState subscriptionState, String cycleType, int cycleLength, String subscriptionType, float price, long startDate, boolean canBatchThisEvent) {
        trackSubscriptionInitialization(subscriptionState, cycleType, cycleLength, subscriptionType, price, startDate, null, canBatchThisEvent);
    }

    public void trackSubscriptionInitialization(SubscriptionState subscriptionState, String cycleType, int cycleLength, @NotNull String subscriptionType, float price, long startDate, HashMap<String, Object> params, boolean canBatchThisEvent) {
        Subscription subscription = Subscription.getInstance(mContext);
        subscription.setSubscriptionState(subscriptionState);
        subscription.setCycleType(cycleType);
        subscription.setCycleLength(cycleLength);
        subscription.setSubscriptionType(subscriptionType);
        subscription.setPrice(price);
        subscription.setStartDate(startDate);
        subscription.setParams(params);
        subscription.save(mContext);

        HashMap<String, Object> eventParams = new HashMap<>();
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
                    trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_UPGRADE, eventParams, canBatchThisEvent);

                case DOWNGRADE:
                    trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_DOWNGRADE, eventParams, canBatchThisEvent);
            }
        }
    }

    public void trackSubscriptionPause(boolean canBatchThisEvent) {
        trackSubscriptionPause(null, canBatchThisEvent);
    }

    public void trackSubscriptionPause(HashMap<String, Object> params, boolean canBatchThisEvent) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_TYPE, subscription.getCycleType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_LENGTH, subscription.getCycleLength());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_AMOUNT, subscription.getPrice());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_PAUSED);

            if (params != null) {
                eventParams.putAll(params);
            }

            trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_DOWNGRADE, eventParams, canBatchThisEvent);
        } else {
            Log.w(LOG_TAG, "No valid subscription was found to pause.");
        }
    }

    public void trackSubscriptionUnpause(boolean canBatchThisEvent) {
        trackSubscriptionUnpause(null, canBatchThisEvent);
    }

    public void trackSubscriptionUnpause(HashMap<String, Object> params, boolean canBatchThisEvent) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_TYPE, subscription.getCycleType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PERIOD_LENGTH, subscription.getCycleLength());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_AMOUNT, subscription.getPrice());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_ACTIVE);

            if (params != null) {
                eventParams.putAll(params);
            }

            trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_UPGRADE, eventParams, canBatchThisEvent);
        } else {
            Log.w(LOG_TAG, "No valid subscription was found to unpause.");
        }
    }

    public void trackSubscriptionCancel(boolean canBatchThisEvent) {
        trackSubscriptionCancel(null, canBatchThisEvent);
    }

    public void trackSubscriptionCancel(HashMap<String, Object> params, boolean canBatchThisEvent) {
        Subscription subscription = Subscription.getInstance(mContext);
        if (subscription.hasValidSubscription()) {
            HashMap<String, Object> eventParams = new HashMap<>();
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_PLAN_TYPE, subscription.getSubscriptionType());
            eventParams.put(BlueshiftConstants.KEY_SUBSCRIPTION_STATUS, BlueshiftConstants.STATUS_CANCELED);

            if (params != null) {
                eventParams.putAll(params);
            }

            trackEvent(BlueshiftConstants.EVENT_SUBSCRIPTION_CANCEL, eventParams, canBatchThisEvent);
        } else {
            Log.w(LOG_TAG, "No valid subscription was found to cancel.");
        }
    }

    public void trackNotificationView(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            trackNotificationView(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
        } else {
            SdkLog.e(LOG_TAG, "No message available");
        }
    }

    public void trackNotificationView(String notificationId, boolean canBatchThisEvent) {
        trackNotificationView(notificationId, null, canBatchThisEvent);
    }

    public void trackNotificationView(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PUSH_VIEW, eventParams, canBatchThisEvent);
    }

    public void trackNotificationClick(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            trackNotificationClick(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
        } else {
            SdkLog.e(LOG_TAG, "No message available");
        }
    }

    public void trackNotificationClick(String notificationId, boolean canBatchThisEvent) {
        trackNotificationClick(notificationId, null, canBatchThisEvent);
    }

    public void trackNotificationClick(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_PUSH_CLICK, eventParams, canBatchThisEvent);
    }

    public void trackNotificationPageOpen(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            trackNotificationPageOpen(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
        } else {
            SdkLog.e(LOG_TAG, "No message available");
        }
    }

    public void trackNotificationPageOpen(String notificationId, boolean canBatchThisEvent) {
        trackNotificationPageOpen(notificationId, null, canBatchThisEvent);
    }

    public void trackNotificationPageOpen(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, eventParams, canBatchThisEvent);
    }

    public void trackAlertDismiss(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            trackAlertDismiss(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
        } else {
            SdkLog.e(LOG_TAG, "No message available");
        }
    }

    public void trackAlertDismiss(String notificationId, boolean canBatchThisEvent) {
        trackAlertDismiss(notificationId, null, canBatchThisEvent);
    }

    public void trackAlertDismiss(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_NOTIFICATION_ID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_DISMISS_ALERT, eventParams, canBatchThisEvent);
    }

    /**
     * Updates the sDeviceParams with advertising ID
     */
    private class FetchAndUpdateAdIdTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            SdkLog.i(LOG_TAG, "Trying to fetch AdvertisingId");
        }

        @Override
        protected String doInBackground(Void... params) {
            return DeviceUtils.getAdvertisingID(mContext);
        }

        @Override
        protected void onPostExecute(String adId) {
            if (!TextUtils.isEmpty(adId)) {
                updateAndroidAdId(adId);
            } else {
                SdkLog.w(LOG_TAG, "Could not fetch AdvertisingId");
            }
        }
    }
}
