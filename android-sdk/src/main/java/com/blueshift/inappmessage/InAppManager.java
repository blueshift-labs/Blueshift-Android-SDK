package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftLogger;

public class InAppManager {

    private static final String LOG_TAG = InAppManager.class.getSimpleName();

    @SuppressLint("StaticFieldLeak") // cleanup happens when unregisterForInAppMessages() is called.
    private static Activity mActivity = null;

    /**
     * Calling this method makes the activity eligible for displaying InAppMessage
     *
     * @param activity valid Activity object.
     */
    public static void registerForInAppMessages(Activity activity) {
        if (mActivity != null) {
            Log.w(LOG_TAG, "Possible memory leak detected! Please unregister ");
        }

        mActivity = activity;

        invokeTriggers();
    }

    /**
     * Calling this method will clean up the memory to avoid memory leak.
     *
     * @param activity valid Activity object.
     */
    public static void unregisterForInAppMessages(Activity activity) {
        mActivity = null;
    }

    public static void invokeTriggers() {
        if (mActivity == null) {
            Log.d(LOG_TAG, "App isn't running with an eligible Activity to display InAppMessage.");
            return;
        }

        InAppMessage inAppMessage = InAppMessageStore.getInstance(mActivity).getInAppMessage();
        if (shouldDisplay(inAppMessage)) {
            boolean isSuccess = buildAndShowInAppMessage(mActivity, inAppMessage);
            if (isSuccess) {
                Blueshift.getInstance(mActivity).trackInAppMessageView(inAppMessage);
                InAppMessageStore.getInstance(mActivity).delete(inAppMessage);
            } else {
                BlueshiftLogger.e(LOG_TAG, "InAppMessage display failed");
            }
        }
    }

    private static boolean shouldDisplay(InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            // check expired at
            return true;
        }

        return false;
    }

    private static boolean buildAndShowInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppTemplate inAppTemplate = inAppMessage.getTemplate();
            if (inAppTemplate != null) {
                switch (inAppTemplate) {
                    case HTML:
                        return buildAndShowHtmlInAppMessage(context, inAppMessage);
                }
            }
        }

        return false;
    }

    private static boolean buildAndShowHtmlInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageHtmlView inAppMessageHtmlView = new InAppMessageHtmlView(context, inAppMessage);
            return displayInAppDialog(context, inAppMessageHtmlView);
        }

        return false;
    }

    private static boolean displayInAppActivity(Context context, View customView) {
        if (mActivity != null && !mActivity.isFinishing()) {
            mActivity.addContentView(customView, customView.getLayoutParams());

            return true;
        }

        return false;
    }

    private static boolean displayInAppDialog(Context context, View customView) {
        if (isOurAppRunning(context)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(customView);
            builder.create().show();
            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    private static boolean isOurAppRunning(Context context) {
        if (context != null) {
            ComponentName topActivity = getCurrentActivity(context);
            if (topActivity != null) {
                Log.d(LOG_TAG, "Component pkg name: " + topActivity.getPackageName());
                Log.d(LOG_TAG, "App pkg name: " + context.getPackageName());
                return topActivity.getPackageName().equals(context.getPackageName());
            }
        }

        return false;
    }

    private static ComponentName getCurrentActivity(Context context) {
        if (context != null) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                return am.getRunningTasks(1).get(0).topActivity;
            }
        }

        return null;
    }
}
