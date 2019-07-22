package com.blueshift.inappmessage;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InAppMessageAnimatedView extends InAppMessageView {

    public InAppMessageAnimatedView(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        LinearLayout linearLayout = new LinearLayout(getContext());

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView imageView = getContentImageView(inAppMessage, CONTENT_ICON);
        if (imageView != null) {

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
