package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.InAppUtils;
import com.blueshift.util.NetworkUtils;
import com.blueshift.util.StorageUtils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static void invokeTriggers(final InAppMessage inAppMessage) {
        if (mActivity == null) {
            Log.d(LOG_TAG, "App isn't running with an eligible Activity to display InAppMessage.");
            return;
        }

        try {
            BlueshiftExecutor.getInstance().runOnDiskIOThread(new Runnable() {
                @Override
                public void run() {
                    InAppMessage input = inAppMessage;
                    if (input == null) {
                        input = InAppMessageStore.getInstance(mActivity).getInAppMessage();
                    }

                    if (input == null) {
                        // this means, there are no pending in-app messages in the db.
                        return;
                    }

                    if (input.isExpired()) {
                        // delete expired one
                        InAppMessageStore.getInstance(mActivity).delete(input);
                        // pick next
                        input = InAppMessageStore.getInstance(mActivity).getInAppMessage();
                    }

                    if (shouldDisplay(input)) {
                        boolean isSuccess = buildAndShowInAppMessage(mActivity, input);
                        if (!isSuccess) {
                            BlueshiftLogger.e(LOG_TAG, "InAppMessage display failed");
                        }
                    } else {
                        // put back as new entry
                        InAppMessageStore.getInstance(mActivity).insert(input);
                        // delete from top
                        InAppMessageStore.getInstance(mActivity).delete(input);

                        // check for next in-app message after interval.
                        Configuration config = BlueshiftUtils.getConfiguration(mActivity);
                        if (config != null && config.getInAppInterval() > 0) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    invokeTriggers();
                                }
                            }, config.getInAppInterval());
                        }
                    }
                }
            });
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private static boolean shouldDisplay(InAppMessage inAppMessage) {
        return checkIsInstantMessage(inAppMessage) || (checkInterval() && checkDisplayTimeInterval(inAppMessage));
    }

    private static boolean checkDisplayTimeInterval(InAppMessage inAppMessage) {
        return checkDisplayFrom(inAppMessage) && checkExpiry(inAppMessage);
    }

    private static boolean checkDisplayFrom(InAppMessage inAppMessage) {
        try {
            if (inAppMessage != null) {
                long currentTime = System.currentTimeMillis();
                long displayFrom = inAppMessage.getDisplayFromMillis();
                return currentTime > displayFrom;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return false;
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
                    case RATING:
                        return buildAndShowRatingInAppMessage(context, inAppMessage);
                }
            }
        }

        return false;
    }

    private static boolean buildAndShowCenterPopupInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, String elementName) {
                    invokeDismissButtonClick(inAppMessage, elementName);
                }
            };

            return displayInAppDialog(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowFullScreenPopupInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, String elementName) {
                    invokeDismissButtonClick(inAppMessage, elementName);
                }
            };

            return displayInAppDialog(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowSlidingBannerInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewBanner inAppMessageViewBanner = new InAppMessageViewBanner(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, String elementName) {
                    invokeDismissButtonClick(inAppMessage, elementName);
                }
            };

            return displayInAppDialogAnimated(context, inAppMessageViewBanner, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowRatingInAppMessage(Context context, InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            InAppMessageViewRating inAppMessageViewRating = new InAppMessageViewRating(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, String elementName) {
                    invokeDismissButtonClick(inAppMessage, elementName);
                }
            };

            return displayInAppDialog(context, inAppMessageViewRating, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowHtmlInAppMessage(final Context context, final InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            BlueshiftExecutor.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    InAppMessageViewHTML inAppMessageViewHTML = new InAppMessageViewHTML(context, inAppMessage) {
                        @Override
                        public void onDismiss(InAppMessage inAppMessage, String elementName) {
                            invokeDismissButtonClick(inAppMessage, elementName);
                        }
                    };

                    displayInAppDialog(context, inAppMessageViewHTML, inAppMessage);
                }
            });

            return true;
        }

        return false;
    }

    private static boolean displayInAppDialog(Context context, View customView, InAppMessage inAppMessage) {
        if (InAppUtils.isTemplateFullScreen(inAppMessage)) {
            return displayInAppDialogFullScreen(context, customView, inAppMessage);
        } else {
            return displayInAppDialogModal(context, customView, inAppMessage);
        }
    }

    private static boolean displayInAppDialogModal(final Context context, final View customView, final InAppMessage inAppMessage) {
        if (isOurAppRunning(context)) {
            buildAndShowAlertDialog(context, inAppMessage, customView, R.style.dialogStyleInApp);

            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    private static boolean displayInAppDialogFullScreen(final Context context, final View customView, final InAppMessage inAppMessage) {
        if (isOurAppRunning(context)) {
            buildAndShowAlertDialog(context, inAppMessage, customView, android.R.style.Theme_NoTitleBar_Fullscreen);

            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    private static boolean displayInAppDialogAnimated(final Context context, final View customView, final InAppMessage inAppMessage) {
        if (isOurAppRunning(context)) {
            buildAndShowAlertDialog(context, inAppMessage, customView, R.style.inAppSlideFromLeft);

            return true;
        } else {
            Log.d(LOG_TAG, "App isn't running. Skipping InAppMessage!" + context.getPackageName());
            return false;
        }
    }

    public static boolean isOurAppRunning(Context context) {
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

    private static void invokeDismissButtonClick(InAppMessage inAppMessage, String elementName) {
        dismissAndCleanupDialog();

        // track the click todo: decide event name
        Blueshift.getInstance(mActivity).trackInAppMessageClick(inAppMessage, elementName);
    }

    private static void invokeOnInAppViewed(InAppMessage inAppMessage) {
        // log display time
        logInAppDisplayTime();
        // send stats
        Blueshift.getInstance(mActivity).trackInAppMessageView(inAppMessage);
        // cleanup db
        InAppMessageStore.getInstance(mActivity).delete(inAppMessage);
    }

    private static void dismissAndCleanupDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private static void buildAndShowAlertDialog(final Context context, final InAppMessage inAppMessage,
                                                final View content, final int theme) {
        BlueshiftExecutor.getInstance().runOnMainThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mDialog == null || !mDialog.isShowing()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context, theme);
                            builder.setView(content);
                            mDialog = builder.create();
                            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    InAppManager.clearCachedAssets(inAppMessage, context);
                                }
                            });
                            mDialog.show();

                            Window window = mDialog.getWindow();
                            if (window != null) {
                                window.setGravity(InAppUtils.getTemplateGravity(inAppMessage));
                                window.setLayout(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                            }

                            invokeOnInAppViewed(inAppMessage);
                        }
                    }
                }
        );
    }

    private static void clearCachedAssets(InAppMessage inAppMessage, Context context) {
        if (inAppMessage != null && context != null) {
            //noinspection StatementWithEmptyBody
            if (InAppConstants.HTML.equals(inAppMessage.getType())) {
                // do nothing. cache is managed by WebView
            } else {
                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    deleteCachedImage(context, bannerImage);
                }
            }
        }
    }

    public static void cacheAssets(final InAppMessage inAppMessage, final Context context) {
        if (inAppMessage != null) {
            if (InAppConstants.HTML.equals(inAppMessage.getType())) {
                // html, cache inside WebView
                BlueshiftExecutor.getInstance().runOnMainThread(new Runnable() {
                    @SuppressLint("SetJavaScriptEnabled")
                    @Override
                    public void run() {
                        String htmlContent = inAppMessage.getContentString(InAppConstants.HTML);
                        if (!TextUtils.isDigitsOnly(htmlContent)) {
                            WebView webView = new WebView(context);

                            // taking consent from dev to enable js
                            Configuration config = BlueshiftUtils.getConfiguration(context);
                            if (config != null && config.isInAppEnableJavascript()) {
                                webView.getSettings().setJavaScriptEnabled(true);
                            }

                            webView.loadData(htmlContent, "text/html; charset=UTF-8", null);
                        }
                    }
                });
            } else {
                // cache for modals and other templates

                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    cacheImage(context, bannerImage);
                }
            }
        }
    }

    private static void deleteCachedImage(Context context, String url) {
        if (context != null && url != null) {
            File imgFile = getCachedImageFile(context, url);
            if (imgFile != null && imgFile.exists()) {
                Log.d(LOG_TAG, "Image delete " + (imgFile.delete() ? "success. " : "failed. ")
                        + imgFile.getAbsolutePath());
            }
        }
    }

    private static void cacheImage(final Context context, final String url) {
        final File file = getCachedImageFile(context, url);

        if (file != null) {
            BlueshiftExecutor.getInstance().runOnNetworkThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean success = NetworkUtils.downloadFile(url, file.getAbsolutePath());
                        Log.d(LOG_TAG, "Download " + (success ? "success!" : "failed!"));
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                }
            });
        }
    }

    private static String getCachedImageFileName(String url) {
        String emailHash = "";

        if (!TextUtils.isEmpty(url)) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(url.getBytes());
                byte[] byteArray = md5.digest();

                StringBuilder sb = new StringBuilder();
                for (byte data : byteArray) {
                    sb.append(Integer.toString((data & 0xff) + 0x100, 16).substring(1));
                }

                emailHash = sb.toString();

                // Log.d("email-md5", emailHash);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return emailHash;
    }

    public static File getCachedImageFile(Context context, String url) {
        File imageCacheDir = getImageCacheDir(context);
        if (imageCacheDir != null && imageCacheDir.exists()) {
            String fileName = getCachedImageFileName(url);
            File imgFile = new File(imageCacheDir, fileName);
            Log.d(LOG_TAG, "Image file name. Remote: " + url + ", Local: " + imgFile.getAbsolutePath());
            return imgFile;
        } else {
            return null;
        }
    }

    private static File getImageCacheDir(Context context) {
        File imagesDir = null;

        if (context != null) {
            File filesDir = context.getFilesDir();
            imagesDir = new File(filesDir, "images");
            if (!imagesDir.exists()) {
                boolean val = imagesDir.mkdirs();
                if (val) {
                    Log.d(LOG_TAG, "Directory created! " + imagesDir.getAbsolutePath());
                } else {
                    Log.d(LOG_TAG, "Could not create dir.");
                }
            }
        }

        return imagesDir;
    }
}
