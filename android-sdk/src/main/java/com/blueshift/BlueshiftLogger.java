package com.blueshift;

import android.text.TextUtils;
import android.util.Log;

public class BlueshiftLogger {

    private static String checkTag(String tag) {
        return TextUtils.isEmpty(tag) ? "Blueshift" : tag;
    }

    public static void d(String tag, String message) {
        Log.d(checkTag(tag), message);
    }

    public static void e(String tag, String message) {
        Log.e(checkTag(tag), message);
    }

    public static void e(String tag, Exception e) {
        Log.e(checkTag(tag), e != null ? e.getMessage() : "Unknown error!");
    }
}
