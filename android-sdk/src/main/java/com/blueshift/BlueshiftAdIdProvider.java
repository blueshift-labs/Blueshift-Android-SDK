package com.blueshift;

import android.content.Context;
import android.support.annotation.WorkerThread;

import java.lang.reflect.Method;

public class BlueshiftAdIdProvider {
    private static final String TAG = "BlueshiftAdIdProvider";
    private static BlueshiftAdIdProvider sInstance = null;
    private Object mAdIdInfoObj;
    private Method mGetIdMethod;
    private Method mIsLimitAdTrackingEnabledMethod;

    private BlueshiftAdIdProvider(Context context) {
        try {
            Class<?> clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            mAdIdInfoObj = clientClass.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, context);
        } catch (Exception e) {
            BlueshiftLogger.w(TAG, e);
        }

        try {
            Class<?> infoClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");
            mGetIdMethod = infoClass.getDeclaredMethod("getId");
            mIsLimitAdTrackingEnabledMethod = infoClass.getDeclaredMethod("isLimitAdTrackingEnabled");
        } catch (Exception e) {
            BlueshiftLogger.w(TAG, e);
        }
    }

    public static synchronized BlueshiftAdIdProvider getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BlueshiftAdIdProvider(context);
        }

        return sInstance;
    }

    /**
     * Returns the advertising id if available. This method must be called from a worker thread.
     * Calling this method from UI/main thread can cause exceptions.
     *
     * @return advertising id, when it is available. else null.
     */
    @WorkerThread
    public String getId() {
        String adId = null;

        if (mAdIdInfoObj != null && mGetIdMethod != null) {
            try {
                adId = (String) mGetIdMethod.invoke(mAdIdInfoObj);
            } catch (Exception e) {
                BlueshiftLogger.w(TAG, e);
            }
        }

        return adId;
    }

    /**
     * Returns the user's ad personalisation preference if available.
     *
     * @return true, if opted in. false, if opted-out
     */
    public boolean isLimitAdTrackingEnabled() {
        boolean enabled = false;

        if (mAdIdInfoObj != null && mIsLimitAdTrackingEnabledMethod != null) {
            try {
                enabled = Boolean.TRUE.equals(mIsLimitAdTrackingEnabledMethod.invoke(mAdIdInfoObj));
            } catch (Exception e) {
                BlueshiftLogger.w(TAG, e);
            }
        }

        return enabled;
    }
}
