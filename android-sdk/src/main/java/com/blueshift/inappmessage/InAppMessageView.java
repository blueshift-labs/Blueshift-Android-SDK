package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.content.ContextCompat;
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

public abstract class InAppMessageView extends RelativeLayout {
    protected static final String ACTION_DISMISS = "dismiss";
    private static final String LOG_TAG = "InAppMessageView";
    private static final String ACTION_KEY_TEXT = "text";
    private static final String ACTION_KEY_TEXT_COLOR = "text_color";
    private static final String ACTION_KEY_BACKGROUND_COLOR = "background_color";

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

    public Button getDismissButton(JSONObject action) {
        try {
            if (action != null) {
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
                button.setText(InAppUtils.getStringFromJSONObject(action, ACTION_KEY_TEXT));

                String textColor = InAppUtils.getStringFromJSONObject(action, ACTION_KEY_TEXT_COLOR);
                if (InAppUtils.validateColorString(textColor)) {
                    int color = Color.parseColor(textColor);
                    button.setTextColor(color);
                }

                String bgColor = InAppUtils.getStringFromJSONObject(action, ACTION_KEY_BACKGROUND_COLOR);
                if (InAppUtils.validateColorString(bgColor)) {
                    int color = Color.parseColor(bgColor);
                    button.setBackgroundColor(color);
                }

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

    public abstract View getView(InAppMessage inAppMessage);
}
