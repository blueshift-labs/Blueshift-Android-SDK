package com.blueshift.inappmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.util.InAppUtils;

import org.json.JSONObject;

public class InAppMessageViewRating extends InAppMessageView {
    private static final String TAG = InAppMessageViewRating.class.getSimpleName();

    public InAppMessageViewRating(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(final InAppMessage inAppMessage) {
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lpRoot = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (InAppUtils.isTemplateFullScreen(getContext(), inAppMessage)) {
            lpRoot.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        rootView.setLayoutParams(lpRoot);

        // banner
        final ImageView bannerImageView = getContentImageView(inAppMessage, InAppConstants.BANNER);
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
        TextView titleTextView = getContentTextView(inAppMessage, InAppConstants.TITLE);
        if (titleTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = InAppUtils.getContentLayoutGravity(getContext(), inAppMessage, InAppConstants.TITLE);
            rootView.addView(titleTextView, lp);
        }

        // message
        TextView messageTextView = getContentTextView(inAppMessage, InAppConstants.MESSAGE);
        if (messageTextView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // message window will take the full screen
            if (InAppUtils.isTemplateFullScreen(getContext(), inAppMessage)) {
                lp.height = 0;
                lp.weight = 1;
            }

            lp.gravity = InAppUtils.getContentLayoutGravity(getContext(), inAppMessage, InAppConstants.MESSAGE);
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
        int orientation = InAppUtils.getActionOrientation(getContext(), inAppMessage);
        if (orientation == LinearLayout.HORIZONTAL || orientation == LinearLayout.VERTICAL) {
            buttonLayout.setOrientation(orientation);
        }

        LinearLayout.LayoutParams lpBtnRoot = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtnRoot.gravity = Gravity.CENTER;
        rootView.addView(buttonLayout, lpBtnRoot);

        // Submit button
        JSONObject submitAction = InAppUtils.getActionFromName(inAppMessage, InAppConstants.ACTION_RATE_APP);
        Button submit = getActionButton(submitAction, BlueshiftConstants.BTN_(0));
        if (submit == null) {
            submit = InAppUtils.getActionButtonDefault(getContext());
            submit.setText(R.string.bsft_in_app_rating_submit);
            submit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    submitRating();
                }
            });
        }

        LinearLayout.LayoutParams lpSubmit;
        if (buttonLayout.getOrientation() == LinearLayout.VERTICAL) {
            lpSubmit = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        } else {
            lpSubmit = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        }
        Rect marginsSubmit = InAppUtils.getActionMargin(inAppMessage, InAppConstants.ACTION_RATE_APP);
        lpSubmit.setMargins(
                dp2px(marginsSubmit.left),
                dp2px(marginsSubmit.top),
                dp2px(marginsSubmit.right),
                dp2px(marginsSubmit.bottom)
        );

        buttonLayout.addView(submit, lpSubmit);

        // Not now button
        JSONObject action = InAppUtils.getActionFromName(inAppMessage, InAppConstants.ACTION_DISMISS);
        Button notNow = getActionButton(action, BlueshiftConstants.BTN_(1));
        if (notNow != null) {
            LinearLayout.LayoutParams lpNotNow;
            if (buttonLayout.getOrientation() == LinearLayout.VERTICAL) {
                lpNotNow = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
            } else {
                lpNotNow = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            }
            Rect marginsNotNow = InAppUtils.getActionMargin(inAppMessage, InAppConstants.ACTION_DISMISS);
            lpNotNow.setMargins(
                    dp2px(marginsNotNow.left),
                    dp2px(marginsNotNow.top),
                    dp2px(marginsNotNow.right),
                    dp2px(marginsNotNow.bottom)
            );
            buttonLayout.addView(notNow, lpNotNow);
        }

        return rootView;
    }

    private void submitRating() {
        // send starts as events
        logRatingToBlueshift();

        // open store
        openMarketApp();

        handleClick(getInAppMessage(), getClickStatsJSONObject(InAppConstants.ACTION_RATE_APP));
    }

    private void logRatingToBlueshift() {
        // TODO: 2019-08-06 decide the event name
    }

    private void openMarketApp() {
        String pkgName = getContext().getPackageName();
        try {
            Uri marketUri = Uri.parse("market://details?id=" + pkgName);
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            getContext().startActivity(marketIntent);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);

            try {
                Uri marketWebUri = Uri.parse("https://play.google.com/store/apps/details?id=" + pkgName);
                Intent marketWebIntent = new Intent(Intent.ACTION_VIEW, marketWebUri);
                getContext().startActivity(marketWebIntent);
            } catch (Exception ex) {
                BlueshiftLogger.e(TAG, ex);
            }
        }
    }
}
