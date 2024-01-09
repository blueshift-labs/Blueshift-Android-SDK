package com.blueshift.rich_push;

import android.content.Context;

/**
 * @author Rahul Raveendran V P
 *         Created on 25/2/15 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public final class RichPushConstants {
    public static final int BIG_IMAGE_WIDTH = 300;
    public static final int BIG_IMAGE_HEIGHT = 150;

    public static final String DEFAULT_CHANNEL_ID = "bsft_channel_General";
    public static final String DEFAULT_CHANNEL_NAME = "General";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_CAROUSEL_INDEX = "carousel_index";
    public static final String EXTRA_CAROUSEL_ELEMENT = "carousel_element";
    public static final String EXTRA_DEEP_LINK_URL = "deep_link_url";

    /**
     * Actions for the push categories handled by SDK.
     */
    private static final String sActionOpenApp = "ACTION_OPEN_APP";

    public static String ACTION_OPEN_APP(Context context) {
        return context.getPackageName() + "." + sActionOpenApp;
    }

    public static String buildAction(Context context, String action) {
        return ACTION_OPEN_APP(context);
    }

    /**
     * Action sent to host app for handling the push message at the user end.
     */
    public static String ACTION_PUSH_RECEIVED(Context context) {
        return context.getPackageName() + ".ACTION_PUSH_RECEIVED";
    }
}
