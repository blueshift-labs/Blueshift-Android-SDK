package com.blueshift.inappmessage;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.util.CommonUtils;

public class InAppMessageAnimatedView extends InAppMessageView {

    public InAppMessageAnimatedView(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setBackgroundResource(android.R.color.transparent);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;

        ImageView imageView = getContentImageView(inAppMessage, CONTENT_ICON);
        if (imageView != null) {
            int dp40 = CommonUtils.dpToPx(40, getContext());
            int dp8 = CommonUtils.dpToPx(8, getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp40, dp40);
            lp.setMargins(dp8, dp8, dp8, dp8);

            linearLayout.addView(imageView, lp);
        }

        TextView messageTextView = getContentTextView(inAppMessage, CONTENT_MESSAGE);
        if (messageTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            linearLayout.addView(messageTextView, lp);
        }

        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }
}
