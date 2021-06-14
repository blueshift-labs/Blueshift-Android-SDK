package com.blueshift.inappmessage;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.InAppUtils;

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
    public View getView(final InAppMessage inAppMessage) {
        final InAppSwipeLinearLayout linearLayout = new InAppSwipeLinearLayout(getContext());
        linearLayout.setGravity(CENTER);
        linearLayout.setBackgroundResource(android.R.color.transparent);

        int dp48 = CommonUtils.dpToPx(48, getContext());

        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp48, MATCH_PARENT);

        TextView iconTextView = getContentIconTextView(inAppMessage, InAppConstants.ICON);
        if (iconTextView != null) {
            linearLayout.addView(iconTextView, lpIcon);
        }

        ViewGroup iconImageView = InAppUtils.createContentIconView(getContext(), inAppMessage, InAppConstants.ICON_IMAGE);
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
        JSONObject actionJson = null;
        JSONArray actions = inAppMessage.getActionsJSONArray();
        if (actions != null && actions.length() > 0) {
            try {
                actionJson = actions.getJSONObject(0);
            } catch (JSONException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        final OnClickListener clickListener = getActionClickListener(actionJson, null);
        linearLayout.enableSwipeAndTap(new InAppSwipeLinearLayout.OnSwipeGestureListener() {
            @Override
            public void onSwipeUp() {
            }

            @Override
            public void onSwipeDown() {
            }

            @Override
            public void onSwipeLeft() {
                playExitAnimation(-linearLayout.getWidth(), linearLayout, new Runnable() {
                    @Override
                    public void run() {
                        onDismiss(inAppMessage, null);
                    }
                });
            }

            @Override
            public void onSwipeRight() {
                playExitAnimation(linearLayout.getWidth(), linearLayout, new Runnable() {
                    @Override
                    public void run() {
                        onDismiss(inAppMessage, null);
                    }
                });
            }

            @Override
            public void onSingleTapConfirmed() {
                playExitAnimation(linearLayout.getWidth(), linearLayout, new Runnable() {
                    @Override
                    public void run() {
                        if (clickListener != null) {
                            clickListener.onClick(linearLayout);
                        }
                    }
                });
            }
        });

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        linearLayout.setMinimumHeight(dp48);

        return linearLayout;
    }

    private void playExitAnimation(int translationX, View view, Runnable onAnimComplete) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (view != null) {
                ViewParent viewParent = view.getParent();
                if (viewParent instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) viewParent;
                    parent.animate()
                            .translationX(translationX)
                            .setDuration(1000)
                            .withEndAction(onAnimComplete)
                            .start();
                }
            }
        }
    }
}
