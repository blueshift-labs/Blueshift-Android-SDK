package com.blueshift.util;

import android.content.Context;

import com.blueshift.BlueshiftLogger;

import java.lang.reflect.Method;

public class BlueshiftAdIdProvider {
    private static final String TAG = "BlueshiftAdIdProvider";
    private Object mAdIDInfo;
    private Method mAdIDMethod;
    private Method mAdIDOptInMethod;

    private BlueshiftAdIdProvider(Context context) {
        try {
            Class<?> clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            mAdIDInfo = clientClass.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            Class<?> infoClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");
            mAdIDMethod = infoClass.getDeclaredMethod("getId");
            mAdIDOptInMethod = infoClass.getDeclaredMethod("isLimitAdTrackingEnabled");
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private static BlueshiftAdIdProvider sInstance = null;

    public static synchronized BlueshiftAdIdProvider getInstance(Context context) {
        if (sInstance == null) sInstance = new BlueshiftAdIdProvider(context);
        return sInstance;
    }

    public String getId() {
        String adId = null;

        if (mAdIDInfo != null && mAdIDMethod != null) {
            try {
                adId = (String) mAdIDMethod.invoke(mAdIDInfo);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return adId;
    }

    public boolean isLimitAdTrackingEnabled() {
        boolean enabled = false;

        if (mAdIDInfo != null && mAdIDOptInMethod != null) {
            try {
                enabled = Boolean.TRUE.equals(mAdIDOptInMethod.invoke(mAdIDInfo));
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return enabled;
    }
}
