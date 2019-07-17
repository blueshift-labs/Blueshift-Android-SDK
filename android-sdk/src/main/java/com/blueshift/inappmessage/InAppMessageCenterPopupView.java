package com.blueshift.inappmessage;

import android.content.Context;
import android.text.TextUtils;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

import org.json.JSONObject;

import java.util.Iterator;

public class InAppMessageCenterPopupView extends InAppMessageView {
    private static final String CONTENT_TITLE = "title";
    private static final String CONTENT_MESSAGE = "message";

    public InAppMessageCenterPopupView(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        int dp8 = CommonUtils.dpToPx(8, getContext());

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
            titleTextView.setPadding(dp8, dp8, dp8, dp8);
            titleTextView.setText(titleText);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, InAppUtils.getContentSize(inAppMessage, CONTENT_TITLE));
            titleTextView.setTextColor(InAppUtils.getContentColor(inAppMessage, CONTENT_TITLE));
            titleTextView.setBackgroundColor(InAppUtils.getContentBackgroundColor(inAppMessage, CONTENT_TITLE));
            rootView.addView(titleTextView, lp);
        }

        // message
        String messageText = inAppMessage.getContentString(CONTENT_MESSAGE);
        if (!TextUtils.isEmpty(messageText)) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView messageTextView = new TextView(getContext());
            messageTextView.setPadding(dp8, dp8, dp8, dp8);
            messageTextView.setText(messageText);
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, InAppUtils.getContentSize(inAppMessage, CONTENT_TITLE));
            messageTextView.setTextColor(InAppUtils.getContentColor(inAppMessage, CONTENT_MESSAGE));
            messageTextView.setBackgroundColor(InAppUtils.getContentBackgroundColor(inAppMessage, CONTENT_MESSAGE));
            rootView.addView(messageTextView, lp);
        }

        // action
        JSONObject actions = inAppMessage.getAction();
        if (actions != null) {
            LinearLayout actionsRootView = new LinearLayout(getContext());

            Iterator<String> actionKeys = actions.keys();
            while (actionKeys.hasNext()) {
                Button actionBtn = null;
                String action = actionKeys.next();
                if (action != null) {
                    switch (action) {
                        case ACTION_DISMISS:
                            actionBtn = getDismissButton(actions.optJSONObject(action));
                            break;

                        case ACTION_APP_OPEN:
                            actionBtn = getOpenAppButton(actions.optJSONObject(action));
                            break;
                    }
                }

                if (actionBtn != null) {
                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    actionsRootView.addView(actionBtn, lp);
                }
            }

            if (actionsRootView.getChildCount() > 0) {
                LinearLayout.LayoutParams actionsRootLp =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rootView.addView(actionsRootView, actionsRootLp);
            }
        }

        return rootView;
    }
}
