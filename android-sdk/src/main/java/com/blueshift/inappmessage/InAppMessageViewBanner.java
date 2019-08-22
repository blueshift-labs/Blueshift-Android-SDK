package com.blueshift.inappmessage;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.R;
import com.blueshift.util.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppMessageViewBanner extends InAppMessageView {

    public InAppMessageViewBanner(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setBackgroundResource(android.R.color.transparent);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        linearLayout.setLayoutParams(layoutParams);

        TextView iconTextView = getContentIconTextView(inAppMessage, InAppConstants.ICON);
        if (iconTextView != null) {
            int dp40 = CommonUtils.dpToPx(40, getContext());
            int dp8 = CommonUtils.dpToPx(8, getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp40, dp40);
            lp.setMargins(dp8, dp8, dp8, dp8);

            linearLayout.addView(iconTextView, lp);
        }

        TextView messageTextView = getContentTextView(inAppMessage, InAppConstants.MESSAGE);
        if (messageTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            linearLayout.addView(messageTextView, lp);
        }

        ImageView fwdArrow = new ImageView(getContext());
        fwdArrow.setImageResource(R.drawable.ic_inapp_arrow_forward);
        int dp20 = CommonUtils.dpToPx(20, getContext());
        int dp8 = CommonUtils.dpToPx(8, getContext());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp20, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(dp8, dp8, dp8, dp8);
        linearLayout.addView(fwdArrow, lp);

        // assumed that only one action is provided. if more actions found, first one is taken.
        JSONArray actions = inAppMessage.getActionsJSONArray();
        if (actions != null && actions.length() > 0) {
            try {
                JSONObject actionJson = actions.getJSONObject(0);
                linearLayout.setOnClickListener(getActionClickListener(actionJson));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return linearLayout;
    }

    @Override
    public void cleanup() {
        InAppManager.clearCachedAssets(getInAppMessage(), getContext());
    }
}
