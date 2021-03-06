package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;
import com.blueshift.util.InAppUtils;

public class InAppMessageViewModal extends InAppMessageView {
    private static final String TAG = InAppMessageViewModal.class.getSimpleName();

    public InAppMessageViewModal(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        boolean heightAvailable = InAppUtils.isHeightSet(
                getContext(), inAppMessage);

        String url = InAppUtils.getTemplateStyleString(
                getContext(), inAppMessage, InAppConstants.BACKGROUND_IMAGE);
        boolean backgroundImageAvailable = url != null;

        // Decide if we should fill the parent to push the buttons to the
        // bottom of the modal based on the availability of bg image or height
        boolean fillParent = heightAvailable || backgroundImageAvailable;

        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);

        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = fillParent
                ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(width, height);
        rootView.setLayoutParams(lp2);

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
                        int width = bannerImageView.getMeasuredWidth();
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
        LinearLayout.LayoutParams lpMsgView = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // message view/placeholder should fill the modal.
        if (fillParent) {
            lpMsgView.height = 0;
            lpMsgView.weight = 1;
        }

        TextView messageTextView = getContentTextView(inAppMessage, InAppConstants.MESSAGE);
        if (messageTextView != null) {
            lpMsgView.gravity = InAppUtils.getContentLayoutGravity(getContext(), inAppMessage, InAppConstants.MESSAGE);
            rootView.addView(messageTextView, lpMsgView);
        } else {
            if (fillParent) {
                rootView.addView(new TextView(getContext()), lpMsgView);
            }
        }

        // action
        LinearLayout actionsLayout = getActionButtons(inAppMessage);
        if (actionsLayout != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Rect margins = InAppUtils.getContentMargin(getContext(), inAppMessage, InAppConstants.ACTIONS);
            if (margins != null) {
                lp.setMargins(
                        dp2px(margins.left),
                        dp2px(margins.top),
                        dp2px(margins.right),
                        dp2px(margins.bottom)
                );
            }
            rootView.addView(actionsLayout, lp);
        }

        return rootView;
    }
}
