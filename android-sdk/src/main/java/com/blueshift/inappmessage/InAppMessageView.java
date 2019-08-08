package com.blueshift.inappmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONObject;

import java.util.Iterator;

public abstract class InAppMessageView extends RelativeLayout {
    private static final String TAG = InAppMessageView.class.getSimpleName();

    private InAppMessage inAppMessage = null;

    public InAppMessageView(Context context, InAppMessage inAppMessage) {
        super(context);

        if (inAppMessage == null) return;

        this.inAppMessage = inAppMessage;

        applyTemplateDimensions(inAppMessage);

        int bgColor = inAppMessage.getTemplateBackgroundColor();
        if (bgColor != 0) {
            setBackgroundColor(bgColor);
        }

        View childView = getView(inAppMessage);
        if (childView != null) {
            addView(childView);
        }

        if (inAppMessage.showCloseButton()) {
            addCloseButton(inAppMessage);
        }
    }

    private void addCloseButton(final InAppMessage inAppMessage) {
        ImageButton closeButton = new ImageButton(getContext());
        closeButton.setImageResource(R.drawable.ic_close);
        closeButton.setBackgroundResource(R.drawable.ic_close_backgroud);

        int dp32 = CommonUtils.dpToPx(32, getContext());
        LayoutParams lp = new LayoutParams(dp32, dp32);

        int dp8 = CommonUtils.dpToPx(8, getContext());
        lp.setMargins(0, dp8, dp8, 0);

        lp.addRule(ALIGN_PARENT_TOP);
        lp.addRule(ALIGN_PARENT_RIGHT);

        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCloseButtonClick(inAppMessage);
            }
        });

        addView(closeButton, lp);
    }

    private void applyTemplateDimensions(InAppMessage inAppMessage) {
        if (inAppMessage != null) {
            LayoutParams lp = new LayoutParams(
                    inAppMessage.getTemplateWidth(getContext()),
                    inAppMessage.getTemplateHeight(getContext()));

            Rect margins = inAppMessage.getTemplateMargin();
            lp.setMargins(
                    CommonUtils.dpToPx(margins.left, getContext()),
                    CommonUtils.dpToPx(margins.top, getContext()),
                    CommonUtils.dpToPx(margins.right, getContext()),
                    CommonUtils.dpToPx(margins.bottom, getContext()));

            setLayoutParams(lp);
        }
    }

    public InAppMessage getInAppMessage() {
        return this.inAppMessage;
    }

    public void onCloseButtonClick(InAppMessage inAppMessage) {
        Log.d(TAG, "Close button clicked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
    }

    public void onDismiss(InAppMessage inAppMessage) {
        Log.d(TAG, "Dismiss invoked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
    }

    private Button getActionButtonBasic(InAppMessage inAppMessage, String actionName) {
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

        InAppUtils.setActionTextView(button, inAppMessage, actionName);

        return button;
    }

    protected Button getActionButton(InAppMessage inAppMessage, String actionName) {
        Button button = null;

        try {
            JSONObject actionJson = InAppUtils.getActionFromName(inAppMessage, actionName);
            if (actionJson != null) {
                button = getActionButtonBasic(inAppMessage, actionName);
                button.setOnClickListener(getActionClickListener(actionName, actionJson));
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return button;
    }

    protected OnClickListener getActionClickListener(String actionName, JSONObject actionJson) {
        OnClickListener listener = null;

        if (actionName != null) {
            switch (actionName) {
                case InAppConstants.ACTION_DISMISS:
                    listener = getDismissDialogClickListener(actionJson);
                    break;

                case InAppConstants.ACTION_OPEN:
                    listener = getStartActivityClickListener(actionJson);
                    break;

                case InAppConstants.ACTION_SHARE:
                    listener = getShareClickListener(actionJson);
                    break;

                case InAppConstants.ACTION_RATE_APP:
                    listener = getRateAppClickListener(actionJson);
                    break;
            }
        }

        if (listener == null) {
            listener = getDismissDialogClickListener(actionJson);
        }

        return listener;
    }

    // action click listeners

    protected OnClickListener getDismissDialogClickListener(final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // dismiss dialog
                    onDismiss(getInAppMessage());
                }
            };
        }

        return listener;
    }

    protected OnClickListener getStartActivityClickListener(final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // open activity
                    startActivity(action);

                    // dismiss dialog
                    onDismiss(getInAppMessage());
                }
            };
        }

        return listener;
    }

    protected OnClickListener getShareClickListener(final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // open activity
                    shareText(action);

                    // dismiss dialog
                    onDismiss(getInAppMessage());
                }
            };
        }

        return listener;
    }

    protected OnClickListener getRateAppClickListener(final JSONObject action) {
        OnClickListener listener = null;

        if (action != null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // rate app in play store
                    rateAppInGooglePlayStore(action);

                    // dismiss dialog
                    onDismiss(getInAppMessage());
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

    private void startActivity(JSONObject action) {
        try {
            if (action != null) {
                String activityName = action.optString(InAppConstants.PAGE);
                if (!TextUtils.isEmpty(activityName)) {
                    String pkgName = getContext().getPackageName();
                    if (!TextUtils.isEmpty(pkgName)) {
                        Class<?> clazz = Class.forName(pkgName + "." + activityName);
                        Intent launcher = new Intent(getContext(), clazz);

                        JSONObject extras = action.optJSONObject(InAppConstants.EXTRAS);
                        if (extras != null) {
                            Iterator<String> keys = extras.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String val = extras.optString(key);
                                launcher.putExtra(key, val);
                            }

                        }

                        getContext().startActivity(launcher);
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public LinearLayout getActionButtons(InAppMessage inAppMessage) {
        if (inAppMessage != null && inAppMessage.getActionsJSONObject() != null) {
            JSONObject actions = inAppMessage.getActionsJSONObject();
            LinearLayout actionsRootView = new LinearLayout(getContext());
            int orientation = InAppUtils.getActionOrientation(inAppMessage);
            if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
                actionsRootView.setOrientation(orientation);
                actionsRootView.setGravity(Gravity.CENTER);
            }

            Drawable drawable = InAppUtils.getContentBackgroundDrawable(inAppMessage, InAppConstants.ACTIONS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                actionsRootView.setBackground(drawable);
            } else {
                actionsRootView.setBackgroundDrawable(drawable);
            }

            Rect padding = InAppUtils.getContentPadding(inAppMessage, InAppConstants.ACTIONS);
            if (padding != null) {
                actionsRootView.setPadding(
                        dp2px(padding.left),
                        dp2px(padding.top),
                        dp2px(padding.right),
                        dp2px(padding.bottom)
                );
            }

            Iterator<String> actionKeys = actions.keys();
            while (actionKeys.hasNext()) {
                Button actionBtn = null;
                String action = actionKeys.next();
                if (action != null) {
                    actionBtn = getActionButton(inAppMessage, action);
                }

                if (actionBtn != null) {
                    LinearLayout.LayoutParams lp;
                    if (actionsRootView.getOrientation() == LinearLayout.VERTICAL) {
                        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
                    } else {
                        lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    }

                    Rect margin = InAppUtils.getActionMargin(inAppMessage, action);
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
