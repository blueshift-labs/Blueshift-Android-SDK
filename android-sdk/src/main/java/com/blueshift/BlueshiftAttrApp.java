package com.blueshift;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.blueshift.util.BlueshiftUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class BlueshiftAttrApp extends JSONObject {
    private static final String TAG = "ApplicationAttributes";
    private static final BlueshiftAttrApp instance = new BlueshiftAttrApp();

    private BlueshiftAttrApp() {
    }

    public static BlueshiftAttrApp getInstance() {
        synchronized (instance) {
            return instance;
        }
    }

    public void init(Context context) {
        synchronized (instance) {
            try {
                instance.putOpt(BlueshiftConstants.KEY_SDK_VERSION, BuildConfig.SDK_VERSION);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        addAppName(context);
        addAppVersion(context);
        addInAppEnabledStatus(context);
        addPushEnabledStatus(context);
    }

    private void addPushEnabledStatus(Context context) {
        boolean isEnabled = true;
        try {
            NotificationManagerCompat notificationMgr = NotificationManagerCompat.from(context);
            isEnabled = notificationMgr.areNotificationsEnabled();
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

    public BlueshiftAttrApp sync(Context context) {
        addPushEnabledStatus(context);

        return instance;
    }
}
