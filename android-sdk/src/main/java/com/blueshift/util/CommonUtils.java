package com.blueshift.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * @author Rahul Raveendran V P
 *         Created on 17/01/18 @ 4:41 PM
 *         https://github.com/rahulrvp
 */


public class CommonUtils {

    public static CharSequence getAppName(Context context) {
        CharSequence appName = "Not Available";

        PackageManager pkgManager = context.getPackageManager();
        String pkgName = context.getPackageName();

        ApplicationInfo appInfo = null;

        try {
            appInfo = pkgManager.getApplicationInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (appInfo != null) {
            appName = pkgManager.getApplicationLabel(appInfo);
        }

        return appName;
    }
}
