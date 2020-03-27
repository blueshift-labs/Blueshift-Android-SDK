package com.blueshift;

import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BlueshiftLogger {

    private static String checkTag(String tag) {
        return TextUtils.isEmpty(tag) ? "Blueshift" : tag;
    }

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(checkTag(tag), message);
        }
    }

    public static void e(String tag, String message) {
        Log.e(checkTag(tag), message);
    }

    public static void e(String tag, Exception e) {
        if (e != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String stackTrace = stringWriter.toString();
            Log.e(checkTag(tag), stackTrace);
        } else {
            Log.e(checkTag(tag), "Unknown error!");
        }
    }

    public static void i(String tag, String message) {
        Log.d(checkTag(tag), message);
    }
}
