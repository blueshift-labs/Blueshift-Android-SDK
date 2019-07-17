package com.blueshift.inappmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONObject;

import java.util.Iterator;

public abstract class InAppMessageView extends RelativeLayout {
    protected static final String ACTION_DISMISS = "dismiss";
    protected static final String ACTION_APP_OPEN = "app_open";

    private static final String LOG_TAG = "InAppMessageView";
    private static final String ACTION_KEY_TEXT = "text";
    private static final String ACTION_KEY_TEXT_COLOR = "text_color";
    private static final String ACTION_KEY_BACKGROUND_COLOR = "background_color";
    private static final String ACTION_KEY_PAGE = "page";
    private static final String ACTION_KEY_EXTRAS = "extras";

    private InAppMessage inAppMessage = null;

    public InAppMessageView(Context context, InAppMessage inAppMessage) {
        super(context);

        if (inAppMessage == null) return;

        this.inAppMessage = inAppMessage;

        applyTemplateDimensions(inAppMessage);

        int bgColor = inAppMessage.getTemplateBackgroundColor();
        if (bgColor > 0) {
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

    public Button getDismissButton(JSONObject action) {
        try {
            Button button = getBasicButton(action);

            if (button != null) {
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onDismiss(inAppMessage);
                    }
                });

                return button;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    public Button getOpenAppButton(final JSONObject action) {
        try {
            Button button = getBasicButton(action);

            if (button != null) {
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(action);
                        onDismiss(inAppMessage);
                    }
                });

                return button;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
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

    public abstract View getView(InAppMessage inAppMessage);
}
