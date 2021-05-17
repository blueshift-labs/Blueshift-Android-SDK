package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.blueshift.BlueshiftAttributesApp;
import com.blueshift.BlueshiftAttributesUser;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftHttpManager;
import com.blueshift.BlueshiftHttpRequest;
import com.blueshift.BlueshiftHttpResponse;
import com.blueshift.BlueshiftImgCache;
import com.blueshift.BlueshiftJSONObject;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.Message;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class InAppManager {
    private static final String LOG_TAG = InAppManager.class.getSimpleName();

    @SuppressLint("StaticFieldLeak") // cleanup happens when unregisterForInAppMessages() is called.
    private static Activity mActivity = null;
    private static String mScreen = null;
    private static AlertDialog mDialog = null;
    private static InAppActionCallback mActionCallback = null;
    private static InAppMessage mInApp = null;
    private static String mInAppOngoingIn = null; // activity class name

    /**
     * Calling this method makes the activity eligible for displaying InAppMessage
     *
     * @param activity Valid Activity object.
     */
    public static void registerForInAppMessages(Activity activity) {
        registerForInAppMessages(activity, null);
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
        mScreen = screenName;

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
        mScreen = null;
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

    /**
     * Creates a Handler with current looper.
     *
     * @param callback callback to invoke after API call
     * @return Handler object to run the callback
     */
    private static Handler getCallbackHandler(InAppApiCallback callback) {
        Handler handler = null;
        if (callback != null) {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                handler = new Handler(looper);
            }
        }

        return handler;
    }

    public static void fetchInAppFromServer(final Context context, final InAppApiCallback callback) {
        boolean isEnabled = BlueshiftUtils.isOptedInForInAppMessages(context);
        if (isEnabled) {
            final Handler callbackHandler = getCallbackHandler(callback);
            BlueshiftExecutor.getInstance().runOnNetworkThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String apiKey = BlueshiftUtils.getApiKey(context);
                                JSONObject requestBody = generateInAppMessageAPIRequestPayload(context);

                                if (apiKey != null && requestBody != null) {
                                    BlueshiftHttpRequest.Builder builder = new BlueshiftHttpRequest.Builder()
                                            .setUrl(BlueshiftConstants.IN_APP_API_URL)
                                            .setMethod(BlueshiftHttpRequest.Method.POST)
                                            .addBasicAuth(apiKey, "")
                                            .setReqBodyJson(requestBody);

                                    BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(builder.build());
                                    int statusCode = response.getCode();
                                    String responseBody = response.getBody();

                                    if (statusCode == 200) {
                                        handleInAppMessageAPIResponse(context, responseBody);

                                        invokeApiSuccessCallback(callbackHandler, callback);
                                    } else {
                                        invokeApiFailureCallback(callbackHandler, callback, statusCode, responseBody);
                                    }
                                } else {
                                    invokeApiFailureCallback(callbackHandler, callback, 0, "Could not make the API call.");
                                }
                            } catch (Exception e) {
                                BlueshiftLogger.e(LOG_TAG, e);

                                invokeApiFailureCallback(callbackHandler, callback, 0, e.getMessage());
                            }
                        }
                    }
            );
        } else {
            BlueshiftLogger.w(LOG_TAG, "In-app is opted-out. Can not fetch in-app messages from API.");
        }
    }

    private static void invokeApiSuccessCallback(Handler handler, final InAppApiCallback callback) {
        if (handler != null && callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            });
        }
    }

    private static void invokeApiFailureCallback(Handler handler, final InAppApiCallback callback,
                                                 final int errorCode, final String errorMessage) {
        if (handler != null && callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onFailure(errorCode, errorMessage);
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
                InAppUtils.invokeInAppDelivered(context, inAppMessage);

                if (!inAppMessage.isExpired()) {
                    InAppMessageStore store = InAppMessageStore.getInstance(context);
                    if (store != null) {
                        boolean inserted = store.insert(inAppMessage);
                        if (inserted) {
                            InAppManager.cacheAssets(inAppMessage, context);
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
                            InAppMessage input = store.getInAppMessage(mActivity, mScreen);

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
        if (inAppMessage != null) {
            cacheAssets(inAppMessage);
            showInAppOnMainThread(inAppMessage);
        } else {
            BlueshiftLogger.e(LOG_TAG, "InApp message is null!");
        }
    }

    private static void cacheAssets(final InAppMessage inAppMessage) {

    }

    private static void showInAppOnMainThread(final InAppMessage inAppMessage) {
        BlueshiftExecutor.getInstance().runOnMainThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity != null) {
                            boolean isSuccess = buildAndShowInAppMessage(mActivity, inAppMessage);
                            if (isSuccess) {
                                mInApp = inAppMessage;
                                markAsDisplayed(inAppMessage);
                            }

                            if (isSuccess) {
                                BlueshiftLogger.d(LOG_TAG, "InApp message displayed successfully!");
                            } else {
                                BlueshiftLogger.e(LOG_TAG, "InApp message display failed!");
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
        if (context != null && inAppMessage != null) {
            InAppMessageViewModal inAppMessageViewModal = new InAppMessageViewModal(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, JSONObject extras) {
                    invokeDismissButtonClick(inAppMessage, extras);
                }
            };

            return displayInAppDialogModal(context, inAppMessageViewModal, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowRatingInAppMessage(Context context, InAppMessage inAppMessage) {
        if (context != null && inAppMessage != null) {
            InAppMessageViewRating inAppMessageViewRating = new InAppMessageViewRating(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, JSONObject extras) {
                    invokeDismissButtonClick(inAppMessage, extras);
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
                public void onDismiss(InAppMessage inAppMessage, JSONObject extras) {
                    invokeDismissButtonClick(inAppMessage, extras);
                }
            };

            return displayInAppDialogModal(context, inAppMessageViewHTML, inAppMessage);
        }

        return false;
    }

    private static boolean buildAndShowSlidingBannerInAppMessage(Context context, InAppMessage inAppMessage) {
        if (context != null && inAppMessage != null) {
            InAppMessageViewBanner inAppMessageViewBanner = new InAppMessageViewBanner(context, inAppMessage) {
                public void onDismiss(InAppMessage inAppMessage, JSONObject extras) {
                    invokeDismissButtonClick(inAppMessage, extras);
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

    private static void invokeDismissButtonClick(InAppMessage inAppMessage, JSONObject extras) {
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
        // log the click event
        InAppUtils.invokeInAppClicked(appContext, inAppMessage, extras);
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
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private static void applyDimensionsToView(View view, int width, int height) {
        if (view != null && view.getLayoutParams() != null) {
            view.getLayoutParams().width = width;
            view.getLayoutParams().height = height;
        }
    }

    private static void adjustDimensionsForModal(final ViewGroup rootView) {
        if (rootView != null) {
            rootView.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            int modalWidth = rootView.getMeasuredWidth();
                            int modalHeight = rootView.getMeasuredHeight();

                            // image background will be at position 0 if available
                            View view = rootView.getChildAt(0);
                            if (view instanceof ImageView) {
                                // background image is present and it will fill the parent view
                                // automatically. Now let's adjust the dimensions of the parent view
                                // to match the dimensions of the dialog container.

                                if (rootView.getChildCount() > 1) {
                                    View contentView = rootView.getChildAt(1);
                                    applyDimensionsToView(contentView, modalWidth, modalHeight);
                                }
                            } else if (view instanceof ViewGroup) {
                                applyDimensionsToView(view, modalWidth, modalHeight);
                            }
                        }
                    }
            );
        }
    }

    private static View applyTemplateStyle(View view, InAppMessage inAppMessage) {
        Context context = view.getContext();

        // The root view of dialog can not accept margins. hence adding a wrapper
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Rect margins = inAppMessage.getTemplateMargin(context);
        if (margins != null) {
            lp.leftMargin = CommonUtils.dpToPx(margins.left, context);
            lp.topMargin = CommonUtils.dpToPx(margins.top, context);
            lp.rightMargin = CommonUtils.dpToPx(margins.right, context);
            lp.bottomMargin = CommonUtils.dpToPx(margins.bottom, context);
        }

        DisplayMetrics metrics = view.getResources().getDisplayMetrics();

        float wPercentage = inAppMessage.getTemplateWidth(context);
        if (wPercentage > 0) {
            int horizontalMargin = (lp.leftMargin + lp.rightMargin);
            lp.width = (int) ((metrics.widthPixels * (wPercentage / 100)) - horizontalMargin);
        } else {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        float hPercentage = inAppMessage.getTemplateHeight(context);
        if (hPercentage > 0) {
            int verticalMargin = lp.topMargin + lp.bottomMargin;
            lp.height = (int) ((metrics.heightPixels * (hPercentage / 100)) - verticalMargin);
        } else {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        LinearLayout rootView = new LinearLayout(view.getContext());
        rootView.addView(view, lp);

        if (InAppUtils.isModal(inAppMessage)) {
            if (view instanceof ViewGroup) adjustDimensionsForModal((ViewGroup) view);
        }

        return rootView;
    }

    private static boolean buildAndShowAlertDialog(
            Context context, final InAppMessage inAppMessage, final View content, final int theme, final float dimAmount) {
        if (mActivity != null && !mActivity.isFinishing()) {
            if (mDialog == null || !mDialog.isShowing()) {
                final Context appContext = mActivity.getApplicationContext();

                AlertDialog.Builder builder = new AlertDialog.Builder(context, theme);
                builder.setView(applyTemplateStyle(content, inAppMessage));
                mDialog = builder.create();

                boolean cancelOnTouchOutside = InAppUtils.shouldCancelOnTouchOutside(context, inAppMessage);
                mDialog.setCanceledOnTouchOutside(cancelOnTouchOutside);

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
                        InAppManager.scheduleNextInAppMessage(appContext);
                        InAppManager.cleanUpOngoingInAppCache();
                    }
                });

                mDialog.show();

                Window window = mDialog.getWindow();
                if (window != null) {
                    window.setGravity(InAppUtils.getTemplateGravity(context, inAppMessage));
                    window.setDimAmount(dimAmount);

                    int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    int height = LinearLayout.LayoutParams.WRAP_CONTENT;

                    if (InAppUtils.isTemplateFullScreen(context, inAppMessage)) {
                        height = LinearLayout.LayoutParams.MATCH_PARENT;
                        width = LinearLayout.LayoutParams.MATCH_PARENT;
                    }

                    window.setLayout(width, height);
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
                BlueshiftLogger.d(LOG_TAG, "Already an in-app is in display.");
            }
        } else {
            BlueshiftLogger.d(LOG_TAG, "App is not running in foreground.");
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
                    BlueshiftImgCache.clean(context, bgImgNormal);
                }

                // bg image: dark
                String bgImgDark = inAppMessage.getTemplateStyleDark() != null
                        ? inAppMessage.getTemplateStyleDark().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (bgImgDark != null && !bgImgDark.isEmpty() && !bgImgDark.equals(bgImgNormal)) {
                    BlueshiftImgCache.clean(context, bgImgDark);
                }

                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    BlueshiftImgCache.clean(context, bannerImage);
                }

                // icon image of slide-in
                String iconImage = inAppMessage.getContentString(InAppConstants.ICON_IMAGE);
                if (!TextUtils.isEmpty(iconImage)) {
                    BlueshiftImgCache.clean(context, iconImage);
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
                    BlueshiftImgCache.preload(context, bgImgNormal);
                }

                // bg image: dark
                String bgImgDark = inAppMessage.getTemplateStyleDark() != null
                        ? inAppMessage.getTemplateStyleDark().optString(InAppConstants.BACKGROUND_IMAGE)
                        : null;
                if (bgImgDark != null && !bgImgDark.isEmpty() && !bgImgDark.equals(bgImgNormal)) {
                    BlueshiftImgCache.preload(context, bgImgDark);
                }

                // modal with banner
                String bannerImage = inAppMessage.getContentString(InAppConstants.BANNER);
                if (!TextUtils.isEmpty(bannerImage)) {
                    BlueshiftImgCache.preload(context, bannerImage);
                }

                // icon image of slide-in
                String iconImage = inAppMessage.getContentString(InAppConstants.ICON_IMAGE);
                if (!TextUtils.isEmpty(iconImage)) {
                    BlueshiftImgCache.preload(context, iconImage);
                }
            }
        }
    }
}
