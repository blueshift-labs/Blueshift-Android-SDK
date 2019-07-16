package com.blueshift.inappmessage;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InAppMessageCenterPopupView extends InAppMessageView {
    private static final String CONTENT_TITLE = "title";
    private static final String CONTENT_MESSAGE = "message";

    public InAppMessageCenterPopupView(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootView.setLayoutParams(lp2);

        // title
        String titleText = inAppMessage.getContentString(CONTENT_TITLE);
        if (!TextUtils.isEmpty(titleText)) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView titleTextView = new TextView(getContext());
            titleTextView.setText(titleText);
            rootView.addView(titleTextView, lp);
        }

        // message
        String messageText = inAppMessage.getContentString(CONTENT_MESSAGE);
        if (!TextUtils.isEmpty(messageText)) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView messageTextView = new TextView(getContext());
            messageTextView.setText(messageText);
            rootView.addView(messageTextView, lp);
        }

        // action
        Button actionButton = new Button(getContext());
        actionButton.setText("Got it!");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootView.addView(actionButton, lp);

        return rootView;
    }
}
