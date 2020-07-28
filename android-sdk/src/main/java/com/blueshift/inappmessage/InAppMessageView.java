package com.blueshift.inappmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
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

        int bgColor = inAppMessage.getTemplateBackgroundColor(getContext());
        if (bgColor != 0) {
            setBackgroundColor(bgColor);
        } else {
            setBackgroundColor(Color.WHITE);
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

        applyTemplateDimensions(inAppMessage);
    }

    private void addBackgroundImageView() {
        String url = InAppUtils.getTemplateString(
                getContext(), inAppMessage, InAppConstants.BACKGROUND_IMAGE);
        if (!TextUtils.isEmpty(url)) {
            ImageView imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            addView(imageView);

            InAppUtils.loadImageAsync(imageView, url);
        }
    }

    private void addCloseButton(final InAppMessage inAppMessage) {
        TextView closeButtonView = new TextView(getContext());
        InAppMessageIconFont.getInstance(getContext()).apply(closeButtonView);
        closeButtonView.setText("\uF00D");
        closeButtonView.setTextColor(Color.parseColor("#ffffff"));
        closeButtonView.setGravity(Gravity.CENTER);

        int dp24 = CommonUtils.dpToPx(24, getContext());

        GradientDrawable background = InAppUtils.getCloseButtonBackground(getContext(), inAppMessage, dp24);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            closeButtonView.setBackground(background);
        } else {
            closeButtonView.setBackgroundDrawable(background);
        }

        LayoutParams lp = new LayoutParams(dp24, dp24);

        int dp8 = CommonUtils.dpToPx(8, getContext());
        lp.setMargins(0, dp8, dp8, 0);

        lp.addRule(ALIGN_PARENT_TOP);
        lp.addRule(ALIGN_PARENT_RIGHT);

        closeButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDismiss(inAppMessage, getClickStatsJSONObject(InAppConstants.ACTION_CLOSE));
            }
        });

        addView(closeButtonView, lp);
    }

    private void applyTemplateDimensions(final InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
                    if (lp != null) {
                        Rect margins = inAppMessage.getTemplateMargin(getContext());
                        if (margins != null) {
                            lp.leftMargin = CommonUtils.dpToPx(margins.left, getContext());
                            lp.topMargin = CommonUtils.dpToPx(margins.top, getContext());
                            lp.rightMargin = CommonUtils.dpToPx(margins.right, getContext());
                            lp.bottomMargin = CommonUtils.dpToPx(margins.bottom, getContext());
                        }

                        DisplayMetrics metrics = getResources().getDisplayMetrics();

                        float wPercentage = inAppMessage.getTemplateWidth(getContext());
                        if (wPercentage > 0) {
                            int horizontalMargn = (lp.leftMargin + lp.rightMargin);
                            lp.width = (int) ((metrics.widthPixels * (wPercentage / 100)) - horizontalMargn);
                        } else {
                            lp.width = (int) wPercentage;
                        }

                        float hPercentage = inAppMessage.getTemplateHeight(getContext());
                        if (hPercentage > 0) {
                            int verticalMargin = lp.topMargin + lp.bottomMargin;
                            lp.height = (int) ((metrics.heightPixels * (hPercentage / 100)) - verticalMargin);
                        } else {
                            lp.height = (int) hPercentage;
                        }

                        lp.gravity = Gravity.CENTER;

                        try {
                            boolean isModal = InAppUtils.isModal(inAppMessage);
                            if (isModal) {
                                int modalWidth = lp.width > 0 ? lp.width : getMeasuredWidth();
                                int modalHeight = lp.height > 0 ? lp.height : getMeasuredHeight();
                                // image background will be at position 0 if available
                                View view = getChildAt(0);
                                if (view instanceof ImageView) {
                                    // background image added, now we need to match it with the size
                                    setViewDimensionsToMatchParent(view, modalWidth, modalHeight);

                                    if (getChildCount() > 1) {
                                        View contentView = getChildAt(1);
                                        setViewDimensionsToMatchParent(contentView, modalWidth, modalHeight);
                                    }
                                } else if (view instanceof ViewGroup) {
                                    setViewDimensionsToMatchParent(view, modalWidth, modalHeight);
                                }
                            }
                        } catch (Exception e) {
                            BlueshiftLogger.e(TAG, e);
                        }

                        setLayoutParams(lp);
                    }
                }
            });
        }
    }

    private void setViewDimensionsToMatchParent(View view, int width, int height) {
        if (view != null && view.getLayoutParams() != null) {
            view.getLayoutParams().width = width;
            view.getLayoutParams().height = height;
        }
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

    public void onDismiss(InAppMessage inAppMessage, JSONObject extras) {
        BlueshiftLogger.d(TAG, "Dismiss invoked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
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
                statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, androidLink);

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
                if (callback != null) callback.onAction(actionName, actionObject);

                // dismiss dialog
                onDismiss(getInAppMessage(), clickStats);
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
                    onDismiss(getInAppMessage(), clickStats);
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

                    // dismiss dialog
                    onDismiss(getInAppMessage(), clickStats);
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
                    onDismiss(getInAppMessage(), clickStats);
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
                    onDismiss(getInAppMessage(), clickStats);
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
                if (!TextUtils.isEmpty(link)) {
                    BlueshiftLogger.d(TAG, "deep-link: " + link);

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
                        openLink(link, bundle);
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

    private void openLink(String link, Bundle extras) {
        Uri linkUri = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(linkUri);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        if (extras != null) {
            intent.putExtras(extras);
        }
        getContext().startActivity(intent);
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
                    Button actionBtn = getActionButton(actionObject, "button_" + i);
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

    public ImageView getContentIconImageView(InAppMessage inAppMessage, String contentName) {
        ImageView imageView = null;

        if (inAppMessage != null && !TextUtils.isEmpty(contentName)) {
            String imageUrl = inAppMessage.getContentString(contentName);

            if (!TextUtils.isEmpty(imageUrl)) {
                imageView = new ImageView(getContext());
                InAppUtils.setContentImageView(imageView, inAppMessage, contentName);
            }
        }

        return imageView;
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
