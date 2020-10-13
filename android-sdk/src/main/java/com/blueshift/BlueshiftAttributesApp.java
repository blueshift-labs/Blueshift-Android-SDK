package com.blueshift;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationManagerCompat;

import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.PermissionUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

public class BlueshiftAttributesApp extends JSONObject {
    private static final String TAG = "ApplicationAttributes";
    private static final BlueshiftAttributesApp instance = new BlueshiftAttributesApp();

    private BlueshiftAttributesApp() {
    }

    public static BlueshiftAttributesApp getInstance() {
        synchronized (instance) {
            return instance;
        }
    }

    public void init(Context context) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_SDK_VERSION, BuildConfig.SDK_VERSION);

                instance.put(BlueshiftConstants.KEY_DEVICE_TYPE, "android");
                instance.put(BlueshiftConstants.KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER);
                instance.put(BlueshiftConstants.KEY_OS_NAME, "Android " + Build.VERSION.RELEASE);

                String carrier = DeviceUtils.getSIMOperatorName(context);
                instance.put(BlueshiftConstants.KEY_NETWORK_CARRIER, carrier != null ? carrier : "");
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        addAppName(context);
        addAppVersion(context);
        addInAppEnabledStatus(context);
        addPushEnabledStatus(context);
        addFirebaseInstanceId(context);
        addFirebaseToken(context);
        addDeviceId(context);
        addDeviceAdId(context);
        addDeviceLocation(context);
        addAdTrackingStatus(context);
    }

    private void addDeviceAdId(final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(
                new Runnable() {
                    @Override
                    public void run() {
                        String adId = DeviceUtils.getAdvertisingId(context);
                        setDeviceAdId(adId);
                    }
                }
        );
    }

    private void setDeviceAdId(String adId) {
        synchronized (instance) {
            if (adId != null) {
                try {
                    instance.putOpt(BlueshiftConstants.KEY_ADVERTISING_ID, adId);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }
    }

    private void addAdTrackingStatus(final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean isEnabled = DeviceUtils.isLimitAdTrackingEnabled(context);
                        setAdTrackingStatus(isEnabled);
                    }
                }
        );
    }

    private void setAdTrackingStatus(boolean isEnabled) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_LIMIT_AD_TRACKING, isEnabled);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addDeviceId(final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(
                new Runnable() {
                    @Override
                    public void run() {
                        String deviceId = DeviceUtils.getDeviceId(context);
                        setDeviceId(deviceId);
                    }
                }
        );
    }

    private void setDeviceId(String deviceId) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_DEVICE_IDENTIFIER, deviceId);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addDeviceLocation(Context context) {
        if (context != null) {
            LocationManager locationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationMgr != null) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                if (PermissionUtils.hasAnyPermission(context, permissions)) {
                    @SuppressLint("MissingPermission")
                    Location location = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    setDeviceLocation(location);
                }
            }
        }
    }

    private void setDeviceLocation(Location location) {
        synchronized (instance) {
            if (location != null) {
                try {
                    instance.putOpt(BlueshiftConstants.KEY_LATITUDE, location.getLatitude());
                    instance.putOpt(BlueshiftConstants.KEY_LONGITUDE, location.getLongitude());
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            } else {
                BlueshiftLogger.w(TAG, "No last-known location available!");
            }
        }
    }

    private void addFirebaseToken(Context context) {
        if (!BlueshiftUtils.isPushEnabled(context)) return;

        try {
            addFirebaseToken();
        } catch (Exception e) {
            // tickets#8919 reported an issue with fcm token fetch. this is the
            // fix for the same. we are manually calling initializeApp and trying
            // to get token again.
            FirebaseApp.initializeApp(context);
            try {
                addFirebaseToken();
            } catch (Exception e1) {
                BlueshiftLogger.e(TAG, e1);
            }
        }
    }

    private void addFirebaseToken() {
        Task<InstanceIdResult> task = FirebaseInstanceId.getInstance().getInstanceId();
        task.addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                setFirebaseToken(deviceToken);
            }
        });
    }

    private void setFirebaseToken(String firebaseToken) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_DEVICE_TOKEN, firebaseToken);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addFirebaseInstanceId(Context context) {
        try {
            addFirebaseInstanceId();
        } catch (Exception e) {
            // tickets#8919 reported an issue with fcm token fetch. this is the
            // fix for the same. we are manually calling initializeApp and trying
            // to get token again.
            FirebaseApp.initializeApp(context);

            try {
                addFirebaseInstanceId();
            } catch (Exception ex) {
                BlueshiftLogger.e(TAG, ex);
            }
        }
    }

    private void addFirebaseInstanceId() {
        String instanceId = FirebaseInstanceId.getInstance().getId();
        setFirebaseInstanceId(instanceId);
    }

    private void setFirebaseInstanceId(String instanceId) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_FIREBASE_INSTANCE_ID, instanceId);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    public void updateFirebaseToken(String newToken) {
        if (newToken != null) {
            setFirebaseToken(newToken);
        }
    }

    private void addPushEnabledStatus(Context context) {
        boolean isEnabled = true;
        try {
            // read from system settings
            NotificationManagerCompat notificationMgr = NotificationManagerCompat.from(context);
            boolean systemPreferenceVal = notificationMgr.areNotificationsEnabled();

            // read from app preferences
            boolean appPreferenceVal = BlueshiftAppPreferences.getInstance(context).getEnablePush();

            // push is enabled if it is enabled on both sides
            isEnabled = systemPreferenceVal && appPreferenceVal;
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        setPushEnabledStatus(isEnabled);
    }

    private void setPushEnabledStatus(boolean isEnabled) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_ENABLE_PUSH, isEnabled);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addInAppEnabledStatus(Context context) {
        synchronized (instance) {
            boolean isEnabled = BlueshiftUtils.isInAppEnabled(context);
            try {
                instance.putOpt(BlueshiftConstants.KEY_ENABLE_INAPP, isEnabled);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addAppName(Context context) {
        synchronized (instance) {
            if (context != null) {
                try {
                    String pkgName = context.getPackageName();
                    instance.putOpt(BlueshiftConstants.KEY_APP_NAME, pkgName);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }
    }

    private void addAppVersion(Context context) {
        if (context != null) {
            try {
                String pkgName = context.getPackageName();
                if (pkgName != null) {
                    PackageManager pkgManager = context.getPackageManager();
                    if (pkgManager != null) {
                        PackageInfo pkgInfo = pkgManager.getPackageInfo(pkgName, 0);
                        if (pkgInfo != null && pkgInfo.versionName != null) {
                            String versionName = pkgInfo.versionName;
                            String versionCode;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                versionCode = String.valueOf(pkgInfo.getLongVersionCode());
                            } else {
                                versionCode = String.valueOf(pkgInfo.versionCode);
                            }

                            String version = versionName + " (" + versionCode + ")";
                            setAppVersion(version);
                        }
                    }
                }
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void setAppVersion(String appVersion) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_APP_VERSION, appVersion);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    /**
     * This will refresh the device id and ad tracking status.
     * This method should not be called from UI thread.
     *
     * @param context valid {@link Context} object
     */
    @WorkerThread
    public BlueshiftAttributesApp sync(Context context) {
        try {
            addDeviceLocation(context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            addPushEnabledStatus(context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            String deviceId = DeviceUtils.getDeviceId(context);
            setDeviceId(deviceId);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            String adId = DeviceUtils.getAdvertisingId(context);
            setDeviceAdId(adId);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            boolean isAdEnabled = DeviceUtils.isLimitAdTrackingEnabled(context);
            setAdTrackingStatus(isAdEnabled);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return instance;
    }
}
