package com.blueshift;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class BlueshiftLogger {
    public static final int VERBOSE = 6;
    public static final int DEBUG = 5;
    public static final int INFO = 4;
    public static final int WARNING = 3;
    public static final int ERROR = 2;
    private static final String TAG = "Blueshift";
    private static int sLogLevel = 0;
    private static boolean sEnableErrorReporting = true;

    public static void setLogLevel(int logLevel) {
        sLogLevel = logLevel;
    }

    public static void setEnableErrorReporting(boolean enable) {
        sEnableErrorReporting = enable;
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

    public static void e(String tag, Exception e) {
        if (sLogLevel >= ERROR) {
            if (e != null) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                String stackTrace = stringWriter.toString();
                e(tag, stackTrace);

                // report error to blueshift
                reportError(e);
            } else {
                e(tag, "Unknown error!");
            }
        }
    }

    private static void reportError(Exception e) {
        if (sEnableErrorReporting && e != null) {
            final HashMap<String, Object> params = new HashMap<>();
            params.put("error_class", e.getClass().getName());
            params.put("error_cause", e.getMessage());
            params.put("error_stack_trace", getStacktraceList(e));

            Blueshift.reportError(params);
        }
    }

    private static List<String> getStacktraceList(Exception e) {
        if (e != null) {
            final List<String> traceArray = new ArrayList<>();
            final StackTraceElement[] elements = e.getStackTrace();

            for (StackTraceElement element : elements) {
                traceArray.add(element.toString());
            }

            return traceArray;
        }

        return null;
    }
}
