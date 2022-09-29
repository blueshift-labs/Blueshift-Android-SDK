package com.blueshift;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("WeakerAccess")
public class BlueshiftLogger {
    public static final int VERBOSE = 6;
    public static final int DEBUG = 5;
    public static final int INFO = 4;
    public static final int WARNING = 3;
    public static final int ERROR = 2;
    private static final String TAG = "Blueshift";
    private static int sLogLevel = 0;

    public static void setLogLevel(int logLevel) {
        sLogLevel = logLevel;
    }

    private static String prepareMessage(String tag, String message) {
        String prefix = tag != null && !TAG.equals(tag) ? tag : "";
        String suffix = message != null ? message : "";
        return "".equals(prefix) ? suffix : (prefix + ": " + suffix);
    }

    public static void v(String tag, String message) {
        if (sLogLevel >= VERBOSE) {
            Log.v(TAG, prepareMessage(tag, message));
        }
    }

    public static void d(String tag, String message) {
        if (sLogLevel >= DEBUG) {
            Log.d(TAG, prepareMessage(tag, message));
        }
    }

    public static void i(String tag, String message) {
        if (sLogLevel >= INFO) {
            Log.i(TAG, prepareMessage(tag, message));
        }
    }

    public static void w(String tag, String message) {
        if (sLogLevel >= WARNING) {
            Log.w(TAG, prepareMessage(tag, message));
        }
    }

    public static void e(String tag, String message) {
        if (sLogLevel >= ERROR) {
            Log.e(TAG, prepareMessage(tag, message));
        }
    }

    public static void w(String tag, Exception e) {
        if (sLogLevel >= WARNING) {
            w(tag, getStacktrace(e));
        }
    }

    public static void e(String tag, Exception e) {
        if (sLogLevel >= ERROR) {
            e(tag, getStacktrace(e));
        }
    }

    private static String getStacktrace(Exception e) {
        String stacktrace = "Unknown error";

        if (e != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            stacktrace = stringWriter.toString();
        }

        return stacktrace;
    }
}
