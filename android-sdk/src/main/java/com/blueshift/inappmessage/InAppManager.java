package com.blueshift.inappmessage;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.blueshift.util.InAppUtils;

import org.json.JSONObject;

public class InAppManager {

    private static final String LOG_TAG = "InAppManager";

    public static void invokeTriggers(Context context) {
        if (context == null) return;

        // TODO: 2019-07-04 fix this
//        Context appContext = context.getApplicationContext();
        Context appContext = context;
        JSONObject payload = getSamplePayload(appContext);
        if (payload != null) {
            InAppMessage inAppMessage = InAppMessage.getInstance(payload);
            boolean isSuccess = buildAndShowInAppMessage(appContext, inAppMessage);
            Log.d(LOG_TAG, "InAppSuccess: " + isSuccess);
        }
    }

    private static JSONObject getSamplePayload(Context context) {
        try {
            return new JSONObject(InAppUtils.readSamplePayload(context, "iam_payload.json"));
        } catch (Exception e) {
            return null;
        }
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
