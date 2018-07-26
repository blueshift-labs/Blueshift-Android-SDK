package com.blueshift.model;

import android.app.AlarmManager;

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
    private String defaultNotificationChannelName;
    private String defaultNotificationChannelDescription;

    // job scheduler
    private int networkChangeListenerJobId;

    public Configuration() {
        batchInterval = AlarmManager.INTERVAL_HALF_HOUR;
        networkChangeListenerJobId = 901;
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
}
