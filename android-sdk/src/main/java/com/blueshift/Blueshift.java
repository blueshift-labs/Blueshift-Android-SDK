package com.blueshift;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.blueshift.batch.BulkEventManager;
import com.blueshift.batch.EventsTable;
import com.blueshift.batch.FailedEventsTable;
import com.blueshift.core.BlueshiftEventManager;
import com.blueshift.core.BlueshiftLambdaQueue;
import com.blueshift.core.BlueshiftNetworkRequestQueueManager;
import com.blueshift.core.app.BlueshiftInstallationStatus;
import com.blueshift.core.app.BlueshiftInstallationStatusHelper;
import com.blueshift.core.events.BlueshiftEventRepositoryImpl;
import com.blueshift.core.network.BlueshiftNetworkConfiguration;
import com.blueshift.core.network.BlueshiftNetworkRepositoryImpl;
import com.blueshift.core.network.BlueshiftNetworkRequestRepositoryImpl;
import com.blueshift.core.schedule.network.BlueshiftNetworkChangeScheduler;
import com.blueshift.httpmanager.Request;
import com.blueshift.inappmessage.InAppActionCallback;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.inappmessage.InAppConstants;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.inappmessage.InAppMessage;
import com.blueshift.inappmessage.InAppMessageIconFont;
import com.blueshift.inbox.BlueshiftInboxStoreSQLite;
import com.blueshift.model.Configuration;
import com.blueshift.model.Product;
import com.blueshift.model.Subscription;
import com.blueshift.model.UserInfo;
import com.blueshift.request_queue.RequestQueue;
import com.blueshift.request_queue.RequestQueueTable;
import com.blueshift.rich_push.Message;
import com.blueshift.type.SubscriptionState;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Rahul Raveendran V P
 * Created on 17/2/15 @ 3:08 PM
 * <a href="https://github.com/rahulrvp">...</a>
 */
public class Blueshift {
    private static final String LOG_TAG = Blueshift.class.getSimpleName();
    private static final String UTF8_SPACE = "%20";

    private Context mContext;
    private Configuration mConfiguration;
    private static Blueshift instance = null;
    private static BlueshiftPushListener blueshiftPushListener;
    private static BlueshiftInAppListener blueshiftInAppListener;

    public enum DeviceIdSource {
        /**
         * @since 3.2.6
         * @deprecated Starting SDK version 3.2.6, we are deprecating the device id sources
         * "Android Advertising ID" and "Android Advertising ID:Package Name" owing to the changes
         * Google introduced to the <a href="https://support.google.com/googleplay/android-developer/answer/6048248?hl=en">Advertising ID</a>
         * collection starting April 1st, 2022.
         * <p>
         * "Firebase Instance ID:Package Name" would be the new default device id source for the SDK,
         * even if the app has explicitly specified the device id source as "Android Advertising ID"
         * or "Android Advertising ID:Package Name".
         * <p>
         * For assistance, reach out to your CSM or send us an email to support@blueshift.com.
         */
        @Deprecated
        ADVERTISING_ID,
        INSTANCE_ID,
        GUID,
        /**
         * @since 3.2.6
         * @deprecated Starting SDK version 3.2.6, we are deprecating the device id sources
         * "Android Advertising ID" and "Android Advertising ID:Package Name" owing to the changes
         * Google introduced to the <a href="https://support.google.com/googleplay/android-developer/answer/6048248?hl=en">Advertising ID</a>
         * collection starting April 1st, 2022.
         * <p>
         * "Firebase Instance ID:Package Name" would be the new default device id source for the SDK,
         * even if the app has explicitly specified the device id source as "Android Advertising ID"
         * or "Android Advertising ID:Package Name".
         * <p>
         * For assistance, reach out to your CSM or send us an email to support@blueshift.com.
         */
        @Deprecated
        ADVERTISING_ID_PKG_NAME,
        INSTANCE_ID_PKG_NAME,
        CUSTOM
    }

    private Blueshift(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }
    }

    /**
     * This method return the only object of the class Blueshift
     *
     * @param context valid context object
     * @return instance of Blueshift
     */
    public synchronized static Blueshift getInstance(Context context) {
        if (instance == null) {
            instance = new Blueshift(context);
        }

        return instance;
    }

    /**
     * Enable/disable the event tracking done by the SDK. By default, the tracking is enabled.
     * <p>
     * When disabled, no new events will be added to the queue. Also, any unsent events
     * (batched and realtime) will be deleted from the SDK's database immediately.
     *
     * @param context   valid context object
     * @param isEnabled the value that decides if the SDK should be enabled (true) or disabled (false)
     * @since 3.1.10
     */
    public static void setTrackingEnabled(Context context, boolean isEnabled) {
        // The events should be removed when the SDK is disabled. This is the default behavior.
        boolean shouldRemoveEvents = !isEnabled;

        setTrackingEnabled(context, isEnabled, shouldRemoveEvents);
    }


    /**
     * Enable/disable the event tracking done by the SDK. By default, the tracking is enabled.
     *
     * @param context            valid context object
     * @param isEnabled          the value that decides if the SDK should be enabled (true) or disabled (false).
     * @param shouldRemoveEvents the value that decided if the SDK should remove unsent events from it's database.
     * @since 3.1.10
     */
    public static void setTrackingEnabled(Context context, boolean isEnabled, boolean shouldRemoveEvents) {
        BlueshiftLogger.d(LOG_TAG, "Event tracking is " + (isEnabled ? "enabled." : "disabled.")
                + " Unsent events will" + (shouldRemoveEvents ? " " : " not ") + "be removed.");

        BlueshiftAppPreferences.getInstance(context).setEnableTracking(context, isEnabled);

        if (shouldRemoveEvents) {
            // Request queue table. This include both realtime and batched events.
            RequestQueueTable.getInstance(context).deleteAllAsync();
            // Events table. These are events waiting to get batched.
            EventsTable.getInstance(context).deleteAllAsync();
            // Failed events table. These will also get batched periodically.
            FailedEventsTable.getInstance(context).deleteAllAsync();

            // Delete the data from events and request queue
            BlueshiftEventManager.INSTANCE.clearAsync();
        }
    }

    public static boolean isTrackingEnabled(Context context) {
        return BlueshiftAppPreferences.getInstance(context).getEnableTracking();
    }

    /**
     * Utility method to set in-app opt-in status. This method will send an identify event
     * with the latest information.
     *
     * @param context   Valid {@link Context} object
     * @param isOptedIn The opt-in status as boolean
     */
    public static void optInForInAppNotifications(final Context context, boolean isOptedIn) {
        BlueshiftAppPreferences preferences = BlueshiftAppPreferences.getInstance(context);
        preferences.setEnableInApp(isOptedIn);
        preferences.save(context);

        // identify call w/ map for avoiding race conditions
        HashMap<String, Object> map = new HashMap<>();
        UserInfo cu = UserInfo.getInstance(context);
        if (cu != null) map.putAll(cu.toHashMap());

        Blueshift.getInstance(context).identifyUser(map, false);

        if (isOptedIn) return;

        // remove all cached in-app messages if user opt-out from in-app
        BlueshiftExecutor.getInstance().runOnDiskIOThread(() -> BlueshiftInboxStoreSQLite.getInstance(context).deleteAllMessages());
    }

    /**
     * Utility method to set push opt-in status. This method will send an identify event
     * with the latest information.
     *
     * @param context   Valid {@link Context} object
     * @param isOptedIn The opt-in status as boolean
     */
    public static void optInForPushNotifications(Context context, boolean isOptedIn) {
        BlueshiftAppPreferences preferences = BlueshiftAppPreferences.getInstance(context);
        preferences.setEnablePush(isOptedIn);
        preferences.save(context);

        // identify call w/ map for avoiding race conditions
        HashMap<String, Object> map = new HashMap<>();
        UserInfo cu = UserInfo.getInstance(context);
        if (cu != null) map.putAll(cu.toHashMap());

        Blueshift.getInstance(context).identifyUser(map, false);
    }


    /**
     * Reset the device_id by replacing the old value with a freshly generated value.
     * <p>
     * Note: This only works for the device_id source, GUID. This will not reset other device_ids.
     *
     * @param context Valid {@link Context} object
     */
    public static void resetDeviceId(final Context context) {
        Configuration config = BlueshiftUtils.getConfiguration(context);
        if (config != null && config.getDeviceIdSource() == DeviceIdSource.GUID) {
            BlueshiftExecutor.getInstance().runOnWorkerThread(new Runnable() {
                @Override
                public void run() {
                    // reset device id.
                    BlueShiftPreference.resetDeviceID(context);
                    // fire identify event.
                    Blueshift.getInstance(context).identifyUser(null, false);
                }
            });
        } else {
            BlueshiftLogger.d(LOG_TAG, "Device ID reset is only applicable to GUID device ID source.");
        }
    }

    /**
     * This is a helper method to ask for push notification permission when running in Android 13.
     * <p>
     * Make sure your app is targeting Android 13 (API 33) and have added the permission in the AndroidManifest.xml file.
     *
     * @param context Your app's context for launching the permission dialog.
     */
    public static void requestPushNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BlueshiftPermissionsActivity.launchForPushNotificationPermission(context);
        }
    }

    public static BlueshiftPushListener getBlueshiftPushListener() {
        return blueshiftPushListener;
    }

    public static BlueshiftInAppListener getBlueshiftInAppListener() {
        return blueshiftInAppListener;
    }

    public static void setBlueshiftPushListener(BlueshiftPushListener pushListener) {
        blueshiftPushListener = pushListener;
    }

    public static void setBlueshiftInAppListener(BlueshiftInAppListener inAppListener) {
        blueshiftInAppListener = inAppListener;
    }

    public void registerForInAppMessages(Activity activity) {
        InAppManager.registerForInAppMessages(activity);
    }

    public void registerForInAppMessages(Activity activity, String screenName) {
        InAppManager.registerForInAppMessages(activity, screenName);
    }

    public void unregisterForInAppMessages(Activity activity) {
        InAppManager.unregisterForInAppMessages(activity);
    }

    public void fetchInAppMessages(InAppApiCallback callback) {
        InAppManager.fetchInAppFromServer(mContext, callback);
    }

    public void displayInAppMessages() {
        InAppManager.invokeTriggers();
    }

    public void setInAppActionCallback(InAppActionCallback callback) {
        InAppManager.setActionCallback(callback);
    }

    public void handleInAppMessageAPIResponse(Context context, String response) {
        InAppManager.handleInAppMessageAPIResponse(context, response);
    }

    public JSONObject getInAppMessageAPIRequestPayload(Context context) {
        return InAppManager.generateInAppMessageAPIRequestPayload(context);
    }

    /**
     * Reset uuid which is being sent as device_id for this app
     */
    public void resetDeviceId() {
        BlueShiftPreference.resetDeviceID(mContext);
    }

    public static boolean hasPermission(Context context, String permission) {
        boolean status = false;

        try {
            status = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            BlueshiftLogger.e(LOG_TAG, String.format("Failure checking permission %s", permission));
        }

        return status;
    }

    public void getLiveContentByEmail(@NonNull String slot, LiveContentCallback callback) {
        getLiveContentByEmail(slot, null, callback);
    }

    public void getLiveContentByDeviceId(@NonNull String slot, LiveContentCallback callback) {
        getLiveContentByDeviceId(slot, null, callback);
    }

    public void getLiveContentByCustomerId(@NonNull String slot, LiveContentCallback callback) {
        getLiveContentByCustomerId(slot, null, callback);
    }

    @SuppressWarnings("WeakerAccess")
    public void getLiveContentByEmail(@NonNull String slot, HashMap<String, Object> liveContentContext, LiveContentCallback callback) {
        fetchLiveContentAsync(mContext, slot, BlueshiftConstants.KEY_EMAIL, liveContentContext, callback);
    }

    @SuppressWarnings("WeakerAccess")
    public void getLiveContentByDeviceId(@NonNull String slot, HashMap<String, Object> liveContentContext, LiveContentCallback callback) {
        fetchLiveContentAsync(mContext, slot, BlueshiftConstants.KEY_DEVICE_IDENTIFIER, liveContentContext, callback);
    }

    @SuppressWarnings("WeakerAccess")
    public void getLiveContentByCustomerId(@NonNull String slot, HashMap<String, Object> liveContentContext, LiveContentCallback callback) {
        fetchLiveContentAsync(mContext, slot, BlueshiftConstants.KEY_CUSTOMER_ID, liveContentContext, callback);
    }

    /**
     * This method initializes the sdk with the configuration set by user
     *
     * @param configuration this object contains all the mandatory parameters like api key, deep-link pages etc.
     */
    public void initialize(@NonNull Configuration configuration) {
        mConfiguration = configuration;

        // get the status of app version change
        String appVersion = CommonUtils.getAppVersion(mContext);
        String previousAppVersion = BlueShiftPreference.getStoredAppVersionString(mContext);
        File database = mContext.getDatabasePath("blueshift_db.sqlite3");
        BlueshiftInstallationStatusHelper helper = new BlueshiftInstallationStatusHelper();
        BlueshiftInstallationStatus status = helper.getInstallationStatus(appVersion, previousAppVersion, database);

        // initialize the encrypted shared preferences if enabled.
        if (configuration.shouldSaveUserInfoAsEncrypted()) {
            BlueshiftEncryptedPreferences.INSTANCE.init(mContext);
        }

        doNetworkConfigurations(mConfiguration);

        BlueshiftAttributesApp.getInstance().init(mContext);

        initAppIcon(mContext, mConfiguration);

        initializeEventSyncModule(mContext, mConfiguration);
        initializeLegacyEventSyncModule(mContext);

        switch (status) {
            case APP_INSTALL -> {
                trackEvent(BlueshiftConstants.EVENT_APP_INSTALL, helper.getEventAttributes(status, previousAppVersion), false);
                BlueShiftPreference.saveAppVersionString(mContext, appVersion);
            }
            case APP_UPDATE -> {
                trackEvent(BlueshiftConstants.EVENT_APP_UPDATE, helper.getEventAttributes(status, previousAppVersion), false);
                BlueShiftPreference.saveAppVersionString(mContext, appVersion);
            }
        }

        handleAppOpenEvent(mContext);

        InAppMessageIconFont.getInstance(mContext).updateFont(mContext);
        InAppManager.fetchInAppFromServer(mContext, null);

        doAutomaticIdentifyChecks(mContext);
    }

    private void doAutomaticIdentifyChecks(Context context) {
        if (BlueShiftPreference.didPushPermissionStatusChange(context)) {
            BlueShiftPreference.saveCurrentPushPermissionStatus(context);

            BlueshiftLogger.d(LOG_TAG, "A change in push permission detected, sending an identify event.");
            identifyUser(null, false);
        }
    }

    void doNetworkConfigurations(Configuration configuration) {
        BlueshiftNetworkConfiguration.INSTANCE.configureBasicAuthentication(configuration.getApiKey(), "");
        BlueshiftNetworkConfiguration.INSTANCE.setDatacenter(configuration.getRegion());
    }

    void handleAppOpenEvent(Context context) {
        boolean isAutoAppOpenEnabled = BlueshiftUtils.isAutomaticAppOpenFiringEnabled(context);
        boolean canSendAppOpenNow = BlueshiftUtils.canAutomaticAppOpenBeSentNow(context);
        if (isAutoAppOpenEnabled && canSendAppOpenNow) {
            trackAppOpen(false);
            // mark the tracking time
            long now = System.currentTimeMillis() / 1000;
            BlueShiftPreference.setAppOpenTrackedAt(context, now);
        }
    }

    void initializeLegacyEventSyncModule(Context context) {
        // Do not schedule any jobs. We have the new events module to do that.

        if (!BlueShiftPreference.isLegacyEventSyncComplete(context)) {
            // Cleanup any cached events by sending them to Blueshift.
            BlueshiftExecutor.getInstance().runOnNetworkThread(() -> {
                try {
                    Request request = RequestQueueTable.getInstance(context).getFirstRecord();
                    ArrayList<HashMap<String, Object>> fEvents = FailedEventsTable.getInstance(context).getBulkEventParameters(1);
                    ArrayList<HashMap<String, Object>> bEvents = EventsTable.getInstance(context).getBulkEventParameters(1);

                    if (request != null || !fEvents.isEmpty() || !bEvents.isEmpty()) {
                        BlueshiftLogger.d(LOG_TAG, "Initiating legacy events sync... (request queue = " + (request != null ? "not empty" : "empty") + ", fEvents = " + fEvents.size() + ", bEvents = " + bEvents.size() + ")");

                        // Move any pending bulk events in the db to request queue.
                        BulkEventManager.enqueueBulkEvents(context);
                        // Sync the http request queue.
                        RequestQueue.getInstance().sync(context);
                    } else {
                        BlueshiftLogger.d(LOG_TAG, "Legacy events sync is done!");
                        // The request queue is empty and the event tables are also empty.
                        // This could mean that there is nothing left to sync.
                        BlueShiftPreference.markLegacyEventSyncAsComplete(context);
                    }
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            });
        }
    }

    void initializeEventSyncModule(Context context, Configuration configuration) {
        BlueshiftNetworkChangeScheduler.INSTANCE.scheduleWithJobScheduler(context, configuration);

        try (BlueshiftNetworkRequestRepositoryImpl networkRequestRepository = new BlueshiftNetworkRequestRepositoryImpl(context)) {
            try (BlueshiftEventRepositoryImpl eventRepository = new BlueshiftEventRepositoryImpl(context)) {
                BlueshiftEventManager.INSTANCE.initialize(eventRepository, networkRequestRepository, BlueshiftLambdaQueue.INSTANCE);
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }

            BlueshiftNetworkRepositoryImpl networkRepository = new BlueshiftNetworkRepositoryImpl();
            BlueshiftNetworkRequestQueueManager.INSTANCE.initialize(networkRequestRepository, networkRepository);
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * Check if a notification icon is provided, else use app icon
     * as notification icon.
     *
     * @param context valid context object
     */
    private void initAppIcon(Context context, @NonNull Configuration configuration) {
        try {
            if (configuration.getAppIcon() == 0) {
                if (context != null) {
                    ApplicationInfo applicationInfo = context.getApplicationInfo();
                    configuration.setAppIcon(applicationInfo.icon);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * Method to get the current configuration
     *
     * @return configuration
     */
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /*
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
            BlueshiftLogger.e(LOG_TAG, "Please initialize the SDK. Call initialize() method with a valid configuration object.");
            return false;
        } else {
            if (configuration.getApiKey() == null || configuration.getApiKey().isEmpty()) {
                BlueshiftLogger.e(LOG_TAG, "Please set a valid API key in your configuration object before initialization.");
                return false;
            }
        }

        return true;
    }

    /**
     * Method to send generic events
     *
     * @param eventName         name of the event
     * @param params            hash map with valid parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    @SuppressWarnings("WeakerAccess")
    public void trackEvent(@NonNull final String eventName, final HashMap<String, Object> params, final boolean canBatchThisEvent) {
        if (Blueshift.isTrackingEnabled(mContext)) {
            String apiKey = BlueshiftUtils.getApiKey(mContext);
            if (apiKey == null || apiKey.isEmpty()) {
                BlueshiftLogger.e(LOG_TAG, "Please set a valid API key in your configuration object before initialization.");
            } else {
                //noinspection ConstantValue
                if (eventName != null) {
                    // Java allows passing null as eventName when calling trackEvent, but Kotlin
                    // will crash the app if eventName is null in the next line.
                    BlueshiftEventManager.INSTANCE.trackEventWithData(mContext, eventName, params, canBatchThisEvent);
                }

                doAutomaticIdentifyChecks(mContext);
            }
        } else {
            BlueshiftLogger.i(LOG_TAG, "Blueshift SDK's event tracking is disabled. Dropping event: " + eventName);
        }
    }

    /**
     * Method to track the campaign measurement
     *
     * @param referrer          the referrer url sent by playstore app
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void trackAppInstall(String referrer, boolean canBatchThisEvent) {
        if (TextUtils.isEmpty(referrer)) {
            BlueshiftLogger.e(LOG_TAG, "No valid referrer url was found for the app installation.");
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
     * This method fires an identify event that fills in customer identifiers from user info and device info
     *
     * @param details           optional additional parameters
     * @param canBatchThisEvent flag to indicate if this event can be sent in bulk event API
     */
    public void identifyUser(HashMap<String, Object> details, boolean canBatchThisEvent) {
        trackEvent(BlueshiftConstants.EVENT_IDENTIFY, details, canBatchThisEvent);
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
            BlueshiftLogger.w(LOG_TAG, "identifyUserByCustomerId() - The retailer customer ID provided is empty.");
        }

        identifyUser(BlueshiftConstants.KEY_CUSTOMER_ID, customerId, details, canBatchThisEvent);
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
            BlueshiftLogger.w(LOG_TAG, "identifyUserByAdId() - The Android Ad ID provided is empty.");
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
            BlueshiftLogger.w(LOG_TAG, "identifyUserByEmail() - The email address provided is invalid.");
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
    private void identifyUser(String key, String value, HashMap<String, Object> details, boolean canBatchThisEvent) {
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
            BlueshiftLogger.e(LOG_TAG, "Error (identifyUser) : Basic credentials validation failed.");
        }
    }

    public void trackScreenView(Activity activity, boolean canBatchThisEvent) {
        if (activity != null) {
            trackScreenView(activity.getClass().getSimpleName(), canBatchThisEvent);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void trackScreenView(String screenName, boolean canBatchThisEvent) {
        HashMap<String, Object> userParams = new HashMap<>();
        userParams.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        trackEvent(BlueshiftConstants.EVENT_PAGE_LOAD, userParams, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackAppOpen(boolean canBatchThisEvent) {
        trackAppOpen(null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackAppOpen(HashMap<String, Object> params, boolean canBatchThisEvent) {
        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, params, canBatchThisEvent);
    }

    public void trackProductView(String sku, boolean canBatchThisEvent) {
        trackProductView(sku, 0, null, canBatchThisEvent);
    }

    public void trackProductView(String sku, HashMap<String, Object> params, boolean canBatchThisEvent) {
        trackProductView(sku, 0, params, canBatchThisEvent);
    }

    public void trackProductView(String sku, int categoryId, boolean canBatchThisEvent) {
        trackProductView(sku, categoryId, null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackProductView(String sku, int categoryId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(BlueshiftConstants.KEY_SKU, sku);

        if (categoryId > 0) {
            eventParams.put(BlueshiftConstants.KEY_CATEGORY_ID, categoryId);
        }

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_VIEW, eventParams, canBatchThisEvent);
    }

    public void trackAddToCart(String sku, int quantity, boolean canBatchThisEvent) {
        trackAddToCart(sku, quantity, null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    public void trackProductSearch(String[] skus, int numberOfResults, int pageNumber, String query, HashMap<String, Object> filters, boolean canBatchThisEvent) {
        trackProductSearch(skus, numberOfResults, pageNumber, query, filters, null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    public void trackSubscriptionInitialization(SubscriptionState subscriptionState, String cycleType, int cycleLength, @NonNull String subscriptionType, float price, long startDate, HashMap<String, Object> params, boolean canBatchThisEvent) {
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

    @SuppressWarnings("WeakerAccess")
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
            BlueshiftLogger.w(LOG_TAG, "No valid subscription was found to pause.");
        }
    }

    public void trackSubscriptionUnpause(boolean canBatchThisEvent) {
        trackSubscriptionUnpause(null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
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
            BlueshiftLogger.w(LOG_TAG, "No valid subscription was found to unpause.");
        }
    }

    public void trackSubscriptionCancel(boolean canBatchThisEvent) {
        trackSubscriptionCancel(null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
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
            BlueshiftLogger.w(LOG_TAG, "No valid subscription was found to cancel.");
        }
    }

    /**
     * @since 3.2.8
     * @deprecated The delivered event sent by the SDK is deprecated. Blueshift now computes the
     * push delivery stats based on the response received from FCM. <a href="https://blueshift.com/library/product-update/release-21-11-1/#definition">Know more</a>
     * <p>
     * This method will be removed from the SDK in a future release.
     */
    @Deprecated
    public void trackNotificationView(Message message) {
        if (message != null) {
            if (message.getBsftSeedListSend()) {
                BlueshiftLogger.d(LOG_TAG, "Seed List Send. Event skipped: " + BlueshiftConstants.EVENT_PUSH_DELIVERED);
            } else {
                //noinspection deprecation
                trackNotificationView(message.getId(), message.getCampaignAttr());
            }
        } else {
            BlueshiftLogger.e(LOG_TAG, "No message available");
        }
    }

    /**
     * @since 3.2.8
     * @deprecated The delivered event sent by the SDK is deprecated. Blueshift now computes the
     * push delivery stats based on the response received from FCM. <a href="https://blueshift.com/library/product-update/release-21-11-1/#definition">Know more</a>
     * <p>
     * This method will be removed from the SDK in a future release.
     */
    @Deprecated
    public void trackNotificationView(String notificationId) {
        //noinspection deprecation
        trackNotificationView(notificationId, null);
    }

    /**
     * @since 3.2.8
     * @deprecated The delivered event sent by the SDK is deprecated. Blueshift now computes the
     * push delivery stats based on the response received from FCM. <a href="https://blueshift.com/library/product-update/release-21-11-1/#definition">Know more</a>
     * <p>
     * This method will be removed from the SDK in a future release.
     */
    @Deprecated
    @SuppressWarnings("WeakerAccess")
    public void trackNotificationView(String notificationId, HashMap<String, Object> params) {
        // The delivered event sent by the SDK was a read-receipt/acknowledgement.
        // Blueshift will now provide the push delivery stats based on the FCM's response.
        BlueshiftLogger.w(LOG_TAG, "delivered event is deprecated. Skipping the event.");
    }

    public void trackNotificationClick(Message message, HashMap<String, Object> extras) {
        if (message != null) {
            if (message.getBsftSeedListSend()) {
                BlueshiftLogger.d(LOG_TAG, "Seed List Send. Event skipped: " + BlueshiftConstants.EVENT_PUSH_CLICK);
            } else {
                HashMap<String, Object> params = new HashMap<>();
                params.put(Message.EXTRA_BSFT_MESSAGE_UUID, message.getId());
                params.putAll(message.getCampaignAttr());
                trackCampaignEventAsync(BlueshiftConstants.EVENT_PUSH_CLICK, params, extras);
            }
        }
    }

    public void trackNotificationClick(Message message) {
        if (message != null) {
            if (message.getBsftSeedListSend()) {
                BlueshiftLogger.d(LOG_TAG, "Seed List Send. Event skipped: " + BlueshiftConstants.EVENT_PUSH_CLICK);
            } else {
                trackNotificationClick(message.getId(), message.getCampaignAttr());
            }
        } else {
            BlueshiftLogger.e(LOG_TAG, "No message available");
        }
    }

    public void trackNotificationClick(String notificationId) {
        trackNotificationClick(notificationId, null);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackNotificationClick(String notificationId, HashMap<String, Object> params) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(Message.EXTRA_BSFT_MESSAGE_UUID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackCampaignEventAsync(BlueshiftConstants.EVENT_PUSH_CLICK, eventParams, null);
    }

    public void trackNotificationPageOpen(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            if (message.getBsftSeedListSend()) {
                BlueshiftLogger.d(LOG_TAG, "Seed List Send. Event skipped: " + BlueshiftConstants.EVENT_APP_OPEN);
            } else {
                trackNotificationPageOpen(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
            }
        } else {
            BlueshiftLogger.e(LOG_TAG, "No message available");
        }
    }

    public void trackNotificationPageOpen(String notificationId, boolean canBatchThisEvent) {
        trackNotificationPageOpen(notificationId, null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackNotificationPageOpen(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(Message.EXTRA_BSFT_MESSAGE_UUID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_APP_OPEN, eventParams, canBatchThisEvent);
    }

    public void trackAlertDismiss(Message message, boolean canBatchThisEvent) {
        if (message != null) {
            trackAlertDismiss(message.getId(), message.getCampaignAttr(), canBatchThisEvent);
        } else {
            BlueshiftLogger.e(LOG_TAG, "No message available");
        }
    }

    public void trackAlertDismiss(String notificationId, boolean canBatchThisEvent) {
        trackAlertDismiss(notificationId, null, canBatchThisEvent);
    }

    @SuppressWarnings("WeakerAccess")
    public void trackAlertDismiss(String notificationId, HashMap<String, Object> params, boolean canBatchThisEvent) {
        HashMap<String, Object> eventParams = new HashMap<>();
        eventParams.put(Message.EXTRA_BSFT_MESSAGE_UUID, notificationId);

        if (params != null) {
            eventParams.putAll(params);
        }

        trackEvent(BlueshiftConstants.EVENT_DISMISS_ALERT, eventParams, canBatchThisEvent);
    }

    public void trackInAppMessageDelivered(InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            trackCampaignEventAsync(InAppConstants.EVENT_DELIVERED, inAppMessage.getCampaignParamsMap(), null);
        }
    }

    public void trackInAppMessageView(InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            HashMap<String, Object> extras = new HashMap<>();
            extras.put(InAppConstants.OPENED_BY, inAppMessage.getOpenedBy().toString());

            trackCampaignEventAsync(
                    InAppConstants.EVENT_OPEN, inAppMessage.getCampaignParamsMap(), extras);
        }
    }

    public void trackInAppMessageClick(InAppMessage inAppMessage, JSONObject extraJson) {
        if (inAppMessage != null) {
            // sending the element name to identify click
            HashMap<String, Object> extras = new HashMap<>();
            if (extraJson != null) {
                Iterator<String> keys = extraJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    extras.put(key, extraJson.opt(key));
                }
            }

            trackCampaignEventAsync(InAppConstants.EVENT_CLICK, inAppMessage.getCampaignParamsMap(), extras);
        }
    }

    public void trackInAppMessageDismiss(InAppMessage inAppMessage, JSONObject extraJson) {
        if (inAppMessage != null) {
            // sending the element name to identify click
            HashMap<String, Object> extras = new HashMap<>();
            if (extraJson != null) {
                Iterator<String> keys = extraJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    extras.put(key, extraJson.opt(key));
                }
            }

            trackCampaignEventAsync(InAppConstants.EVENT_DISMISS, inAppMessage.getCampaignParamsMap(), extras);
        }
    }

    void trackUniversalLinks(Uri uri) {
        try {
            if (uri != null) {
                Set<String> paramNames = uri.getQueryParameterNames();
                if (paramNames != null) {
                    StringBuilder builder = new StringBuilder();
                    boolean firstIteration = true;
                    for (String name : paramNames) {
                        // this check is needed as remove() on the Set was giving error
                        if (!BlueshiftConstants.KEY_REDIR.equals(name)) {
                            if (firstIteration) {
                                firstIteration = false;
                            } else {
                                builder.append("&");
                            }

                            builder.append(name).append("=").append(uri.getQueryParameter(name));
                        }
                    }

                    BlueshiftEventManager.INSTANCE.enqueueCampaignEvent(builder.toString());
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private void appendAnd(StringBuilder builder) {
        if (builder != null && builder.length() > 0) {
            builder.append("&");
        }
    }

    private boolean sendNotificationEvent(String action, HashMap<String, Object> campaignParams, HashMap<String, Object> extras) {
        if (campaignParams != null) {
            StringBuilder q = new StringBuilder();

            if (action != null) q.append(BlueshiftConstants.KEY_ACTION).append("=").append(action);

            Object uid = campaignParams.get(Message.EXTRA_BSFT_USER_UUID);
            if (uid != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_UID).append("=").append(uid);
            }

            Object eid = campaignParams.get(Message.EXTRA_BSFT_EXPERIMENT_UUID);
            if (eid != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_EID).append("=").append(eid);
            }

            Object aid = campaignParams.get(Message.EXTRA_ADAPTER_UUID);
            if (aid != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_BSFT_AAID).append("=").append(aid);
            }

            Object ek = campaignParams.get(Message.EXTRA_BSFT_EXECUTION_KEY);
            if (ek != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_BSFT_EK).append("=").append(ek);
            }

            Object tid = campaignParams.get(Message.EXTRA_BSFT_TRANSACTIONAL_UUID);
            if (tid != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_TXNID).append("=").append(tid);
            }

            Object mid = campaignParams.get(Message.EXTRA_BSFT_MESSAGE_UUID);
            if (mid != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_MID).append("=").append(mid);
            }

            appendAnd(q);
            q.append(BlueshiftConstants.KEY_SDK_VERSION).append("=").append(BuildConfig.SDK_VERSION);

            String dId = DeviceUtils.getDeviceId(mContext);
            if (dId != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_DEVICE_IDENTIFIER).append("=").append(dId);
            }

            String pkgName = mContext != null ? mContext.getPackageName() : null;
            if (pkgName != null) {
                appendAnd(q);
                q.append(BlueshiftConstants.KEY_APP_NAME).append("=").append(pkgName);
            }

            appendAnd(q);
            q.append(BlueshiftConstants.KEY_TIMESTAMP).append("=").append(CommonUtils.getCurrentUtcTimestamp());

            appendAnd(q);
            q.append(BlueshiftConstants.KEY_BROWSER_PLATFORM).append("=Android").append(UTF8_SPACE).append(Build.VERSION.RELEASE);

            if (extras != null && extras.size() > 0) {
                String clickUrl = null;
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    if (key != null) {
                        if (key.equals(BlueshiftConstants.KEY_CLICK_URL)) {
                            // there is a click url inside the params, we need to push it to the end
                            clickUrl = String.valueOf(extras.get(key));
                        } else {
                            Object val = extras.get(key);
                            if (val != null) {
                                appendAnd(q);
                                q.append(key).append("=").append(val);
                            }
                        }
                    }
                }

                if (clickUrl != null) {
                    appendAnd(q);
                    String encodedUrl = NetworkUtils.encodeUrlParam(clickUrl);
                    q.append(BlueshiftConstants.KEY_CLICK_URL).append("=").append(encodedUrl);
                }
            }

            String paramsUrl = q.toString();
            if (!TextUtils.isEmpty(paramsUrl)) {
                // replace whitespace with %20 to avoid URL damage.
                paramsUrl = paramsUrl.replace(" ", UTF8_SPACE);

                BlueshiftEventManager.INSTANCE.enqueueCampaignEvent(paramsUrl);

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void trackCampaignEventAsync(final String eventName,
                                         final HashMap<String, Object> campaignAttr,
                                         final HashMap<String, Object> extraAttr) {
        if (Blueshift.isTrackingEnabled(mContext)) {
            BlueshiftExecutor.getInstance().runOnDiskIOThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            boolean tracked = sendNotificationEvent(eventName, campaignAttr, extraAttr);
                            BlueshiftLogger.d(LOG_TAG, "Event tracking { name: " + eventName + ", status: " + tracked + " }");
                        }
                    }
            );
        } else {
            BlueshiftLogger.i(LOG_TAG, "Blueshift SDK's event tracking is disabled. Dropping event: " + eventName);
        }
    }

    private String fetchLiveContentFromAPI(
            Context context,
            String slot,
            String idKey,
            HashMap<String, Object> liveContentContext
    ) {
        JSONObject reqParams = new JSONObject();

        if (!TextUtils.isEmpty(slot)) {
            try {
                reqParams.put(BlueshiftConstants.KEY_SLOT, slot);
            } catch (JSONException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        } else {
            BlueshiftLogger.e(LOG_TAG, "Live Content Api: No slot provided.");
        }

        Configuration config = getConfiguration();
        if (config != null) {
            String apiKey = config.getApiKey();
            if (!TextUtils.isEmpty(apiKey)) {
                try {
                    reqParams.put(BlueshiftConstants.KEY_API_KEY, apiKey);
                } catch (JSONException e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            } else {
                BlueshiftLogger.e(LOG_TAG, "Live Content Api: No Api Key provided.");
            }
        } else {
            BlueshiftLogger.e(LOG_TAG, "Live Content Api: No valid config provided.");
        }

        JSONObject userHash = new JSONObject();

        if (idKey != null) {
            switch (idKey) {
                case BlueshiftConstants.KEY_EMAIL:
                    UserInfo userInfo = UserInfo.getInstance(context);
                    if (userInfo != null) {
                        String email = userInfo.getEmail();
                        if (!TextUtils.isEmpty(email)) {
                            try {
                                userHash.put(BlueshiftConstants.KEY_EMAIL, email);
                            } catch (JSONException e) {
                                BlueshiftLogger.e(LOG_TAG, e);
                            }
                        } else {
                            BlueshiftLogger.e(LOG_TAG, "Live Content Api: No email id provided in UserInfo.");
                        }
                    }

                    break;

                case BlueshiftConstants.KEY_DEVICE_IDENTIFIER:
                    String deviceId = DeviceUtils.getDeviceId(context);
                    if (!TextUtils.isEmpty(deviceId)) {
                        try {
                            userHash.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, deviceId);
                        } catch (JSONException e) {
                            BlueshiftLogger.e(LOG_TAG, e);
                        }
                    } else {
                        BlueshiftLogger.e(LOG_TAG, "Live Content Api: No advertisingID available.");
                    }

                    break;

                case BlueshiftConstants.KEY_CUSTOMER_ID:
                    UserInfo bsftUserInfo = UserInfo.getInstance(context);
                    if (bsftUserInfo != null) {
                        String customerId = bsftUserInfo.getRetailerCustomerId();
                        if (!TextUtils.isEmpty(customerId)) {
                            try {
                                userHash.put(BlueshiftConstants.KEY_CUSTOMER_ID, customerId);
                            } catch (JSONException e) {
                                BlueshiftLogger.e(LOG_TAG, e);
                            }
                        } else {
                            BlueshiftLogger.e(LOG_TAG, "Live Content Api: No customerId provided in UserInfo.");
                        }
                    }

                    break;
            }
        }

        // add user params
        try {
            reqParams.put(BlueshiftConstants.KEY_USER, userHash);
        } catch (JSONException e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        // add extra params if available
        if (liveContentContext != null && liveContentContext.size() > 0) {
            try {
                reqParams.put(BlueshiftConstants.KEY_CONTEXT, liveContentContext);
            } catch (JSONException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }

        BlueshiftHttpRequest request = new BlueshiftHttpRequest.Builder()
                .setUrl(BlueshiftConstants.LIVE_CONTENT_API_URL(context))
                .setMethod(BlueshiftHttpRequest.Method.POST)
                .setReqBodyJson(reqParams)
                .build();

        BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(request);

        return response.getCode() == 200 ? response.getBody() : null;
    }

    private void fetchLiveContentAsync(
            final Context context,
            final String slot,
            final String idKey,
            final HashMap<String, Object> liveContentContext,
            final LiveContentCallback callback
    ) {

        Handler handler = null;
        Looper looper = Looper.myLooper();
        if (looper != null) {
            handler = new Handler(looper);
        }

        final Handler finalHandler = handler;
        BlueshiftExecutor.getInstance().runOnNetworkThread(
                new Runnable() {
                    @Override
                    public void run() {
                        final String liveContent = fetchLiveContentFromAPI(
                                context,
                                slot,
                                idKey,
                                liveContentContext
                        );

                        // invoke callback from caller's handler
                        if (finalHandler != null) {
                            finalHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onReceive(liveContent);
                                    }
                                }
                            });
                        }
                    }
                }
        );
    }
}
