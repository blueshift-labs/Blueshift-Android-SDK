package com.blueshift.model;

import android.app.AlarmManager;

/**
 * Created by rahul on 19/2/15.
 */
public class Configuration {
    int appIcon;
    Class productPage;
    Class cartPage;
    Class offerDisplayPage;
    String apiKey;
    long batchInterval;
    int dialogTheme;

    public Configuration() {
        batchInterval = AlarmManager.INTERVAL_HALF_HOUR;
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
}
