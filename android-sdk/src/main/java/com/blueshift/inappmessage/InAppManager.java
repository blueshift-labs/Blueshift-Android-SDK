package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.InAppUtils;
import com.blueshift.util.StorageUtils;

public class InAppManager {

    public static final long DEFAULT_INTERVAL = 1000 * 60 * 5; // 5 min

    private static final String PREF_FILE = "inappmanager";
    private static final String PREF_KEY_LAST_DISPLAY_TIME = "last_display_time";

    private static final String LOG_TAG = InAppManager.class.getSimpleName();

    @SuppressLint("StaticFieldLeak") // cleanup happens when unregisterForInAppMessages() is called.
    private static Activity mActivity = null;
    private static AlertDialog mDialog = null;

    /**
     * Calling this method makes the activity eligible for displaying InAppMessage
     *
     * @param activity valid Activity object.
     */
    public static void registerForInAppMessages(Activity activity) {
        if (mActivity != null) {
            Log.w(LOG_TAG, "Possible memory leak detected! Cleaning up. ");
            // do the clean up for old activity to avoid mem leak
            unregisterForInAppMessages(mActivity);
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
        // if unregister is called with old activity when new activity is started,
        // we need to skip this call. because the clean up would already been done
        // during the registration call.
        if (activity != null && mActivity != null) {
            String oldName = mActivity.getLocalClassName();
            String newName = activity.getLocalClassName();
            if (!oldName.equals(newName)) {
                return;
            }
        }

        // clean up the dialog and activity
        dismissAndCleanupDialog();

        mDialog = null;
        mActivity = null;
    }

    public static void invokeTriggers() {
        invokeTriggers(null);
    }

    public static void invokeTriggers(InAppMessage inAppMessage) {
        if (mActivity == null) {
            Log.d(LOG_TAG, "App isn't running with an eligible Activity to display InAppMessage.");
            return;
        }

        try {
            if (inAppMessage == null) {
                inAppMessage = InAppMessageStore.getInstance(mActivity).getInAppMessage();
            }

            if (shouldDisplay(inAppMessage)) {
                boolean isSuccess = buildAndShowInAppMessage(mActivity, inAppMessage);
                if (isSuccess) {
                    logInAppDisplayTime();
                    Blueshift.getInstance(mActivity).trackInAppMessageView(inAppMessage);
                    InAppMessageStore.getInstance(mActivity).delete(inAppMessage);
                } else {
                    BlueshiftLogger.e(LOG_TAG, "InAppMessage display failed");
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private static boolean shouldDisplay(InAppMessage inAppMessage) {
        return checkIsInstantMessage(inAppMessage) || (checkInterval() && checkExpiry(inAppMessage));
    }

    private static boolean checkIsInstantMessage(InAppMessage inAppMessage) {
        return inAppMessage != null && inAppMessage.shouldShowNow();
    }

    private static boolean checkInterval() {
        boolean result = false;

        Configuration config = BlueshiftUtils.getConfiguration(mActivity);
        if (config != null) {
            long intervalMs = config.getInAppInterval();
            long lastDisplayTimeMs = getLastInAppDisplayTime();
            long diffMs = System.currentTimeMillis() - lastDisplayTimeMs;

            result = diffMs >= intervalMs;

            if (!result) {
                BlueshiftLogger.d(LOG_TAG, "Interval between In App Messages should be " + intervalMs / 1000 + " seconds.");
            }
        }

        return result;
    }

    private static boolean checkExpiry(InAppMessage inAppMessage) {
        boolean result = false;

        if (inAppMessage != null) {
            long currentTimeMs = System.currentTimeMillis();
            long expiresAtMs = inAppMessage.getExpiresAt() * 1000;
            result = currentTimeMs < expiresAtMs;

            if (!result) {
                BlueshiftLogger.d(LOG_TAG, "In App Message expired at " + expiresAtMs);
            }
        }

        return result;
    }

    private static void logInAppDisplayTime() {
        StorageUtils.saveLongInPrefStore(
                mActivity,
                PREF_FILE,
                PREF_KEY_LAST_DISPLAY_TIME,
                System.currentTimeMillis()
        );
    }

    private static long getLastInAppDisplayTime() {
        return StorageUtils.getLongFromPrefStore(
                mActivity,
                PREF_FILE,
                PREF_KEY_LAST_DISPLAY_TIME
        );
    }

    private static boolean buildAndShowInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppTemplate inAppTemplate = inAppMessage.getTemplate();
            if (inAppTemplate != null) {
                switch (inAppTemplate) {
                    case HTML:
                        return buildAndShowHtmlInAppMessage(context, inAppMessage);
                    case MODAL:
                        return buildAndShowCenterPopupInAppMessage(context, inAppMessage);
                    case SLIDE_IN_BANNER:
                        return buildAndShowSlidingBannerInAppMessage(context, inAppMessage);
                }
            }
        }

        return false;
    }

    private static boolean buildAndShowCenterPopupInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                @Override
                public void onCloseButtonClick(InAppMessage inAppMessage) {
                    invokeCloseButtonClick(inAppMessage);
                }

                @Override
                public void onDismiss(InAppMessage inAppMessage) {
                    invokeDismissButtonClick(inAppMessage);
                }
            };

            return displayInAppDialog(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowFullScreenPopupInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                @Override
                public void onCloseButtonClick(InAppMessage inAppMessage) {
                    invokeCloseButtonClick(inAppMessage);
                }

                @Override
                public void onDismiss(InAppMessage inAppMessage) {
                    invokeDismissButtonClick(inAppMessage);
                }
            };

            return displayInAppDialog(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowSlidingBannerInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewBanner inAppMessageViewBanner = new InAppMessageViewBanner(context, inAppMessage) {
                @Override
                public void onCloseButtonClick(InAppMessage inAppMessage) {
                    invokeCloseButtonClick(inAppMessage);
                }

                @Override
                public void onDismiss(InAppMessage inAppMessage) {
                    invokeDismissButtonClick(inAppMessage);
                }
            };

            return displayInAppDialogAnimated(context, inAppMessageViewBanner, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowHtmlInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewHTML inAppMessageViewHTML = new InAppMessageViewHTML(context, inAppMessage) {
                @Override
                public void onCloseButtonClick(InAppMessage inAppMessage) {
                    invokeCloseButtonClick(inAppMessage);
                }

                @Override
                public void onDismiss(InAppMessage inAppMessage) {
                    invokeDismissButtonClick(inAppMessage);
                }
            };

            return displayInAppDialog(context, inAppMessageViewHTML, inAppMessage);
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

    private static boolean displayInAppDialog(Context context, View customView, InAppMessage inAppMessage) {
        if (InAppUtils.isTemplateFullScreen(inAppMessage)) {
            return displayInAppDialogFullScreen(context, customView);
        } else {
            return displayInAppDialogModal(context, customView);
        }
    }

    private static boolean displayInAppDialogModal(Context context, View customView) {
        if (isOurAppRunning(context)) {
            // todo: check with aswani if this is the right way to do this. should we skip the current dialog and display the new one or not?
            dismissAndCleanupDialog();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(customView);

            mDialog = builder.create();
            mDialog.show();
            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    private static boolean displayInAppDialogFullScreen(Context context, View customView) {
        if (isOurAppRunning(context)) {
            // todo: check with aswani if this is the right way to do this. should we skip the current dialog and display the new one or not?
            dismissAndCleanupDialog();

            AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_NoTitleBar_Fullscreen);
            builder.setView(customView);

            mDialog = builder.create();
            mDialog.show();
            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    private static boolean displayInAppDialogAnimated(Context context, View customView, InAppMessage inAppMessage) {
        if (isOurAppRunning(context)) {
            // todo: check with aswani if this is the right way to do this. should we skip the current dialog and display the new one or not?
            dismissAndCleanupDialog();

            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialogStyleSlideFromLeft);
            builder.setView(customView);
            mDialog = builder.create();
            mDialog.show();

            Window window = mDialog.getWindow();
            if (window != null) {
                window.setGravity(InAppUtils.getTemplateGravity(inAppMessage));
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }

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

    private static void invokeCloseButtonClick(InAppMessage inAppMessage) {
        dismissAndCleanupDialog();

        // track the click todo: decide event name
        Blueshift.getInstance(mActivity).trackInAppMessageClick("close", inAppMessage);
    }

    private static void invokeDismissButtonClick(InAppMessage inAppMessage) {
        dismissAndCleanupDialog();

        // track the click todo: decide event name
        Blueshift.getInstance(mActivity).trackInAppMessageClick("dismiss", inAppMessage);
    }

    private static void dismissAndCleanupDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
