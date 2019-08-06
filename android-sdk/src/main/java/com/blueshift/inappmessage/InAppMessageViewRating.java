package com.blueshift.inappmessage;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.util.InAppUtils;

public class InAppMessageViewRating extends InAppMessageView {
    private static final String TAG = "CenterPopupView";

    public InAppMessageViewRating(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(final InAppMessage inAppMessage) {
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lpRoot = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (InAppUtils.isTemplateFullScreen(inAppMessage)) {
            lpRoot.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        rootView.setLayoutParams(lpRoot);

        // banner
        final ImageView bannerImageView = getContentImageView(inAppMessage, CONTENT_BANNER);
        if (bannerImageView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rootView.addView(bannerImageView, lp);

            bannerImageView.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        int width = bannerImageView.getWidth();
                        bannerImageView.getLayoutParams().height = width / 2;
                    } catch (Exception e) {
                        BlueshiftLogger.e(TAG, e);
                    }
                }
            });
        }

        // title
        TextView titleTextView = getContentTextView(inAppMessage, CONTENT_TITLE);
        if (titleTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = InAppUtils.getContentLayoutGravity(inAppMessage, CONTENT_TITLE);
            rootView.addView(titleTextView, lp);
        }

        // message
        TextView messageTextView = getContentTextView(inAppMessage, CONTENT_MESSAGE);
        if (messageTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // message window will take the full screen
            if (InAppUtils.isTemplateFullScreen(inAppMessage)) {
                lp.height = 0;
                lp.weight = 1;
            }

            lp.gravity = InAppUtils.getContentLayoutGravity(inAppMessage, CONTENT_MESSAGE);
            rootView.addView(messageTextView, lp);
        }

        // Rating
        RatingBar ratingBar = new RatingBar(getContext());
        ratingBar.setMax(5);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);

        LinearLayout.LayoutParams lpRating = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );

        lpRating.gravity = Gravity.CENTER;
        rootView.addView(ratingBar, lpRating);

        // Action buttons layout
        LinearLayout buttonLayout = new LinearLayout(getContext());
        int orientation = InAppUtils.getActionOrientation(inAppMessage);
        if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
            buttonLayout.setOrientation(orientation);
        }

        LinearLayout.LayoutParams lpBtnRoot = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtnRoot.gravity = Gravity.CENTER;
        rootView.addView(buttonLayout, lpBtnRoot);

        LinearLayout.LayoutParams lpAction;
        if (buttonLayout.getOrientation() == LinearLayout.VERTICAL) {
            lpAction = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        } else {
            lpAction = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        }

        // Submit
        Button submit = getActionButton(inAppMessage, "submit");
        if (submit == null) {
            submit = InAppUtils.getActionButtonDefault(getContext());
            submit.setText(R.string.bsft_rating_submit);
        }

        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRating();
            }
        });

        buttonLayout.addView(submit, lpAction);

        // Not now
        Button notNow = getActionButton(inAppMessage, "dismiss");
        if (notNow != null) {
            buttonLayout.addView(notNow, lpAction);
        }

        return rootView;
    }

    private void submitRating() {
        onDismiss(getInAppMessage());
    }
}
