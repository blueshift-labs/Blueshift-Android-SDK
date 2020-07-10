package com.blueshift;

import android.content.Context;
import android.os.Build;

import com.blueshift.util.DeviceUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class BlueshiftDeviceAttributes extends JSONObject {
    private static final String TAG = "DeviceAttributes";
    private static final BlueshiftDeviceAttributes instance = new BlueshiftDeviceAttributes();

    private BlueshiftDeviceAttributes() {
    }

    public static BlueshiftDeviceAttributes getInstance() {
        synchronized (instance) {
            return instance;
        }
    }

    private HashMap<String, Object> getHashMap() {
        synchronized (instance) {
            HashMap<String, Object> map = new HashMap<>();
            Iterator<String> keys = instance.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, instance.opt(key));
            }
            return map;
        }
    }

    public void init(Context context) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_DEVICE_TYPE, "android");
                instance.put(BlueshiftConstants.KEY_DEVICE_IDFA, "");
                instance.put(BlueshiftConstants.KEY_DEVICE_IDFV, "");
                instance.put(BlueshiftConstants.KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER);
                instance.put(BlueshiftConstants.KEY_OS_NAME, "Android " + Build.VERSION.RELEASE);

                String carrier = DeviceUtils.getSIMOperatorName(context);
                instance.put(BlueshiftConstants.KEY_NETWORK_CARRIER, carrier != null ? carrier : "");
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        refreshDeviceId(context);
        refreshDeviceToken(context);
    }

    private void refreshDeviceId(final Context context) {
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

    private void refreshDeviceToken(Context context) {
        try {
            refreshDeviceToken();
        } catch (Exception e) {
            // tickets#8919 reported an issue with fcm token fetch. this is the
            // fix for the same. we are manually calling initializeApp and trying
            // to get token again.
            FirebaseApp.initializeApp(context);
            try {
                refreshDeviceToken();
            } catch (Exception e1) {
                BlueshiftLogger.e(TAG, e1);
            }
        }
    }

    private void refreshDeviceToken() {
        Task<InstanceIdResult> task = FirebaseInstanceId.getInstance().getInstanceId();
        task.addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                setDeviceToken(deviceToken);
            }
        });
    }

    private void setDeviceToken(String deviceToken) {
        synchronized (instance) {
            try {
                instance.put(BlueshiftConstants.KEY_DEVICE_TOKEN, deviceToken);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    public void log() {
        synchronized (instance) {
            BlueshiftLogger.v(TAG, instance.toString());
        }
    }
}
