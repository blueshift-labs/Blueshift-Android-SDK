package com.blueshift.rich_push;

import android.content.Context;

/**
 * Created by rahul on 25/2/15.
 */
public final class RichPushConstants {
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    /**
     * Actions for the push categories handled by SDK.
     */
    public static String ACTION_VIEW(Context context) { return context.getPackageName() + ".ACTION_VIEW"; }
    public static String ACTION_BUY(Context context) { return context.getPackageName() + ".ACTION_BUY"; }
    public static String ACTION_OPEN_CART(Context context) { return context.getPackageName() + ".ACTION_OPEN_CART"; }
    public static String ACTION_OPEN_OFFER_PAGE(Context context) { return context.getPackageName() + ".ACTION_OPEN_OFFER_PAGE"; }
    public static String ACTION_OPEN_APP(Context context) { return context.getPackageName() + ".ACTION_OPEN_APP"; }

    /**
     * Action sent to host app for handling the push message at the user end.
     */
    public static String ACTION_PUSH_RECEIVED(Context context) { return context.getPackageName() + ".ACTION_PUSH_RECEIVED"; }
}
