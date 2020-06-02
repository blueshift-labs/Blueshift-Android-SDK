package com.blueshift.util;

import android.util.Log;

import com.blueshift.BuildConfig;

/**
 * @author Rahul Raveendran V P
 *         Created on 7/10/16 @ 2:55 PM
 *         https://github.com/rahulrvp
 */


public class SdkLog {
    private static boolean isDebug = BuildConfig.DEBUG;

    public static void i(String tag, String message) {
        if (isDebug) Log.i(tag, message);
    }

    public static void w(String tag, String message) {
        if (isDebug) Log.w(tag, message);
    }

    public static void e(String tag, String message) {
        if (isDebug) Log.e(tag, message != null ? message : "Unknown error!");
    }
}
