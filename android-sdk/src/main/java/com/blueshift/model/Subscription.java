package com.blueshift.model;

import android.content.Context;

import com.blueshift.BlueshiftLogger;
import com.blueshift.type.SubscriptionState;
import com.blueshift.util.StorageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 11/3/15 @ 3:06 PM
 *         https://github.com/rahulrvp
 */
public class Subscription {
    private static final String LOG_TAG = "Subscription";

    private static final String PREF_FILE = "subscription_pref_file";
    private static final String PREF_KEY = "subscription_pref_key";

    private SubscriptionState subscriptionState;
    private String cycleType;
    private String subscriptionType;
    private float price;
    private long startDate;
    private int cycleLength;
    private HashMap<String, Object> params;

    private static Subscription instance;

    private Subscription() {}
    public static Subscription getInstance(Context context) {
        if (instance == null) {
            instance = load(context);
            if (instance == null) {
                instance = new Subscription();
            }
        }

        return instance;
    }

    private static Subscription load(Context context) {
        String json = StorageUtils.getStringFromPrefStore(context, PREF_FILE, PREF_KEY);
        if (json != null) {
            try {
                return new Gson().fromJson(json, Subscription.class);
            } catch (JsonSyntaxException e) {
                BlueshiftLogger.e(LOG_TAG, "Invalid JSON: " + json);
            }
        }

        return null;
    }

    public void save(Context context) {
        String json = new Gson().toJson(this);
        StorageUtils.saveStringInPrefStore(context, PREF_FILE, PREF_KEY, json);
    }

    public boolean hasValidSubscription() {
        return subscriptionType != null && !subscriptionType.isEmpty();
    }

    public SubscriptionState getSubscriptionState() {
        return subscriptionState;
    }

    public void setSubscriptionState(SubscriptionState subscriptionState) {
        this.subscriptionState = subscriptionState;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public int getCycleLength() {
        return cycleLength;
    }

    public void setCycleLength(int cycleLength) {
        this.cycleLength = cycleLength;
    }

    public HashMap<String, Object> getParams() {
        return params;
    }

    public void setParams(HashMap<String, Object> params) {
        this.params = params;
    }
}
