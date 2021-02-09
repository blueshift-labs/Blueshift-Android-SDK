package com.blueshift.model;

import android.app.AlarmManager;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppConstants;
import com.blueshift.util.DeviceUtils;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/2/15 @ 1:01 PM
 *         https://github.com/rahulrvp
 */
public class Configuration {
    // common
    private int appIcon;
    private String apiKey;

    // deep linking
    private Class productPage;
    private Class cartPage;
    private Class offerDisplayPage;

    // bulk event
    private long batchInterval;

    // notifications
    private int dialogTheme;
    private int smallIconResId;
    private int largeIconResId;
    private int notificationColor;
    private boolean pushEnabled;
    private String defaultNotificationChannelId;
    private String defaultNotificationChannelName;
    private String defaultNotificationChannelDescription;

    // job scheduler
    private int networkChangeListenerJobId;
    private int bulkEventsJobId;

    // in app message
    private long inAppInterval;
    private boolean inAppEnableJavascript;
    private boolean inAppEnabled;
    private boolean inAppManualTriggerEnabled;
    private boolean inAppBackgroundFetchEnabled;

    private boolean enableAutoAppOpen;

    // This is the time interval between the app_open events fired by the SDK in seconds.
    private long autoAppOpenInterval;

    private Blueshift.DeviceIdSource deviceIdSource;
    private String customDeviceId;

    public Configuration() {
        // In-App Messaging
        inAppEnabled = false;
        inAppEnableJavascript = false;
        inAppManualTriggerEnabled = false;
        inAppBackgroundFetchEnabled = true;
        inAppInterval = InAppConstants.IN_APP_INTERVAL;

        // Push Messaging
        pushEnabled = true;

        // Default device_id: Android Ad Id
        deviceIdSource = Blueshift.DeviceIdSource.ADVERTISING_ID;

        // Default bulk event interval: 30min
        batchInterval = AlarmManager.INTERVAL_HALF_HOUR;

        // Default app_open: will not be fired
        enableAutoAppOpen = false;

        // Job ids used in the SDK
        networkChangeListenerJobId = 901;
        bulkEventsJobId = 902;

        // The default value is 0. When set to default, app_open
        // event will be fired on each app restart.
        autoAppOpenInterval = 0;
    }

    public int getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(int appIcon) {
        this.appIcon = appIcon;
    }

    public Class getProductPage() {
        return productPage;
    }

    public void setProductPage(Class productPage) {
        this.productPage = productPage;
    }

    public Class getCartPage() {
        return cartPage;
    }

    public void setCartPage(Class cartPage) {
        this.cartPage = cartPage;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Class getOfferDisplayPage() {
        return offerDisplayPage;
    }

    public void setOfferDisplayPage(Class offerDisplayPage) {
        this.offerDisplayPage = offerDisplayPage;
    }

    public long getBatchInterval() {
        return batchInterval;
    }

    /**
     * Set interval between bulk event api calls here.
     * Default value is AlarmManager.INTERVAL_HALF_HOUR
     *
     * @param batchInterval batch interval in milliseconds
     */
    public void setBatchInterval(long batchInterval) {
        this.batchInterval = batchInterval;
    }

    public int getDialogTheme() {
        return dialogTheme;
    }

    /**
     * Theme used for creating dialog type notifications.
     * Default value is `Theme.AppCompat.Dialog.Alert`
     *
     * @param dialogTheme user define theme's reference id
     */
    public void setDialogTheme(int dialogTheme) {
        this.dialogTheme = dialogTheme;
    }

    public int getSmallIconResId() {
        return smallIconResId == 0 ? appIcon : smallIconResId;
    }

    public void setSmallIconResId(int smallIconResId) {
        this.smallIconResId = smallIconResId;
    }

    public int getLargeIconResId() {
        return largeIconResId;
    }

    public void setLargeIconResId(int largeIconResId) {
        this.largeIconResId = largeIconResId;
    }

    public int getNotificationColor() {
        return notificationColor;
    }

    /**
     * This value will be used for passing into setColor() method of the Notification Builder
     *
     * @param notificationColor color value
     */
    public void setNotificationColor(int notificationColor) {
        this.notificationColor = notificationColor;
    }

    public String getDefaultNotificationChannelId() {
        return defaultNotificationChannelId;
    }

    public void setDefaultNotificationChannelId(String defaultNotificationChannelId) {
        this.defaultNotificationChannelId = defaultNotificationChannelId;
    }

    public String getDefaultNotificationChannelName() {
        return defaultNotificationChannelName;
    }

    public void setDefaultNotificationChannelName(String channelName) {
        this.defaultNotificationChannelName = channelName;
    }

    public String getDefaultNotificationChannelDescription() {
        return defaultNotificationChannelDescription;
    }

    public void setDefaultNotificationChannelDescription(String channelDescription) {
        this.defaultNotificationChannelDescription = channelDescription;
    }

    public int getNetworkChangeListenerJobId() {
        return networkChangeListenerJobId;
    }

    public void setNetworkChangeListenerJobId(int networkChangeListenerJobId) {
        this.networkChangeListenerJobId = networkChangeListenerJobId;
    }

    public int getBulkEventsJobId() {
        return bulkEventsJobId;
    }

    public void setBulkEventsJobId(int bulkEventsJobId) {
        this.bulkEventsJobId = bulkEventsJobId;
    }

    public long getAutoAppOpenInterval() {
        return autoAppOpenInterval;
    }

    public void setAutoAppOpenInterval(long intervalInSeconds) {
        this.autoAppOpenInterval = intervalInSeconds;
    }

    public boolean isAutoAppOpenFiringEnabled() {
        return enableAutoAppOpen;
    }

    /**
     * This enables/disables the app_open event firing when app is started based on the boolean
     * value supplied in enableAutoAppOpen.
     *
     * By default the automatic firing of app_open is disabled.
     *
     * @param enableAutoAppOpen boolean value that enable/disable auto app open firing.
     */
    public void setEnableAutoAppOpenFiring(boolean enableAutoAppOpen) {
        this.enableAutoAppOpen = enableAutoAppOpen;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public long getInAppInterval() {
        return inAppInterval;
    }

    public void setInAppInterval(long milliseconds) {
        this.inAppInterval = milliseconds;
    }

    public boolean isJavaScriptForInAppWebViewEnabled() {
        return inAppEnableJavascript;
    }

    public void setJavaScriptForInAppWebViewEnabled(boolean enable) {
        this.inAppEnableJavascript = enable;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isInAppManualTriggerEnabled() {
        return inAppManualTriggerEnabled;
    }

    public void setInAppManualTriggerEnabled(boolean inAppManualTriggerEnabled) {
        this.inAppManualTriggerEnabled = inAppManualTriggerEnabled;
    }

    public boolean isInAppBackgroundFetchEnabled() {
        return inAppBackgroundFetchEnabled;
    }

    public void setInAppBackgroundFetchEnabled(boolean inAppBackgroundFetchEnabled) {
        this.inAppBackgroundFetchEnabled = inAppBackgroundFetchEnabled;
    }

    public Blueshift.DeviceIdSource getDeviceIdSource() {
        return deviceIdSource;
    }

    public void setDeviceIdSource(Blueshift.DeviceIdSource deviceIdSource) {
        this.deviceIdSource = deviceIdSource;
    }

    public String getCustomDeviceId() {
        return customDeviceId;
    }

    public void setCustomDeviceId(String deviceId) {
        if (this.deviceIdSource == Blueshift.DeviceIdSource.CUSTOM) {
            if (TextUtils.isEmpty(deviceId)) {
                BlueshiftLogger.e(null, "No valid device_id is provided by the host app.");
            } else {
                BlueshiftLogger.d(null, "Custom device id available: " + deviceId);
                this.customDeviceId = deviceId;
            }
        } else {
            BlueshiftLogger.e(null, "Can not use custom device id without setting the deviceIdSource as Blueshift.DeviceIdSource.CUSTOM");
        }
    }
}
