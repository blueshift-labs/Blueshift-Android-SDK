package com.blueshift.util;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.util.TypedValue;

import com.blueshift.BlueshiftLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Rahul Raveendran V P
 * Created on 17/01/18 @ 4:41 PM
 * https://github.com/rahulrvp
 */


public class CommonUtils {

    private static final String TAG = "CommonUtils";

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

    public static int dpToPx(int dpValue, Context context) {
        int value = 0;
        if (context != null && context.getResources() != null) {
            value = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
        }

        return value;
    }

    public static String formatMilliseconds(long milliseconds) {
        return new SimpleDateFormat("dd/MMM/yyyy hh:mm:ss aa", Locale.getDefault()).format(milliseconds);
    }

    public static String getBase64(String input) {
        String output = null;

        if (input != null) {
            output = Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
        }

        return output;
    }

    public static boolean isJobPending(Context context, int jobId) {
        try {
            if (context != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    if (jobScheduler != null) {
                        List<JobInfo> jobs = jobScheduler.getAllPendingJobs();
                        for (JobInfo jobInfo : jobs) {
                            if (jobInfo.getId() == jobId) {
                                BlueshiftLogger.d(TAG, "Pending job found. JobId: " + jobId);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return false;
    }

    public static String getCurrentUtcTimestamp() {
        String formatString = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(formatString, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    /**
     * This method will append the IMMUTABLE flag to the provided input flag
     * This is required by the Android 12 (S) update to create pending intents.
     *
     * @param input flags
     * @return input flag or flag appended with IMMUTABLE flag
     */
    public static int appendImmutableFlag(int input) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return input | PendingIntent.FLAG_IMMUTABLE;
        }

        return input;
    }
}
