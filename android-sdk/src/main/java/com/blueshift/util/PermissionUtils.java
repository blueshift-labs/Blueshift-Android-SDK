package com.blueshift.util;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

public class PermissionUtils {
    private static final String LOG_TAG = "Blueshift";

    /**
     * Checks if any of the permissions inside the array is granted by user.
     *
     * @param context     {@link Context object}
     * @param permissions Array of valid permission strings.
     * @return true if ANY ONE of the permissions is granted. else false.
     */
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

    /**
     * Checks if all the permssions are granted by user.
     *
     * @param context     {@link Context object}
     * @param permissions Array of valid permission strings
     * @return true if ALL of the permissions are granted. else false.
     */
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

    /**
     * Checks if the provided permission is granted by user.
     *
     * @param context    {@link Context object}
     * @param permission permission string to be checked.
     * @return true if permission is granted. false, if not.
     */
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
