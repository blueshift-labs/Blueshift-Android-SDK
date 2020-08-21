package com.blueshift.inappmessage;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.util.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.view.Gravity.CENTER;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class InAppMessageViewBanner extends InAppMessageView {
    private static final String TAG = InAppMessageViewBanner.class.getSimpleName();

    public InAppMessageViewBanner(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setGravity(CENTER);
        linearLayout.setBackgroundResource(android.R.color.transparent);

        int dp48 = CommonUtils.dpToPx(48, getContext());

        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp48, MATCH_PARENT);

        TextView iconTextView = getContentIconTextView(inAppMessage, InAppConstants.ICON);
        if (iconTextView != null) {
            linearLayout.addView(iconTextView, lpIcon);
        }

        ImageView iconImageView = getContentIconImageView(inAppMessage, InAppConstants.ICON_IMAGE);
        if (iconImageView != null) {
            linearLayout.addView(iconImageView, lpIcon);
        }

        TextView messageTextView = getContentTextView(inAppMessage, InAppConstants.MESSAGE);
        if (messageTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
            linearLayout.addView(messageTextView, lp);
        }

        TextView secondaryIconTextView = getContentIconTextView(inAppMessage, InAppConstants.SECONDARY_ICON);
        if (secondaryIconTextView != null) {
            linearLayout.addView(secondaryIconTextView, lpIcon);
        }

        // assumed that only one action is provided. if more actions found, first one is taken.
        JSONArray actions = inAppMessage.getActionsJSONArray();
        if (actions != null && actions.length() > 0) {
            try {
                JSONObject actionJson = actions.getJSONObject(0);
                linearLayout.setOnClickListener(getActionClickListener(actionJson));
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        linearLayout.setMinimumHeight(dp48);

        return linearLayout;
    }
}
