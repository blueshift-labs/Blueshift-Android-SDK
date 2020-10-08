package com.blueshift;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.blueshift.util.BlueshiftUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

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
        addFirebaseInstanceId(context);
        addFirebaseToken(context);
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
