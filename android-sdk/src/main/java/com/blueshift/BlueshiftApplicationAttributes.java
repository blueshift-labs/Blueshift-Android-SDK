package com.blueshift;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

public class BlueshiftApplicationAttributes extends JSONObject {
    private static final String TAG = "ApplicationAttributes";
    private static final BlueshiftApplicationAttributes instance = new BlueshiftApplicationAttributes();

    private BlueshiftApplicationAttributes() {
    }

    public static BlueshiftApplicationAttributes getInstance() {
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

        refreshAppName(context);
        refreshAppVersion(context);
    }

    private void refreshAppName(Context context) {
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

    private void refreshAppVersion(Context context) {
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
}
