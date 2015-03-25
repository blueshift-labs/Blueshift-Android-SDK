package com.blueshift.model;

/**
 * Created by rahul on 19/2/15.
 */
public class Configuration {
    int appIcon;
    Class productPage;
    Class cartPage;
    Class offerDisplayPage;
    String apiKey;

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
}
