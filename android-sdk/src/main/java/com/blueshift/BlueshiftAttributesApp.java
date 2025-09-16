package com.blueshift;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationManagerCompat;

import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.DeviceUtils;
import com.blueshift.util.PermissionUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

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

    private void runPermissionChecksAndLogAsync(final Context context) {
        BlueshiftExecutor.getInstance().runOnDiskIOThread(
                new Runnable() {
                    @Override
                    public void run() {
                        runPermissionChecksAndLog(context);
                    }
                }
        );
    }

    private void runPermissionChecksAndLog(Context context) {
        try {
            // Location
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            boolean isAvailable = PermissionUtils.hasAnyPermission(context, permissions);
            BlueshiftLogger.i(TAG, "Location permission is" + (isAvailable ? "" : " not") + " available.");
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            // read from system settings
            NotificationManagerCompat notificationMgr = NotificationManagerCompat.from(context);
            boolean isPushEnabledAndroid = notificationMgr.areNotificationsEnabled();
            BlueshiftLogger.i(TAG, "Notifications turned" + (isPushEnabledAndroid ? " ON" : " OFF") + " in Android settings.");
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            // read from app preferences
            boolean isPushEnabledApp = BlueshiftAppPreferences.getInstance(context).getEnablePush();
            BlueshiftLogger.i(TAG, "Notifications turned" + (isPushEnabledApp ? " ON" : " OFF") + " in app's preferences.");
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public void init(Context context) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_SDK_VERSION, BuildConfig.SDK_VERSION);

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
        addDeviceToken(context);
        addDeviceId(context);
        addDeviceAdId(context);
        addDeviceLocation(context);
        addAdTrackingStatus(context);
        addDeviceCountryCode();
        addDeviceLanguageCode();

        runPermissionChecksAndLogAsync(context);
    }

    private void addDeviceCountryCode() {
        String countryCode = Locale.getDefault().getCountry();
        setDeviceCountryCode(countryCode);
    }

    private void setDeviceCountryCode(String countryCode) {
        synchronized (instance) {
            if (countryCode != null) {
                try {
                    instance.put(BlueshiftConstants.KEY_COUNTRY_CODE, countryCode);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }
    }

    private void addDeviceLanguageCode() {
        String languageCode = Locale.getDefault().getLanguage();
        setDeviceLanguageCode(languageCode);
    }

    private void setDeviceLanguageCode(String languageCode) {
        synchronized (instance) {
            if (languageCode != null) {
                try {
                    instance.put(BlueshiftConstants.KEY_LANGUAGE_CODE, languageCode);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }
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
                    instance.put(BlueshiftConstants.KEY_ADVERTISING_ID, adId);
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
                instance.put(BlueshiftConstants.KEY_LIMIT_AD_TRACKING, isEnabled);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void addDeviceId(final Context context) {
        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        if (configuration != null && configuration.getDeviceIdSource() != null) {
            switch (configuration.getDeviceIdSource()) {
                case INSTANCE_ID:
                    setFirebaseInstanceIdAsDeviceId();
                    break;

                case GUID:
                    setGUIDAsDeviceId(context);
                    break;

                case CUSTOM:
                    setCustomStringAsDeviceId(context);
                    break;

                default:
                    // DEFAULT value is FID:package_name
                    setFirebaseInstanceIdPackageNameAsDeviceId(context);
            }
        } else {
            // DEFAULT value is FID:package_name
            setFirebaseInstanceIdPackageNameAsDeviceId(context);
        }
    }

    private void setFirebaseInstanceIdAsDeviceId() {
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String instanceId) {
                            if (instanceId == null || instanceId.isEmpty()) {
                                BlueshiftLogger.w(TAG, "Instance id not available.");
                            } else {
                                setDeviceId(instanceId);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            BlueshiftLogger.w(TAG, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void setFirebaseInstanceIdPackageNameAsDeviceId(Context context) {
        if (context != null) {
            try {
                final String pkgName = context.getPackageName();
                FirebaseInstallations.getInstance().getId()
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String instanceId) {
                                if (instanceId == null || instanceId.isEmpty()) {
                                    BlueshiftLogger.w(TAG, "Instance id not available.");
                                } else {
                                    String deviceId = instanceId + ":" + pkgName;
                                    // Instance ID and package name combo.
                                    setDeviceId(deviceId);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                BlueshiftLogger.w(TAG, e.getMessage());
                            }
                        });
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void setAdvertisingIdAsDeviceId(Context context) {
        try {
            if (context != null) {
                String advertisingId = DeviceUtils.getAdvertisingId(context);
                if (advertisingId == null || advertisingId.isEmpty()) {
                    BlueshiftLogger.w(TAG, "Advertising id not available.");
                } else {
                    setDeviceId(advertisingId);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void setAdvertisingIdPackageNameAsDeviceId(Context context) {
        try {
            if (context != null) {
                String advertisingId = DeviceUtils.getAdvertisingId(context);
                if (advertisingId == null || advertisingId.isEmpty()) {
                    BlueshiftLogger.w(TAG, "Advertising id not available.");
                } else {
                    String deviceId = advertisingId + ":" + context.getPackageName();
                    setDeviceId(deviceId);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void setGUIDAsDeviceId(Context context) {
        String guid = BlueShiftPreference.getDeviceID(context);
        if (guid == null || guid.isEmpty()) {
            BlueshiftLogger.w(TAG, "GUID id not available.");
        } else {
            setDeviceId(guid);
        }
    }

    private void setCustomStringAsDeviceId(Context context) {
        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        String deviceId = configuration != null ? configuration.getCustomDeviceId() : null;
        if (deviceId == null || deviceId.isEmpty()) {
            BlueshiftLogger.w(TAG, "Custom device id is not provided in the configuration.");
        } else {
            setDeviceId(deviceId);
        }
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

    public String getCachedDeviceId() {
        synchronized (instance) {
            try {
                return instance.getString(BlueshiftConstants.KEY_DEVICE_IDENTIFIER);
            } catch (JSONException e) {
                BlueshiftLogger.w(TAG, "device_id is not ready yet. Try again later.");
            }
        }

        return null;
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
                    instance.put(BlueshiftConstants.KEY_LATITUDE, location.getLatitude());
                    instance.put(BlueshiftConstants.KEY_LONGITUDE, location.getLongitude());
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            } else {
                BlueshiftLogger.w(TAG, "No last-known location available!");
            }
        }
    }

    private void addDeviceToken(Context context) {
        if (!BlueshiftUtils.isPushEnabled(context)) return;

        String cachedToken = BlueShiftPreference.getSavedDeviceToken(context);
        if (cachedToken != null) {
            setFirebaseToken(cachedToken);
        }

        try {
            addFirebaseToken(context);
        } catch (Exception e) {
            // tickets#8919 reported an issue with fcm token fetch. this is the
            // fix for the same. we are manually calling initializeApp and trying
            // to get token again.
            FirebaseApp.initializeApp(context);
            try {
                addFirebaseToken(context);
            } catch (Exception e1) {
                BlueshiftLogger.e(TAG, e1);
            }
        }
    }

    private void addFirebaseToken(Context context) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String token) {
                            BlueShiftPreference.saveDeviceToken(context, token);
                            setFirebaseToken(token);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            BlueshiftLogger.w(TAG, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
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

    public String getCachedFirebaseToken() {
        synchronized (instance) {
            try {
                return instance.getString(BlueshiftConstants.KEY_DEVICE_TOKEN);
            } catch (JSONException e) {
                BlueshiftLogger.w(TAG, "device_token is not ready yet. Try again later.");
            }
        }

        return null;
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
        try {
            FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String instanceId) {
                            setFirebaseInstanceId(instanceId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            BlueshiftLogger.w(TAG, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void setFirebaseInstanceId(String instanceId) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_FIREBASE_INSTANCE_ID, instanceId);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    public void updateFirebaseToken(Context context, String newToken) {
        if (newToken != null) {
            BlueShiftPreference.saveDeviceToken(context, newToken);
            setFirebaseToken(newToken);
        }
    }

    private void addPushEnabledStatus(Context context) {
        boolean isEnabled = BlueshiftUtils.isOptedInForPushNotification(context);
        setPushEnabledStatus(isEnabled);
    }

    private void setPushEnabledStatus(boolean isEnabled) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_ENABLE_PUSH, isEnabled);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    public boolean getPushEnabledStatus() {
        synchronized (instance) {
            try {
                return instance.getBoolean(BlueshiftConstants.KEY_ENABLE_PUSH);
            } catch (JSONException e) {
                BlueshiftLogger.w(TAG, "push status is not ready yet. Try again later.");
            }
        }
        return false;
    }

    private void addInAppEnabledStatus(Context context) {
        boolean isEnabled = BlueshiftUtils.isOptedInForInAppMessages(context);
        setInAppEnabledStatus(isEnabled);
    }

    private void setInAppEnabledStatus(boolean isEnabled) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_ENABLE_INAPP, isEnabled);
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
                    instance.put(BlueshiftConstants.KEY_APP_NAME, pkgName);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }
    }

    private void addAppVersion(Context context) {
        String version = CommonUtils.getAppVersion(context);
        setAppVersion(version);
    }

    private void setAppVersion(String appVersion) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_APP_VERSION, appVersion);
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
            addDeviceCountryCode();
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            addDeviceLanguageCode();
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

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
            addInAppEnabledStatus(context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        try {
            // this call will sync and get the latest device id and store it in the instance
            // however this is not synchronous. so we might not get the value immediately.
            addDeviceId(context);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        BlueshiftExecutor.getInstance().runOnDiskIOThread(() -> {
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
        });

        return instance;
    }
}
