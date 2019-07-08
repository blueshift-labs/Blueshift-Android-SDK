package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.blueshift.R;
import com.blueshift.util.CommonUtils;

public abstract class InAppMessageView extends RelativeLayout {
    private static final String LOG_TAG = "InAppMessageView";

    public InAppMessageView(Context context, InAppMessage inAppMessage) {
        super(context);

        applyTemplateDimensions(inAppMessage);

        int bgColor = inAppMessage.getTemplateBackgroundColor();
        if (bgColor > 0) {
            setBackgroundColor(bgColor);
        }

        View childView = getView(inAppMessage);
        if (childView != null) {
            addView(childView);
        }

        addCloseButton(inAppMessage);
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

    public void onCloseButtonClick(InAppMessage inAppMessage) {
        Log.d(LOG_TAG, "Close button clicked on InAppMessage: " + (inAppMessage != null ? inAppMessage.toString() : "null"));
    }

    public void applyTemplateDimensions(InAppMessage inAppMessage) {
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

    public abstract View getView(InAppMessage inAppMessage);
}
