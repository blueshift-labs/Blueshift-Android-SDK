package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;

import com.blueshift.BlueshiftAttributesApp;
import com.blueshift.BlueshiftAttributesUser;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftHttpManager;
import com.blueshift.BlueshiftHttpRequest;
import com.blueshift.BlueshiftHttpResponse;
import com.blueshift.BlueshiftImageCache;
import com.blueshift.BlueshiftJSONObject;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.Message;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppManager {
    private static final String LOG_TAG = InAppManager.class.getSimpleName();
    private static final int MAX_HEIGHT_PERCENTAGE = 90;
    private static final int MAX_WIDTH_PERCENTAGE = 90;

    static class IAMDisplayConfig {
        String screenName;
        int templateWidth;
        int templateHeight;
        int templateMarginLeft;
        int templateMarginTop;
        int templateMarginRight;
        int templateMarginBottom;

        IAMDisplayConfig() {
            reset();
        }

        void reset() {
            screenName = null;
            templateWidth = 0;
            templateHeight = 0;
            templateMarginLeft = 0;
            templateMarginTop = 0;
            templateMarginRight = 0;
            templateMarginBottom = 0;
        }
    }

    @SuppressLint("StaticFieldLeak") // cleanup happens when unregisterForInAppMessages() is called.
    private static Activity mActivity = null;
    private static AlertDialog mDialog = null;
    private static InAppActionCallback mActionCallback = null;
    private static InAppMessage mInApp = null;
    private static String mInAppOngoingIn = null; // activity class name
    private static final IAMDisplayConfig displayConfig = new IAMDisplayConfig();

    /**
     * Calling this method makes the activity eligible for displaying InAppMessage
     *
     * @param activity Valid Activity object.
     */
    public static void registerForInAppMessages(Activity activity) {
        registerForInAppMessages(activity, activityClassCanonicalName(activity));
    }

    /**
     * Calling this method makes the activity eligible for displaying InAppMessage.
     * It also takes in a unique screen name to be used instead of the activity class name.
     *
     * @param activity   Valid Activity object.
     * @param screenName The screen name in which the in-app should be displayed
     */
    public static void registerForInAppMessages(Activity activity, String screenName) {
        if (mActivity != null) {
            // BlueshiftLogger.w(LOG_TAG, "Possible memory leak detected! Cleaning up. ");
            // do the clean up for old activity to avoid mem leak
            unregisterForInAppMessages(mActivity);
        }

        mActivity = activity;
        displayConfig.screenName = screenName != null ? screenName : activityClassCanonicalName(activity);

        logScreen("REGISTER");

        // check if there is an ongoing in-app display (orientation change)
        // if found, display the cached in-app message.
        if (isOngoingInAppMessagePresent()) {
            displayCachedOngoingInApp();
        } else {
            invokeTriggerWithinSdk();
        }
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
                // we don't need to re-display the in-app if we were moved to a new page
                // so we should clean the cached in-app message
                cleanUpOngoingInAppCache();
                return;
            }
        }

        logScreen("UNREGISTER");

        // unregistering when in-app is in display, I am assuming this will only happen if
        // you are moving to a new page with the in-app in display. or you are changing the
        // screen orientation and unregister got called automatically. for re-display, we
        // need to cache the in-app message in display.
        if (mDialog != null && mDialog.isShowing()) {
            cacheOngoingInApp();
        }

        // clean up the dialog and activity
        dismissAndCleanupDialog();

        mDialog = null;
        mActivity = null;
        displayConfig.reset();
    }

    private static void logScreen(String action) {
        BlueshiftLogger.d(LOG_TAG, action + " { screen: " + displayConfig.screenName + ", activity: " + activityClassCanonicalName(mActivity) + " }");
    }

    private static String activityClassCanonicalName(Activity activity) {
        return activity != null ? activity.getClass().getCanonicalName() : "";
    }

    private static void displayCachedOngoingInApp() {
        displayInAppMessage(mInApp);
    }

    private static void cleanUpOngoingInAppCache() {
        mInApp = null;
        mInAppOngoingIn = null;
    }

    private static boolean isOngoingInAppMessagePresent() {
        return mInAppOngoingIn != null
                && mActivity != null
                && mInAppOngoingIn.equals(mActivity.getClass().getName());
    }

    private static void cacheOngoingInApp() {
        if (mActivity != null) {
            mInAppOngoingIn = mActivity.getClass().getName();
        }
    }

    public static InAppActionCallback getActionCallback() {
        return mActionCallback;
    }

    public static void setActionCallback(InAppActionCallback callback) {
        mActionCallback = callback;
    }

    public static void fetchInAppFromServer(final Context context, final InAppApiCallback callback) {
        boolean isEnabled = BlueshiftUtils.isOptedInForInAppMessages(context);
        if (isEnabled) {
            BlueshiftExecutor.getInstance().runOnNetworkThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String apiKey = BlueshiftUtils.getApiKey(context);
                                JSONObject requestBody = generateInAppMessageAPIRequestPayload(context);

                                if (apiKey != null && requestBody != null) {
                                    BlueshiftHttpRequest.Builder builder = new BlueshiftHttpRequest.Builder()
                                            .setUrl(BlueshiftConstants.IN_APP_API_URL(context))
                                            .setMethod(BlueshiftHttpRequest.Method.POST)
                                            .addBasicAuth(apiKey, "")
                                            .setReqBodyJson(requestBody);

                                    BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(builder.build());
                                    int statusCode = response.getCode();
                                    String responseBody = response.getBody();

                                    if (statusCode == 200) {
                                        handleInAppMessageAPIResponse(context, responseBody);
                                        invokeApiSuccessCallback(callback);
                                    } else {
                                        invokeApiFailureCallback(callback, statusCode, responseBody);
                                    }
                                } else {
                                    invokeApiFailureCallback(callback, 0, "Could not make the API call.");
                                }
                            } catch (Exception e) {
                                BlueshiftLogger.e(LOG_TAG, e);
                                invokeApiFailureCallback(callback, 0, e.getMessage());
                            }
                        }
                    }
            );
        } else {
            BlueshiftLogger.w(LOG_TAG, "In-app is opted-out. Can not fetch in-app messages from API.");
        }
    }

    private static void invokeApiSuccessCallback(final InAppApiCallback callback) {
        if (callback != null) {
            BlueshiftExecutor.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            });
        }
    }

    private static void invokeApiFailureCallback(final InAppApiCallback callback, final int code, final String message) {
        if (callback != null) {
            BlueshiftExecutor.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    callback.onFailure(code, message);
                }
            });
        }
    }

    /**
     * This method is a helper for getting the request body expected by the in-app API end-point
     * on Blueshift server-side.
     * <p>
     * The host app should call this method from a worker thread as this method involves db access
     * and advertising id requests (depending on device_id source).
     *
     * @param context a valid context object
     * @return valid JSONObject filled with params, null if any error happens in getting params.
     */
    @WorkerThread
    public static JSONObject generateInAppMessageAPIRequestPayload(Context context) {
        try {
            BlueshiftJSONObject params = new BlueshiftJSONObject();

            JSONObject userAttributes = BlueshiftAttributesUser.getInstance().sync(context);
            params.putAll(userAttributes);

            JSONObject appAttributes = BlueshiftAttributesApp.getInstance().sync(context);
            params.putAll(appAttributes);

            // api key
            String apiKey = BlueshiftUtils.getApiKey(context);
            params.put(BlueshiftConstants.KEY_API_KEY, apiKey != null ? apiKey : "");

            String msgUUID = null;
            String timestamp = null;

            InAppMessageStore store = InAppMessageStore.getInstance(context);
            InAppMessage inAppMessage = store != null ? store.getLastInAppMessage() : null;
            if (inAppMessage != null) {
                msgUUID = inAppMessage.getMessageUuid();
                timestamp = inAppMessage.getTimestamp();
            }

            // message uuid
            params.put(Message.EXTRA_BSFT_MESSAGE_UUID, msgUUID != null ? msgUUID : "");

            // timestamp
            params.put(BlueshiftConstants.KEY_LAST_TIMESTAMP, timestamp != null ? timestamp : 0);

            return params;
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);

            return null;
        }
    }

    /**
     * This method can accept the in-app API response (JSON) and decode in-app messages from it.
     * The decoded in-app messages will be inserted into the database and displayed to the user.
     *
     * @param context     valid context object
     * @param apiResponse valid API response in JSON format
     */
    public static void handleInAppMessageAPIResponse(Context context, String apiResponse) {
        if (context != null && apiResponse != null && !apiResponse.isEmpty()) {
            JSONArray messages = decodeResponse(apiResponse);
            if (messages != null) onInAppMessageArrayReceived(context, messages);
        } else {
            BlueshiftLogger.d(LOG_TAG, "The context is null or the in-app API response is null or empty.");
        }
    }

    private static JSONArray decodeResponse(String response) {
        JSONArray jsonArray = null;

        try {
            if (response != null) {
                JSONObject responseJson = new JSONObject(response);
                jsonArray = responseJson.optJSONArray("content");
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return jsonArray;
    }

    private static void onInAppMessageArrayReceived(Context context, JSONArray inAppContentArray) {
        try {
            if (inAppContentArray != null) {
                int len = inAppContentArray.length();
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        JSONObject content = inAppContentArray.getJSONObject(i);
                        if (content != null) {
                            JSONObject inApp = content.optJSONObject("data");
                            if (inApp != null) {
                                InAppMessage message = InAppMessage.getInstance(inApp);
                                InAppManager.onInAppMessageReceived(context, message);
                            }
                        }
                    }

                    InAppManager.invokeTriggerWithinSdk();
                } else {
                    BlueshiftLogger.d(LOG_TAG, "No items found inside 'content'.");
                }
            } else {
                BlueshiftLogger.d(LOG_TAG, "'content' is NULL.");
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * This method accepts one in-app message instance from the host app for storing.
     *
     * @param context      valid context object
     * @param inAppMessage valid inAppMessage object
     */
    public static void onInAppMessageReceived(Context context, InAppMessage inAppMessage) {
        boolean isEnabled = BlueshiftUtils.isOptedInForInAppMessages(context);
        if (isEnabled) {
            BlueshiftLogger.d(LOG_TAG, "In-app message received. Message UUID: " + (inAppMessage != null ? inAppMessage.getMessageUuid() : null));

            if (inAppMessage != null) {
                if (!inAppMessage.isExpired()) {
                    InAppMessageStore store = InAppMessageStore.getInstance(context);
                    if (store != null) {
                        boolean inserted = store.insert(inAppMessage);
                        if (inserted) {
                            InAppUtils.invokeInAppDelivered(context, inAppMessage);
                        } else {
                            BlueshiftLogger.d(LOG_TAG, "Possible duplicate in-app received. Skipping! Message UUID: " + inAppMessage.getMessageUuid());
                        }
                    } else {
                        BlueshiftLogger.w(LOG_TAG, "Could not open the database. Dropping the in-app message. Message UUID: " + inAppMessage.getMessageUuid());
                    }
                } else {
                    BlueshiftLogger.d(LOG_TAG, "Expired in-app received. Message UUID: " + inAppMessage.getMessageUuid());
                }
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "In-app is opted-out. Can not accept in-app messages.");
        }
    }

    /**
     * This method checks if the dev has enabled the manual triggering of in-app
     * and then decides if we should call the invokeTrigger or not based on that
     */
    public static void invokeTriggerWithinSdk() {
        if (mActivity != null) {
            Configuration config = BlueshiftUtils.getConfiguration(mActivity);
            if (config != null && !config.isInAppManualTriggerEnabled()) {
                invokeTriggers();
            }
        }
    }

    public static void invokeTriggers() {
        if (mActivity == null) {
            BlueshiftLogger.d(LOG_TAG, "App isn't running with an eligible Activity to display InAppMessage.");
            return;
        }

        boolean isEnabled = BlueshiftUtils.isOptedInForInAppMessages(mActivity);
        if (isEnabled) {
            try {
                BlueshiftExecutor.getInstance().runOnDiskIOThread(new Runnable() {
                    @Override
                    public void run() {
                        InAppMessageStore store = InAppMessageStore.getInstance(mActivity);
                        if (store != null) {
                            InAppMessage input = store.getInAppMessage(mActivity, displayConfig.screenName);

                            if (input == null) {
                                BlueshiftLogger.d(LOG_TAG, "No pending in-app messages found.");
                                return;
                            }

                            if (!validate(input)) {
                                BlueshiftLogger.d(LOG_TAG, "Invalid in-app messages found. Message UUID: " + input.getMessageUuid());
                                return;
                            }

                            displayInAppMessage(input);
                        }
                    }
                });
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "In-app opted-out. Can not display in-app messages.");
        }
    }

    private static void scheduleNextInAppMessage(final Context context) {
        try {
            Configuration config = BlueshiftUtils.getConfiguration(context);
            if (config != null) {
                long delay = config.getInAppInterval();
                BlueshiftLogger.d(LOG_TAG, "Scheduling next in-app in " + delay + "ms");
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InAppManager.invokeTriggerWithinSdk();
                    }
                }, delay);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private static void displayInAppMessage(final InAppMessage inAppMessage) {
        if (inAppMessage != null && mActivity != null) {
            cacheAssets(inAppMessage, mActivity.getApplicationContext());

            prepareTemplateSize(mActivity, inAppMessage);
            prepareTemplateMargins(mActivity, inAppMessage);

            showInAppOnMainThread(inAppMessage);
        } else {
            BlueshiftLogger.e(LOG_TAG, "InApp message or mActivity is null!");
        }
    }

    private static void prepareTemplateMargins(Context context, InAppMessage inAppMessage) {
        Rect margins = inAppMessage.getTemplateMargin(context);
        if (margins != null) {
            displayConfig.templateMarginLeft = CommonUtils.dpToPx(margins.left, context);
            displayConfig.templateMarginTop = CommonUtils.dpToPx(margins.top, context);
            displayConfig.templateMarginRight = CommonUtils.dpToPx(margins.right, context);
            displayConfig.templateMarginBottom = CommonUtils.dpToPx(margins.bottom, context);
        }
    }

    private static void prepareTemplateSize(Context context, InAppMessage inAppMessage) {
        if (context == null || inAppMessage == null) return;

        // Calculate the space consumed by the status bar
        int topMargin = (int) (24 * context.getResources().getDisplayMetrics().density);

        double maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        double maxHeight = context.getResources().getDisplayMetrics().heightPixels - topMargin;

        BlueshiftLogger.d(LOG_TAG, "Screen w= " + maxWidth + ", h= " + maxHeight);

        int width, height;

        // Read the width % and height % available in the payload
        int widthPercentage = InAppUtils.getTemplateStyleInt(context, inAppMessage, InAppConstants.WIDTH, -1);
        int heightPercentage = InAppUtils.getTemplateStyleInt(context, inAppMessage, InAppConstants.HEIGHT, -1);

        BlueshiftLogger.d(LOG_TAG, "Template w= " + widthPercentage + " %, h= " + heightPercentage + " %");

        String url = InAppUtils.getTemplateStyleString(context, inAppMessage, InAppConstants.BACKGROUND_IMAGE);
        Bitmap bitmap;
        if (url != null && (bitmap = BlueshiftImageCache.getBitmap(context, url)) != null) {
            // we have a background image! The following code will set the width and height
            // based on the aspect ratio of the image available. The width and height % provided
            // in the payload will still be respected. The in-app will take maximum of that %
            // of the screen to render itself.

            double iWidth = bitmap.getWidth();
            double iHeight = bitmap.getHeight();
            double iRatio = iHeight / iWidth;

            if (widthPercentage < 0 && heightPercentage < 0) {
                // No dimension provided in the template. The in-app dimensions should be
                // calculated based on the background image's dimensions and aspect ratio.

                widthPercentage = MAX_WIDTH_PERCENTAGE;
                heightPercentage = MAX_HEIGHT_PERCENTAGE;

                maxWidth = (maxWidth * widthPercentage) / 100;
                maxHeight = (maxHeight * heightPercentage) / 100;

                int iWidthPx = CommonUtils.dpToPx((int) iWidth, context);
                int iHeightPx = CommonUtils.dpToPx((int) iHeight, context);

                if (iWidthPx < maxWidth && iHeightPx < maxHeight) {
                    width = iWidthPx;
                    height = iHeightPx;
                } else {
                    width = (int) maxWidth;
                    height = (int) (maxWidth * iRatio);

                    if (height > maxHeight) {
                        width = (int) (maxHeight / iRatio);
                        height = (int) maxHeight;
                    }
                }

                BlueshiftLogger.d(LOG_TAG, "Automatic (FULL) maxW= " + maxWidth + "px, maxH= " + maxHeight + "px");
                BlueshiftLogger.d(LOG_TAG, "Automatic (FULL) w= " + width + "px, h= " + height + "px");
            } else {
                // We have dimensions provided in the payload. Based on the provided values,
                // and the background image and it's aspect ratio, we need to calculate the
                // dimensions for the in-app messages.

                // ** Backward compatible approach **
                // If we have both height and width provided, we will respect the width and
                // calculate the height to keep the aspect ratio.

                if (widthPercentage > 0) {
                    // when widthPercentage is more than zero there are two possible options for
                    // heightPercentage.
                    // a. heightPercentage can be -1 (automatic)
                    // b. heightPercentage can be a positive value (ex; 80%)
                    // in both these cases, we treat them as same. We will generate the height
                    // regardless what is mentioned as heightPercentage.

                    if (widthPercentage != 100) {
                        // let's not add margin if the user wish to have 100% size
                        maxHeight = (maxHeight * MAX_HEIGHT_PERCENTAGE) / 100;
                    }

                    maxWidth = (maxWidth * widthPercentage) / 100;

                    width = (int) maxWidth;
                    height = (int) (width * iRatio);

                    if (height > maxHeight) {
                        BlueshiftLogger.d(LOG_TAG, "Height overflow! Recalculate size.");

                        width = (int) (maxHeight / iRatio);
                        height = (int) maxHeight;
                    }
                } else {
                    // We come here only when heightPercentage > 0 and widthPercentage = -1 (auto)
                    // Now we need to calculate the width based on the image's aspect ration.
                    maxHeight = (maxHeight * heightPercentage) / 100;

                    if (heightPercentage != 100) {
                        // let's not add margin if the user wish to have 100% size
                        maxWidth = (maxWidth * MAX_WIDTH_PERCENTAGE) / 100;
                    }

                    height = (int) maxHeight;
                    width = (int) (height / iRatio);

                    if (width > maxWidth) {
                        BlueshiftLogger.d(LOG_TAG, "Width overflow! Recalculate size.");

                        width = (int) maxWidth;
                        height = (int) (width * iRatio);
                    }
                }

                BlueshiftLogger.d(LOG_TAG, "Automatic (SEMI) maxW= " + maxWidth + "px, maxH= " + maxHeight + "px");
                BlueshiftLogger.d(LOG_TAG, "Automatic (SEMI) w= " + width + "px, h= " + height + "px");
            }
        } else {
            if (widthPercentage < 0) {
                width = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                width = (int) ((maxWidth * widthPercentage) / 100);
            }

            if (heightPercentage < 0) {
                height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                height = (int) ((maxHeight * heightPercentage) / 100);
            }

            BlueshiftLogger.d(LOG_TAG, "Template size: w=" + width + "px, h=" + height + "px");
        }

        displayConfig.templateWidth = width;
        displayConfig.templateHeight = height;
    }

    private static void showInAppOnMainThread(final InAppMessage inAppMessage) {
        BlueshiftExecutor.getInstance().runOnMainThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity != null) {
                            boolean isSuccess = buildAndShowInAppMessage(mActivity, inAppMessage);
                            if (isSuccess) {
                                BlueshiftLogger.d(LOG_TAG, "InApp message displayed successfully!");
                                mInApp = inAppMessage;
                                markAsDisplayed(inAppMessage);
                            }
                        } else {
                            BlueshiftLogger.e(LOG_TAG, "No activity is running, skipping in-app display.");
                        }
                    }
                }
        );
    }

    private static void markAsDisplayed(final InAppMessage input) {
        BlueshiftExecutor.getInstance().runOnDiskIOThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (input != null && mActivity != null) {
                            input.setDisplayedAt(System.currentTimeMillis());
                            InAppMessageStore store = InAppMessageStore.getInstance(mActivity);
                            if (store != null) store.update(input);
                        }
                    }
                }
        );
    }

    private static boolean validate(InAppMessage inAppMessage) {
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

            InAppMessageStore store = InAppMessageStore.getInstance(mActivity);
            long lastDisplayedAt = store != null ? store.getLastDisplayedAt() : 0;

            BlueshiftLogger.d(LOG_TAG, "Last In App Message was displayed at " + CommonUtils.formatMilliseconds(lastDisplayedAt));

            long diffMs = System.currentTimeMillis() - lastDisplayedAt;
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

    public static boolean buildAndShowInAppMessage(Context context, InAppMessage inAppMessage) {
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
        if (context != null && inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                @Override
                public void handleClick(InAppMessage inAppMessage, JSONObject params) {
                    super.handleClick(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }

                @Override
                public void handleDismiss(InAppMessage inAppMessage, JSONObject params) {
                    super.handleDismiss(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }
            };

            return displayInAppDialogModal(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowRatingInAppMessage(Context context, InAppMessage inAppMessage) {
        if (context != null && inAppMessage != null) {
            InAppMessageViewRating inAppMessageViewRating = new InAppMessageViewRating(context, inAppMessage) {
                @Override
                public void handleClick(InAppMessage inAppMessage, JSONObject params) {
                    super.handleClick(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }

                @Override
                public void handleDismiss(InAppMessage inAppMessage, JSONObject params) {
                    super.handleDismiss(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }
            };

            return displayInAppDialogModal(context, inAppMessageViewRating, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowHtmlInAppMessage(final Context context, final InAppMessage inAppMessage) {
        if (context != null && inAppMessage != null) {
            InAppMessageViewHTML inAppMessageViewHTML = new InAppMessageViewHTML(context, inAppMessage) {
                @Override
                public void handleClick(InAppMessage inAppMessage, JSONObject params) {
                    super.handleClick(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }

                @Override
                public void handleDismiss(InAppMessage inAppMessage, JSONObject params) {
                    super.handleDismiss(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }
            };

            return displayInAppDialogModal(context, inAppMessageViewHTML, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowSlidingBannerInAppMessage(Context context, InAppMessage inAppMessage) {
        if (context != null && inAppMessage != null) {
            InAppMessageViewBanner inAppMessageViewBanner = new InAppMessageViewBanner(context, inAppMessage) {
                @Override
                public void handleClick(InAppMessage inAppMessage, JSONObject params) {
                    super.handleClick(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }

                @Override
                public void handleDismiss(InAppMessage inAppMessage, JSONObject params) {
                    super.handleDismiss(inAppMessage, params);
                    invokeCleanUp(inAppMessage, params);
                }
            };

            return displayInAppDialogAnimated(context, inAppMessageViewBanner, inAppMessage);
        }

        return false;
    }

    private static boolean displayInAppDialogModal(final Context context, final View customView, final InAppMessage inAppMessage) {
        float dimAmount = (float) InAppUtils.getTemplateBackgroundDimAmount(context, inAppMessage, 0.5);
        return buildAndShowAlertDialog(context, inAppMessage, customView, R.style.dialogStyleInApp, dimAmount);
    }

    private static boolean displayInAppDialogAnimated(final Context context, final View customView, final InAppMessage inAppMessage) {
        float dimAmount = (float) InAppUtils.getTemplateBackgroundDimAmount(context, inAppMessage, 0.0);
        return buildAndShowAlertDialog(context, inAppMessage, customView, R.style.inAppSlideFromLeft, dimAmount);
    }

    private static void invokeCleanUp(InAppMessage inAppMessage, JSONObject extras) {
        // use app context to avoid leaks on this activity
        Context appContext = mActivity != null ? mActivity.getApplicationContext() : null;
        // reschedule next in-app here as the dialog callbacks are going to get removed in cleanup
        InAppManager.scheduleNextInAppMessage(appContext);
        // remove asset cache
        InAppManager.clearCachedAssets(inAppMessage, appContext);
        // clean up the ongoing in-app cache
        cleanUpOngoingInAppCache();
        // dismiss the dialog and cleanup memory
        dismissAndCleanupDialog();
    }

    private static void invokeOnInAppViewed(InAppMessage inAppMessage) {
        if (isRedundantDisplay()) return;

        // use app context to avoid leaks on this activity
        Context appContext = mActivity != null ? mActivity.getApplicationContext() : null;
        if (appContext != null) {
            // send stats
            InAppUtils.invokeInAppOpened(appContext, inAppMessage);
            // update with displayed at timing
            inAppMessage.setDisplayedAt(System.currentTimeMillis());

            InAppMessageStore store = InAppMessageStore.getInstance(appContext);
            if (store != null) store.update(inAppMessage);
        }
    }

    // checks if the display is already made and this display is duplicate.
    // this happens usually for orientation change based displays
    private static boolean isRedundantDisplay() {
        return mInAppOngoingIn != null;
    }

    private static void dismissAndCleanupDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setOnCancelListener(null);
            mDialog.setOnDismissListener(null);
            mDialog.setOnKeyListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private static View applyTemplateStyle(View view) {
        Context context = view.getContext();

        // The root view of dialog can not accept margins. hence adding a wrapper
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                displayConfig.templateWidth, displayConfig.templateHeight
        );

        lp.leftMargin = displayConfig.templateMarginLeft;
        lp.topMargin = displayConfig.templateMarginTop;
        lp.rightMargin = displayConfig.templateMarginRight;
        lp.bottomMargin = displayConfig.templateMarginBottom;

        // Fix height and width based on the margins we should provide.
        if (lp.width > 0) lp.width = lp.width - (lp.leftMargin + lp.rightMargin);
        if (lp.height > 0) lp.height = lp.height - (lp.topMargin + lp.bottomMargin);

        BlueshiftLogger.d(LOG_TAG, "Template margin: ("
                + lp.leftMargin + ", "
                + lp.topMargin + ", "
                + lp.rightMargin + ", "
                + lp.bottomMargin + ")");

        LinearLayout rootView = new LinearLayout(context);
        rootView.addView(view, lp);

        return rootView;
    }

    private static boolean canDisplayInThisScreen(InAppMessage inAppMessage) {
        String targetScreen = inAppMessage != null ? inAppMessage.getDisplayOn() : "";
        return targetScreen.isEmpty() || targetScreen.equals(displayConfig.screenName);
    }

    private static boolean buildAndShowAlertDialog(
            Context context, final InAppMessage inAppMessage, final View content, final int theme, final float dimAmount) {
        if (mActivity != null && !mActivity.isFinishing()) {
            if (mDialog == null || !mDialog.isShowing()) {
                if (canDisplayInThisScreen(inAppMessage)) {
                    final Context appContext = mActivity.getApplicationContext();

                    AlertDialog.Builder builder = new AlertDialog.Builder(context, theme);
                    builder.setView(applyTemplateStyle(content));
                    mDialog = builder.create();

                    boolean cancelOnTouchOutside = InAppUtils.shouldCancelOnTouchOutside(context, inAppMessage);
                    mDialog.setCanceledOnTouchOutside(cancelOnTouchOutside);

                    mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                JSONObject json = new JSONObject();
                                try {
                                    json.put(
                                            BlueshiftConstants.KEY_CLICK_ELEMENT,
                                            InAppConstants.ACT_BACK);
                                } catch (JSONException ignored) {
                                }
                                InAppUtils.invokeInAppDismiss(appContext, inAppMessage, json);

                                dialog.dismiss();
                                return true;
                            }

                            return false;
                        }
                    });

                    // dismiss happens when user interacts with the dialog
                    mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            BlueshiftExecutor.getInstance().runOnDiskIOThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            InAppManager.clearCachedAssets(inAppMessage, appContext);
                                            InAppManager.scheduleNextInAppMessage(appContext);
                                            InAppManager.cleanUpOngoingInAppCache();
                                        }
                                    }
                            );
                        }
                    });

                    // cancel happens when it gets cancelled by actions like tap on outside,
                    // back button press or programmatically cancelling
                    mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            JSONObject json = new JSONObject();
                            try {
                                json.put(
                                        BlueshiftConstants.KEY_CLICK_ELEMENT,
                                        InAppConstants.ACT_TAP_OUTSIDE);
                            } catch (JSONException ignored) {
                            }
                            InAppUtils.invokeInAppDismiss(appContext, inAppMessage, json);
                        }
                    });

                    try {
                        mDialog.show();
                    } catch (Exception e) {
                        BlueshiftLogger.w(LOG_TAG, "Skipping in-app message! Reason: " + e.getMessage());

                        if (mDialog != null && mDialog.isShowing()) {
                            mDialog.dismiss();
                        }

                        mDialog = null;
                        return false;
                    }

                    Window window = mDialog.getWindow();
                    if (window != null) {
                        window.setGravity(InAppUtils.getTemplateGravity(context, inAppMessage));
                        window.setDimAmount(dimAmount);
                        window.setLayout(displayConfig.templateWidth, displayConfig.templateHeight);

                        if (InAppUtils.shouldEnableBackgroundActions(context, inAppMessage)) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        }
                    }

                    BlueshiftExecutor.getInstance().runOnDiskIOThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    InAppManager.invokeOnInAppViewed(inAppMessage);
                                }
                            }
                    );

                    return true;
                } else {
                    BlueshiftLogger.w(LOG_TAG, "Skipping in-app message! Reason: We're not in the targeted screen.");
                }
            } else {
                BlueshiftLogger.w(LOG_TAG, "Skipping in-app message! Reason: We have an in-app message in display.");
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "Skipping in-app message! Reason: We don't have the app running in foreground.");
        }

        return false;
    }

    private static void clearCachedAssets(InAppMessage inAppMessage, Context context) {
        if (inAppMessage != null && context != null) {
            //noinspection StatementWithEmptyBody
            if (InAppConstants.HTML.equals(inAppMessage.getType())) {
                // do nothing. cache is managed by WebView
            } else {
                // modal with background
                // bg image: normal
                String bgImgNormal = inAppMessage.getTemplateStyle() != null
                        ? inAppMessage.getTemplateStyle().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (!TextUtils.isEmpty(bgImgNormal)) {
                    BlueshiftImageCache.clean(context, bgImgNormal);
                }

                // bg image: dark
                String bgImgDark = inAppMessage.getTemplateStyleDark() != null
                        ? inAppMessage.getTemplateStyleDark().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (bgImgDark != null && !bgImgDark.isEmpty() && !bgImgDark.equals(bgImgNormal)) {
                    BlueshiftImageCache.clean(context, bgImgDark);
                }

                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    BlueshiftImageCache.clean(context, bannerImage);
                }

                // icon image of slide-in
                String iconImage = inAppMessage.getContentString(InAppConstants.ICON_IMAGE);
                if (!TextUtils.isEmpty(iconImage)) {
                    BlueshiftImageCache.clean(context, iconImage);
                }
            }
        }
    }

    private static void cacheAssets(final InAppMessage inAppMessage, final Context context) {
        if (inAppMessage != null) {
            if (InAppConstants.HTML.equals(inAppMessage.getType())) {
                // html, cache inside WebView
                BlueshiftExecutor.getInstance().runOnMainThread(new Runnable() {
                    @SuppressLint("SetJavaScriptEnabled")
                    @Override
                    public void run() {
                        String htmlContent = inAppMessage.getContentString(InAppConstants.HTML);
                        if (!TextUtils.isEmpty(htmlContent)) {
                            WebView webView = new WebView(context);

                            // taking consent from dev to enable js
                            Configuration config = BlueshiftUtils.getConfiguration(context);
                            if (config != null && config.isJavaScriptForInAppWebViewEnabled()) {
                                webView.getSettings().setJavaScriptEnabled(true);
                            }

                            webView.loadData(CommonUtils.getBase64(htmlContent), "text/html; charset=UTF-8", "base64");
                        }
                    }
                });
            } else {
                // cache for modals and other templates

                // modal with background
                // bg image: normal
                String bgImgNormal = inAppMessage.getTemplateStyle() != null
                        ? inAppMessage.getTemplateStyle().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (!TextUtils.isEmpty(bgImgNormal)) {
                    BlueshiftImageCache.preload(context, bgImgNormal);
                }

                // bg image: dark
                String bgImgDark = inAppMessage.getTemplateStyleDark() != null
                        ? inAppMessage.getTemplateStyleDark().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (bgImgDark != null && !bgImgDark.isEmpty() && !bgImgDark.equals(bgImgNormal)) {
                    BlueshiftImageCache.preload(context, bgImgDark);
                }

                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    BlueshiftImageCache.preload(context, bannerImage);
                }

                // icon image of slide-in
                String iconImage = inAppMessage.getContentString(InAppConstants.ICON_IMAGE);
                if (!TextUtils.isEmpty(iconImage)) {
                    BlueshiftImageCache.preload(context, iconImage);
                }
            }
        }
    }
}
