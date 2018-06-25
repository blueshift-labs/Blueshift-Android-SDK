package com.blueshift.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

public class PermissionUtils {
    private static final String LOG_TAG = "Blueshift";

    public static boolean hasAnyPermission(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (hasPermission(context, permission)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasAllPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (!hasPermission(context, permission)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean hasPermission(Context context, String permission) {
        boolean hasPermission = false;
        try {
            if (context != null && !TextUtils.isEmpty(permission)) {
                hasPermission =
                        ContextCompat.checkSelfPermission(context, permission)
                                == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return hasPermission;
    }
}
