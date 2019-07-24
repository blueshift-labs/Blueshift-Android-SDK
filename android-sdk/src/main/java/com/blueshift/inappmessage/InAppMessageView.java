package com.blueshift.inappmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
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
    protected static final String ACTION_DISMISS = "dismiss";
    protected static final String ACTION_APP_OPEN = "app_open";
    protected static final String ACTION_SHARE = "share";

    protected static final String CONTENT_TITLE = "title";
    protected static final String CONTENT_MESSAGE = "message";
    protected static final String CONTENT_ICON = "icon";

    private static final String LOG_TAG = "InAppMessageView";
    private static final String ACTION_KEY_TEXT = "text";
    private static final String ACTION_KEY_TEXT_COLOR = "text_color";
    private static final String ACTION_KEY_BACKGROUND_COLOR = "background_color";
    private static final String ACTION_KEY_PAGE = "page";
    private static final String ACTION_KEY_EXTRAS = "extras";
    private static final String ACTION_KEY_CONTENT = "content";

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
        Log.d(LOG_TAG, "Close button clicked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
    }

    public void onDismiss(InAppMessage inAppMessage) {
        Log.d(LOG_TAG, "Dismiss invoked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
    }

    private Button getBasicButton(JSONObject action) {
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

        // apply basic style
        if (action != null) {
            button.setText(InAppUtils.getStringFromJSONObject(action, ACTION_KEY_TEXT));

            String textColor = InAppUtils.getStringFromJSONObject(action, ACTION_KEY_TEXT_COLOR);
            InAppUtils.applyTextColor(button, textColor);

            String bgColor = InAppUtils.getStringFromJSONObject(action, ACTION_KEY_BACKGROUND_COLOR);
            InAppUtils.applyBackgroundColor(button, bgColor);
        }

        return button;
    }

    protected Button getActionButton(String actionName, JSONObject actionJson) {
        Button button = null;

        if (!TextUtils.isEmpty(actionName) && actionJson != null) {
            button = getBasicButton(actionJson);
            button.setOnClickListener(getActionClickListener(actionName, actionJson));
        }

        return button;
    }

    protected OnClickListener getActionClickListener(String actionName, JSONObject actionJson) {
        OnClickListener listener = null;

        if (actionName != null) {
            switch (actionName) {
                case ACTION_DISMISS:
                    listener = getDismissDialogClickListener(actionJson);
                    break;

                case ACTION_APP_OPEN:
                    listener = getStartActivityClickListener(actionJson);
                    break;

                case ACTION_SHARE:
                    listener = getShareClickListener(actionJson);
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

    // action functions

    private void shareText(JSONObject action) {
        try {
            if (action != null) {
                JSONObject content = action.optJSONObject(ACTION_KEY_CONTENT);
                if (content != null) {
                    String text = content.optString(ACTION_KEY_TEXT);
                    if (!TextUtils.isEmpty(text)) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                        shareIntent.setType("text/plain");
                        getContext().startActivity(shareIntent);
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private void startActivity(JSONObject action) {
        try {
            if (action != null) {
                String activityName = action.optString(ACTION_KEY_PAGE);
                if (!TextUtils.isEmpty(activityName)) {
                    String pkgName = getContext().getPackageName();
                    if (!TextUtils.isEmpty(pkgName)) {
                        Class<?> clazz = Class.forName(pkgName + "." + activityName);
                        Intent launcher = new Intent(getContext(), clazz);

                        JSONObject extras = action.optJSONObject(ACTION_KEY_EXTRAS);
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
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    public LinearLayout getActionButtons(JSONObject actions, int orientation) {
        if (actions != null) {
            LinearLayout actionsRootView = new LinearLayout(getContext());
            if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
                actionsRootView.setOrientation(orientation);
            }

            int dp4 = CommonUtils.dpToPx(4, getContext());
            Iterator<String> actionKeys = actions.keys();
            while (actionKeys.hasNext()) {
                Button actionBtn = null;
                String action = actionKeys.next();
                if (action != null) {
                    actionBtn = getActionButton(action, actions.optJSONObject(action));
                }

                if (actionBtn != null) {
                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    lp.setMargins(dp4, dp4, dp4, dp4);
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
                textView.setText(titleText);

                int padding = InAppUtils.getContentPadding(inAppMessage, contentName, 8);
                int dpPadding = CommonUtils.dpToPx(padding, getContext());
                textView.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);

                int contentGravity = InAppUtils.getContentGravity(inAppMessage, contentName);
                textView.setGravity(contentGravity);

                int val = InAppUtils.getContentSize(inAppMessage, contentName, 14);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, val);

                String contentColor = InAppUtils.getContentColor(inAppMessage, contentName);
                InAppUtils.applyTextColor(textView, contentColor);

                String backgroundColor = InAppUtils.getContentBackgroundColor(inAppMessage, contentName);
                InAppUtils.applyBackgroundColor(textView, backgroundColor);
            }
        }

        return textView;
    }

    public ImageView getContentImageView(InAppMessage inAppMessage, String contentName) {
        ImageView imageView = null;

        if (inAppMessage != null && !TextUtils.isEmpty(contentName)) {
            int dp4 = CommonUtils.dpToPx(4, getContext());
            imageView = new ImageView(getContext());

            Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_inapp_videocam);
            if (icon != null) {
                try {
                    String colorString = InAppUtils.getContentColor(inAppMessage, contentName);
                    boolean isValidColor = InAppUtils.validateColorString(colorString);
                    if (isValidColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        icon.setTint(Color.parseColor(colorString));
                    }
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }

                imageView.setImageDrawable(icon);
            }

            Drawable iconBackground = ContextCompat.getDrawable(getContext(), R.drawable.inapp_icon_background);
            if (iconBackground != null) {
                try {
                    String colorString = InAppUtils.getContentBackgroundColor(inAppMessage, contentName);
                    boolean isValidColor = InAppUtils.validateColorString(colorString);
                    if (isValidColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        iconBackground.setTint(Color.parseColor(colorString));
                    }
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }

                imageView.setBackgroundResource(R.drawable.inapp_icon_background);
            }

            imageView.setPadding(dp4, dp4, dp4, dp4);
        }

        return imageView;
    }

    public abstract View getView(InAppMessage inAppMessage);
}
