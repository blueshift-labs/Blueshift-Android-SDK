package com.blueshift.inappmessage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public abstract class InAppMessageView extends RelativeLayout {
    private static final String TAG = InAppMessageView.class.getSimpleName();

    private InAppMessage inAppMessage = null;

    public InAppMessageView(Context context, InAppMessage inAppMessage) {
        super(context);

        if (inAppMessage == null) return;

        this.inAppMessage = inAppMessage;

        Drawable background = InAppUtils.getTemplateBackgroundDrawable(context, inAppMessage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(background);
        } else {
            setBackgroundDrawable(background);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }

        addBackgroundImageView();

        View childView = getView(inAppMessage);
        if (childView != null) {
            addView(childView);
        }

        // We will show close button by default for the html templates and the modal template
        // that has no action button in it.
        boolean enableClose = InAppUtils.isModalWithNoActionButtons(inAppMessage) || InAppUtils.isHTML(inAppMessage);
        boolean showCloseButton = InAppUtils.shouldShowCloseButton(getContext(), inAppMessage, enableClose);
        if (showCloseButton) {
            addCloseButton(inAppMessage);
        }
    }

    private void addBackgroundImageView() {
        String url = InAppUtils.getTemplateStyleString(
                getContext(), inAppMessage, InAppConstants.BACKGROUND_IMAGE);
        if (!TextUtils.isEmpty(url)) {
            ImageView imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            addView(imageView, lp);

            InAppUtils.loadImageAsync(imageView, url);
        }
    }

    private void addCloseButton(final InAppMessage inAppMessage) {
        // default text/font size for icon is 20 SP
        // default size of the icon text view is 28 DP (8 DP padding on all sides)
        int iconHeight = InAppUtils.getCloseButtonIconTextSize(getContext(), inAppMessage) + 8; // 8dp padding
        TextView closeButtonView = InAppUtils.getCloseButtonIconTextView(getContext(), inAppMessage, iconHeight);

        int dpSize = CommonUtils.dpToPx(iconHeight, getContext());
        LayoutParams lp = new LayoutParams(dpSize, dpSize);

        int dp8 = CommonUtils.dpToPx(8, getContext());
        lp.setMargins(0, dp8, dp8, 0);

        lp.addRule(ALIGN_PARENT_TOP);
        lp.addRule(ALIGN_PARENT_RIGHT);

        closeButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleDismiss(inAppMessage, getClickStatsJSONObject(InAppConstants.BTN_CLOSE));
            }
        });

        addView(closeButtonView, lp);
    }

    public InAppMessage getInAppMessage() {
        return this.inAppMessage;
    }

    public JSONObject getClickStatsJSONObject(String element) {
        JSONObject statsParams = new JSONObject();
        try {
            if (element != null) {
                statsParams.putOpt(BlueshiftConstants.KEY_CLICK_ELEMENT, element);
            }
        } catch (JSONException e) {
            BlueshiftLogger.e(TAG, e);
        }
        return statsParams;
    }

    public void handleDismiss(InAppMessage inAppMessage, JSONObject params) {
        BlueshiftLogger.d(TAG, "handleDismiss: " + inAppMessage.getMessageUuid());
        // track dismiss
        InAppUtils.invokeInAppDismiss(getContext(), inAppMessage, params);
    }

    public void handleClick(InAppMessage inAppMessage, JSONObject params) {
        BlueshiftLogger.d(TAG, "handleClick: " + inAppMessage.getMessageUuid());
        // track click
        InAppUtils.invokeInAppClicked(getContext(), inAppMessage, params);
    }

    private Button getActionButtonBasic(JSONObject actionJson) {
        Button button = new Button(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TypedValue typedValue = new TypedValue();
            boolean isResolved = getContext()
                    .getTheme()
                    .resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);

            if (isResolved) {
                button.setForeground(ContextCompat.getDrawable(getContext(), typedValue.resourceId));
            }
        }

        button.setAllCaps(false);

        InAppUtils.setActionTextView(button, actionJson);

        return button;
    }

    protected Button getActionButton(JSONObject actionJson, String element) {
        Button button = null;

        try {
            if (actionJson != null) {
                button = getActionButtonBasic(actionJson);
                button.setOnClickListener(getActionClickListener(actionJson, element));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return button;
    }

    protected OnClickListener getActionClickListener(JSONObject actionJson, String element) {
        OnClickListener listener = null;

        try {
            if (actionJson != null) {
                String actionName = actionJson.optString(InAppConstants.ACTION_TYPE);

                // add additional params for analytics
                JSONObject statsParams = getClickStatsJSONObject(element);
                String androidLink = actionJson.optString(InAppConstants.ANDROID_LINK);
                if (!InAppUtils.isDismissUrl(androidLink)) {
                    statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, androidLink);
                }

                if (InAppManager.getActionCallback() != null) {
                    // user has overridden the clicks by setting an action callback.
                    JSONObject actionArgs = getCallbackActionJson(actionJson);
                    return getActionCallbackListener(actionName, statsParams, actionArgs);
                }

                switch (actionName) {
                    case InAppConstants.ACTION_DISMISS:
                        listener = getDismissDialogClickListener(statsParams, actionJson);
                        break;

                    case InAppConstants.ACTION_OPEN:
                        listener = getStartActivityClickListener(statsParams, actionJson);
                        break;

                    case InAppConstants.ACTION_SHARE:
                        listener = getShareClickListener(statsParams, actionJson);
                        break;

                    case InAppConstants.ACTION_RATE_APP:
                        listener = getRateAppClickListener(statsParams, actionJson);
                        break;
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        if (listener == null) {
            listener = getDismissDialogClickListener(null, actionJson);
        }

        return listener;
    }

    /**
     * Filter out unnecessary values from the action json
     *
     * @param actionJson the actual action json
     * @return filtered action args, null if args are empty
     */
    private JSONObject getCallbackActionJson(JSONObject actionJson) {
        try {
            if (actionJson != null) {
                String actionName = actionJson.optString(InAppConstants.ACTION_TYPE);
                JSONObject actionArgs = new JSONObject();

                switch (actionName) {
                    case InAppConstants.ACTION_OPEN:
                        String link = actionJson.optString(InAppConstants.ANDROID_LINK);
                        actionArgs.put(InAppConstants.ANDROID_LINK, link);
                        break;

                    case InAppConstants.ACTION_SHARE:
                        String shareContent = actionJson.optString(InAppConstants.SHAREABLE_TEXT);
                        actionArgs.put(InAppConstants.SHAREABLE_TEXT, shareContent);
                        break;
                }

                JSONObject extras = actionJson.optJSONObject(InAppConstants.EXTRAS);
                actionArgs.put(InAppConstants.EXTRAS, extras);

                return actionArgs;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    // action click listeners

    protected OnClickListener getActionCallbackListener(final String actionName, final JSONObject clickStats, final JSONObject actionObject) {
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                InAppActionCallback callback = InAppManager.getActionCallback();
                if (callback != null) {
                    callback.onAction(actionName, actionObject);
                }

                handleClick(getInAppMessage(), clickStats);
            }
        };
    }

    protected OnClickListener getDismissDialogClickListener(final JSONObject clickStats, final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // dismiss dialog
                    handleDismiss(getInAppMessage(), clickStats);
                }
            };
        }

        return listener;
    }

    protected OnClickListener getStartActivityClickListener(final JSONObject clickStats, final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // open
                    open(action);

                    // validate the CTA URL and decide what action needs to be taken
                    String link = action.optString(InAppConstants.ANDROID_LINK);
                    if (InAppUtils.isDismissUrl(link)) {
                        handleDismiss(getInAppMessage(), clickStats);
                    } else {
                        handleClick(getInAppMessage(), clickStats);
                    }
                }
            };
        }

        return listener;
    }

    protected OnClickListener getShareClickListener(final JSONObject clickStats, final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // open activity
                    shareText(action);

                    // dismiss dialog
                    handleClick(getInAppMessage(), clickStats);
                }
            };
        }

        return listener;
    }

    protected OnClickListener getRateAppClickListener(final JSONObject clickStats, final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // rate app in play store
                    rateAppInGooglePlayStore(action);

                    // dismiss dialog
                    handleClick(getInAppMessage(), clickStats);
                }
            };
        }

        return listener;
    }

    // action functions

    private void rateAppInGooglePlayStore(JSONObject action) {
        try {
            String pkgName = getContext().getPackageName();
            try {
                Uri marketUri = Uri.parse("market://details?id=" + pkgName);
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                getContext().startActivity(marketIntent);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);

                try {
                    Uri marketWebUri = Uri.parse("https://play.google.com/store/apps/details?id=" + pkgName);
                    Intent marketWebIntent = new Intent(Intent.ACTION_VIEW, marketWebUri);
                    getContext().startActivity(marketWebIntent);
                } catch (Exception ex) {
                    BlueshiftLogger.e(TAG, ex);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void shareText(JSONObject action) {
        try {
            if (action != null) {
                String text = action.optString(InAppConstants.SHAREABLE_TEXT);
                if (!TextUtils.isEmpty(text)) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                    shareIntent.setType("text/plain");
                    getContext().startActivity(shareIntent);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void open(JSONObject action) {
        try {
            if (action != null) {
                String link = action.optString(InAppConstants.ANDROID_LINK);
                BlueshiftLogger.d(TAG, "android_link: " + link);

                if (InAppUtils.isDismissUrl(link)) {
                    BlueshiftLogger.d(TAG, "Dismiss URL detected.");
                } else if (InAppUtils.isAskPNPermissionUri(Uri.parse(link))) {
                    Blueshift.requestPushNotificationPermission(getContext());
                } else {
                    JSONObject extras = action.optJSONObject(InAppConstants.EXTRAS);
                    Bundle bundle = null;
                    if (extras != null) {
                        bundle = new Bundle();
                        Iterator<String> keys = extras.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String val = extras.optString(key);
                            bundle.putString(key, val);
                        }
                    }

                    try {
                        Context context = getContext();
                        if (context instanceof Activity) {
                            BlueshiftUtils.openURL(link, (Activity) context, bundle, BlueshiftConstants.LINK_SOURCE_INAPP);
                        }
                    } catch (Exception e) {
                        BlueshiftLogger.e(TAG, e);

                        try {
                            openActivity(link, bundle);
                        } catch (Exception ex) {
                            BlueshiftLogger.e(TAG, ex);
                        }
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void openActivity(String className, Bundle extras) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        Intent intent = new Intent(getContext(), clazz);
        if (extras != null) {
            intent.putExtras(extras);
        }
        getContext().startActivity(intent);
    }

    public LinearLayout getActionButtons(InAppMessage inAppMessage) {
        if (inAppMessage != null && inAppMessage.getActionsJSONArray() != null) {
            JSONArray actions = inAppMessage.getActionsJSONArray();
            LinearLayout actionsRootView = new LinearLayout(getContext());
            int orientation = InAppUtils.getActionOrientation(getContext(), inAppMessage);
            if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
                actionsRootView.setOrientation(orientation);
                actionsRootView.setGravity(Gravity.CENTER);
            }

            Drawable drawable = InAppUtils.getContentBackgroundDrawable(getContext(), inAppMessage, InAppConstants.ACTIONS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                actionsRootView.setBackground(drawable);
            } else {
                actionsRootView.setBackgroundDrawable(drawable);
            }

            Rect padding = InAppUtils.getContentPadding(getContext(), inAppMessage, InAppConstants.ACTIONS);
            if (padding != null) {
                actionsRootView.setPadding(
                        dp2px(padding.left),
                        dp2px(padding.top),
                        dp2px(padding.right),
                        dp2px(padding.bottom)
                );
            }

            for (int i = 0; i < actions.length(); i++) {
                try {
                    JSONObject actionObject = actions.getJSONObject(i);
                    Button actionBtn = getActionButton(actionObject, BlueshiftConstants.BTN_(i));
                    if (actionBtn != null) {
                        LinearLayout.LayoutParams lp;
                        if (actionsRootView.getOrientation() == LinearLayout.VERTICAL) {
                            lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
                        } else {
                            lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                        }

                        Rect margin = InAppUtils.getRectFromJSONObject(actionObject, InAppConstants.MARGIN);
                        if (margin != null) {
                            lp.setMargins(
                                    dp2px(margin.left),
                                    dp2px(margin.top),
                                    dp2px(margin.right),
                                    dp2px(margin.bottom)
                            );
                        }

                        actionsRootView.addView(actionBtn, lp);
                    }
                } catch (Exception e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }

            return actionsRootView.getChildCount() > 0 ? actionsRootView : null;
        }

        return null;
    }

    public TextView getContentTextView(InAppMessage inAppMessage, String contentName) {
        TextView textView = null;

        if (inAppMessage != null && !TextUtils.isEmpty(contentName)) {
            String titleText = inAppMessage.getContentString(contentName);

            if (!TextUtils.isEmpty(titleText)) {
                textView = new TextView(getContext());
                InAppUtils.setContentTextView(textView, inAppMessage, contentName);
            }
        }

        return textView;
    }

    public TextView getContentIconTextView(InAppMessage inAppMessage, String contentName) {
        TextView textView = null;

        if (inAppMessage != null && !TextUtils.isEmpty(contentName)) {
            String titleText = inAppMessage.getContentString(contentName);

            if (!TextUtils.isEmpty(titleText)) {
                textView = new TextView(getContext());
                // font-awesome icon font (free)
                InAppMessageIconFont.getInstance(getContext()).apply(textView);
                InAppUtils.setContentTextView(textView, inAppMessage, contentName);
            }
        }

        return textView;
    }

    public ImageView getContentImageView(InAppMessage inAppMessage, String contentName) {
        ImageView imageView = null;

        if (inAppMessage != null && !TextUtils.isEmpty(contentName)) {
            String imageUrl = inAppMessage.getContentString(contentName);
            if (TextUtils.isEmpty(imageUrl)) return null;

            imageView = new ImageView(getContext());
            InAppUtils.loadImageAsync(imageView, imageUrl);

            // fill image
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        return imageView;
    }

    protected int dp2px(int dp) {
        return CommonUtils.dpToPx(dp, getContext());
    }

    public abstract View getView(InAppMessage inAppMessage);
}
